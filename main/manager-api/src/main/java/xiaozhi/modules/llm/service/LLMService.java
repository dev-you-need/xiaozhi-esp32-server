package xiaozhi.modules.llm.service;

/**
 * Интерфейс службы LLM
 * Поддержка вызова различных больших моделей
 */
public interface LLMService {

    /**
     * Генерация сводки истории чата
     * 
     * @param conversation   Содержимое диалога
     * @param promptTemplate Шаблон подсказки
     * @return Результат сводки
     */
    String generateSummary(String conversation, String promptTemplate);

    /**
     * Генерация сводки истории чата (с использованием подсказки по умолчанию)
     * 
     * @param conversation Содержимое диалога
     * @return Результат сводки
     */
    String generateSummary(String conversation);

    /**
     * Генерация сводки истории чата (с указанным ID модели)
     * 
     * @param conversation Содержимое диалога
     * @param modelId      ID модели
     * @return Результат сводки
     */
    String generateSummaryWithModel(String conversation, String modelId);

    /**
     * Генерация сводки истории чата (с указанным ID модели и шаблоном подсказки)
     * 
     * @param conversation   Содержимое диалога
     * @param promptTemplate Шаблон подсказки
     * @param modelId        ID модели
     * @return Результат сводки
     */
    String generateSummary(String conversation, String promptTemplate, String modelId);

    /**
     * Генерация сводки истории чата (с объединением исторической памяти)
     * 
     * @param conversation   Содержимое диалога
     * @param historyMemory  Историческая память
     * @param promptTemplate Шаблон подсказки
     * @param modelId        ID модели
     * @return Результат сводки
     */
    String generateSummaryWithHistory(String conversation, String historyMemory, String promptTemplate, String modelId);

    /**
     * Проверка доступности службы
     * 
     * @return Доступно ли
     */
    boolean isAvailable();

    /**
     * Проверка доступности службы для указанной модели
     * 
     * @param modelId ID модели
     * @return Доступно ли
     */
    boolean isAvailable(String modelId);

    /**
     * Генерация заголовка сессии
     * 
     * @param conversation Содержимое диалога
     * @param modelId      ID модели
     * @return Заголовок (около 15 символов)
     */
    String generateTitle(String conversation, String modelId);
}