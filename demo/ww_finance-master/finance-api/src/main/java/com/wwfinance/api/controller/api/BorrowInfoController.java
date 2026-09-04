package com.wwfinance.api.controller.api;


import com.wwfinance.api.entity.BorrowInfo;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.service.BorrowInfoService;
import com.wwfinance.api.service.UserService;
import com.wwfinance.api.utils.TokenUtil;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@Api(tags = "借款信息")
@RestController
@RequestMapping("/api/core/borrowInfo")
@Slf4j
public class BorrowInfoController {

    @Autowired
    private UserService userService;

    @Autowired
    private BorrowInfoService borrowInfoService;

    private static final TokenUtil tu = new TokenUtil();

    @ApiOperation("获取当前登录用户的借款申请审批状态")
    @GetMapping("/auth/getBorrowInfoStatus")
    public PccAjaxResult getBorrowInfoStatus(
            @ApiParam(value = "认证token，格式：5grcs xxx", required = true)
            @RequestHeader("Authorization") String authorizationHeader) {
        // 从请求头解析 token 获取当前登录用户，再查借款审批状态
        User user = getLoginUser(authorizationHeader);
        Integer status = borrowInfoService.getStatusByUserId(user.getId());
        return new PccAjaxResult(200, "获取借款申请审批状态", status);
    }

    @ApiOperation("获取当前登录用户的借款额度")
    @GetMapping("/auth/getBorrowAmount")
    public PccAjaxResult getBorrowAmount(
            @ApiParam(value = "认证token，格式：5grcs xxx", required = true)
            @RequestHeader("Authorization") String authorizationHeader) {
        User user = getLoginUser(authorizationHeader);
        BigDecimal borrowAmount = borrowInfoService.getBorrowAmount(user.getId());
        return new PccAjaxResult(200, "获取借款额度", borrowAmount);
    }

    @ApiOperation("提交借款申请")
    @PostMapping("/auth/save")
    public PccAjaxResult save(
            @ApiParam(value = "借款信息", required = true) @RequestBody BorrowInfo borrowInfo,
            @ApiParam(value = "认证token，格式：5grcs xxx", required = true)
            @RequestHeader("Authorization") String authorizationHeader) {
        User user = getLoginUser(authorizationHeader);
        borrowInfoService.saveBorrowInfo(borrowInfo, user.getId());
        return new PccAjaxResult(200, "提交成功");
    }

    /**
     * 从 Authorization 头解析 token 获取当前登录用户
     * 注：老师版用 token_phone 按手机号反查 user；
     * 本项目登录 token claims 存的是 token_userid（用户ID），直接取用户ID，结果一致。
     */
    private User getLoginUser(String authorizationHeader) {
        String token = authorizationHeader;
        log.info("token: {}", token);
        Map<String, String> map = tu.getMapInfoFromToken(token);
        String uid = map.get("token_userid");
        log.info("当前登录用户 uid: {}", uid);
        return userService.getById(Long.valueOf(uid));
    }
}
