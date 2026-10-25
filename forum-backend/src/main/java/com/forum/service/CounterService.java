package com.forum.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.Map;
/**
 * 计数对账：并发场景下 like_count / collect_count / comment_count / tags.post_count
 * 采用原子自增仍然可能因为异常回滚/手工改库而漂移，这里提供一次性重算能力。
 */
@Service
public class CounterService {
    private final JdbcTemplate jdbcTemplate;
    public CounterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    @Transactional
    public Map<String, Object> rebuild() {
        int posts = jdbcTemplate.update(
                "UPDATE posts p SET " +
                "like_count = (SELECT COUNT(*) FROM likes l WHERE l.post_id = p.id), " +
                "collect_count = (SELECT COUNT(*) FROM collects c WHERE c.post_id = p.id), " +
                "comment_count = (SELECT COUNT(*) FROM comments cm WHERE cm.post_id = p.id)");
        int tags = jdbcTemplate.update(
                "UPDATE tags t SET post_count = (SELECT COUNT(*) FROM posts p " +
                "WHERE p.tags LIKE CONCAT('%\"', t.name, '\"%'))");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("postsScanned", posts);
        result.put("tagsScanned", tags);
        return result;
    }
}
