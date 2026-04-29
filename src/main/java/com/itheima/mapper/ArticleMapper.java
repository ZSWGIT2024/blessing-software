package com.itheima.mapper;


import com.itheima.pojo.Article;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleMapper {

    //添加文章
    @Insert("insert into article(title,content,cover_img,state,category_id,create_user,create_time,update_time) " +
            "values(#{title},#{content},#{coverImg},#{state},#{categoryId},#{createUser},now(),now())")
    void add(Article article);

    //条件分页查询文章列表
    List<Article> list( @Param("userId") Integer userId,
                        @Param("categoryId") Integer categoryId,
                        @Param("state") String state);

    //查询所有文章
    @Select("select * from article")
    List<Article> getAll();

    //根据文章分类和发布状态搜索文章
    @Select("select * from article where category_id = #{categoryId} or state = #{state}")
    List<Article> search(@Param("categoryId") Integer categoryId,@Param("state") String state);
}
