package com.example.library.service.ai;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话会话管理器
 * 使用内存存储实现多轮对话记忆
 */
@Component
public class ChatSessionManager {

    /** 每个会话保留的最大消息轮数 */
    private static final int MAX_ROUNDS = 10;

    /** 会话过期时间（30分钟） */
    private static final long SESSION_EXPIRE_MS = 30 * 60 * 1000;

    /** 内存存储: sessionId -> 消息列表 */
    private final ConcurrentHashMap<String, SessionData> sessions = new ConcurrentHashMap<>();

    /**
     * 获取或创建会话
     */
    public String getOrCreateSession(Long userId) {
        // 查找用户最近的活跃会话
        String existingSession = findActiveSession(userId);
        if (existingSession != null) {
            return existingSession;
        }
        // 创建新会话
        String newSessionId = userId + "_" + UUID.randomUUID().toString().substring(0, 8);
        sessions.put(newSessionId, new SessionData(userId));
        return newSessionId;
    }

    /**
     * 获取会话的历史消息（用于发送给AI）
     */
    public List<Map<String, String>> getHistory(String sessionId) {
        SessionData session = sessions.get(sessionId);
        if (session == null) {
            return new ArrayList<>();
        }
        session.lastAccessTime = System.currentTimeMillis();
        return new ArrayList<>(session.messages);
    }

    /**
     * 添加一条消息到会话
     */
    public void addMessage(String sessionId, String role, String content) {
        SessionData session = sessions.get(sessionId);
        if (session == null) return;

        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        session.messages.add(msg);

        // 超过最大轮数时删除最早的消息
        int maxMessages = MAX_ROUNDS * 2; // 每轮 = 1 user + 1 assistant
        while (session.messages.size() > maxMessages) {
            session.messages.remove(0);
        }

        session.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 清除会话
     */
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * 清理过期会话
     */
    public void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry ->
                now - entry.getValue().lastAccessTime > SESSION_EXPIRE_MS);
    }

    /**
     * 查找用户的活跃会话
     */
    private String findActiveSession(Long userId) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, SessionData> entry : sessions.entrySet()) {
            SessionData data = entry.getValue();
            if (data.userId.equals(userId) &&
                    now - data.lastAccessTime < SESSION_EXPIRE_MS) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 会话数据内部类
     */
    private static class SessionData {
        Long userId;
        List<Map<String, String>> messages = new ArrayList<>();
        long lastAccessTime;

        SessionData(Long userId) {
            this.userId = userId;
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}
