package com.techhub.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String avatarUrl;
    @Size(max = 500, message = "个人简介不能超过500字")
    private String bio;
}
