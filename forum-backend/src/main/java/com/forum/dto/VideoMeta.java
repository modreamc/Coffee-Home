package com.forum.dto;

/**
 * 视频元信息：创建帖子时由后端抓取并随帖子一起返回。
 * cover 已下载到本地 /picture/，避免第三方图床防盗链。
 */
public record VideoMeta(
        String url,
        String provider,
        String videoId,
        String title,
        String cover,
        Integer durationSeconds,
        String owner,
        boolean embeddable
) {
}
