package com.techhub.dto.post;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostUpdateRequest {
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    private String content;

    private Integer visibility;  // PATCH semantics: all fields optional
}
