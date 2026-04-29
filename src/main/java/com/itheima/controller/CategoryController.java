package com.itheima.controller;


import com.itheima.pojo.Category;
import com.itheima.pojo.Result;
import com.itheima.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    //添加文章分类
    @PostMapping
    public Result add(@RequestBody @Validated(Category.Add.class) Category category) {
        categoryService.add(category);
        return Result.success();
    }

    //查询文章分类
    @GetMapping
    public Result<List<Category>> list() {
        List<Category> cs = categoryService.list();
        return Result.success(cs);
    }


    //根据id查看文章详情
    @GetMapping("/detail")
    public Result<Category> detail(@RequestParam(name = "id") Integer id) {
        Category category = categoryService.getById(id);
        return Result.success(category);
    }

    //更新文章分类
    @PutMapping
    public Result update(@RequestBody @Validated(Category.Update.class) Category category) {
        categoryService.update(category);
        return Result.success();
    }

    //根据id删除文章分类
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable(name = "id") Integer id) {
        categoryService.deleteById(id);
        return Result.success();
    }
}
