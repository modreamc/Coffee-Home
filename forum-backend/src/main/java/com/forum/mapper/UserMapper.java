package com.forum.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
public interface UserMapper extends BaseMapper<User> {
    @Update("UPDATE users SET points = GREATEST(points + #{delta}, 0) WHERE id = #{id}")
    int addPoints(@Param("id") Long id, @Param("delta") int delta);
}
