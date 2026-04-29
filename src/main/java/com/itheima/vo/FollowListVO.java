package com.itheima.vo;
import lombok.Data;

import java.util.List;

/**
 * 关注列表VO
 */
@Data
public class FollowListVO {
    private Integer total;           // 总数
    private Integer page;           // 当前页
    private Integer pageSize;       // 每页大小
    private Integer totalPages;     // 总页数
    private List<FollowUserVO> list; // 列表数据
}
