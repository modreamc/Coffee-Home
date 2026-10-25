package com.forum.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
@Data
public class PostRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最多 200 字")
    private String title;
    @NotBlank(message = "内容不能为空")
    @Size(max = 20000, message = "内容最多 20000 字")
    private String content;
    @NotBlank(message = "分类不能为空")
    @Pattern(regexp = "GAME|ANIME", message = "分类只能是 GAME 或 ANIME")
    private String category;
    @Size(max = 2000, message = "图片列表过长")
    private String images;
    @Size(max = 1000, message = "链接过长")
    private String links;
    @Size(max = 3, message = "最多只能选择 3 个标签")
    private List<@Size(max = 20, message = "单个标签最多 20 字") String> tags;
    /** 视频链接，最多 3 个；后端会按平台白名单过滤。 */
    @Size(max = 3, message = "最多只能添加 3 个视频")
    private List<@Size(max = 500, message = "视频链接过长") String> videos;
}
