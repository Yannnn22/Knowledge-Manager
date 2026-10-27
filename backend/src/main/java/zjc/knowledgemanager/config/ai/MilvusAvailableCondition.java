package zjc.knowledgemanager.config.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 通过 TCP Socket 快速检测 Milvus 服务是否可达。
 *
 * <p>在 Spring 创建 Milvus 相关 Bean 之前执行，避免因 Milvus 不可用导致
 * MilvusServiceClient 构造函数阻塞 ~10 秒后抛出 DEADLINE_EXCEEDED。
 *
 * <p>检测超时 2 秒，远短于 Milvus SDK 默认的 10 秒超时。
 */
public class MilvusAvailableCondition implements Condition {

    private static final Logger log = LoggerFactory.getLogger(MilvusAvailableCondition.class);

    private static final int PROBE_TIMEOUT_MS = 2000;

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String host = env.getProperty("spring.ai.vectorstore.milvus.client.host", "localhost");
        int port = Integer.parseInt(env.getProperty("spring.ai.vectorstore.milvus.client.port", "19530"));

        log.info("检测 Milvus 服务可达性: {}:{} (超时 {}ms)", host, port, PROBE_TIMEOUT_MS);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), PROBE_TIMEOUT_MS);
            log.info("Milvus 服务可达，启用向量存储相关功能");
            return true;
        } catch (Exception e) {
            log.warn("Milvus 服务不可达 ({}:{})，向量存储/长期记忆/RAG/文件上传功能将不可用: {}",
                    host, port, e.getMessage());
            return false;
        }
    }
}
