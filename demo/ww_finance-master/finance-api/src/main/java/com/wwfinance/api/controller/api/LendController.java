package com.wwfinance.api.controller.api;

import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.service.LendService;
import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Api(tags = "标的")
@RestController
@RequestMapping("/api/core/lend")
@Slf4j
public class LendController {

    @Autowired
    private LendService lendService;

    @ApiOperation("标的列表")
    @GetMapping("/list")
    public PccAjaxResult list() {
        List<Lend> list = lendService.getLendList();
        return new PccAjaxResult(200, "获取标的列表", list);
    }

    @ApiOperation("标的详情（含借款人信息、投资进度）")
    @GetMapping("/show/{id}")
    public PccAjaxResult show(@ApiParam(value = "标的id", required = true) @PathVariable Long id) {
        Map<String, Object> detail = lendService.getLendDetail(id);
        return new PccAjaxResult(200, "获取标的详情", detail);
    }

    @ApiOperation("投资收益计算器")
    @GetMapping("/getInterestCount/{invest}/{yearRate}/{totalmonth}/{returnMethod}")
    public PccAjaxResult getInterestCount(
            @ApiParam(value = "投资金额", required = true) @PathVariable BigDecimal invest,
            @ApiParam(value = "年化利率", required = true) @PathVariable BigDecimal yearRate,
            @ApiParam(value = "期数", required = true) @PathVariable int totalmonth,
            @ApiParam(value = "还款方式 1-等额本息 2-等额本金 3-每月还息一次还本 4-一次还本", required = true) @PathVariable int returnMethod) {
        BigDecimal interest = lendService.getInterestCount(invest, yearRate, totalmonth, returnMethod);
        return new PccAjaxResult(200, "计算成功", interest);
    }

    @ApiOperation("推荐标的（demo 简化：按热度取 topN）")
    @GetMapping("/auth/recommend")
    public PccAjaxResult recommend(
            @ApiParam(value = "topN，默认5") @RequestParam(value = "topN", required = false, defaultValue = "5") int topN) {
        log.info("推荐标的, userId={}, topN={}", LoginUserContext.getUserid(), topN);
        List<Lend> list = lendService.getRecommendList(topN);
        return new PccAjaxResult(200, "获取推荐标的", list);
    }
}
