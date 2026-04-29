package com.itheima.mapper;


import com.itheima.pojo.Category;
import org.apache.ibatis.annotations.*;


import java.util.List;

@Mapper
public interface CategoryMapper {

    //添加文章分类
    @Insert("insert into category(category_name,category_alias,create_user,create_time,update_time) values(#{categoryName},#{categoryAlias},#{createUser},now(),now())")
    void add(Category category);

    @Select("select * from category where create_user = #{id}")
    List<Category> list(Integer id);

    //根据id查看文章详情
    @Select("select * from category where id = #{id}")
    Category getById(Integer id);

    //更新文章分类
    @Update("update category set category_name = #{categoryName},category_alias = #{categoryAlias},update_time = #{updateTime} where id = #{id}")
    void update(Category category);

    //根据id删除文章分类
    @Delete("delete from category where id = #{id}")
    void deleteById(Integer id);
}
