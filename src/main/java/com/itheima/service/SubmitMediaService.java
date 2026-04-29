package com.itheima.service;

import com.itheima.dto.MediaQueryDTO;
import com.itheima.dto.MediaUpdateDTO;
import com.itheima.dto.MediaUploadDTO;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.UserMedia;
import com.itheima.vo.MediaVO;

import java.util.List;

public interface SubmitMediaService {

    /**
     * 上传投稿媒体文件
     */
//    MediaVO upload(MediaUploadDTO dto);

    List<MediaVO> batchUpload(MediaUploadDTO dto);

    //查询用户投稿作品列表
    PageBean<MediaVO> querySubmitList(MediaQueryDTO queryDTO);

    /**
     * 根据用户ID和状态查询投稿媒体列表
     */
    PageBean<MediaVO> querySubmitByUserAndStatus(MediaQueryDTO queryDTO);

    /**
     * 查询用户投稿所有状态的作品（不筛选状态）
     */
    PageBean<MediaVO> queryAllStatusSubmitByUser(MediaQueryDTO queryDTO);

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


    //更新媒体状态
    boolean updateStatus(Integer id, String status);

    //批量更新媒体状态
    boolean batchUpdateStatus(List<Integer> ids, String status, String tags);

    //查询用户最近上传
    List<UserMedia> getRecentMedia(Integer userId, Integer limit);

}
