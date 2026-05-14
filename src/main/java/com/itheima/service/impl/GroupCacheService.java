package com.itheima.service.impl;

import com.itheima.common.RedisConstants;
import com.itheima.dto.*;
import com.itheima.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 群聊缓存服务
 * <p>
 * 使用 Redis 缓存群聊相关的热点数据，减少数据库查询压力。
 * 缓存策略遵循与 SocialCacheService 相同的写穿透/失效模式。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupCacheService {

    private final RedisUtil redisUtil;

    // ==================== 群信息缓存 ====================

    public void cacheGroupInfo(String groupId, GroupInfoDTO info) {
        String key = String.format(RedisConstants.GROUP_INFO_KEY, groupId);
        try {
            redisUtil.set(key, info, RedisConstants.GROUP_INFO_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存群信息失败 groupId:{}", groupId, e);
        }
    }

    public GroupInfoDTO getCachedGroupInfo(String groupId) {
        String key = String.format(RedisConstants.GROUP_INFO_KEY, groupId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof GroupInfoDTO) {
                return (GroupInfoDTO) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存群信息失败 groupId:{}", groupId, e);
        }
        return null;
    }

    public void evictGroupInfoCache(String groupId) {
        String key = String.format(RedisConstants.GROUP_INFO_KEY, groupId);
        redisUtil.delete(key);
    }

    // ==================== 群成员列表缓存 ====================

    @SuppressWarnings("unchecked")
    public void cacheGroupMembers(String groupId, List<GroupMemberDTO> members) {
        String key = String.format(RedisConstants.GROUP_MEMBERS_KEY, groupId);
        try {
            redisUtil.set(key, members, RedisConstants.GROUP_MEMBERS_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存群成员列表失败 groupId:{}", groupId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<GroupMemberDTO> getCachedGroupMembers(String groupId) {
        String key = String.format(RedisConstants.GROUP_MEMBERS_KEY, groupId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof List) {
                return (List<GroupMemberDTO>) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存群成员列表失败 groupId:{}", groupId, e);
        }
        return null;
    }

    public void evictGroupMembersCache(String groupId) {
        String key = String.format(RedisConstants.GROUP_MEMBERS_KEY, groupId);
        redisUtil.delete(key);
    }

    // ==================== 用户群列表缓存 ====================

    @SuppressWarnings("unchecked")
    public void cacheMyGroups(Integer userId, List<GroupListDTO> groups) {
        String key = String.format(RedisConstants.GROUP_LIST_KEY, userId);
        try {
            redisUtil.set(key, groups, RedisConstants.GROUP_LIST_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存用户群列表失败 userId:{}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<GroupListDTO> getCachedMyGroups(Integer userId) {
        String key = String.format(RedisConstants.GROUP_LIST_KEY, userId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof List) {
                return (List<GroupListDTO>) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存用户群列表失败 userId:{}", userId, e);
        }
        return null;
    }

    public void evictMyGroupsCache(Integer userId) {
        String key = String.format(RedisConstants.GROUP_LIST_KEY, userId);
        redisUtil.delete(key);
    }

    // ==================== 群消息历史缓存 ====================

    @SuppressWarnings("unchecked")
    public void cacheGroupMessages(String groupId, List<GroupMessageDTO> messages) {
        String key = String.format(RedisConstants.GROUP_MESSAGES_KEY, groupId);
        try {
            redisUtil.set(key, messages, RedisConstants.GROUP_MESSAGES_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存群消息列表失败 groupId:{}", groupId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<GroupMessageDTO> getCachedGroupMessages(String groupId) {
        String key = String.format(RedisConstants.GROUP_MESSAGES_KEY, groupId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof List) {
                return (List<GroupMessageDTO>) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存群消息列表失败 groupId:{}", groupId, e);
        }
        return null;
    }

    public void evictGroupMessagesCache(String groupId) {
        String key = String.format(RedisConstants.GROUP_MESSAGES_KEY, groupId);
        redisUtil.delete(key);
    }

    // ==================== 群未读计数 ====================

    public void cacheUnreadCount(String groupId, Integer userId, Integer count) {
        String key = String.format(RedisConstants.GROUP_UNREAD_KEY, groupId, userId);
        try {
            redisUtil.set(key, count, RedisConstants.GROUP_INFO_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存群未读数失败 groupId:{}, userId:{}", groupId, userId, e);
        }
    }

    public Integer getCachedUnreadCount(String groupId, Integer userId) {
        String key = String.format(RedisConstants.GROUP_UNREAD_KEY, groupId, userId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof Number) {
                return ((Number) cached).intValue();
            }
        } catch (Exception e) {
            log.error("获取缓存群未读数失败 groupId:{}, userId:{}", groupId, userId, e);
        }
        return null;
    }

    public void incrementUnreadCount(String groupId, Integer userId) {
        String key = String.format(RedisConstants.GROUP_UNREAD_KEY, groupId, userId);
        try {
            redisUtil.increment(key, 1);
            redisUtil.expire(key, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("累加群未读数失败 groupId:{}, userId:{}", groupId, userId, e);
        }
    }

    public void resetUnreadCount(String groupId, Integer userId) {
        String key = String.format(RedisConstants.GROUP_UNREAD_KEY, groupId, userId);
        redisUtil.delete(key);
    }

    // ==================== 群在线成员 ====================

    @SuppressWarnings("unchecked")
    public void cacheOnlineMembers(String groupId, List<Integer> userIds) {
        String key = String.format(RedisConstants.GROUP_ONLINE_KEY, groupId);
        try {
            redisUtil.set(key, userIds, RedisConstants.GROUP_ONLINE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存在线成员失败 groupId:{}", groupId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getCachedOnlineMembers(String groupId) {
        String key = String.format(RedisConstants.GROUP_ONLINE_KEY, groupId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof List) {
                return (List<Integer>) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存在线成员失败 groupId:{}", groupId, e);
        }
        return null;
    }

    // ==================== 批量清除 ====================

    public void evictAllGroupCache(String groupId) {
        evictGroupInfoCache(groupId);
        evictGroupMembersCache(groupId);
        evictGroupMessagesCache(groupId);
    }

    public void evictGroupOnMemberChange(String groupId, Integer userId) {
        evictGroupMembersCache(groupId);
        evictMyGroupsCache(userId);
    }

    public void evictGroupOnMessageChange(String groupId) {
        evictGroupMessagesCache(groupId);
    }
}
