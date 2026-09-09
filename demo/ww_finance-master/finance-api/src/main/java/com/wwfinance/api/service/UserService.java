package com.wwfinance.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.dto.AdminUserQuery;
import com.wwfinance.api.entity.dto.UserDto;

import java.util.Map;

public interface UserService extends IService<User> {

    IPage<User> listPage(Page<User> pageParam, AdminUserQuery adminUserQuery);

    User getUserById(Long id);

    /** 获取用户信息（转 DTO，不暴露敏感字段） */
    UserDto getUserDto(Long id);

    /** 校验手机号是否可注册（sms 服务 Feign 调用，无需登录） */
    boolean checkMobile(String mobile);

    /** 发送注册验证码：手机号校验 + 已注册检查 + 生成验证码存 Redis */
    String sendCode(String mobile);

    /** 用户注册：验证码校验 + 查重 + 密码一致性 + 保存用户 + 创建托管账户 */
    void register(UserDto userDTO);

    /** 用户登录：校验密码/状态 + 生成 token + 组装返回数据 */
    Map<String, Object> login(UserDto userDTO);
}
