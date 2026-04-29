package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类表实体类
 */
@Data
public class Category {
    @NotNull(groups = {Update.class})
    private Integer id;
    @NotEmpty
    //指定分组
//    @NotEmpty(groups = {Add.class,Update.class})
    private String categoryName;
    @NotEmpty
    //指定分组
//    @NotEmpty(groups = {Add.class,Update.class})
    private String categoryAlias;
    private Integer createUser;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;


    //如果说某个校验项没有指定分组，默认属于Default分组
    //分组之间可以继承，A extends B 那么A中拥有B中所有的校验项

    public interface Add extends Default {

    }

    public interface Update extends Default {

    }
}
