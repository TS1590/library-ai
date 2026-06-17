package com.example.library.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.library.entity.Book;
import com.example.library.entity.Borrow;
import com.example.library.entity.User;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.service.CategoryService;
import com.example.library.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.library.entity.Category;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 管理员控制器
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 管理后台首页
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // 统计数据
            model.addAttribute("totalBooks", bookService.countTotal());
            model.addAttribute("totalReaders", userService.countReaders());
            model.addAttribute("borrowingCount",
                    borrowService.listAllBorrows().stream()
                            .filter(b -> b.getReturnTime() == null)
                            .count());

            // 最近借阅记录
            List<Borrow> recentBorrows = borrowService.listAllBorrows();
            if (recentBorrows.size() > 10) {
                recentBorrows = recentBorrows.subList(0, 10);
            }
            model.addAttribute("recentBorrows", recentBorrows);
        } catch (Exception e) {
            // 数据库可能未初始化，使用默认值避免500错误
            model.addAttribute("totalBooks", 0L);
            model.addAttribute("totalReaders", 0);
            model.addAttribute("borrowingCount", 0L);
            model.addAttribute("recentBorrows", java.util.Collections.emptyList());
            model.addAttribute("dbError", "数据库查询异常: " + e.getMessage() + "。请检查MySQL是否运行且已执行schema.sql。");
        }

        return "admin/dashboard";
    }

    /**
     * 图书管理（分页）
     */
    @GetMapping("/books")
    public String books(@RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        IPage<Book> bookPage = bookService.listByPage(page, size);
        model.addAttribute("page", bookPage);
        return "admin/books";
    }

    /**
     * 用户管理（分页）
     */
    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        IPage<User> userPage = userService.listReadersByPage(page, size);
        model.addAttribute("page", userPage);
        return "admin/users";
    }

    /**
     * 禁用/启用用户
     */
    @GetMapping("/user/toggle/{id}")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("success", "用户状态已更新");
        return "redirect:/admin/users";
    }

    /**
     * 借阅管理
     */
    @GetMapping("/borrows")
    public String borrows(Model model) {
        // 更新逾期状态
        borrowService.updateOverdueStatus();
        List<Borrow> allBorrows = borrowService.listAllBorrows();
        model.addAttribute("borrows", allBorrows);
        return "admin/borrows";
    }

    /**
     * 分类管理
     */
    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.listAll());
        return "admin/categories";
    }

    /**
     * 添加分类
     */
    @PostMapping("/category/add")
    public String addCategory(@RequestParam String name,
                              @RequestParam(required = false) String description,
                              RedirectAttributes redirectAttributes) {
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "分类名称不能为空");
            return "redirect:/admin/categories";
        }
        Category category = new Category();
        category.setName(name.trim());
        category.setDescription(description != null ? description.trim() : null);
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("success", "分类「" + name + "」添加成功");
        return "redirect:/admin/categories";
    }

    /**
     * 编辑分类
     */
    @PostMapping("/category/edit/{id}")
    public String editCategory(@PathVariable Long id,
                               @RequestParam String name,
                               @RequestParam(required = false) String description,
                               RedirectAttributes redirectAttributes) {
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "分类名称不能为空");
            return "redirect:/admin/categories";
        }
        Category category = categoryService.getById(id);
        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "分类不存在");
            return "redirect:/admin/categories";
        }
        category.setName(name.trim());
        category.setDescription(description != null ? description.trim() : null);
        categoryService.update(category);
        redirectAttributes.addFlashAttribute("success", "分类「" + name + "」更新成功");
        return "redirect:/admin/categories";
    }

    /**
     * 删除分类
     */
    @GetMapping("/category/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (categoryService.delete(id)) {
            redirectAttributes.addFlashAttribute("success", "分类已删除");
        } else {
            redirectAttributes.addFlashAttribute("error", "删除失败");
        }
        return "redirect:/admin/categories";
    }
}
