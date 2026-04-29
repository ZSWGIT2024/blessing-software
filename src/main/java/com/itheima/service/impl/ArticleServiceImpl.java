package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.ArticleMapper;
import com.itheima.pojo.Article;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.service.ArticleService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;

    //添加文章
    @Override
    public void add(Article article) {
        //获取用户id
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        article.setCreateUser(userId);
        articleMapper.add(article);
    }

    //条件分页查询文章列表
    @Override
    public PageBean<Article> list(@Param("pageNum") Integer pageNum,
                                  @Param("pageSize") Integer pageSize,
                                  @Param("categoryId") Integer categoryId,
                                  @Param("state") String state) {
        //创建pageBean对象
        PageBean<Article> pb = new PageBean<>();

        //开启分页查询pageHelper
        PageHelper.startPage(pageNum, pageSize);

        //调用mapper
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        List<Article> list = articleMapper.list(userId, categoryId, state);
        //Page中提供了方法，可以获取pagehelper分页查询后，得到的总记录数和当前页数据
        Page<Article> page = (Page<Article>) list;

        //封装数据到pageBean中
        pb.setTotal(Long.valueOf(page.getTotal()));
        pb.setItems(page.getResult());
        return pb;
    }

    //获取全部文章数据
    @Override
    public List<Article> getAll() {
        //调用mapper
        List<Article> list = articleMapper.getAll();
        if (list != null) {
            return list;
        }
        return null;

    }

    //根据文章分类和发布状态搜索文章
    @Override
    public List<Article> search(@Param("categoryId") Integer categoryId, @Param("state") String state) {
        List<Article> list = articleMapper.search(categoryId, state);
        return list;
    }
}
