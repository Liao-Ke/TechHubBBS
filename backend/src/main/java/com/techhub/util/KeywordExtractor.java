package com.techhub.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.entity.PostKeyword;
import com.techhub.mapper.PostKeywordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 中文关键词提取工具 — 基于 ansj_seg 分词 + TF-IDF 加权。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordExtractor {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一",
            "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着",
            "没有", "看", "好", "自己", "这", "他", "她", "它", "们", "那", "些",
            "什么", "怎么", "如何", "为什么", "可以", "这个", "那个", "还", "被", "把",
            "让", "从", "对", "与", "或", "但", "而", "且", "因为", "所以", "如果",
            "虽然", "然后", "之后", "之前", "时候", "已经", "正在", "将", "能", "会",
            "可能", "应该", "需要", "想", "觉得", "知道", "做", "来", "去", "进行",
            "使用", "通过", "以及", "等", "等等", "各", "每", "比", "较", "最", "更",
            "只", "才", "便", "再", "又", "却", "啊", "吧", "呢", "吗", "嘛", "哦",
            "嗯", "哈", "呀", "哇", "呵", "唉", "喂", "啦", "呗", "咚"
    ));

    private final PostKeywordMapper postKeywordMapper;

    /**
     * 从文本中提取 topN 个关键词（TF-IDF 加权）。
     */
    public List<String> extractKeywords(String text, int topN) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        // 1. 分词
        List<Term> terms = ToAnalysis.parse(text).getTerms();
        if (terms.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 过滤停用词 + 过滤短词（<2 字符）+ 只保留有意义的词性
        List<String> words = terms.stream()
                .map(Term::getName)
                .filter(w -> w != null && w.length() >= 2)
                .filter(w -> !STOPWORDS.contains(w))
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (words.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 计算 TF（词频 / 总词数）
        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) {
            freq.merge(w, 1, Integer::sum);
        }
        int totalWords = words.size();

        // 4. 计算 TF-IDF
        Map<String, Double> tfidf = new HashMap<>();
        for (Map.Entry<String, Integer> entry : freq.entrySet()) {
            String keyword = entry.getKey();
            double tf = (double) entry.getValue() / totalWords;
            double idf = computeIdf(keyword);
            tfidf.put(keyword, tf * idf);
        }

        // 5. 按权重降序取 topN
        return tfidf.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 提取帖子关键词并保存到 post_keyword 表。
     */
    public void extractAndSave(Long postId, String title, String content) {
        if (postId == null) {
            return;
        }
        String text = (title != null ? title + " " : "") + (content != null ? content : "");
        if (text.isBlank()) {
            return;
        }

        // 提取关键词（含权重）
        List<Term> terms = ToAnalysis.parse(text).getTerms();
        List<String> words = terms.stream()
                .map(Term::getName)
                .filter(w -> w != null && w.length() >= 2)
                .filter(w -> !STOPWORDS.contains(w))
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (words.isEmpty()) {
            return;
        }

        // 计算 TF-IDF
        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) {
            freq.merge(w, 1, Integer::sum);
        }
        int totalWords = words.size();
        Map<String, Double> tfidf = new HashMap<>();
        for (Map.Entry<String, Integer> entry : freq.entrySet()) {
            String keyword = entry.getKey();
            double tf = (double) entry.getValue() / totalWords;
            double idf = computeIdf(keyword);
            tfidf.put(keyword, tf * idf);
        }

        // 去重：先删旧再插新
        postKeywordMapper.deleteByPostId(postId);

        for (Map.Entry<String, Double> entry : tfidf.entrySet()) {
            PostKeyword pk = new PostKeyword();
            pk.setPostId(postId);
            pk.setKeyword(entry.getKey());
            pk.setTfidfWeight(entry.getValue());
            postKeywordMapper.insert(pk);
        }
        log.debug("KeywordExtractor: 帖子 {} 提取关键词 {} 个", postId, tfidf.size());
    }

    /**
     * 计算逆文档频率 IDF = log(1 + N / df)，N=总帖数，df=含该词的帖数。
     * 首次调用时 N 可能为 0，此时使用默认 IDF 值 1.0。
     */
    private double computeIdf(String keyword) {
        try {
            long totalPosts = postKeywordMapper.countTotalPosts();
            if (totalPosts == 0) {
                return 1.0;
            }
            long df = postKeywordMapper.countPostsByKeyword(keyword);
            if (df == 0) {
                return Math.log(1 + totalPosts);
            }
            return Math.log(1 + (double) totalPosts / df);
        } catch (Exception e) {
            return 1.0;
        }
    }
}
