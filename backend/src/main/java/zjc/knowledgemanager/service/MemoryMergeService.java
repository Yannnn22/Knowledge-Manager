package zjc.knowledgemanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import zjc.knowledgemanager.config.ai.MemoryProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记忆冲突检测与存储服务 —— 双时间机制。
 *
 * <h3>双时间机制（Dual-Timestamp）</h3>
 * 每条记忆有两个时间戳：
 * <pre>
 *   created_at  — 事实首次记录的 Unix 时间戳
 *   expired_at  — 事实被新事实取代的时间戳（null = 仍有效）
 *   status      — "active"（当前有效） / "expired"（已被新事实取代）
 *   superseded_by — 取代当前记忆的新记忆 docId（用于追溯事实演变链）
 * </pre>
 *
 * <h3>写入流程</h3>
 * <ol>
 *   <li>检索语义相似的旧记忆（threshold = mergeThreshold）</li>
 *   <li>无命中 → 直接存储，status=active，expired_at=null</li>
 *   <li>有命中 → 冲突检测：
 *     <ul>
 *       <li>similarity 模式：语义重叠即视为冲突</li>
 *       <li>llm 模式：LLM 判断是否真正矛盾；不矛盾视为"补充"而非冲突</li>
 *     </ul>
 *     若冲突：旧记忆 status→expired, expired_at=now，新记忆单独存储为 active
 *     若不冲突（llm 模式且判定为补充）：新旧各自保留，旧的不失效
 *   </li>
 * </ol>
 *
 * <h3>与旧版 LLM 合并的区别</h3>
 * <pre>
 *   旧版: 旧记忆 + 新信息 → LLM 合并 → upsert 覆盖（旧内容丢失）
 *   新版: 旧记忆 → status=expired（保留），新信息 → 单独存储（独立向量）
 *   好处: 事实演变链可追溯，旧事实不丢失，语义冲突可回溯
 * </pre>
 *
 * <h3>检索端配合</h3>
 * {@link zjc.knowledgemanager.advisor.LongTermMemoryRetrievalAdvisor}
 * 检索时过滤 {@code status == "active"}，只注入当前有效的记忆。
 */
@Service
@ConditionalOnBean(name = "memoryVectorStore")
public class MemoryMergeService {

    private static final Logger log = LoggerFactory.getLogger(MemoryMergeService.class);

    private final MemoryStoreService memoryStoreService;
    private final ChatClient conflictClient;
    private final MemoryProperties memoryProperties;

    /** LLM 冲突判断 prompt */
    private static final String CONFLICT_CHECK_PROMPT = """
            判断以下两条信息是否构成事实矛盾（不可同时为真）。
            仅回复 YES 或 NO，不要解释。

            旧信息: %s
            新信息: %s

            矛盾示例: "用户住在北京" vs "用户住在上海" → YES
            非矛盾示例: "用户会 Python" vs "用户会 Java" → NO（可以同时掌握）
            非矛盾示例: "项目使用 Spring Boot" vs "项目使用 Spring Boot 3.5" → NO（补充细化）

            回答:""";

    public MemoryMergeService(MemoryStoreService memoryStoreService,
                              ChatClient.Builder chatClientBuilder,
                              MemoryProperties memoryProperties) {
        this.memoryStoreService = memoryStoreService;
        this.conflictClient = chatClientBuilder.build();
        this.memoryProperties = memoryProperties;
    }

    /**
     * 存储一条记忆，自动处理冲突检测和双时间失效。
     *
     * @param content       待存储的记忆文本
     * @param metadata      元数据（会自动填充 status/created_at/expired_at）
     * @param mergeThreshold 语义相似度阈值（超过此值触发冲突检测）
     */
    public void storeWithMerge(String content, Map<String, Object> metadata,
                               double mergeThreshold) {
        long now = System.currentTimeMillis() / 1000;
        String mode = memoryProperties.getLongTerm().getConflictDetectionMode();

        // ① 检索语义相似的旧记忆
        List<Document> similar = memoryStoreService.searchSimilar(content, 3, mergeThreshold);

        if (similar.isEmpty()) {
            // ② 无命中 → 直接存储为 active
            metadata.put("status", "active");
            metadata.put("created_at", now);
            metadata.put("expired_at", null);
            metadata.put("version", 1);
            memoryStoreService.store(content, metadata);
            log.debug("存储新记忆[active]: type={}, content={}",
                    metadata.get("memory_type"), content);
            return;
        }

        // ③ 有命中 → 检测是否真实冲突
        boolean isConflict = true; // similarity 模式默认视为冲突
        if ("llm".equals(mode)) {
            Document best = similar.get(0);
            isConflict = checkConflictWithLLM(best.getText(), content);
        }

        if (!isConflict) {
            // llm 模式判定为"补充"而非"矛盾"：新旧各自独立保留
            // 旧记忆保持 active，新记忆也存储为 active
            metadata.put("status", "active");
            metadata.put("created_at", now);
            metadata.put("expired_at", null);
            metadata.put("version", 1);
            metadata.put("related_to", similar.get(0).getId());
            memoryStoreService.store(content, metadata);
            log.debug("LLM 判定非矛盾，新旧共存: old={}, new={}",
                    truncate(similar.get(0).getText(), 30), truncate(content, 30));
            return;
        }

        // ④ 冲突：expire 所有相似的旧记忆，存储新记忆为 active
        int expiredCount = 0;
        String newDocId = null;
        String firstSupersededId = null;

        for (Document old : similar) {
            // 只 expire 当前为 active 的旧记忆（已 expired 的跳过）
            String oldStatus = (String) old.getMetadata().get("status");
            if ("expired".equals(oldStatus)) continue;

            Map<String, Object> oldMeta = new HashMap<>(old.getMetadata());
            oldMeta.put("status", "expired");
            oldMeta.put("expired_at", now);
            // superseded_by 会在新记忆存储后回填

            memoryStoreService.upsert(old.getId(), old.getText(), oldMeta);
            expiredCount++;

            if (firstSupersededId == null) {
                firstSupersededId = old.getId();
            }

            log.debug("记忆失效[expired]: id={}, content={}",
                    old.getId(), truncate(old.getText(), 40));
        }

        // 存储新记忆
        metadata.put("status", "active");
        metadata.put("created_at", now);
        metadata.put("expired_at", null);
        // version = 旧版本号 + 1
        int oldVersion = getVersion(similar.get(0));
        metadata.put("version", oldVersion + 1);
        if (firstSupersededId != null) {
            metadata.put("supersedes", firstSupersededId);
        }

        memoryStoreService.store(content, metadata);
        log.info("双时间冲突处理: expired={}条, stored=1条[active], mode={}, content={}",
                expiredCount, mode, truncate(content, 50));
    }

    /**
     * 调用 LLM 判断新旧信息是否构成事实矛盾。
     *
     * @param oldContent 旧记忆内容
     * @param newContent 新信息内容
     * @return true = 矛盾（需要 expire 旧记忆），false = 补充（新旧共存）
     */
    private boolean checkConflictWithLLM(String oldContent, String newContent) {
        try {
            String prompt = String.format(CONFLICT_CHECK_PROMPT, oldContent, newContent);
            String answer = conflictClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            boolean isConflict = answer != null && answer.trim().toUpperCase().startsWith("YES");
            log.debug("LLM 冲突检测: old={}, new={}, result={}",
                    truncate(oldContent, 30), truncate(newContent, 30),
                    isConflict ? "CONFLICT" : "COEXIST");
            return isConflict;

        } catch (Exception e) {
            // LLM 调用失败 → 安全侧：默认视为冲突，expire 旧记忆
            log.warn("LLM 冲突检测失败，默认视为冲突: {}", e.getMessage());
            return true;
        }
    }

    private int getVersion(Document doc) {
        Object v = doc.getMetadata().get("version");
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return 1;
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
