package com.wwfinance.api.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwfinance.api.entity.TransFlow;
import com.wwfinance.api.service.TransFlowService;
import com.wwfinance.api.service.UserIntegralService;
import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户中心：资金流水 + 积分等级（我的账户附加能力）
 */
@Api(tags = "用户中心")
@RestController
@RequestMapping("/api/user/center")
@Slf4j
public class UserCenterController {

    @Resource
    private TransFlowService transFlowService;

    @Resource
    private UserIntegralService userIntegralService;

    @ApiOperation("我的资金流水（分页）")
    @GetMapping("/transFlow/page")
    public PccAjaxResult transFlowPage(
            @ApiParam(value = "当前页码", required = false, defaultValue = "1")
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") long pageNum,
            @ApiParam(value = "每页记录数", required = false, defaultValue = "10")
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        Long userId = LoginUserContext.getUserid().longValue();
        Page<TransFlow> page = transFlowService.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<TransFlow>()
                        .eq(TransFlow::getUserId, userId)
                        .apply("is_deleted = 0")
                        .orderByDesc(TransFlow::getId));
        return new PccAjaxResult(200, "获取成功", page);
    }

    @ApiOperation("我的积分等级")
    @GetMapping("/integral/info")
    public PccAjaxResult integralInfo() {
        Long userId = LoginUserContext.getUserid().longValue();
        return new PccAjaxResult(200, "获取成功", userIntegralService.getIntegralInfo(userId));
    }
}
