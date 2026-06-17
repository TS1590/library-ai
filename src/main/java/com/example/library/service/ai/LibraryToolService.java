package com.example.library.service.ai;

import com.example.library.entity.Book;
import com.example.library.entity.Borrow;
import com.example.library.mapper.BookMapper;
import com.example.library.mapper.BorrowMapper;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 图书馆工具服务 — AI 可调用的实际操作函数
 * 支持 Function Calling 模式
 */
@Service
public class LibraryToolService {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BorrowMapper borrowMapper;

    @Autowired
    private BookService bookService;

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookRecommendService recommendService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 构建 Map 的辅助方法 */
    private Map<String, Object> map(Object... kvs) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) m.put((String) kvs[i], kvs[i + 1]);
        return m;
    }
    private List<String> list(String... items) { return java.util.Arrays.asList(items); }

    /**
     * 获取所有可用工具定义（传给 AI）
     */
    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // 搜索图书
        tools.add(buildTool("search_books", "搜索图书馆里的图书",
                map("type", "object", "properties",
                    map("keyword", map("type", "string", "description", "搜索关键词，可以是书名或作者"))),
                list("keyword")));

        // 借阅图书
        tools.add(buildTool("borrow_book", "帮用户借阅一本图书",
                map("type", "object", "properties",
                    map("book_title", map("type", "string", "description", "要借阅的图书书名"))),
                list("book_title")));

        // 归还图书
        tools.add(buildTool("return_book", "帮用户归还一本已借的图书",
                map("type", "object", "properties",
                    map("book_title", map("type", "string", "description", "要归还的图书书名"))),
                list("book_title")));

        // 查看借阅记录
        tools.add(buildTool("get_my_borrows", "查看用户当前借阅的所有图书", null, null));

        // 查看图书详情
        tools.add(buildTool("get_book_info", "查看某本图书的详细信息",
                map("type", "object", "properties",
                    map("book_title", map("type", "string", "description", "图书书名"))),
                list("book_title")));

        // 获取推荐
        tools.add(buildTool("get_recommendations", "获取AI推荐的热门图书", null, null));

        return tools;
    }

    /**
     * 执行工具调用
     */
    public String executeTool(String toolName, Map<String, Object> arguments, Long userId) {
        try {
            switch (toolName) {
                case "search_books": {
                    String keyword = (String) arguments.get("keyword");
                    List<Book> books = bookMapper.searchBooks(keyword);
                    if (books.isEmpty()) return "未找到与「" + keyword + "」相关的图书。";
                    StringBuilder sb = new StringBuilder("搜索到 " + books.size() + " 本相关图书：\n");
                    for (int i = 0; i < Math.min(books.size(), 5); i++) {
                        Book b = books.get(i);
                        sb.append(String.format("%d. 《%s》- %s [可借:%d] ID:%d\n",
                                i + 1, b.getTitle(), b.getAuthor(),
                                b.getAvailableCopies(), b.getId()));
                    }
                    return sb.toString();
                }

                case "borrow_book": {
                    String bookTitle = (String) arguments.get("book_title");
                    List<Book> books = bookMapper.searchBooks(bookTitle);
                    if (books.isEmpty()) return "未找到《" + bookTitle + "》，请确认书名是否正确。";

                    Book target = null;
                    for (Book b : books) {
                        if (b.getTitle().contains(bookTitle) || bookTitle.contains(b.getTitle())) {
                            target = b;
                            break;
                        }
                    }
                    if (target == null) target = books.get(0);

                    String result = borrowService.borrowBook(userId, target.getId());
                    return result;
                }

                case "return_book": {
                    String bookTitle = (String) arguments.get("book_title");
                    List<Borrow> borrows = borrowMapper.selectByUserWithBook(userId);
                    Borrow target = null;
                    for (Borrow b : borrows) {
                        if (b.getReturnTime() == null && b.getBookTitle() != null
                                && b.getBookTitle().contains(bookTitle)) {
                            target = b;
                            break;
                        }
                    }
                    if (target == null)
                        return "您当前没有借阅《" + bookTitle + "》，请确认书名。";

                    return borrowService.returnBook(target.getId(), userId);
                }

                case "get_my_borrows": {
                    List<Borrow> borrows = borrowMapper.selectByUserWithBook(userId);
                    List<Borrow> active = new ArrayList<>();
                    for (Borrow b : borrows) {
                        if (b.getReturnTime() == null) active.add(b);
                    }
                    if (active.isEmpty()) return "您当前没有在借的图书。";
                    StringBuilder sb = new StringBuilder("您当前借阅了 " + active.size() + " 本书：\n");
                    for (Borrow b : active) {
                        sb.append(String.format("· 《%s》借于 %s，应还 %s\n",
                                b.getBookTitle(),
                                b.getBorrowTime().toLocalDate(),
                                b.getDueTime().toLocalDate()));
                    }
                    return sb.toString();
                }

                case "get_book_info": {
                    String bookTitle = (String) arguments.get("book_title");
                    List<Book> books = bookMapper.searchBooks(bookTitle);
                    if (books.isEmpty()) return "未找到《" + bookTitle + "》";
                    Book b = books.get(0);
                    return String.format("《%s》作者：%s 出版社：%s 分类：%s 可借：%d/%d 简介：%s",
                            b.getTitle(), b.getAuthor(), b.getPublisher(),
                            b.getCategoryName(), b.getAvailableCopies(),
                            b.getTotalCopies(),
                            b.getDescription() != null ? b.getDescription() : "暂无");
                }

                case "get_recommendations": {
                    List<com.example.library.entity.AiRecommendation> recs =
                            recommendService.generateRecommendations(userId, 5);
                    if (recs.isEmpty()) return "暂无推荐，多借几本书后AI就能为您推荐了。";
                    StringBuilder sb = new StringBuilder("为您推荐以下图书：\n");
                    for (int i = 0; i < recs.size(); i++) {
                        com.example.library.entity.AiRecommendation r = recs.get(i);
                        sb.append(String.format("%d. 《%s》- %s\n",
                                i + 1, r.getBookTitle(), r.getReason()));
                    }
                    return sb.toString();
                }

                default:
                    return "未知操作: " + toolName;
            }
        } catch (Exception e) {
            return "操作失败: " + e.getMessage();
        }
    }

    private Map<String, Object> buildTool(String name, String description,
                                           Map<String, Object> parameters,
                                           List<String> required) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");

        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);
        if (parameters != null) {
            Map<String, Object> params = new HashMap<>(parameters);
            if (required != null) params.put("required", required);
            function.put("parameters", params);
        }
        tool.put("function", function);
        return tool;
    }
}
