package com.itheima.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FriendGroupDTO {
    private Long id;
    private Integer userId;
    private String groupName;
    private Integer sortOrder;
    private String color;
    private String description;
    private Integer friendCount;
    private List<FriendRelationDTO> friends;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}