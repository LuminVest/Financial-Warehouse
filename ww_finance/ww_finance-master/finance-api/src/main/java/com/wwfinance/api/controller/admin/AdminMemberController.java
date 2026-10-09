package com.wwfinance.api.controller.admin;

import com.wwfinance.api.service.MemberService;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台-会员管理
 * 与前端 ww_finance_admin 对齐：
 *  - GET  /admin/core/member/page         分页列表（keyword/status）
 *  - POST /admin/core/member              新增会员
 *  - PUT  /admin/core/member/{id}/status  启用/禁用
 */
@Api(tags = "管理后台-会员管理")
@RestController
@RequestMapping("/admin/core/member")
@Slf4j
public class AdminMemberController {

    @Autowired
    private MemberService memberService;

    @ApiOperation("会员分页列表")
    @GetMapping("/page")
    public PccAjaxResult page(
            @ApiParam(value = "当前页码", required = false, defaultValue = "1")
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") long pageNum,
            @ApiParam(value = "每页记录数", required = false, defaultValue = "10")
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize,
            @ApiParam(value = "关键词(手机号/昵称/姓名)", required = false)
            @RequestParam(value = "keyword", required = false) String keyword,
            @ApiParam(value = "状态(1正常 0禁用)", required = false)
            @RequestParam(value = "status", required = false) Integer status) {
        return new PccAjaxResult(200, "获取成功",
                memberService.pageForAdmin(pageNum, pageSize, keyword, status));
    }

    @ApiOperation("新增会员")
    @PostMapping
    public PccAjaxResult add(@RequestBody Map<String, Object> body) {
        memberService.addForAdmin(
                str(body.get("phone")),
                str(body.get("nickname")),
                str(body.get("realName")),
                str(body.get("idCard")),
                intV(body.get("gender")),
                intV(body.get("score")),
                str(body.get("remark")));
        return new PccAjaxResult(200, "新增成功，初始密码为123456");
    }

    @ApiOperation("启用/禁用会员")
    @PutMapping("/{id}/status")
    public PccAjaxResult updateStatus(
            @ApiParam(value = "会员id", required = true) @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        memberService.updateStatus(id, intV(body.get("status")));
        return new PccAjaxResult(200, "操作成功");
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private Integer intV(Object v) {
        return v == null ? null : Integer.valueOf(String.valueOf(v));
    }
}
