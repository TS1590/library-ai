package com.example.library.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理器
 * 统一捕获 Controller 层异常，返回友好的错误页面
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("系统异常: {}", e.getMessage(), e);

        model.addAttribute("errorTitle", "系统异常");
        model.addAttribute("errorMessage",
                e.getMessage() != null ? e.getMessage() : "未知错误");

        // 数据库连接异常
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("Communications link failure") || msg.contains("Connection refused")) {
            model.addAttribute("errorTitle", "数据库连接失败");
            model.addAttribute("errorMessage", "无法连接到 MySQL 数据库，请检查：\n"
                    + "1. MySQL 服务是否已启动\n"
                    + "2. application.yml 中的数据库密码是否正确\n"
                    + "3. 数据库 library_ai 是否已创建（执行 schema.sql）");
        }

        // AI 调用异常
        if (msg.contains("DeepSeek") || msg.contains("Connection timed out")) {
            model.addAttribute("errorTitle", "AI 服务异常");
            model.addAttribute("errorMessage", "AI 服务暂时不可用，请稍后重试。\n"
                    + "您可以在 application.yml 中设置 ai.enabled=false 暂时关闭 AI 功能。");
        }

        return "error";
    }

    /**
     * 处理 404
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound(NoHandlerFoundException e, Model model) {
        model.addAttribute("errorTitle", "页面不存在 (404)");
        model.addAttribute("errorMessage", "您访问的页面不存在："
                + (e.getRequestURL() != null ? e.getRequestURL() : ""));
        return "error";
    }
}
