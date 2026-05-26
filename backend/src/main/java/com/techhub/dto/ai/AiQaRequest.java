package com.techhub.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiQaRequest {
    @NotBlank(message = "问题不能为空")
    private String question;
}
