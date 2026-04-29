package com.itheima.service.impl;

import com.itheima.mapper.CategoryMapper;
import com.itheima.pojo.Category;
import com.itheima.pojo.Result;
import com.itheima.service.CategoryService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;


    //添加文章分类
    @Override
    public void add(Category category) {
        //获取用户id
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");
        category.setCreateUser(id);
        categoryMapper.add(category);
    }


    //查询文章分类
    @Override
    public List<Category> list() {
        //获取当前用户id
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        List<Category> cs = categoryMapper.list(userId);
        return cs;
    }

    //根据id查看文章详情
    @Override
    public Category getById(Integer id) {
        Category category = categoryMapper.getById(id);
        return category;
    }

    //更新文章分类
    @Override
    public Result update(Category category) {
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
        return Result.success();
    }

    //根据id删除文章分类
    @Override
    public void deleteById(Integer id) {
        categoryMapper.deleteById(id);
    }
}
