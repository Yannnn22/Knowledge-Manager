package zjc.knowledgemanager.config.ai;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * 长期记忆向量存储配置
 * 创建独立的 memoryVectorStore Bean，指向 long_term_memory 集合，
 * 与文档向量存储 vector_store_v5 隔离，避免检索噪声。
 *
 * <p>仅在 Milvus 可用时创建。Milvus 不可用时本配置及所有依赖 bean 自动跳过，
 * 应用仍可正常启动（但记忆/RAG 功能不可用）。
 */
@Configuration
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
@Conditional(MilvusAvailableCondition.class)
public class LongTermMemoryConfig {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryConfig.class);

    private final MemoryProperties memoryProperties;

    public LongTermMemoryConfig(MemoryProperties memoryProperties) {
        this.memoryProperties = memoryProperties;
    }

    @Bean("memoryVectorStore")
    public VectorStore memoryVectorStore(MilvusServiceClient milvusServiceClient,
                                         EmbeddingModel embeddingModel) {
        MemoryProperties.LongTerm config = memoryProperties.getLongTerm();
        log.info("初始化长期记忆向量存储: collection={}", config.getCollectionName());
        return MilvusVectorStore.builder(milvusServiceClient, embeddingModel)
                .collectionName(config.getCollectionName())
                .databaseName("default")
                .embeddingDimension(1024)
                .indexType(IndexType.IVF_FLAT)
                .metricType(MetricType.COSINE)
                .initializeSchema(true)
                .build();
    }
}
