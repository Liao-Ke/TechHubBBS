package com.techhub.util;

import com.techhub.mapper.PostKeywordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordExtractor")
class KeywordExtractorTest {

    @Mock
    private PostKeywordMapper postKeywordMapper;

    @InjectMocks
    private KeywordExtractor keywordExtractor;

    @BeforeEach
    void setUp() {
        lenient().when(postKeywordMapper.countTotalPosts()).thenReturn(100L);
        lenient().when(postKeywordMapper.countPostsByKeyword(anyString())).thenReturn(10L);
    }

    @Nested
    @DisplayName("extractKeywords")
    class ExtractKeywordsTests {

        @Test
        @DisplayName("从中文文本中提取关键词")
        void extractsChineseKeywords() {
            String text = "Spring Boot 是一个优秀的 Java 后端开发框架，用于构建微服务架构";
            List<String> keywords = keywordExtractor.extractKeywords(text, 5);

            assertNotNull(keywords);
            assertFalse(keywords.isEmpty(), "应提取到至少一个关键词");
        }

        @Test
        @DisplayName("过滤停用词")
        void filtersStopwords() {
            // 确保此测试会用到 mapper（调用 extractKeywords 才会触发 computeIdf）
            String text = "的 了 在 是 我 这是 Java Spring 微服务";
            List<String> keywords = keywordExtractor.extractKeywords(text, 10);

            boolean hasStopword = keywords.stream()
                    .anyMatch(k -> k.equals("的") || k.equals("了") || k.equals("在"));
            assertFalse(hasStopword, "停用词应被过滤");
        }

        @Test
        @DisplayName("过滤短词（<2字符）")
        void filtersShortWords() {
            String text = "A B C Java Spring 微服务 的";

            List<String> keywords = keywordExtractor.extractKeywords(text, 10);

            boolean hasSingleChar = keywords.stream().anyMatch(k -> k.length() < 2);
            assertFalse(hasSingleChar, "应过滤掉长度 <2 的词");
        }

        @Test
        @DisplayName("空文本返回空列表")
        void returnsEmptyForBlankText() {
            // 空文本直接返回空列表，不使用 mapper
            assertTrue(keywordExtractor.extractKeywords("", 10).isEmpty());
            assertTrue(keywordExtractor.extractKeywords(null, 10).isEmpty());
            assertTrue(keywordExtractor.extractKeywords("   ", 10).isEmpty());
        }

        @Test
        @DisplayName("返回数量不超过 topN")
        void respectsTopNLimit() {
            String text = "机器学习 深度学习 神经网络 自然语言 处理 计算机 视觉 人工智能";

            List<String> keywords = keywordExtractor.extractKeywords(text, 3);
            assertTrue(keywords.size() <= 3, "返回数量应不超过 topN，实际=" + keywords.size());
        }
    }

    @Nested
    @DisplayName("extractAndSave")
    class ExtractAndSaveTests {

        @Test
        @DisplayName("null postId 直接返回不操作")
        void skipsNullPostId() {
            assertDoesNotThrow(() -> keywordExtractor.extractAndSave(null, "title", "content"));
        }

        @Test
        @DisplayName("空白文本直接返回不操作")
        void skipsBlankText() {
            assertDoesNotThrow(() -> keywordExtractor.extractAndSave(1L, "", ""));
        }
    }
}
