package com.example.library.interceptor;

import com.example.library.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器：检查用户是否已登录
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        User user = (User) request.getSession().getAttribute("loginUser");

        if (user == null) {
            // 未登录，重定向到登录页
            response.sendRedirect("/login");
            return false;
        }

        // 检查账号是否被禁用
        if (!user.isActive()) {
            request.getSession().invalidate();
            response.sendRedirect("/login?error=disabled");
            return false;
        }

        return true;
    }
}
