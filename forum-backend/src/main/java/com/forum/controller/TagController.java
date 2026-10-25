package com.forum.controller;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.forum.dto.ApiResponse;
import com.forum.entity.Tag;
import com.forum.mapper.TagMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/tags")
public class TagController {
    private final TagMapper tagMapper;
    public TagController(TagMapper tagMapper) { this.tagMapper = tagMapper; }
    @GetMapping
    public ResponseEntity<ApiResponse<List<Tag>>> list() {
        List<Tag> tags = tagMapper.selectList(Wrappers.<Tag>lambdaQuery()
                .orderByDesc(Tag::getPostCount)
                .last("LIMIT 40"));
        return ResponseEntity.ok(ApiResponse.success(tags));
    }
}
