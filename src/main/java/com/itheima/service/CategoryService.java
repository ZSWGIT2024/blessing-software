package com.itheima.service;

import com.itheima.pojo.Category;
import com.itheima.pojo.Result;

import java.util.List;

public interface CategoryService {

    //添加文章分类
    void add(Category category);

    //查询文章分类
    List<Category> list();

    //根据id查看文章详情
    Category getById(Integer id);

    //更新文章分类
    Result update(Category category);

    //根据id删除文章分类
    void deleteById(Integer id);
}
