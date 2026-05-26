package com.techhub.service.impl;

import com.techhub.entity.PostKeyword;
import com.techhub.entity.PostSimilarity;
import com.techhub.mapper.PostKeywordMapper;
import com.techhub.mapper.PostSimilarityMapper;
import com.techhub.service.PostKeywordService;
import com.techhub.util.KeywordExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 帖子关键词服务实现 — 关键词提取 + 余弦相似度计算 + 相似度矩阵维护。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostKeywordServiceImpl implements PostKeywordService {

    private final KeywordExtractor keywordExtractor;
    private final PostKeywordMapper postKeywordMapper;
    private final PostSimilarityMapper postSimilarityMapper;

    @Override
    @Async("recommendationExecutor")
    @Transactional
    public void updatePostKeywords(Long postId, String title, String content) {
        log.info("PostKeywordService: 开始更新帖子 {} 的关键词与相似度", postId);

        // 1. 提取并保存关键词
        keywordExtractor.extractAndSave(postId, title, content);

        // 2. 获取当前帖子的关键词向量
        Map<String, Double> currentVector = getPostKeywordVector(postId);
        if (currentVector.isEmpty()) {
            log.info("PostKeywordService: 帖子 {} 无关键词，跳过相似度计算", postId);
            return;
        }

        // 3. 获取所有其他帖子的关键词向量
        List<PostKeyword> allKeywords = postKeywordMapper.selectAllKeywords();
        Map<Long, Map<String, Double>> allVectors = groupKeywordsByPost(allKeywords);

        // 4. 删除该帖子的旧相似度记录
        postSimilarityMapper.deleteByPostId(postId);

        // 5. 计算与每篇其他帖子的余弦相似度
        for (Map.Entry<Long, Map<String, Double>> entry : allVectors.entrySet()) {
            Long otherPostId = entry.getKey();
            if (otherPostId.equals(postId)) {
                continue;
            }
            double similarity = computeCosineSimilarity(currentVector, entry.getValue());
            if (similarity > 0) {
                upsertSimilarity(postId, otherPostId, similarity);
            }
        }

        log.info("PostKeywordService: 帖子 {} 相似度更新完成", postId);
    }

    /**
     * 获取单个帖子的关键词权重向量。
     */
    private Map<String, Double> getPostKeywordVector(Long postId) {
        List<PostKeyword> keywords = postKeywordMapper.selectByPostId(postId);
        Map<String, Double> vector = new HashMap<>();
        for (PostKeyword pk : keywords) {
            vector.put(pk.getKeyword(), pk.getTfidfWeight());
        }
        return vector;
    }

    /**
     * 将所有帖子的关键词按 postId 分组为向量 Map。
     */
    private Map<Long, Map<String, Double>> groupKeywordsByPost(List<PostKeyword> allKeywords) {
        Map<Long, Map<String, Double>> result = new HashMap<>();
        for (PostKeyword pk : allKeywords) {
            result.computeIfAbsent(pk.getPostId(), k -> new HashMap<>())
                    .put(pk.getKeyword(), pk.getTfidfWeight());
        }
        return result;
    }

    /**
     * 计算两个关键词向量的余弦相似度。
     */
    private double computeCosineSimilarity(Map<String, Double> vecA, Map<String, Double> vecB) {
        if (vecA.isEmpty() || vecB.isEmpty()) {
            return 0.0;
        }

        // 点积
        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : vecA.entrySet()) {
            Double weightB = vecB.get(entry.getKey());
            if (weightB != null) {
                dotProduct += entry.getValue() * weightB;
            }
        }

        // 模长
        double magnitudeA = 0.0;
        for (double v : vecA.values()) {
            magnitudeA += v * v;
        }
        magnitudeA = Math.sqrt(magnitudeA);

        double magnitudeB = 0.0;
        for (double v : vecB.values()) {
            magnitudeB += v * v;
        }
        magnitudeB = Math.sqrt(magnitudeB);

        if (magnitudeA == 0.0 || magnitudeB == 0.0) {
            return 0.0;
        }

        return dotProduct / (magnitudeA * magnitudeB);
    }

    /**
     * Upsert 相似度记录（保持 postIdA < postIdB）。
     */
    private void upsertSimilarity(Long postIdA, Long postIdB, double score) {
        if (postIdA > postIdB) {
            Long temp = postIdA;
            postIdA = postIdB;
            postIdB = temp;
        }

        PostSimilarity existing = postSimilarityMapper.selectByPostIds(postIdA, postIdB);
        if (existing != null) {
            existing.setSimilarityScore(score);
            postSimilarityMapper.updateById(existing);
        } else {
            PostSimilarity ps = new PostSimilarity();
            ps.setPostIdA(postIdA);
            ps.setPostIdB(postIdB);
            ps.setSimilarityScore(score);
            postSimilarityMapper.insert(ps);
        }
    }
}
