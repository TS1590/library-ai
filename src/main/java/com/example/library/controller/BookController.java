package com.example.library.controller;

import com.example.library.entity.Book;
import com.example.library.entity.Category;
import com.example.library.entity.User;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.beans.factory.annotation.Value;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 图书管理控制器
 */
@Controller
@RequestMapping("/book")
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BorrowService borrowService;

    /**
     * 添加图书页面（管理员）
     */
    @GetMapping("/add")
    public String addPage(Model model) {
        model.addAttribute("categories", categoryService.listAll());
        return "book/add";
    }

    /**
     * 添加图书处理
     */
    @PostMapping("/doAdd")
    public String doAdd(Book book,
                        @RequestParam(value = "coverImage", required = false) MultipartFile file,
                        RedirectAttributes redirectAttributes) {
        // 检查ISBN是否已存在
        if (book.getIsbn() != null && !book.getIsbn().isEmpty()) {
            Book existing = bookService.getByIsbn(book.getIsbn());
            if (existing != null) {
                redirectAttributes.addFlashAttribute("error", "ISBN已存在");
                return "redirect:/book/add";
            }
        }

        // 处理封面图片上传
        if (file != null && !file.isEmpty()) {
            String coverPath = saveCoverImage(file);
            if (coverPath == null) {
                redirectAttributes.addFlashAttribute("error", "封面图片上传失败：仅支持 jpg/png/gif/webp 格式");
                return "redirect:/book/add";
            }
            book.setCoverUrl(coverPath);
        }

        bookService.add(book);
        redirectAttributes.addFlashAttribute("success", "图书添加成功");
        return "redirect:/admin/books";
    }

    /**
     * 编辑图书页面（管理员）
     */
    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id, Model model) {
        Book book = bookService.getById(id);
        if (book == null) {
            return "redirect:/admin/books";
        }
        model.addAttribute("book", book);
        model.addAttribute("categories", categoryService.listAll());
        return "book/edit";
    }

    /**
     * 编辑图书处理
     */
    @PostMapping("/doEdit")
    public String doEdit(Book book,
                         @RequestParam(value = "coverImage", required = false) MultipartFile file,
                         RedirectAttributes redirectAttributes) {
        // 处理封面图片上传（如果选择了新图片）
        if (file != null && !file.isEmpty()) {
            String coverPath = saveCoverImage(file);
            if (coverPath == null) {
                redirectAttributes.addFlashAttribute("error", "封面图片上传失败：仅支持 jpg/png/gif/webp 格式");
                return "redirect:/book/edit/" + book.getId();
            }
            book.setCoverUrl(coverPath);
        } else {
            // 未选择新图片，保留原封面
            Book existing = bookService.getById(book.getId());
            if (existing != null && existing.getCoverUrl() != null) {
                book.setCoverUrl(existing.getCoverUrl());
            }
        }

        bookService.update(book);
        redirectAttributes.addFlashAttribute("success", "图书更新成功");
        return "redirect:/admin/books";
    }

    /**
     * 下架图书
     */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookService.delete(id);
        redirectAttributes.addFlashAttribute("success", "图书已下架");
        return "redirect:/admin/books";
    }

    /**
     * 借阅图书
     */
    @PostMapping("/borrow")
    @ResponseBody
    public java.util.Map<String, Object> borrow(@RequestParam Long bookId, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }

        String msg = borrowService.borrowBook(user.getId(), bookId);
        result.put("success", msg.startsWith("借阅成功"));
        result.put("message", msg);
        return result;
    }

    /**
     * 归还图书
     */
    @PostMapping("/return")
    @ResponseBody
    public java.util.Map<String, Object> returnBook(@RequestParam Long borrowId, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }

        String msg = borrowService.returnBook(borrowId, user.getId());
        result.put("success", msg.startsWith("归还成功"));
        result.put("message", msg);
        return result;
    }

    /**
     * 续借图书
     */
    @PostMapping("/renew")
    @ResponseBody
    public java.util.Map<String, Object> renew(@RequestParam Long borrowId, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }

        String msg = borrowService.renewBook(borrowId, user.getId());
        result.put("success", msg.startsWith("续借成功"));
        result.put("message", msg);
        return result;
    }

    /** 允许的图片MIME类型 */
    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    ));

    /** 封面图片保存目录（可通过 application.yml 配置） */
    @Value("${app.upload.dir:./upload/covers/}")
    private String coverDir;

    /** 封面图片访问 URL 前缀 */
    @Value("${app.upload.url-prefix:/upload/covers/}")
    private String coverUrlPrefix;

    /**
     * 保存封面图片到 upload/covers/ 目录
     * @return 访问路径，失败返回 null
     */
    private String saveCoverImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return null;
        }

        try {
            // 确保目录存在（转为绝对路径避免 Tomcat/WAR 部署时路径漂移）
            Path dir = Paths.get(coverDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            // 生成唯一文件名
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path destPath = dir.resolve(filename);
            file.transferTo(destPath.toFile());

            return coverUrlPrefix + filename;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
