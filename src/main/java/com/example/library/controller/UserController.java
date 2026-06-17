package com.example.library.controller;

import com.example.library.entity.Borrow;
import com.example.library.entity.User;
import com.example.library.service.BorrowService;
import com.example.library.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 用户中心控制器
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BorrowService borrowService;

    /**
     * 用户中心首页
     */
    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        // 借阅记录
        List<Borrow> borrows = borrowService.getUserBorrows(loginUser.getId());
        model.addAttribute("borrows", borrows);

        return "user/profile";
    }

    /**
     * 修改个人信息
     */
    @PostMapping("/updateProfile")
    public String updateProfile(@RequestParam String realName,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String email,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        loginUser.setRealName(realName);
        loginUser.setPhone(phone);
        loginUser.setEmail(email);
        userService.update(loginUser);

        // 更新session中的用户信息
        session.setAttribute("loginUser", loginUser);
        redirectAttributes.addFlashAttribute("success", "个人信息更新成功");
        return "redirect:/user/profile";
    }

    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "新密码长度至少6位");
            return "redirect:/user/profile";
        }

        boolean success = userService.changePassword(loginUser.getId(), oldPassword, newPassword);
        if (success) {
            redirectAttributes.addFlashAttribute("success", "密码修改成功，请重新登录");
            return "redirect:/logout";
        } else {
            redirectAttributes.addFlashAttribute("error", "原密码错误");
            return "redirect:/user/profile";
        }
    }
}
