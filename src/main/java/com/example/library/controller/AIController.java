package com.example.library.controller;

import com.example.library.entity.AiRecommendation;
import com.example.library.entity.User;
import com.example.library.service.ai.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * AI智能体控制器
 * 提供聊天、推荐、图书分析等AI功能接口
 */
@Controller
@RequestMapping("/ai")
public class AIController {

    private static final Logger log = LoggerFactory.getLogger(AIController.class);

    /** AI功能开关 */
    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private ChatSessionManager sessionManager;

    @Autowired
    private BookRecommendService recommendService;

    @Autowired
    private BookAnalysisService analysisService;

    @Autowired
    private LibraryToolService libraryToolService;

    /** 最多 function calling 循环次数（防止死循环） */
    private static final int MAX_TOOL_ROUNDS = 3;

    /** 图书馆助手系统提示词 */
    private static final String LIBRARY_ASSISTANT_PROMPT =
            "你是校园图书馆的智能助手「小书虫」。你可以直接操作图书馆系统！\n\n" +
            "你可以使用以下工具帮读者：\n" +
            "- search_books: 搜索图书\n" +
            "- borrow_book: 帮读者借书（读者说「借」或「帮我借」时主动调用）\n" +
            "- return_book: 帮读者还书\n" +
            "- get_my_borrows: 查看读者当前借阅\n" +
            "- get_book_info: 查看图书详情\n" +
            "- get_recommendations: 获取推荐\n\n" +
            "重要规则：\n" +
            "- 读者说'帮我借《XXX》'时，立刻调用 borrow_book\n" +
            "- 读者说'帮我还《XXX》'时，立刻调用 return_book\n" +
            "- 读者问'我借了哪些书'时，调用 get_my_borrows\n" +
            "- 读者搜索图书时，调用 search_books\n" +
            "- 不要凭空捏造借阅结果，必须调用工具获取真实数据\n" +
            "- 使用中文，回答简洁友好\n" +
            "- 借阅规则：每人最多借5本，借期30天";

    /**
     * AI推荐页面
     */
    @GetMapping("/recommend")
    public String recommendPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) return "redirect:/login";

        if (!aiEnabled) {
            model.addAttribute("aiDisabled", true);
            return "ai/recommend";
        }

        try {
            List<AiRecommendation> recommendations =
                    recommendService.getUserRecommendations(user.getId(), 8);
            model.addAttribute("recommendations", recommendations);
        } catch (Exception e) {
            log.error("获取推荐失败: {}", e.getMessage());
            model.addAttribute("recommendError", "AI推荐服务暂时不可用");
        }

        return "ai/recommend";
    }

    /**
     * 聊天接口 — 支持 Function Calling，AI 可直接操作图书馆系统
     */
    @PostMapping("/chat")
    @ResponseBody
    public Map<String, Object> chat(@RequestParam String message,
                                     @RequestParam(required = false) String sessionId,
                                     HttpSession httpSession) {
        Map<String, Object> result = new HashMap<>();

        User user = (User) httpSession.getAttribute("loginUser");
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录后再使用AI助手");
            return result;
        }

        if (!aiEnabled) {
            result.put("success", false);
            result.put("message", "AI服务暂未开启");
            return result;
        }

        try {
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = sessionManager.getOrCreateSession(user.getId());
            }

            // 构建消息列表
            List<Map<String, Object>> messages = new ArrayList<>();

            // 历史对话
            List<Map<String, String>> history = sessionManager.getHistory(sessionId);
            for (Map<String, String> h : history) {
                Map<String, Object> m = new HashMap<>();
                m.put("role", h.get("role"));
                m.put("content", h.get("content"));
                messages.add(m);
            }

            // 添加当前用户消息
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);
            sessionManager.addMessage(sessionId, "user", message);

            // 获取工具定义
            List<Map<String, Object>> tools = libraryToolService.getToolDefinitions();

            // Function Calling 循环（最多 MAX_TOOL_ROUNDS 轮）
            String finalReply = "";
            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                JsonNode response = deepSeekClient.callWithTools(
                        LIBRARY_ASSISTANT_PROMPT, tools, messages);

                if (response == null) {
                    finalReply = "抱歉，AI服务暂时不可用";
                    break;
                }

                JsonNode choice = response.path("choices").get(0);
                JsonNode msg = choice.path("message");

                // 检查是否有 tool_calls
                JsonNode toolCalls = msg.path("tool_calls");
                if (toolCalls.size() > 0) {
                    // 将AI的工具调用消息加入对话
                    Map<String, Object> assistantMsg = new HashMap<>();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", msg.path("content").asText(""));
                    List<Map<String, Object>> tcList = new ArrayList<>();
                    for (JsonNode tc : toolCalls) {
                        Map<String, Object> tcMap = new HashMap<>();
                        tcMap.put("id", tc.path("id").asText());
                        tcMap.put("type", "function");
                        Map<String, Object> func = new HashMap<>();
                        func.put("name", tc.path("function").path("name").asText());
                        String argsStr = tc.path("function").path("arguments").asText();
                        try {
                            func.put("arguments", argsStr);
                        } catch (Exception e) {
                            func.put("arguments", "{}");
                        }
                        tcMap.put("function", func);
                        tcList.add(tcMap);
                    }
                    assistantMsg.put("tool_calls", tcList);
                    messages.add(assistantMsg);

                    // 执行每个工具调用
                    for (JsonNode tc : toolCalls) {
                        String toolName = tc.path("function").path("name").asText();
                        String argsStr = tc.path("function").path("arguments").asText();
                        Map<String, Object> args = parseArgs(argsStr);

                        log.info("AI调用工具: {} 参数: {}", toolName, args);
                        String toolResult = libraryToolService.executeTool(toolName, args, user.getId());

                        // 工具结果消息
                        Map<String, Object> toolMsg = new HashMap<>();
                        toolMsg.put("role", "tool");
                        toolMsg.put("tool_call_id", tc.path("id").asText());
                        toolMsg.put("content", toolResult);
                        messages.add(toolMsg);

                        // 如果用户要求借书/还书，工具结果直接作为回复
                        if ("borrow_book".equals(toolName) || "return_book".equals(toolName)) {
                            finalReply = toolResult;
                        }
                    }

                    // 如果已经有了最终结果（借书/还书），停止循环
                    if (!finalReply.isEmpty()) break;

                } else {
                    // 纯文本回复，结束循环
                    finalReply = msg.path("content").asText("");
                    break;
                }
            }

            if (finalReply.isEmpty()) {
                finalReply = "操作已执行，请查看结果。";
            }

            sessionManager.addMessage(sessionId, "assistant", finalReply);

            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("reply", finalReply);

        } catch (Exception e) {
            log.error("AI聊天异常: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "对话异常：" + e.getMessage());
        }

        return result;
    }

    /** 简易 JSON 参数解析 */
    private Map<String, Object> parseArgs(String json) {
        Map<String, Object> args = new HashMap<>();
        try {
            if (json != null && !json.isEmpty()) {
                com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
                java.util.Iterator<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> iter = node.fields();
                while (iter.hasNext()) {
                    java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = iter.next();
                    args.put(entry.getKey(), entry.getValue().asText());
                }
            }
        } catch (Exception e) {
            // ignore parse errors
        }
        return args;
    }

    /**
     * 清除会话（开始新对话）
     */
    @PostMapping("/chat/clear")
    @ResponseBody
    public Map<String, Object> clearChat(@RequestParam String sessionId) {
        sessionManager.clearSession(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "会话已清除");
        return result;
    }

    /**
     * AI图书分析接口
     */
    @GetMapping("/analyze-book/{bookId}")
    @ResponseBody
    public Map<String, Object> analyzeBook(@PathVariable Long bookId) {
        Map<String, Object> result = new HashMap<>();

        if (!aiEnabled) {
            result.put("success", false);
            result.put("message", "AI服务暂未开启");
            return result;
        }

        try {
            BookAnalysisService.BookAnalysis analysis = analysisService.analyzeBook(bookId);
            if (analysis != null) {
                result.put("success", true);
                result.put("summary", analysis.getSummary());
                result.put("tags", analysis.getTags());
                result.put("targetReaders", analysis.getTargetReaders());
                result.put("difficulty", analysis.getDifficulty());
            } else {
                result.put("success", false);
                result.put("message", "图书不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "分析失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 检查AI服务状态
     */
    @GetMapping("/status")
    @ResponseBody
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", aiEnabled);
        return result;
    }

}
