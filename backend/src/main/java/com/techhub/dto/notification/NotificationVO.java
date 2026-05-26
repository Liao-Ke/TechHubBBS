package com.techhub.dto.notification;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private String id;
    private String type;
    private String sourceId;
    private String content;
    private Boolean isRead;
    private LocalDateTime createTime;
}
