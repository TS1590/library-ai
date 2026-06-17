package com.example.library.controller;

import com.example.library.entity.User;
import com.example.library.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

/**
 * 登录注册控制器
 */
@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * 注册页面
     */
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    /**
     * 登录处理
     */
    @PostMapping("/doLogin")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        User user = userService.login(username, password);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "用户名或密码错误，或账号已被禁用");
            return "redirect:/login";
        }

        session.setAttribute("loginUser", user);

        // 根据角色跳转
        if (user.isAdmin()) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/";
    }

    /**
     * 注册处理
     */
    @PostMapping("/doRegister")
    public String doRegister(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String realName,
                             @RequestParam(required = false) String phone,
                             @RequestParam(required = false) String email,
                             RedirectAttributes redirectAttributes) {
        // 简单校验
        if (username.length() < 3 || username.length() > 20) {
            redirectAttributes.addFlashAttribute("error", "用户名长度3-20个字符");
            return "redirect:/register";
        }
        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "密码长度至少6位");
            return "redirect:/register";
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);

        boolean success = userService.register(user);
        if (!success) {
            redirectAttributes.addFlashAttribute("error", "注册失败，用户名可能已存在");
            return "redirect:/register";
        }

        redirectAttributes.addFlashAttribute("success", "注册成功，请登录");
        return "redirect:/login";
    }

    /**
     * 登出
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
