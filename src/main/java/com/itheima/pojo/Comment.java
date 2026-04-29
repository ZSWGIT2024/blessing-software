package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Comment {
    private Integer id;
    private Integer mediaId;           // 作品ID
    private Integer userId;           // 用户ID
    private String content;           // 评论内容
    private String contentHtml;       // HTML格式内容（用于富文本）
    private List<Integer> mentionedUsers; // @的用户列表
    private Integer parentId = 0;     // 父评论ID（0表示顶级评论）
    private Integer rootId = 0;       // 根评论ID
    private Integer replyToUserId;    // 回复给哪个用户
    private Integer replyToCommentId; // 回复给哪条评论
    private String status = "active"; // 状态：active-正常，hidden-隐藏，deleted-删除
    private Boolean isTop = false;    // 是否置顶
    private Boolean isHot = false;    // 是否热门
    private Integer likeCount = 0;    // 点赞数
    private Integer replyCount = 0;   // 回复数
    private Integer reportCount = 0;  // 举报数
    private String auditStatus = "approved"; // 审核状态：pending-待审，approved-通过，rejected-拒绝
    private String auditReason;       // 审核原因
    private LocalDateTime auditTime;  // 审核时间
    private Integer auditUserId;      // 审核人ID
    private String ipAddress;         // IP地址
    private String userAgent;         // User-Agent

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // 关联的用户信息（用于前端显示）
    private User user;                // 评论用户信息
    private User replyToUser;         // 被回复用户信息

    // 关联的子评论（二级回复）
    private List<Comment> replies;

    // 当前用户是否点赞
    private Boolean isLiked = false;
}