package xiaozhi.modules.sys.dto;

import lombok.Data;
import xiaozhi.modules.sys.enums.ServerActionEnum;

import java.util.Map;

/**
 * DTO действия сервера
 */
@Data
public class ServerActionPayloadDTO
{
    /**
    * Тип (всё, что отправляется с панели управления на сервер, является server)
    */
    private String type;
    /**
    * Действие
    */
    private ServerActionEnum action;
    /**
    * Содержимое
    */
    private Map<String, Object> content;

    public static ServerActionPayloadDTO build(ServerActionEnum action, Map<String, Object> content) {
        ServerActionPayloadDTO serverActionPayloadDTO = new ServerActionPayloadDTO();
        serverActionPayloadDTO.setAction(action);
        serverActionPayloadDTO.setContent(content);
        serverActionPayloadDTO.setType("server");
        return serverActionPayloadDTO;
    }
    // Приватный конструктор
    private ServerActionPayloadDTO() {}
}
