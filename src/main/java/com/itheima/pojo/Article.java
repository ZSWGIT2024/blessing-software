package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itheima.anno.State;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * 文章表实体类
 */
@Data
public class Article {
    @NotNull(groups = {Update.class})
    private Integer id;//文章id
    @NotEmpty
    @Pattern(regexp = "^\\S{1,10}$")
    private String title;//文章标题
    @NotEmpty
    private String content;//文章内容
    @NotEmpty
    @URL
    private String coverImg;//文章封面图
    @State//自定义校验的注解
    private String state;//文章状态
    @NotNull
    private Integer categoryId;//文章分类id
    private Integer createUser;//创建用户id
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;//更新时间

    //如果说某个校验项没有指定分组，默认属于Default分组
    //分组之间可以继承，A extends B 那么A中拥有B中所有的校验项

    public interface Add extends Default {

    }

    public interface Update extends Default {

    }

}
