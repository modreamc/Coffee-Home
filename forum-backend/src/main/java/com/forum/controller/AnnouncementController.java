package com.forum.controller;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.forum.dto.ApiResponse;
import com.forum.entity.Announcement;
import com.forum.mapper.AnnouncementMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementMapper announcementMapper;
    public AnnouncementController(AnnouncementMapper announcementMapper) { this.announcementMapper = announcementMapper; }
    @GetMapping
    public ResponseEntity<ApiResponse<List<Announcement>>> list() {
        List<Announcement> list = announcementMapper.selectList(Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getIsActive, true)
                .orderByDesc(Announcement::getCreatedAt)
                .last("LIMIT 5"));
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
