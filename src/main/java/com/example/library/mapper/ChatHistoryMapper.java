package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.ChatHistory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI聊天历史 Mapper 接口
 */
@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    /**
     * 查询某个会话的最近N条记录
     */
    @Select("SELECT * FROM chat_history " +
            "WHERE user_id = #{userId} AND session_id = #{sessionId} " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<ChatHistory> selectRecentBySession(@Param("userId") Long userId,
                                            @Param("sessionId") String sessionId,
                                            @Param("limit") int limit);

    /**
     * 清理过期会话（保留最近7天）
     */
    @Delete("DELETE FROM chat_history WHERE create_time < DATE_SUB(NOW(), INTERVAL 7 DAY)")
    void deleteExpired();
}
