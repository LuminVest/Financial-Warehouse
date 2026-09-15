package com.wwfinance.api.controller.admin;

import com.wwfinance.api.service.LoanProjectService;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 管理后台-标的管理
 * 与前端 ww_finance_admin 对齐：
 *  - GET  /admin/core/loan/project/page           标的分页列表
 *  - POST /admin/core/loan/project                发布标的
 *  - PUT  /admin/core/loan/project/{id}/offline   下架标的
 *  - GET  /admin/core/loan/project/{projectId}/investments  投资列表
 */
@Api(tags = "管理后台-标的管理")
@RestController
@RequestMapping("/admin/core/loan/project")
@Slf4j
public class AdminLoanProjectController {

    @Autowired
    private LoanProjectService loanProjectService;

    @ApiOperation("标的分页列表")
    @GetMapping("/page")
    public PccAjaxResult page(
            @ApiParam(value = "当前页码", required = false, defaultValue = "1")
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") long pageNum,
            @ApiParam(value = "每页记录数", required = false, defaultValue = "10")
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize,
            @ApiParam(value = "关键词(标的名称/借款人)", required = false)
            @RequestParam(value = "keyword", required = false) String keyword,
            @ApiParam(value = "状态(0待发布 1募资中 2已完成 3已逾期 4已下架)", required = false)
            @RequestParam(value = "status", required = false) Integer status) {
        return new PccAjaxResult(200, "获取成功",
                loanProjectService.pageForAdmin(pageNum, pageSize, keyword, status));
    }

    @ApiOperation("发布标的（测试）")
    @PostMapping
    public PccAjaxResult publish(@RequestBody Map<String, Object> body) {
        loanProjectService.publishForAdmin(
                str(body.get("title")),
                longV(body.get("borrowerId")),
                dec(body.get("amount")),
                dec(body.get("rate")),
                intV(body.get("term")),
                str(body.get("purpose")),
                intV(body.get("riskLevel")));
        return new PccAjaxResult(200, "发布成功");
    }

    @ApiOperation("下架标的")
    @PutMapping("/{id}/offline")
    public PccAjaxResult offline(@ApiParam(value = "标的id", required = true) @PathVariable Long id) {
        loanProjectService.offlineByAdmin(id);
        return new PccAjaxResult(200, "下架成功");
    }

    @ApiOperation("放款（满标后手动触发放款，幂等）")
    @PutMapping("/{id}/loan")
    public PccAjaxResult loan(@ApiParam(value = "标的id", required = true) @PathVariable Long id) {
        loanProjectService.loanByAdmin(id);
        return new PccAjaxResult(200, "放款成功");
    }

    @ApiOperation("标的投资列表")
    @GetMapping("/{projectId}/investments")
    public PccAjaxResult investments(
            @ApiParam(value = "标的id", required = true) @PathVariable Long projectId) {
        return new PccAjaxResult(200, "获取成功", loanProjectService.listInvestments(projectId));
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private Integer intV(Object v) {
        return v == null ? null : Integer.valueOf(String.valueOf(v));
    }

    private Long longV(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private BigDecimal dec(Object v) {
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }
}
