
package com.itheima.utils;

import com.itheima.pojo.UserMedia;
import com.itheima.vo.MediaVO;
import org.springframework.beans.BeanUtils;

import java.time.format.DateTimeFormatter;

public class MediaConverter {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 实体类转VO
     */
    public static MediaVO toVO(UserMedia media) {
        if (media == null) {
            return null;
        }

        MediaVO vo = new MediaVO();

        // 复制相同字段
        BeanUtils.copyProperties(media, vo);

        // 设置特殊字段
        vo.setFilePath(media.getFilePath()); // OSS URL
        vo.setThumbnailPath(media.getThumbnailPath()); // 简化处理，使用原图作为缩略图

        // 格式化文件大小
        vo.setFileSize(formatFileSize(media.getFileSize()));

        // 设置中文类型
        vo.setMediaTypeCN(getMediaTypeCN(media.getMediaType()));

        // 格式化上传日期
        if (media.getUploadTime() != null) {
            vo.setUploadDate(media.getUploadTime().format(DATE_FORMATTER));
        }

        return vo;
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(Long size) {
        if (size == null) return "0 B";

        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 获取中文类型
     */
    private static String getMediaTypeCN(String mediaType) {
        switch (mediaType) {
            case "image":
                return "图片";
            case "video":
                return "视频";
            default:
                return "文件";
        }
    }
}










//package com.itheima.utils;
//
//import com.itheima.pojo.UserMedia;
//import com.itheima.vo.MediaVO;
//import org.springframework.beans.BeanUtils;
//
//import java.time.format.DateTimeFormatter;
//
///**
// * 媒体文件转换工具
// */
//public class MediaConverter {
//
//    // 静态方法，直接调用
//
//    /**
//     * 基础转换：复制相同字段
//     */
//    public static MediaVO toVO(UserMedia media) {
//        if (media == null) return null;
//
//        MediaVO vo = new MediaVO();
//        // 复制所有同名同类型的字段
//        BeanUtils.copyProperties(media, vo);
//        return vo;
//    }
//
//    /**
//     * 完整转换：包含格式化和计算字段
//     */
//    public static MediaVO toDetailVO(UserMedia media) {
//        MediaVO vo = toVO(media);
//        if (vo == null) return null;
//
//        // 格式化文件大小
//        vo.setFileSizeWithUnit(formatSize(media.getFileSize()));
//
//        // 设置分辨率
//        if (media.getWidth() != null && media.getHeight() != null) {
//            vo.setResolution(media.getWidth() + "×" + media.getHeight());
//        }
//
//        // 设置中文类型
//        vo.setMediaTypeCN("image".equals(media.getMediaType()) ? "图片" : "视频");
//
//        return vo;
//    }
//
//    /**
//     * 列表转换：只转换必要字段（性能优化）
//     */
//    public static MediaVO toSimpleVO(UserMedia media) {
//        if (media == null) return null;
//
//        MediaVO vo = new MediaVO();
//        vo.setId(media.getId());
//        vo.setFilename(media.getFilename());
//        vo.setMediaType(media.getMediaType());
//        vo.setMediaTypeCN("image".equals(media.getMediaType()) ? "图片" : "视频");
//        vo.setFileSizeWithUnit(formatSize(media.getFileSize()));
//        vo.setUploadTime(media.getUploadTime());
//
//        // 缩略图URL（如果有）
//        if (media.getThumbnailPath() != null) {
//            vo.setThumbnailUrl("/uploads/thumbnails/" + media.getThumbnailPath());
//        } else {
//            vo.setUrl("/uploads/" + media.getFilePath());
//        }
//
//        return vo;
//    }
//
//    /**
//     * 格式化文件大小
//     */
//    private static String formatSize(Long size) {
//        if (size == null) return "0 B";
//
//        if (size < 1024) {
//            return size + " B";
//        } else if (size < 1024 * 1024) {
//            return String.format("%.1f KB", size / 1024.0);
//        } else if (size < 1024 * 1024 * 1024) {
//            return String.format("%.1f MB", size / (1024.0 * 1024));
//        } else {
//            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
//        }
//    }
//
//    /**
//     * 格式化时长（视频用）
//     */
//    public static String formatDuration(Integer seconds) {
//        if (seconds == null) return null;
//
//        int min = seconds / 60;
//        int sec = seconds % 60;
//        return String.format("%02d:%02d", min, sec);
//    }
//}
