package com.wwfinance.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwfinance.api.entity.TransFlow;
import com.wwfinance.api.service.TransFlowService;
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
import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台-资金流水监控
 * 与前端 ww_finance_admin 对齐：
 *   - GET /admin/core/transFlow/page?pageNum&pageSize&userName&transType  分页列表（支持按用户名/类型筛选）
 */
@Api(tags = "管理后台-资金流水监控")
@RestController
@RequestMapping("/admin/core/transFlow")
@Slf4j
public class AdminTransFlowController {

    @Resource
    private TransFlowService transFlowService;

    @ApiOperation("资金流水分页列表")
    @GetMapping("/page")
    public PccAjaxResult page(
            @ApiParam(value = "当前页码", required = false, defaultValue = "1")
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") long pageNum,
            @ApiParam(value = "每页记录数", required = false, defaultValue = "10")
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize,
            @ApiParam(value = "用户名筛选", required = false)
            @RequestParam(value = "userName", required = false) String userName,
            @ApiParam(value = "交易类型筛选(1充值 2提现 3投标 4投资回款 5放款 6还款)", required = false)
            @RequestParam(value = "transType", required = false) Integer transType) {
        LambdaQueryWrapper<TransFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("is_deleted = 0");
        if (userName != null && !userName.isEmpty()) {
            wrapper.like(TransFlow::getUserName, userName);
        }
        if (transType != null) {
            wrapper.eq(TransFlow::getTransType, transType);
        }
        wrapper.orderByDesc(TransFlow::getId);
        Page<TransFlow> page = transFlowService.page(new Page<>(pageNum, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("list", page.getRecords());
        data.put("total", page.getTotal());
        return new PccAjaxResult(200, "获取成功", data);
    }
}
