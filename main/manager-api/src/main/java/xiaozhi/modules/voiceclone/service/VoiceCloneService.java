package xiaozhi.modules.voiceclone.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.voiceclone.dto.VoiceCloneDTO;
import xiaozhi.modules.voiceclone.dto.VoiceCloneResponseDTO;
import xiaozhi.modules.voiceclone.entity.VoiceCloneEntity;

/**
 * Управление клонированием голоса
 */
public interface VoiceCloneService extends BaseService<VoiceCloneEntity> {

    /**
     * Пагинационный запрос
     */
    PageData<VoiceCloneEntity> page(Map<String, Object> params);

    /**
     * Сохранение клонирования голоса
     */
    void save(VoiceCloneDTO dto);

    /**
     * Пакетное удаление
     */
    void delete(String[] ids);

    /**
     * Запрос списка клонирования голоса по ID пользователя
     * 
     * @param userId ID пользователя
     * @return Список клонирования голоса
     */
    List<VoiceCloneEntity> getByUserId(Long userId);

    /**
     * Пагинационный запрос списка клонирования голоса с именами модели и пользователя
     */
    PageData<VoiceCloneResponseDTO> pageWithNames(Map<String, Object> params);

    /**
     * Запрос информации о клонировании голоса с именами модели и пользователя по ID
     */
    VoiceCloneResponseDTO getByIdWithNames(String id);

    /**
     * Запрос списка клонирования голоса с именем модели по ID пользователя
     */
    List<VoiceCloneResponseDTO> getByUserIdWithNames(Long userId);

    /**
     * Загрузка аудиофайла
     */
    void uploadVoice(String id, MultipartFile voiceFile) throws Exception;

    /**
     * Обновление имени клонирования голоса
     */
    void updateName(String id, String name);

    /**
     * Получение аудиоданных
     */
    byte[] getVoiceData(String id);

    /**
     * Клонирование аудио, вызов движка Volcano для обучения ремиксации голоса
     * 
     * @param cloneId ID записи клонирования голоса
     */
    void cloneAudio(String cloneId);
}
