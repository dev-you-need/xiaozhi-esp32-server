package xiaozhi.modules.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;


/**
 * Объект, возвращаемый интерфейсом распознавания голосового отпечатка
 */

@Data
public class IdentifyVoicePrintResponse {
    
/**
     * ID наиболее подходящего голосового отпечатка
     */

    @JsonProperty("speaker_id")
    private String speakerId;
    
/**
     * Оценка голосового отпечатка
     */

    private Double score;
}
