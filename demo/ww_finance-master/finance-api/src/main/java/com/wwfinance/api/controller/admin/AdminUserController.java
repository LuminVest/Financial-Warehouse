package com.wwfinance.api.controller.admin;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.UserLoginRecord;
import com.wwfinance.api.entity.dto.AdminUserQuery;
import com.wwfinance.api.mapper.UserLoginRecordMapper;
import com.wwfinance.api.mapper.UserMapper;
import com.wwfinance.api.service.UserService;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
*后台用户管理
 */
@RestController
@RequestMapping("/admin/core/user")
@Slf4j
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserLoginRecordMapper userLoginRecordMapper;

    @Autowired
    private UserMapper userMapper;


    @ApiOperation("获取会员分页列表")
    @PostMapping("/list/{page}/{limit}")
    public PccAjaxResult listPage(
            @ApiParam(value = "当前页码", required = true)
            @PathVariable Long page,
            @ApiParam(value = "每页记录数", required = true)
            @PathVariable Long limit,
            @ApiParam(value = "查询对象", required = false)
            @RequestBody(required = false) AdminUserQuery adminUserQuery) {
        Page<User> pageParam = new Page<>(page, limit);
        IPage<User> pageModel = userService.listPage(pageParam, adminUserQuery);
        return new PccAjaxResult(200, "获取会员分页列表成功", pageModel);
    }

    @ApiOperation("登录日志分页列表")
    @PostMapping("/loginRecord/{page}/{limit}")
    public PccAjaxResult loginRecord(
            @ApiParam(value = "当前页码", required = true) @PathVariable Long page,
            @ApiParam(value = "每页记录数", required = true) @PathVariable Long limit,
            @ApiParam(value = "查询对象：mobile 手机号（可选）", required = false)
            @RequestBody(required = false) Map<String, String> query) {
        Page<UserLoginRecord> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<UserLoginRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLoginRecord::getIsDeleted, false);
        // 按手机号过滤：先查对应 userId
        if (query != null && query.get("mobile") != null && !query.get("mobile").trim().isEmpty()) {
            List<Long> userIds = userMapper.selectList(
                            new LambdaQueryWrapper<User>().like(User::getMobile, query.get("mobile").trim()))
                    .stream().map(User::getId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("records", new ArrayList<>());
                empty.put("total", 0L);
                return new PccAjaxResult(200, "获取登录日志成功", empty);
            }
            wrapper.in(UserLoginRecord::getUserId, userIds);
        }
        wrapper.orderByDesc(UserLoginRecord::getId);
        IPage<UserLoginRecord> pageModel = userLoginRecordMapper.selectPage(pageParam, wrapper);
        // 组装用户信息：手机号、昵称
        Map<Long, User> userMap = pageModel.getRecords().stream()
                .map(r -> userMapper.selectById(r.getUserId()))
                .filter(u -> u != null)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<Map<String, Object>> records = pageModel.getRecords().stream().map(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("userId", r.getUserId());
            User u = userMap.get(r.getUserId());
            item.put("mobile", u == null ? "" : u.getMobile());
            item.put("nickName", u == null ? "" : (u.getNickName() == null ? u.getName() : u.getNickName()));
            item.put("ip", r.getIp());
            item.put("createTime", r.getCreateTime());
            return item;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", pageModel.getTotal());
        return new PccAjaxResult(200, "获取登录日志成功", data);
    }

}
