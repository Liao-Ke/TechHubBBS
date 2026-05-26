package com.techhub.dto.ai;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiQaResponse {
    private String id;
    private String question;
    private String answer;
    private LocalDateTime createTime;
}
