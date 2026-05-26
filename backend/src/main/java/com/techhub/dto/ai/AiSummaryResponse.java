package com.techhub.dto.ai;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiSummaryResponse {
    private String id;
    private String content;
    private Integer status;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
