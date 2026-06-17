package com.example.library.service.ai;

import com.example.library.entity.Book;
import com.example.library.mapper.BookMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI图书分析服务
 * 生成AI图书简介、关键词标签、读者分析等
 */
@Service
public class BookAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(BookAnalysisService.class);

    /** 缓存AI分析结果，避免重复调用API */
    private final ConcurrentHashMap<Long, BookAnalysis> cache = new ConcurrentHashMap<>();

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private BookMapper bookMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 分析图书并返回结果
     */
    public BookAnalysis analyzeBook(Long bookId) {
        // 检查缓存
        if (cache.containsKey(bookId)) {
            return cache.get(bookId);
        }

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return null;
        }

        String systemPrompt = buildAnalysisSystemPrompt();
        String userMessage = String.format(
                "请分析以下图书：\n" +
                        "书名：《%s》\n作者：%s\n出版社：%s\n原简介：%s\n",
                book.getTitle(),
                book.getAuthor() != null ? book.getAuthor() : "未知",
                book.getPublisher() != null ? book.getPublisher() : "未知",
                book.getDescription() != null ? book.getDescription() : "暂无"
        );

        String response = deepSeekClient.call(systemPrompt, userMessage);

        BookAnalysis analysis = parseAnalysis(response);
        if (analysis != null) {
            cache.put(bookId, analysis);
        }

        return analysis;
    }

    /**
     * 清除分析缓存
     */
    public void clearCache() {
        cache.clear();
    }

    private String buildAnalysisSystemPrompt() {
        return "你是一个专业的图书分析师。请根据提供的图书信息，生成分析内容。" +
                "请以JSON格式返回，不要包含其他内容。JSON格式如下：\n\n" +
                "{\n" +
                "  \"summary\": \"100字以内的精彩摘要（AI生成）\",\n" +
                "  \"tags\": [\"标签1\", \"标签2\", \"标签3\", \"标签4\", \"标签5\"],\n" +
                "  \"targetReaders\": \"适合阅读此书的读者群体描述（30字内）\",\n" +
                "  \"difficulty\": \"入门/适中/进阶/专业\"\n" +
                "}";
    }

    private BookAnalysis parseAnalysis(String response) {
        try {
            String jsonStr = response;
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');
            if (start >= 0 && end > start) {
                jsonStr = response.substring(start, end + 1);
            }

            JsonNode root = objectMapper.readTree(jsonStr);

            BookAnalysis analysis = new BookAnalysis();
            analysis.setSummary(root.path("summary").asText("暂无AI摘要"));

            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = root.path("tags");
            if (tagsNode.isArray()) {
                for (JsonNode tag : tagsNode) {
                    tags.add(tag.asText());
                }
            }
            analysis.setTags(tags);

            analysis.setTargetReaders(root.path("targetReaders").asText("广大读者"));
            analysis.setDifficulty(root.path("difficulty").asText("适中"));

            return analysis;
        } catch (Exception e) {
            log.error("解析AI分析结果失败: {}", e.getMessage());
            return defaultAnalysis();
        }
    }

    private BookAnalysis defaultAnalysis() {
        BookAnalysis analysis = new BookAnalysis();
        analysis.setSummary("本书是一本值得阅读的优秀作品。");
        analysis.setTags(Arrays.asList("经典", "推荐", "好书"));
        analysis.setTargetReaders("所有读者");
        analysis.setDifficulty("适中");
        return analysis;
    }

    /**
     * AI分析结果内部类
     */
    public static class BookAnalysis {
        private String summary;
        private List<String> tags;
        private String targetReaders;
        private String difficulty;

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public String getTargetReaders() { return targetReaders; }
        public void setTargetReaders(String targetReaders) { this.targetReaders = targetReaders; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    }
}
