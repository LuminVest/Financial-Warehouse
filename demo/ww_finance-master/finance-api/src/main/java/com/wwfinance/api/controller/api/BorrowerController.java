package com.wwfinance.api.controller.api;

import com.wwfinance.api.entity.dto.BorrowerDTO;
import com.wwfinance.api.enums.BorrowerStatusEnum;
import com.wwfinance.api.service.BorrowerService;
import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "借款人认证")
@RestController
@RequestMapping("/api/core/borrower")
@Slf4j
public class BorrowerController {

    @Autowired
    private BorrowerService borrowerService;

    @ApiOperation("借款人认证提交")
    @PostMapping("/auth/save")
    public PccAjaxResult save(
            @ApiParam(value = "借款人认证信息", required = true) @RequestBody BorrowerDTO borrowerDTO) {
        Long userId = LoginUserContext.getUserid().longValue();
        borrowerService.saveBorrowerVOByUserId(borrowerDTO, userId);
        return new PccAjaxResult(200, "获取认证结果");
    }

    @ApiOperation("获取借款人认证状态")
    @GetMapping("/auth/getBorrowerStatus")
    public PccAjaxResult getBorrowerStatus() {
        Long userId = LoginUserContext.getUserid().longValue();
        Integer status = borrowerService.getStatusByUserId(userId);
        if (status == null) {
            return new PccAjaxResult(200, "获取认证结果", BorrowerStatusEnum.NO_AUTH.getStatus());
        }
        return new PccAjaxResult(200, "获取认证结果", status);
    }
}
