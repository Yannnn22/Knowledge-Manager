package zjc.knowledgemanager.config.ai;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Milvus 向量存储配置 —— 替代默认的 MilvusVectorStoreAutoConfiguration。
 *
 * <p>通过 {@link MilvusAvailableCondition} 在 Bean 创建前快速检测 Milvus 可达性。
 * Milvus 不可用时，整个配置类被跳过，所有消费者通过 ObjectProvider 优雅降级。
 */
@Configuration
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
@Conditional(MilvusAvailableCondition.class)
public class MilvusConfig {

    private static final Logger log = LoggerFactory.getLogger(MilvusConfig.class);

    // ---- milvusClient 连接参数 ----
    @Value("${spring.ai.vectorstore.milvus.client.host:localhost}")
    private String host;

    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    private int port;

    @Value("${spring.ai.vectorstore.milvus.client.username:root}")
    private String username;

    @Value("${spring.ai.vectorstore.milvus.client.password:milvus}")
    private String password;

    // ---- vectorStore 参数 ----
    @Value("${spring.ai.vectorstore.milvus.collectionName:vector_store_v5}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.milvus.databaseName:default}")
    private String databaseName;

    @Value("${spring.ai.vectorstore.milvus.embeddingDimension:1024}")
    private int embeddingDimension;

    @Value("${spring.ai.vectorstore.milvus.indexType:IVF_FLAT}")
    private String indexType;

    @Value("${spring.ai.vectorstore.milvus.metricType:COSINE}")
    private String metricType;

    @Value("${spring.ai.vectorstore.milvus.initialize-schema:true}")
    private boolean initializeSchema;

    @Bean
    public MilvusServiceClient milvusClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withAuthorization(username, password)
                .withConnectTimeout(5, TimeUnit.SECONDS)
                .build();
        log.info("创建 MilvusServiceClient: {}:{}", host, port);
        return new MilvusServiceClient(connectParam);
    }

    @Bean
    public VectorStore vectorStore(MilvusServiceClient milvusServiceClient,
                                   EmbeddingModel embeddingModel) {
        log.info("初始化默认向量存储: collection={}, database={}, dim={}",
                collectionName, databaseName, embeddingDimension);
        return MilvusVectorStore.builder(milvusServiceClient, embeddingModel)
                .collectionName(collectionName)
                .databaseName(databaseName)
                .embeddingDimension(embeddingDimension)
                .indexType(IndexType.valueOf(indexType))
                .metricType(MetricType.valueOf(metricType))
                .initializeSchema(initializeSchema)
                .build();
    }
}
