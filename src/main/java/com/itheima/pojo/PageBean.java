package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageBean<T> {
    private Long total;           // 总记录数
    private List<T> items;        // 当前页的数据的集合

    // 新增字段（可选）
    private Integer pageNum;      // 当前页码（如果前端需要）
    private Integer pageSize;     // 每页大小（如果前端需要）
    private Integer pages;        // 总页数（如果前端需要）

    // 保留原有构造方法
    public PageBean(Long total, List<T> items) {
        this.total = total;
        this.items = items;
    }

    // 工具方法：计算总页数
    public Integer getPages() {
        if (pageSize == null || pageSize == 0) return 0;
        return (int) Math.ceil((double) total / pageSize);
    }
}