package com.itheima.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//统一响应结果
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Result<T> {

    private Integer code;//状态码 0代表成功，1代表失败
    private String msg;//提示信息
    private T data;//响应数据

    //快速返回操作成功响应结果（需响应数据）
    public static <E> Result<E> success(E data) {
        return new Result<>(0, "操作成功", data);
    }

    //快速返回操作成功响应结果
    public static Result success() {
        return new Result(0, "操作成功", null);
    }

    public static Result error(String msg) {
        return new Result(1, msg, null);
    }

}
