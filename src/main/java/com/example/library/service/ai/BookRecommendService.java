package com.example.library.service.ai;

import com.example.library.entity.Book;
import com.example.library.entity.Borrow;
import com.example.library.mapper.AiRecommendationMapper;
import com.example.library.mapper.BookMapper;
import com.example.library.mapper.BorrowMapper;
import com.example.library.entity.AiRecommendation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI图书推荐服务
 * 基于用户借阅历史，调用DeepSeek生成个性化推荐
 */
@Service
public class BookRecommendService {

    private static final Logger log = LoggerFactory.getLogger(BookRecommendService.class);

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BorrowMapper borrowMapper;

    @Autowired
    private AiRecommendationMapper recommendationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 为用户生成个性化推荐
     *
     * @param userId 用户ID
     * @param limit  推荐数量
     * @return 推荐图书列表
     */
    public List<AiRecommendation> generateRecommendations(Long userId, int limit) {
        // 1. 清除旧的推荐记录
        recommendationMapper.deleteOldRecommendations(userId);

        // 2. 获取用户借阅历史（最近5条）
        List<Borrow> history = borrowMapper.selectByUserWithBook(userId);
        List<Borrow> recentHistory = history.stream()
                .limit(5)
                .collect(Collectors.toList());

        // 3. 获取全部在架图书
        List<Book> allBooks = bookMapper.selectBookListWithCategory();

        if (allBooks.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 构建Prompt
        String userHistoryStr = buildHistoryString(recentHistory);
        String bookListStr = buildBookListString(allBooks, 20);

        String systemPrompt = buildRecommendSystemPrompt();
        String userMessage = String.format(
                "## 用户的借阅历史\n%s\n\n## 图书馆所有在架图书（前20本）\n%s\n\n" +
                        "请根据用户的借阅历史分析阅读偏好，从图书列表中推荐 %d 本用户可能喜欢的图书。" +
                        "请以JSON格式返回，格式为: [{\"bookId\": 图书ID, \"reason\": \"推荐理由(30字内)\", \"score\": 0.0-1.0}]",
                userHistoryStr.isEmpty() ? "新用户，暂无借阅记录" : userHistoryStr,
                bookListStr,
                limit
        );

        // 5. 调用DeepSeek
        String response = deepSeekClient.call(systemPrompt, userMessage);

        // 6. 解析推荐结果
        List<AiRecommendation> recommendations = parseRecommendations(response, userId, allBooks);

        // 7. 保存推荐记录
        for (AiRecommendation rec : recommendations) {
            recommendationMapper.insert(rec);
        }

        // 8. 查询完整推荐（含图书信息）
        return recommendationMapper.selectByUserWithBook(userId, limit);
    }

    /**
     * 获取用户已有的推荐记录
     */
    public List<AiRecommendation> getUserRecommendations(Long userId, int limit) {
        List<AiRecommendation> recs = recommendationMapper.selectByUserWithBook(userId, limit);

        // 如果没有推荐记录，生成新的
        if (recs.isEmpty()) {
            recs = generateRecommendations(userId, limit);
        }

        return recs;
    }

    /**
     * 构建借阅历史字符串
     */
    private String buildHistoryString(List<Borrow> history) {
        if (history.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Borrow b : history) {
            if (b.getBookTitle() != null) {
                sb.append(String.format("- 《%s》 by %s\n",
                        b.getBookTitle(),
                        b.getBookAuthor() != null ? b.getBookAuthor() : "未知"));
            }
        }
        return sb.toString();
    }

    /**
     * 构建图书列表字符串
     */
    private String buildBookListString(List<Book> books, int maxCount) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Book b : books) {
            if (count >= maxCount) break;
            sb.append(String.format("- ID:%d 《%s》 作者:%s 分类:%s\n",
                    b.getId(),
                    b.getTitle(),
                    b.getAuthor() != null ? b.getAuthor() : "未知",
                    b.getCategoryName() != null ? b.getCategoryName() : "未分类"));
            count++;
        }
        return sb.toString();
    }

    /**
     * 推荐系统提示词
     */
    private String buildRecommendSystemPrompt() {
        return "你是一个专业的图书推荐系统。你的任务是根据用户的借阅历史，分析其阅读偏好，" +
                "然后从图书列表中推荐用户可能感兴趣的图书。\n\n" +
                "推荐策略：\n" +
                "1. 优先推荐与用户历史借阅分类相同或相关的图书\n" +
                "2. 考虑图书的经典程度和受欢迎程度\n" +
                "3. 为新用户推荐各类别的经典热门图书\n" +
                "4. 推荐理由要简洁有说服力（30字以内）\n\n" +
                "请只返回JSON格式结果，不要包含其他文字。";
    }

    /**
     * 解析AI返回的推荐结果
     */
    private List<AiRecommendation> parseRecommendations(String response, Long userId, List<Book> allBooks) {
        List<AiRecommendation> results = new ArrayList<>();

        try {
            // 提取JSON部分（AI可能返回markdown包裹的JSON）
            String jsonStr = response;
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start >= 0 && end > start) {
                jsonStr = response.substring(start, end + 1);
            }

            JsonNode array = objectMapper.readTree(jsonStr);
            for (JsonNode item : array) {
                Long bookId = item.path("bookId").asLong();
                String reason = item.path("reason").asText("根据您的阅读偏好推荐");
                double score = item.path("score").asDouble(0.5);

                // 验证图书ID是否存在
                boolean exists = allBooks.stream().anyMatch(b -> b.getId().equals(bookId));
                if (exists) {
                    AiRecommendation rec = new AiRecommendation();
                    rec.setUserId(userId);
                    rec.setBookId(bookId);
                    rec.setReason(reason);
                    rec.setScore(score);
                    rec.setIsRead(0);
                    results.add(rec);
                }
            }
        } catch (Exception e) {
            log.error("解析AI推荐结果失败: {}", e.getMessage());
        }

        // 如果AI推荐不足，用热门图书补充
        if (results.size() < 3) {
            List<Book> popular = bookMapper.selectPopularBooks(5);
            Set<Long> existingBookIds = results.stream()
                    .map(AiRecommendation::getBookId)
                    .collect(Collectors.toSet());

            for (Book book : popular) {
                if (!existingBookIds.contains(book.getId())) {
                    AiRecommendation rec = new AiRecommendation();
                    rec.setUserId(userId);
                    rec.setBookId(book.getId());
                    rec.setReason("热门推荐：《" + book.getTitle() + "》");
                    rec.setScore(0.8);
                    rec.setIsRead(0);
                    results.add(rec);
                }
                if (results.size() >= 5) break;
            }
        }

        return results;
    }
}
