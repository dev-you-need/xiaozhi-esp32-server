package xiaozhi.modules.knowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xiaozhi.modules.knowledge.rag.KnowledgeBaseAdapterFactory;

/**
 * Конфигурационный класс базы знаний
 * Настройка бинов, связанных с базой знаний
 */
@Configuration
public class KnowledgeBaseConfig {

    /**
     * Предоставить экземпляр бина KnowledgeBaseAdapterFactory
     * @return экземпляр KnowledgeBaseAdapterFactory
     */
    @Bean
    public KnowledgeBaseAdapterFactory knowledgeBaseAdapterFactory() {
        return new KnowledgeBaseAdapterFactory();
    }
}