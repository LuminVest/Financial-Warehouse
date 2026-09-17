package com.wwfinance.api.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwfinance.api.entity.BorrowInfo;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.LendReturn;
import com.wwfinance.api.entity.TransFlow;
import com.wwfinance.api.service.BorrowInfoService;
import com.wwfinance.api.service.LendReturnService;
import com.wwfinance.api.service.LendService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Resource
    private BorrowInfoService borrowInfoService;

    @Resource
    private LendService lendService;

    @Resource
    private LendReturnService lendReturnService;

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

    /**
     * 我的借款记录：当前用户的借款申请列表 + 关联标的（一个借款申请审核通过后生成一个标的）
     */
    @ApiOperation("我的借款记录")
    @GetMapping("/myBorrowInfo")
    public PccAjaxResult myBorrowInfo() {
        Long userId = LoginUserContext.getUserid().longValue();
        List<BorrowInfo> borrows = borrowInfoService.list(
                new LambdaQueryWrapper<BorrowInfo>()
                        .eq(BorrowInfo::getUserId, userId)
                        .apply("is_deleted = 0")
                        .orderByDesc(BorrowInfo::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (BorrowInfo b : borrows) {
            Map<String, Object> item = new HashMap<>();
            item.put("borrowInfo", b);
            Lend lend = lendService.getOne(
                    new LambdaQueryWrapper<Lend>()
                            .eq(Lend::getBorrowInfoId, b.getId())
                            .apply("is_deleted = 0")
                            .last("limit 1"));
            item.put("lend", lend);
            result.add(item);
        }
        return new PccAjaxResult(200, "获取成功", result);
    }

    /**
     * 我的还款计划：当前用户作为借款人发布的标的对应的还款计划
     */
    @ApiOperation("我的还款计划")
    @GetMapping("/myLendReturn")
    public PccAjaxResult myLendReturn() {
        Long userId = LoginUserContext.getUserid().longValue();
        List<Lend> lends = lendService.list(
                new LambdaQueryWrapper<Lend>()
                        .eq(Lend::getUserId, userId)
                        .apply("is_deleted = 0")
                        .orderByDesc(Lend::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Lend lend : lends) {
            Map<String, Object> item = new HashMap<>();
            item.put("lend", lend);
            item.put("returns", lendReturnService.listByLendId(lend.getId()));
            result.add(item);
        }
        return new PccAjaxResult(200, "获取成功", result);
    }
}
