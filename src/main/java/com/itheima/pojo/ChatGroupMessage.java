package com.itheima.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 群聊消息实体类
 * <p>
 * 表示群聊中的一条消息，支持多种内容类型（文本、图片、文件、系统通知等）。
 * 包含消息内容过滤、@全体成员、撤回机制、扩展数据等完整功能。
 * 通过 extraData 字段以JSON格式存储扩展信息，并提供 extraDataMap
 * 属性在需要时自动解析为Map，用于关联文件信息和格式化消息展示。
 * </p>
 *
 * @author blessing-software
 */
@Data
public class ChatGroupMessage {

    /** 主键ID，自增 */
    private Long id;

    /** 消息业务唯一标识（雪花ID或UUID），用于对外暴露和消息去重 */
    private String messageId;

    /** 所属群聊的业务唯一标识，关联 chat_group 表 */
    private String groupId;

    /** 发送者用户ID，关联用户表 */
    private Integer senderId;

    /**
     * 消息内容类型
     * <ul>
     *   <li>text - 纯文本消息</li>
     *   <li>image - 图片消息</li>
     *   <li>file - 文件消息</li>
     *   <li>video - 视频消息</li>
     *   <li>audio - 语音消息</li>
     *   <li>system - 系统通知（如入群、退群提示）</li>
     * </ul>
     */
    private String contentType;

    /** 消息正文内容（过滤后的安全内容），文本类型时存储文本，多媒体类型时存储描述文本 */
    private String content;

    /** 消息原始内容（未经敏感词过滤），用于编辑消息时恢复原始文本或审核追溯 */
    private String originalContent;

    /** 关联的文件ID，当 contentType 为 image/file/video/audio 时关联文件表中的文件记录 */
    private Long fileId;

    /**
     * 扩展数据（JSON格式字符串）
     * <p>
     * 不同 contentType 下存储不同结构的数据：
     * <ul>
     *   <li>image - {"width":800,"height":600,"thumbnail":"/thumb/xxx.jpg","fileId":123}</li>
     *   <li>file - {"fileName":"文档.pdf","fileSize":1024000,"fileId":456}</li>
     *   <li>system - {"type":"member_join","userId":789,"nickname":"新成员"}</li>
     * </ul>
     * </p>
     */
    private String extraData;

    /**
     * 消息状态
     * <ul>
     *   <li>sent - 已发送（服务端已接收）</li>
     *   <li>delivered - 已投递（已推送至在线群成员）</li>
     *   <li>withdrawn - 已撤回</li>
     *   <li>deleted - 已删除（软删除）</li>
     * </ul>
     */
    private String status;

    /** 是否已被软删除：true-已删除（回收站），false-正常 */
    private Boolean isDeleted;

    /** 消息是否经过了敏感词过滤：true-已被过滤，false-未触发过滤 */
    private Boolean isFiltered;

    /** 被命中的敏感词列表（逗号分隔），用于审核追溯和申诉处理 */
    private String filteredWords;

    /** 是否@全体成员：true-该消息@了所有人，false-普通消息 */
    private Boolean mentionAll;

    /** 消息撤回时间，非空表示该消息已被发送者撤回 */
    private LocalDateTime withdrawTime;

    /** 撤回原因（可选），管理员撤回时可填写撤回理由 */
    private String withdrawReason;

    /** 消息发送/创建时间 */
    private LocalDateTime createTime;

    /** 消息最后更新时间（如编辑、撤回等操作会更新此字段） */
    private LocalDateTime updateTime;

    /**
     * 扩展数据的Map形式（非数据库字段，仅用于内存）
     * <p>
     * 通过 getExtraDataMap() 方法在首次访问时自动从 extraData 字段
     * 的JSON字符串解析得到，用于Java代码中方便地读写扩展字段。
     * </p>
     */
    private Map<String, Object> extraDataMap;

    /** 关联的文件实体（非数据库字段，仅用于内存），当消息包含文件/图片时通过关联查询填充 */
    private ChatFile chatFile;

    /**
     * 获取扩展数据Map
     * <p>
     * 如果 extraDataMap 为空且 extraData 字段有值，则自动将JSON字符串解析为Map。
     * 解析失败时返回空HashMap，避免空指针异常。
     * </p>
     *
     * @return 扩展数据的Map形式，可能为空Map但不会为null
     */
    public Map<String, Object> getExtraDataMap() {
        if (extraDataMap == null && extraData != null && !extraData.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                extraDataMap = mapper.readValue(extraData, Map.class);
            } catch (Exception e) {
                extraDataMap = new HashMap<>();
            }
        }
        return extraDataMap;
    }
}
