package xiaozhi.modules.timbre.service;

import java.util.List;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.model.dto.VoiceDTO;
import xiaozhi.modules.timbre.dto.TimbreDataDTO;
import xiaozhi.modules.timbre.dto.TimbrePageDTO;
import xiaozhi.modules.timbre.entity.TimbreEntity;
import xiaozhi.modules.timbre.vo.TimbreDetailsVO;

/**
 * Определение бизнес-уровня тембра
 * 
 * @author zjy
 * @since 2025-3-21
 */
public interface TimbreService extends BaseService<TimbreEntity> {
    /**
     * Получение тембров для указанной TTS-модели с пагинацией
     * 
     * @param dto Параметры поиска с пагинацией
     * @return Пагинированные данные списка тембров
     */
    PageData<TimbreDetailsVO> page(TimbrePageDTO dto);

    /**
     * Получение подробной информации о тембре по указанному id
     * 
     * @param timbreId ID таблицы тембров
     * @return Информация о тембре
     */
    TimbreDetailsVO get(String timbreId);

    /**
     * Сохранение информации о тембре
     * 
     * @param dto Данные для сохранения
     */
    void save(TimbreDataDTO dto);

    /**
     * Сохранение информации о тембре
     * 
     * @param timbreId ID для изменения
     * @param dto      Данные для изменения
     */
    void update(String timbreId, TimbreDataDTO dto);

    /**
     * Пакетное удаление тембров
     * 
     * @param ids Список ID тембров для удаления
     */
    void delete(String[] ids);

    List<VoiceDTO> getVoiceNames(String ttsModelId, String voiceName);

    /**
     * Получение первого действующего языка конфигурации обычного тембра или клонированного тембра.
     *
     * @param id ID тембра
     * @return Язык по умолчанию; возвращает null, если тембр не существует или эффективный язык не настроен
     */
    String getDefaultLanguageById(String id);

    /**
     * Получение имени тембра по ID
     * 
     * @param id ID тембра
     * @return Имя тембра
     */
    String getTimbreNameById(String id);

    /**
     * Получение информации о тембре по коду тембра
     * 
     * @param ttsModelId ID модели тембра
     * @param voiceCode  Код тембра
     * @return Информация о тембре
     */
    VoiceDTO getByVoiceCode(String ttsModelId, String voiceCode);
}
