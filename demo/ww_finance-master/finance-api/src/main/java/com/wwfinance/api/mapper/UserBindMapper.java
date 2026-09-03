package com.wwfinance.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwfinance.api.entity.UserBind;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserBindMapper extends BaseMapper<UserBind> {

    /**
     * 根据用户 id 查询绑定信息（逻辑删除只查未删除记录）
     */
    @Select("SELECT * FROM user_bind WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY id DESC LIMIT 1")
    UserBind getBindInfoByUserId(@Param("userId") Long userId);
}
