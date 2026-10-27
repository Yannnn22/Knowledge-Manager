
package zjc.knowledgemanager;

import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = MilvusVectorStoreAutoConfiguration.class)
public class KnowledgeManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeManagerApplication.class, args);
    }

}
