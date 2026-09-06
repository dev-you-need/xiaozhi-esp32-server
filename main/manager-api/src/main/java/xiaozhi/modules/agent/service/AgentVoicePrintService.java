package xiaozhi.modules.agent.service;

import java.util.List;

import xiaozhi.modules.agent.dto.AgentVoicePrintSaveDTO;
import xiaozhi.modules.agent.dto.AgentVoicePrintUpdateDTO;
import xiaozhi.modules.agent.vo.AgentVoicePrintVO;


/**
 * Сервис обработки голосового отпечатка агента
 *
 * @author zjy
 */

public interface AgentVoicePrintService {
    
/**
     * Добавить новый голосовой отпечаток агента
     *
     * @ param dto Сохранить данные голосового агента
     * @ return T-успех, F-неудача
     */

    boolean insert(AgentVoicePrintSaveDTO dto);

    
/**
     * Удалить указанный голосовой отпечаток агента
     *
     * @param userId       ID текущего пользователя
     * @param voicePrintId ID голосового отпечатка
     * @return Успешность: T — успех, F — неудача
     */

    boolean delete(Long userId, String voicePrintId);

    
/**
     * Получить все данные голосовых отпечатков указанного агента
     *
     * @param userId  ID текущего пользователя
     * @param agentId ID агента
     * @return Коллекция данных голосовых отпечатков
     */

    List<AgentVoicePrintVO> list(Long userId, String agentId);

    
/**
     * Обновить данные указанного голосового отпечатка агента
     *
     * @param userId ID текущего пользователя
     * @param dto    Данные изменяемого голосового отпечатка
     * @return Успешность: T — успех, F — неудача
     */

    boolean update(Long userId, AgentVoicePrintUpdateDTO dto);

}
