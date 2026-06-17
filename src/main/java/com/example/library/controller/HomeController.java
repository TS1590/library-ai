package com.example.library.controller;

import com.example.library.entity.AiRecommendation;
import com.example.library.entity.Book;
import com.example.library.entity.User;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.service.ai.BookRecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 首页和基础页面控制器
 */
@Controller
public class HomeController {

    @Autowired
    private BookService bookService;

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookRecommendService recommendService;

    /**
     * 首页
     */
    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");

        // 热门图书
        List<Book> popularBooks = bookService.listPopular(6);
        model.addAttribute("popularBooks", popularBooks);

        // 最新上架
        List<Book> latestBooks = bookService.listLatest(6);
        model.addAttribute("latestBooks", latestBooks);

        // 统计信息
        model.addAttribute("totalBooks", bookService.countTotal());

        if (user != null) {
            // AI推荐
            try {
                List<AiRecommendation> recommendations =
                        recommendService.getUserRecommendations(user.getId(), 4);
                model.addAttribute("aiRecommendations", recommendations);
            } catch (Exception e) {
                // AI推荐失败不阻塞首页展示
            }

            // 当前借阅数量
            int borrowingCount = borrowService.countCurrentBorrows(user.getId());
            model.addAttribute("borrowingCount", borrowingCount);
        }

        return "index";
    }

    /**
     * 图书列表页
     */
    @GetMapping("/books")
    public String bookList(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long categoryId,
                           Model model) {
        List<Book> books;
        if (keyword != null && !keyword.trim().isEmpty()) {
            books = bookService.search(keyword.trim());
            model.addAttribute("keyword", keyword);
        } else if (categoryId != null) {
            books = bookService.listByCategory(categoryId);
        } else {
            books = bookService.listAvailable();
        }

        model.addAttribute("books", books);
        return "book/list";
    }

    /**
     * 图书详情页
     */
    @GetMapping("/book/detail")
    public String bookDetail(@RequestParam Long id, Model model, HttpSession session) {
        Book book = bookService.getById(id);
        if (book == null) {
            return "redirect:/books";
        }
        model.addAttribute("book", book);

        User user = (User) session.getAttribute("loginUser");
        if (user != null) {
            // 检查是否已借阅
            List<com.example.library.entity.Borrow> borrows = borrowService.getUserBorrows(user.getId());
            boolean hasBorrowed = borrows.stream()
                    .anyMatch(b -> b.getBookId().equals(id) && b.getReturnTime() == null);
            model.addAttribute("hasBorrowed", hasBorrowed);

            // AI推荐
            try {
                List<AiRecommendation> recommendations =
                        recommendService.getUserRecommendations(user.getId(), 3);
                model.addAttribute("aiRecommendations", recommendations);
            } catch (Exception e) {
                // ignore
            }
        }

        return "book/detail";
    }

    /**
     * 搜索
     */
    @GetMapping("/search")
    public String search(@RequestParam String keyword, Model model) {
        List<Book> books = bookService.search(keyword);
        model.addAttribute("books", books);
        model.addAttribute("keyword", keyword);
        return "book/list";
    }
}
