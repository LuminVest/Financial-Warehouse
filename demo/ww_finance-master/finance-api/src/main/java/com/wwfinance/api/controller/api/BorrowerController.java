package com.wwfinance.api.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wwfinance.api.entity.Borrower;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.dto.BorrowerDTO;
import com.wwfinance.api.enums.BorrowerStatusEnum;
import com.wwfinance.api.service.BorrowerService;
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

import java.util.Map;

@Api(tags = "借款人认证")
@RestController
@RequestMapping("/api/core/borrower")
@Slf4j
public class BorrowerController {

    @Autowired
    private BorrowerService borrowerService;

    @Autowired
    private UserService userService;

    private static final TokenUtil tu = new TokenUtil();

    @ApiOperation("借款人认证提交")
    @PostMapping("/auth/save")
    public PccAjaxResult save(
            @ApiParam(value = "借款人认证信息", required = true) @RequestBody BorrowerDTO borrowerDTO,
            @ApiParam(value = "认证token，格式：5grcs xxx", required = true)
            @RequestHeader("Authorization") String authorization) {
        // 1. 从请求头解析 token，获取当前登录用户
        // 2. 添加 borrower
        // 3. 添加 borrower_attach
        // 4. 更新 user 表（认证状态 -> 认证中）
        Map<String, String> map = tu.getMapInfoFromToken(authorization);
        String uid = map.get("token_userid");
        User user = userService.getById(Long.valueOf(uid));
        log.info("借款人认证提交，userId={}", user.getId());
        borrowerService.saveBorrowerVOByUserId(borrowerDTO, user);
        return new PccAjaxResult(200, "获取认证结果");
    }

    @ApiOperation("获取借款人认证状态")
    @GetMapping("/auth/getBorrowerStatus")
    public PccAjaxResult getBorrowerStatus(
            @ApiParam(value = "认证token，格式：5grcs xxx", required = true)
            @RequestHeader("Authorization") String authorization) {
        // 1. 从请求头中获取 token
        // 2. 通过工具类解析 token，获取用户 id
        // 3. 根据用户 id 查询 borrower 表
        // 4. 得到认证状态 {"msg":"获取认证状态成功","code":200,"data":0}
        Map<String, String> map = tu.getMapInfoFromToken(authorization);
        String uid = map.get("token_userid");
        User user = userService.getById(Long.valueOf(uid));
        LambdaQueryWrapper<Borrower> borrowerQueryWrapper = new LambdaQueryWrapper<>();
        borrowerQueryWrapper.select(Borrower::getStatus).eq(Borrower::getUserId, user.getId());
        Borrower borrower = borrowerService.getOne(borrowerQueryWrapper);
        if (borrower == null) {
            return new PccAjaxResult(200, "获取认证结果", BorrowerStatusEnum.NO_AUTH.getStatus());
        }
        return new PccAjaxResult(200, "获取认证结果", borrower.getStatus());
    }
}
