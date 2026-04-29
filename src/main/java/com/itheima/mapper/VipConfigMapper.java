package com.itheima.mapper;

import com.itheima.pojo.VipConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VipConfigMapper {

    /**
     * 查询所有VIP配置
     */
    @Select("SELECT * FROM vip_config ORDER BY vip_type")
    List<VipConfig> findAll();

    /**
     * 根据VIP类型查询
     */
    @Select("SELECT * FROM vip_config WHERE vip_type = #{vipType}")
    VipConfig findByVipType(@Param("vipType") Integer vipType);

    /**
     * 查询启用的VIP配置
     */
    @Select("SELECT * FROM vip_config WHERE is_active = 1 ORDER BY vip_type")
    List<VipConfig> findActiveConfigs();

    /**
     * 添加VIP配置
     */
    @Insert("INSERT INTO vip_config(vip_type, vip_name, original_price, current_price, " +
            "duration_days, max_daily_upload, ad_free, " +
            "exclusive_badge, priority_review, icon_url, description, " +
            "highlight_features, is_active, discount_tag) " +
            "VALUES(#{vipType}, #{vipName}, #{originalPrice}, #{currentPrice}, " +
            "#{durationDays}, #{maxDailyUpload}, #{adFree}, " +
            "#{exclusiveBadge}, #{priorityReview}, #{iconUrl}, #{description}, " +
            "#{highlightFeatures, typeHandler=com.itheima.handler.JsonTypeHandler}, " +
            "#{isActive}, #{discountTag})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VipConfig vipConfig);

    /**
     * 更新VIP配置
     */
    @Update("UPDATE vip_config SET " +
            "vip_name = #{vipName}, original_price = #{originalPrice}, current_price = #{currentPrice}, " +
            "duration_days = #{durationDays}, " +
            "max_daily_upload = #{maxDailyUpload}," +
            "ad_free = #{adFree}, exclusive_badge = #{exclusiveBadge}, " +
            "priority_review = #{priorityReview}, " +
            "icon_url = #{iconUrl}, description = #{description}, " +
            "highlight_features = #{highlightFeatures, typeHandler=com.itheima.handler.JsonTypeHandler}, " +
            "is_active = #{isActive}, discount_tag = #{discountTag}, " +
            "update_time = now() WHERE id = #{id}")
    int update(VipConfig vipConfig);

    /**
     * 删除VIP配置
     */
    @Delete("DELETE FROM vip_config WHERE id = #{id}")
    int delete(@Param("id") Integer id);

    /**
     * 启用/禁用VIP配置
     */
    @Update("UPDATE vip_config SET is_active = #{isActive}, update_time = now() WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("isActive") Boolean isActive);


    /**
     * 检查VIP类型是否已存在
     */
    @Select("SELECT COUNT(*) FROM vip_config WHERE vip_type = #{vipType}")
    int countByVipType(@Param("vipType") Integer vipType);
}
