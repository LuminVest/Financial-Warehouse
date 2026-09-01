package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.User;
import com.example.demo.entity.dto.AdminUserQuery;

import java.util.List;

public interface UserService  extends IService<User> {

    IPage<User> listPage(Page<User> pageParam, AdminUserQuery adminUserQuery);

    User getUserById(Long id);

    /**
     * 按条件查询用户（走 UserMapper.xml 的 selectByCondition）
     */
//    List<User> selectByCondition(String name, String mobile);

}
