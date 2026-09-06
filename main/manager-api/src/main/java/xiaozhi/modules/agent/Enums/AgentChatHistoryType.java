package xiaozhi.modules.agent.Enums;


import lombok.Getter;


/**
 * Тип записи чата агента
 */

@Getter
public enum AgentChatHistoryType {

    USER((byte) 1),
    AGENT((byte) 2);

    private final byte value;

    AgentChatHistoryType(byte i) {
        this.value = i;
    }

}
