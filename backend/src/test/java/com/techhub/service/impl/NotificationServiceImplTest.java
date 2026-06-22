package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.dto.notification.NotificationVO;
import com.techhub.entity.Notification;
import com.techhub.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

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
    private static final String SOURCE_TYPE = "COMMENT";
    private static final Long PARENT_ID = 50L;
    private static final String CONTENT = "回复了你的帖子";

    @Test
    @DisplayName("create() should build and insert Notification with correct fields including sourceType and parentId")
    void testCreate() {
        notificationService.create(USER_ID, TYPE, SOURCE_ID, SOURCE_TYPE, PARENT_ID, CONTENT);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper, times(1)).insert(captor.capture());

        Notification n = captor.getValue();
        assertEquals(USER_ID, n.getUserId());
        assertEquals(TYPE, n.getType());
        assertEquals(SOURCE_ID, n.getSourceId());
        assertEquals(SOURCE_TYPE, n.getSourceType());
        assertEquals(PARENT_ID, n.getParentId());
        assertEquals(CONTENT, n.getContent());
        assertEquals(0, n.getIsRead());
        assertNotNull(n.getCreateTime());
    }

    @Nested
    @DisplayName("toVO mapping")
    class ToVOMapping {

        @Test
        @DisplayName("should map stored sourceType and parentId to VO (uppercase)")
        void shouldMapSourceTypeAndParentId() {
            Notification n = buildNotification("LIKE", "POST", null);
            Page<Notification> mpPage = new Page<>(1, 10);
            mpPage.setRecords(List.of(n));
            mpPage.setTotal(1);
            when(notificationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);

            var result = notificationService.listNotifications(1, 10, USER_ID);
            assertEquals(1, result.getTotal());
            NotificationVO vo = result.getRecords().get(0);
            assertEquals("POST", vo.getSourceType());
            assertNull(vo.getParentId());
        }

        @Test
        @DisplayName("should return parentId for COMMENT notifications")
        void shouldReturnParentIdForCommentNotifications() {
            Notification n = buildNotification("LIKE", "COMMENT", PARENT_ID);
            Page<Notification> mpPage = new Page<>(1, 10);
            mpPage.setRecords(List.of(n));
            mpPage.setTotal(1);
            when(notificationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);

            var result = notificationService.listNotifications(1, 10, USER_ID);
            NotificationVO vo = result.getRecords().get(0);
            assertEquals("COMMENT", vo.getSourceType());
            assertEquals(PARENT_ID.toString(), vo.getParentId());
        }

        @Test
        @DisplayName("should default to UNKNOWN when sourceType is null in DB")
        void shouldDefaultToUnknownWhenSourceTypeIsNull() {
            Notification n = buildNotification("LIKE", null, null);
            Page<Notification> mpPage = new Page<>(1, 10);
            mpPage.setRecords(List.of(n));
            mpPage.setTotal(1);
            when(notificationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);

            var result = notificationService.listNotifications(1, 10, USER_ID);
            NotificationVO vo = result.getRecords().get(0);
            assertEquals("UNKNOWN", vo.getSourceType());
        }
    }

    private static Notification buildNotification(String type, String sourceType, Long parentId) {
        Notification n = new Notification();
        n.setId(1L);
        n.setUserId(USER_ID);
        n.setType(type);
        n.setSourceId(SOURCE_ID);
        n.setSourceType(sourceType);
        n.setParentId(parentId);
        n.setContent(CONTENT);
        n.setIsRead(0);
        n.setCreateTime(LocalDateTime.now());
        return n;
    }
}
