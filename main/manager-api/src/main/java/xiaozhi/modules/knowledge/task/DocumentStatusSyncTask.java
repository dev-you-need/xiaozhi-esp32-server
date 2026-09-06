package xiaozhi.modules.knowledge.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.modules.knowledge.service.KnowledgeFilesService;

/**
 * Задача синхронизации статуса документа в базе знаний
 * 
 * Должность:
 * 1. Автоматическое сканирование документов в состоянии __ STR0 __ (парсинг)
 * 2. Позвоните в интерфейс RAGFlow, чтобы получить последнюю информацию о состоянии
 * 3. Синхронное обновление базы данных при изменении статуса (запуск - > успех/сбой)
 * 4. [Critical] Компенсация за обновление статистики базы знаний (TokenCount) при успешном анализе
 */
@Component
@AllArgsConstructor
@Slf4j
public class DocumentStatusSyncTask {

    private final KnowledgeFilesService knowledgeFilesService;

    /**
     * Синхронизация каждые 30 секунд
     * Предотвратите отставание, используя fixedDelay, чтобы гарантировать, что следующее выполнение начнется через 30 секунд после последнего выполнения
     */
    @Scheduled(fixedDelay = 30000)
    public void syncRunningDocuments() {
        try {
            // log.debug("开始执行文档状态同步任务...");
            knowledgeFilesService.syncRunningDocuments();
        } catch (Exception e) {
            log.error("文档状态同步任务异常", e);
        }
    }
}
