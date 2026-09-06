package xiaozhi.modules.agent.service;

import java.util.List;
import java.util.Map;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.agent.dto.AgentCreateDTO;
import xiaozhi.modules.agent.dto.AgentDTO;
import xiaozhi.modules.agent.dto.AgentMemoryDTO;
import xiaozhi.modules.agent.dto.AgentUpdateDTO;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.agent.vo.AgentInfoVO;


/**
 * АгентСервис обработки таблицы
 *
 * @author Goody
 * @version 1.0, 2025/4/30
 * @since 1.0.0
 */

public interface AgentService extends BaseService<AgentEntity> {
    
/**
     * Получить список агентов администратора
     *
     * @param params Параметры запроса
     * @return Данные постраничной навигации
     */

    PageData<AgentEntity> adminAgentList(Map<String, Object> params);

    
/**
     * Получить агента по ID
     *
     * @param id ID агента
     * @return Сущность агента
     */

    AgentInfoVO getAgentById(String id);

    
/**
     * Получить агента, доступного текущему пользователю, по ID
     *
     * @param id     ID агента
     * @ param userId текущий идентификатор пользователя     * @return Сущность агента
     */

    AgentInfoVO getAgentById(String id, Long userId);

    
/**
     * Вставить агента
     *
     * @param entity Сущность агента
     * @return Успешность выполнения
     */

    boolean insert(AgentEntity entity);

    
/**
     * Удалить агента по ID пользователя
     *
     * @param userId ID пользователя
     */

    void deleteAgentByUserId(Long userId);

    
/**
     * Удалить агента и связанные данные
     *
     * @param agentId ID агента
     */

    void deleteAgent(String agentId);

    
/**
     * Получить список агентов пользователя
     *
     * @param userId ID пользователя
     * @param keyword Ключевое слово поиска
     * @ param searchType тип поиска (name - Поиск по названиy, mac - поиск по MAC-адресу)     * @ return Список агентов     */

    List<AgentDTO> getUserAgents(Long userId, String keyword, String searchType);

    
/**
     * Получить количество устройств по ID агента
     *
     * @param agentId ID агента
     * @return Количество устройств
     */

    Integer getDeviceCountByAgentId(String agentId);

    
/**
     * Запросить информацию агента по умолчанию для устройства по MAC-адресу
     *
     * @ param macAddress устройство MAC-адрес     * @return Информация агента по умолчанию，Возвращает null, если не существует
     */

    AgentEntity getDefaultAgentByMacAddress(String macAddress);

    
/**
     * Проверить, имеет ли пользователь доступ к агенту
     *
     * @param agentId ID агента
     * @param userId  ID пользователя
     * @return Наличие доступа
     */

    boolean checkAgentPermission(String agentId, Long userId);

    
/**
     * Обновить агента
     *
     * @param agentId ID агента
     * @param dto     Обновить агентаНеобходимая информация
     */

    void updateAgentById(String agentId, AgentUpdateDTO dto);

    
/**
     * Обновить агента, доступного текущему пользователю
     *
     * @param agentId ID агента
     * @param dto     Обновить агентаНеобходимая информация
     * @ param userId текущий идентификатор пользователя     */

    void updateAgentById(String agentId, AgentUpdateDTO dto, Long userId);

    
/**
     * Обновить память агента по MAC-адресу устройства
     *
     * @ param macAddress устройство MAC-адрес     * @ param dto Память агента     * @ param userId текущий идентификатор пользователя     */

    void updateAgentMemoryByDeviceMacAddress(String macAddress, AgentMemoryDTO dto, Long userId);

    
/**
     * Удалить агента, доступного текущему пользователю
     *
     * @param agentId ID агента
     * @ param userId текущий идентификатор пользователя     */

    void deleteAgentById(String agentId, Long userId);

    
/**
     * Обновить агента
     *
     * @param agentId        ID агента
     * @param dto            Обновить агентаНеобходимая информация
     * @param createSnapshot Создавать ли снимок конфигурации
     */

    void updateAgentById(String agentId, AgentUpdateDTO dto, boolean createSnapshot);

    
/**
     * Создать агента
     *
     * @param dto Создать агентаНеобходимая информация
     * @return ID созданного агента
     */

    String createAgent(AgentCreateDTO dto);


}
