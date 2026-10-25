package com.forum.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.entity.Tag;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
public interface TagMapper extends BaseMapper<Tag> {
    /** 原子增减话题计数，避免读-改-写丢更新 */
    @Update("UPDATE tags SET post_count = GREATEST(post_count + #{delta}, 0) WHERE name = #{name}")
    int addPostCount(@Param("name") String name, @Param("delta") int delta);
}
