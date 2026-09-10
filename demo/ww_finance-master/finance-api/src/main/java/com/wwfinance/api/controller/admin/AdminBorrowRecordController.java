package com.wwfinance.api.controller.admin;

import com.wwfinance.api.service.BorrowInfoService;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台-借款记录管理
 * 与前端 ww_finance_admin 对齐：
 *  - GET /admin/core/borrow/record/page       借款记录分页列表（keyword/status 过滤）
 *  - PUT /admin/core/borrow/record/{id}/audit 审核借款申请（通过/拒绝）
 */
@Api(tags = "管理后台-借款记录管理")
@RestController
@RequestMapping("/admin/core/borrow/record")
@Slf4j
public class AdminBorrowRecordController {

    @Autowired
    private BorrowInfoService borrowInfoService;

    @ApiOperation("借款记录分页列表")
    @GetMapping("/page")
    public PccAjaxResult page(
            @ApiParam(value = "当前页码", required = false, defaultValue = "1")
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") long pageNum,
            @ApiParam(value = "每页记录数", required = false, defaultValue = "10")
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize,
            @ApiParam(value = "关键字（借款人姓名）", required = false)
            @RequestParam(value = "keyword", required = false) String keyword,
            @ApiParam(value = "状态 0-待审核 2-还款中 3-已结清 4-已拒绝", required = false)
            @RequestParam(value = "status", required = false) Integer status) {
        return new PccAjaxResult(200, "获取成功",
                borrowInfoService.pageForAdmin(pageNum, pageSize, keyword, status));
    }

    @ApiOperation("审核借款申请")
    @PutMapping("/{id}/audit")
    public PccAjaxResult audit(
            @ApiParam(value = "借款记录id", required = true) @PathVariable Long id,
            @ApiParam(value = "审核参数 {status: 1通过 4拒绝, rejectReason?}", required = true)
            @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") == null ? null : Integer.valueOf(String.valueOf(body.get("status")));
        String rejectReason = body.get("rejectReason") == null ? null : String.valueOf(body.get("rejectReason"));
        borrowInfoService.auditByAdmin(id, status, rejectReason);
        return new PccAjaxResult(200, "审核完成");
    }
}
