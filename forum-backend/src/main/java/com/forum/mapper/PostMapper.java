package com.forum.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.entity.Post;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
public interface PostMapper extends BaseMapper<Post> {
    @Update("UPDATE posts SET like_count = GREATEST(like_count + #{delta}, 0) WHERE id = #{id}")
    int addLikeCount(@Param("id") Long id, @Param("delta") int delta);
    @Update("UPDATE posts SET collect_count = GREATEST(collect_count + #{delta}, 0) WHERE id = #{id}")
    int addCollectCount(@Param("id") Long id, @Param("delta") int delta);
    @Update("UPDATE posts SET comment_count = GREATEST(comment_count + #{delta}, 0) WHERE id = #{id}")
    int addCommentCount(@Param("id") Long id, @Param("delta") int delta);
}
