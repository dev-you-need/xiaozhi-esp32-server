package xiaozhi.modules.agent.service.biz;

import xiaozhi.modules.agent.dto.AgentChatHistoryReportDTO;


/**
 * Бизнес-логика истории чата агента
 *
 * @author Goody
 * @version 1.0, 2025/4/30
 * @since 1.0.0
 */

public interface AgentChatHistoryBizService {

    
/**
     * Метод отчёта чата
     *
     * @param agentChatHistoryReportDTO Входной объект, содержащий информацию для отчёта чата
     *                                  Например: MAC-адрес устройства, тип файла, содержимое и т. д.
     * @return Результат загрузки: true — успех, false — неудача
     */

    Boolean report(AgentChatHistoryReportDTO agentChatHistoryReportDTO);
}
