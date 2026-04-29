package com.itheima.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "media")
@Data
public class MediaProperties {
    private Ffmpeg ffmpeg;
    private Image image;
    private Video video;

    @Data
    public static class Ffmpeg {
        private String path;  // 对应 media.ffmpeg.path
    }

    @Data
    public static class Image {
        private long maxSize;       // 对应 media.image.max-size
        private int maxBatchCount;  // 对应 media.image.max-batch-count
    }

    @Data
    public static class Video {
        private long maxSize;       // 对应 media.video.max-size
        private int maxBatchCount;  // 对应 media.video.max-batch-count
    }
}
