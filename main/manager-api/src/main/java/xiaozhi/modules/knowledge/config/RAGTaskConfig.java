package xiaozhi.modules.knowledge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Конфигурация запланированных задач модуля базы знаний
 * Включить возможность планировщика Spring
 */
@Configuration
@EnableScheduling
public class RAGTaskConfig {
}
