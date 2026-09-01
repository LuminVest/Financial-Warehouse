package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.User;
import com.example.demo.entity.dto.AdminUserQuery;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User>  implements UserService {

    @Override
    public User getUserById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public IPage<User> listPage(Page<User> pageParam, AdminUserQuery adminUserQuery){
        if(adminUserQuery == null){
            return baseMapper.selectPage(pageParam, null);
        }
        String mobile = adminUserQuery.getMobile();
        Integer status = adminUserQuery.getStatus();
        Integer userType = adminUserQuery.getUserType();

        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq(StringUtils.isNotBlank(mobile),"mobile",mobile)
        .eq(status!=null,"status",status)
        .eq(userType !=null,"user_type",userType);
        return baseMapper.selectPage(pageParam, userQueryWrapper);
    }
//
//    @Override
//    public List<User> selectByCondition(String name, String mobile) {
//        return baseMapper.selectByCondition(name, mobile);
//    }

}
