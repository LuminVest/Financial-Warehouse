package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.LendItem;
import com.wwfinance.api.entity.dto.InvestDTO;

import java.util.List;
import java.util.Map;

/**
 * 投资记录 Service
 */
public interface LendItemService extends IService<LendItem> {

    /**
     * 某标的的投资记录列表（公开）
     */
    List<LendItem> getListByLendId(Long lendId);

    /**
     * 提交投资：校验 + 组装旺旺银行投标表单，返回 form 表单串
     */
    String commitInvest(InvestDTO investDTO, Long userId);

    /**
     * 投资异步回调：验签后写投资记录、扣减标的金额（满标置状态）
     * 返回纯文本 success / fail（托管平台约定）
     */
    String notify(Map<String, Object> paramMap);

}
