package com.techhub.dto.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDetailVO extends UserProfileVO {
    private String email;
    private Integer status;
    private LocalDateTime updateTime;
}
