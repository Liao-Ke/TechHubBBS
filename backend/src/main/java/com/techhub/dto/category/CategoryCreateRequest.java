package com.techhub.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateRequest {
    @NotBlank(message = "版块名称不能为空")
    @Size(max = 50, message = "版块名称不能超过50字")
    private String name;

    @Size(max = 255, message = "版块描述不能超过255字")
    private String description;

    private Integer sortOrder;
}
