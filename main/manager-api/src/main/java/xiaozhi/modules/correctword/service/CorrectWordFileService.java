package xiaozhi.modules.correctword.service;

import java.util.List;
import java.util.Map;

import xiaozhi.common.page.PageData;
import xiaozhi.modules.correctword.dto.CorrectWordFileCreateDTO;
import xiaozhi.modules.correctword.vo.CorrectWordFileVO;
import xiaozhi.modules.correctword.vo.CorrectWordSimpleVO;

public interface CorrectWordFileService {

    /**
     * Создание файла замены слов
     *
     * @param dto Параметры создания
     * @return VO файла
     */
    CorrectWordFileVO createFile(CorrectWordFileCreateDTO dto);

    /**
     * Изменение файла замены слов (полная замена элементов)
     *
     * @param fileId ID файла
     * @param dto    Параметры изменения
     */
    void updateFile(String fileId, CorrectWordFileCreateDTO dto);

    /**
     * Получение списка файлов замены слов текущего пользователя
     *
     * @param params Параметры пагинации
     * @return Пагинированные данные
     */
    PageData<CorrectWordFileVO> listFiles(Map<String, Object> params);

    /**
     * Получение списка файлов замены слов текущего пользователя (без пагинации, для выпадающего выбора)
     *
     * @return Список файлов
     */
    List<CorrectWordFileVO> listAllFiles();

    /**
     * Получение исходного содержимого файла (для загрузки)
     *
     * @param fileId ID файла
     * @return Сущность файла
     */
    CorrectWordFileVO getFileContent(String fileId);

    /**
     * Удаление файла замены слов вместе со всеми элементами и связанными записями
     *
     * @param fileId ID файла
     */
    void deleteFile(String fileId);

    /**
     * Удаление записей связи файлов замены слов агента (сам файл не удаляется)
     *
     * @param agentId ID агента
     */
    void deleteMappingsByAgentId(String agentId);

    /**
     * Получение всех элементов замены слов агента (упрощенная версия для использования на устройстве)
     *
     * @param agentId ID агента
     * @return Список замен слов
     */
    List<CorrectWordSimpleVO> getAllItemsByAgentId(String agentId);

    /**
     * Получение списка ID файлов замены слов, связанных с агентом
     *
     * @param agentId ID агента
     * @return Список ID файлов
     */
    List<String> getAgentCorrectWordFileIds(String agentId);

    /**
     * Сохранение файлов замены слов, связанных с агентом (полная замена)
     *
     * @param agentId ID агента
     * @param fileIds Список ID файлов
     */
    void saveAgentCorrectWords(String agentId, List<String> fileIds);

    /**
     * Пакетное удаление файлов замены слов
     *
     * @param fileIds Список ID файлов
     */
    void batchDeleteFiles(List<String> fileIds);
}
