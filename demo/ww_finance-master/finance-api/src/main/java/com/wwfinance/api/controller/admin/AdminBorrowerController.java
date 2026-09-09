package com.wwfinance.api.controller.admin;

import com.wwfinance.api.service.BorrowerService;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台-借款人管理
 * 与老师版前端 ww_finance_admin 对齐：
 *  - GET /admin/core/borrower/page      借款人分页列表（keyword/auditStatus 过滤）
 *  - PUT /admin/core/borrower/{id}/audit 审核借款人（通过/拒绝）
 */
@Api(tags = "管理后台-借款人管理")
@RestController
@RequestMapping("/admin/core/borrower")
@Slf4j
public class AdminBorrowerController {

    @Autowired
    private BorrowerService borrowerService;

    @ApiOperation("借款人分页列表")
    @GetMapping("/page")
    public PccAjaxResult page(
            @ApiParam(value = "当前页码", required = false, defaultValue = "1")
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") long pageNum,
            @ApiParam(value = "每页记录数", required = false, defaultValue = "10")
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize,
            @ApiParam(value = "关键字（姓名/手机号/身份证）", required = false)
            @RequestParam(value = "keyword", required = false) String keyword,
            @ApiParam(value = "审核状态 0-待审核 1-通过 2-拒绝", required = false)
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus) {
        return new PccAjaxResult(200, "获取成功",
                borrowerService.pageForAdmin(pageNum, pageSize, keyword, auditStatus));
    }

    @ApiOperation("审核借款人")
    @PutMapping("/{id}/audit")
    public PccAjaxResult audit(
            @ApiParam(value = "借款人记录id", required = true) @PathVariable Long id,
            @ApiParam(value = "审核参数 {auditStatus: 1通过 2拒绝, remark?}", required = true)
            @RequestBody Map<String, Object> body) {
        Integer auditStatus = body.get("auditStatus") == null ? null : Integer.valueOf(String.valueOf(body.get("auditStatus")));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        borrowerService.auditByAdmin(id, auditStatus, remark);
        return new PccAjaxResult(200, "审核完成");
    }
}
