package com.wwfinance.api.controller.api;

import com.wwfinance.api.entity.BorrowInfo;
import com.wwfinance.api.service.BorrowInfoService;
import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Api(tags = "借款信息")
@RestController
@RequestMapping("/api/core/borrowInfo")
@Slf4j
public class BorrowInfoController {

    @Autowired
    private BorrowInfoService borrowInfoService;

    @ApiOperation("获取当前登录用户的借款申请审批状态")
    @GetMapping("/auth/getBorrowInfoStatus")
    public PccAjaxResult getBorrowInfoStatus() {
        Long userId = LoginUserContext.getUserid().longValue();
        Integer status = borrowInfoService.getStatusByUserId(userId);
        return new PccAjaxResult(200, "获取借款申请审批状态", status);
    }

    @ApiOperation("获取当前登录用户的借款额度")
    @GetMapping("/auth/getBorrowAmount")
    public PccAjaxResult getBorrowAmount() {
        Long userId = LoginUserContext.getUserid().longValue();
        BigDecimal borrowAmount = borrowInfoService.getBorrowAmount(userId);
        return new PccAjaxResult(200, "获取借款额度", borrowAmount);
    }

    @ApiOperation("提交借款申请")
    @PostMapping("/auth/save")
    public PccAjaxResult save(
            @ApiParam(value = "借款信息", required = true) @RequestBody BorrowInfo borrowInfo) {
        Long userId = LoginUserContext.getUserid().longValue();
        borrowInfoService.saveBorrowInfo(borrowInfo, userId);
        return new PccAjaxResult(200, "提交成功");
    }
}
