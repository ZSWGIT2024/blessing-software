package com.itheima.controller;


import com.itheima.pojo.Article;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.service.ArticleService;
import com.itheima.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/article")
@CrossOrigin
@RequiredArgsConstructor
public class ArticleController {


    private final ArticleService articleService;


    //新增文章
    @PostMapping
    public Result add(@RequestBody @Validated(Article.Add.class) Article article) {
        articleService.add(article);
        return Result.success();
    }

    //条件分页查询文章列表
    @GetMapping("/list")
    public Result<PageBean<Article>> list(
            @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum, @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "state", required = false) String state) {
        PageBean<Article> pb = articleService.list(pageNum, pageSize, categoryId, state);
        return Result.success(pb);
    }

    //查询所有文章
    @GetMapping("/getAll")
    public List<Article> getAll() {
        List<Article> articles = articleService.getAll();
        return articles;
    }

    //根据文章分类和发布状态搜索文章
    @GetMapping("/search")
    public List<Article> search(@RequestParam(name = "categoryId") Integer categoryId,
                                @RequestParam(name = "state", required = false) String state) {
        List<Article> articles = articleService.search(categoryId, state);
        return articles;
    }
}
