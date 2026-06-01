package com.techhub.service.impl;

import com.techhub.entity.Notification;
import com.techhub.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl")
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private static final Long USER_ID = 1L;
    private static final String TYPE = "REPLY";
    private static final Long SOURCE_ID = 100L;
    private static final String CONTENT = "回复了你的帖子";

    @Test
    @DisplayName("create() should build and insert Notification with correct fields")
    void testCreate() {
        notificationService.create(USER_ID, TYPE, SOURCE_ID, CONTENT);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper, times(1)).insert(captor.capture());

        Notification n = captor.getValue();
        assertEquals(USER_ID, n.getUserId());
        assertEquals(TYPE, n.getType());
        assertEquals(SOURCE_ID, n.getSourceId());
        assertEquals(CONTENT, n.getContent());
        assertEquals(0, n.getIsRead());
        assertNotNull(n.getCreateTime());
    }
}
