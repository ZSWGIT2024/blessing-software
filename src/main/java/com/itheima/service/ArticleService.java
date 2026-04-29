package com.itheima.service;

import com.itheima.pojo.Article;
import com.itheima.pojo.PageBean;

import java.util.List;

public interface ArticleService {

    //添加文章
    void add(Article article);

    //条件分页查询文章列表
    PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state);

    //查询所有文章
    List<Article> getAll();

    //根据文章分类和发布状态搜索文章
    List<Article> search(Integer categoryId, String state);
}
