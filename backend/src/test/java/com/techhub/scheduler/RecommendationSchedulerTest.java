package com.techhub.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.entity.Post;
import com.techhub.mapper.PostMapper;
import com.techhub.service.PostKeywordService;
import com.techhub.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationScheduler")
class RecommendationSchedulerTest {

    @Mock
    private UserProfileService userProfileService;
    @Mock
    private PostKeywordService postKeywordService;
    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private RecommendationScheduler recommendationScheduler;

    @Nested
    @DisplayName("refreshRecommendationModels")
    class RefreshModelsTests {

        @Test
        @DisplayName("更新所有帖子关键词与所有用户画像")
        void updatesAllPostsAndProfiles() {
            Post p1 = new Post();
            p1.setId(1L);
            p1.setTitle("Test 1");
            p1.setContent("Content 1");
            p1.setDeleted(0);

            Post p2 = new Post();
            p2.setId(2L);
            p2.setTitle("Test 2");
            p2.setContent("Content 2");
            p2.setDeleted(0);

            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(p1, p2));
            doNothing().when(postKeywordService)
                    .updatePostKeywords(anyLong(), anyString(), anyString());
            doNothing().when(userProfileService).buildAllProfiles();

            assertDoesNotThrow(() -> recommendationScheduler.refreshRecommendationModels());

            verify(postKeywordService).updatePostKeywords(eq(1L), eq("Test 1"), eq("Content 1"));
            verify(postKeywordService).updatePostKeywords(eq(2L), eq("Test 2"), eq("Content 2"));
            verify(userProfileService).buildAllProfiles();
        }

        @Test
        @DisplayName("无帖子时只更新画像")
        void onlyUpdatesProfilesWhenNoPosts() {
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());
            doNothing().when(userProfileService).buildAllProfiles();

            assertDoesNotThrow(() -> recommendationScheduler.refreshRecommendationModels());

            verify(userProfileService).buildAllProfiles();
            verify(postKeywordService, never()).updatePostKeywords(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("单个帖子关键词更新失败不影响整体流程")
        void handlesSinglePostKeywordFailure() {
            Post p1 = new Post();
            p1.setId(1L);
            p1.setTitle("OK");
            p1.setContent("OK");
            p1.setDeleted(0);

            Post p2 = new Post();
            p2.setId(2L);
            p2.setTitle("Bad");
            p2.setContent("Bad");
            p2.setDeleted(0);

            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(p1, p2));
            doNothing().when(postKeywordService)
                    .updatePostKeywords(eq(1L), anyString(), anyString());
            doThrow(new RuntimeException("DB error"))
                    .when(postKeywordService)
                    .updatePostKeywords(eq(2L), anyString(), anyString());
            doNothing().when(userProfileService).buildAllProfiles();

            // 不应中断整体流程
            assertDoesNotThrow(() -> recommendationScheduler.refreshRecommendationModels());

            verify(userProfileService).buildAllProfiles();
        }
    }
}
