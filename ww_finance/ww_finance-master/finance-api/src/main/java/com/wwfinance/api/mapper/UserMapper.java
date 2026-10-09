package com.wwfinance.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwfinance.api.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper  extends BaseMapper<User> {

    /**
     * 自定义 XML 查询：按 name / mobile 动态条件查询
     * 对应 UserMapper.xml 中的 selectByCondition
     */
//    List<User> selectByCondition(@Param("name") String name, @Param("mobile") String mobile);

}
