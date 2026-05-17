package com.itheima.service;

import com.itheima.dto.MediaQueryDTO;
import com.itheima.dto.MediaUpdateDTO;
import com.itheima.dto.MediaUploadDTO;
import com.itheima.dto.SubmitBatchRequest;
import com.itheima.pojo.UserMedia;
import com.itheima.vo.MediaVO;
import com.itheima.pojo.PageBean;

import java.util.List;

public interface MediaService {

    /**
     * 上传媒体文件
     */
//    MediaVO upload(MediaUploadDTO dto);

    List<MediaVO> batchUpload(MediaUploadDTO dto);

    /**
     * 查询媒体列表（分页）
     */
    PageBean<MediaVO> queryMediaList(MediaQueryDTO queryDTO);


    /**
     * 根据用户ID和状态查询媒体列表
     */
    PageBean<MediaVO> queryMediaByUserAndStatus(MediaQueryDTO queryDTO);

    /**
     * 查询用户所有状态的作品（不筛选状态）
     */
    PageBean<MediaVO> queryAllStatusMediaByUser(MediaQueryDTO queryDTO);
    /**
     * 获取媒体详情
     */
    MediaVO getMediaDetail(Integer mediaId, Integer userId);

    /**
     * 删除媒体（逻辑删除）
     */
    boolean deleteMedia(Integer mediaId, Integer userId);

    /**
     * 更新媒体信息
     */
    MediaVO updateMedia(Integer mediaId, MediaUpdateDTO updateDTO);

    /**
     * 点赞/取消点赞
     */
    boolean toggleLike(Integer mediaId, Integer userId);


    /**
     * 获取用户总文件数
     */
    default Long getTotalCount(Integer userId) {
        // 实际实现需要在实现类中注入mapper调用
        return 0L;
    }

    /**
     * 获取用户图片数
     */
    default Long getImageCount(Integer userId) {
        return 0L;
    }

    /**
     * 获取用户视频数
     */
    default Long getVideoCount(Integer userId) {
        return 0L;
    }


    //更新媒体状态
    boolean updateStatus(Integer id, String status);

    //批量更新媒体状态
    boolean batchUpdateStatus(List<Integer> ids, String status,String tags);

    //查询用户最近上传
    List<UserMedia> getRecentMedia(Integer userId, Integer limit);

    /** 批量提交AI作品（两阶段上传的第二阶段：元数据写入user_media表） */
    List<MediaVO> batchSubmit(Integer userId, SubmitBatchRequest request);
}