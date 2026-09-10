package com.wwfinance.api.controller.admin;

import com.wwfinance.api.service.IntegralGradeService;
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
 * 管理后台-积分等级管理
 * 与前端 ww_finance_admin 对齐：
 *  - GET    /admin/core/point/level/page   分页列表
 *  - POST   /admin/core/point/level        新增
 *  - PUT    /admin/core/point/level        修改
 *  - DELETE /admin/core/point/level/{id}   删除
 */
@Api(tags = "管理后台-积分等级管理")
@RestController
@RequestMapping("/admin/core/point/level")
@Slf4j
public class AdminPointLevelController {

    @Autowired
    private IntegralGradeService integralGradeService;

    @ApiOperation("积分等级分页列表")
    @GetMapping("/page")
    public PccAjaxResult page(
            @ApiParam(value = "当前页码", required = false, defaultValue = "1")
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") long pageNum,
            @ApiParam(value = "每页记录数", required = false, defaultValue = "10")
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return new PccAjaxResult(200, "获取成功",
                integralGradeService.pageForAdmin(pageNum, pageSize));
    }

    @ApiOperation("新增积分等级")
    @PostMapping
    public PccAjaxResult add(@RequestBody Map<String, Object> body) {
        integralGradeService.addForAdmin(
                str(body.get("levelName")),
                intV(body.get("minScore")),
                intV(body.get("maxScore")),
                dec(body.get("borrowLimit")));
        return new PccAjaxResult(200, "新增成功");
    }

    @ApiOperation("修改积分等级")
    @PutMapping
    public PccAjaxResult update(@RequestBody Map<String, Object> body) {
        integralGradeService.updateForAdmin(
                Long.valueOf(String.valueOf(body.get("id"))),
                str(body.get("levelName")),
                intV(body.get("minScore")),
                intV(body.get("maxScore")),
                dec(body.get("borrowLimit")));
        return new PccAjaxResult(200, "修改成功");
    }

    @ApiOperation("删除积分等级")
    @DeleteMapping("/{id}")
    public PccAjaxResult delete(@ApiParam(value = "积分等级id", required = true) @PathVariable Long id) {
        integralGradeService.deleteByAdmin(id);
        return new PccAjaxResult(200, "删除成功");
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private Integer intV(Object v) {
        return v == null ? null : Integer.valueOf(String.valueOf(v));
    }

    private BigDecimal dec(Object v) {
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }
}
