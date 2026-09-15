package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alibaba.fastjson.JSON;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.UserAccount;
import com.wwfinance.api.entity.UserBind;
import com.wwfinance.api.mapper.LendMapper;
import com.wwfinance.api.service.LendService;
import com.wwfinance.api.service.TransFlowService;
import com.wwfinance.api.service.UserAccountService;
import com.wwfinance.api.service.UserBindService;
import com.wwfinance.api.service.UserIntegralService;
import com.wwfinance.api.service.UserService;
import com.wwfinance.api.utils.Amount1Helper;
import com.wwfinance.api.utils.Amount2Helper;
import com.wwfinance.api.utils.Amount3Helper;
import com.wwfinance.api.utils.Amount4Helper;
import com.wwfinance.api.utils.FormHelper;
import com.wwfinance.api.utils.HfbConst;
import com.wwfinance.api.utils.HttpPostUtil;
import com.wwfinance.api.utils.LendNoUtils;
import com.wwfinance.api.utils.RequestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LendServiceImpl extends ServiceImpl<LendMapper, Lend> implements LendService {

    @Autowired
    private UserService userService;

    @Autowired
    private UserBindService userBindService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private TransFlowService transFlowService;

    @Autowired
    private UserIntegralService userIntegralService;

    /**
     * 标的列表：未删除，按 id 倒序（最新在前）
     */
    @Override
    public List<Lend> getLendList() {
        return this.list(new LambdaQueryWrapper<Lend>()
                .eq(Lend::getDeleted, false)
                .orderByDesc(Lend::getId));
    }

    /**
     * 标的详情：返回 标的 + 借款人姓名/手机号 + 投资进度
     */
    @Override
    public Map<String, Object> getLendDetail(Long id) {
        Lend lend = this.getById(id);
        Map<String, Object> result = new HashMap<>();
        if (lend == null) {
            return result;
        }
        result.put("lend", lend);

        // 借款人信息
        User user = userService.getById(lend.getUserId());
        if (user != null) {
            result.put("borrowerName", user.getName());
            result.put("borrowerMobile", user.getMobile());
        }

        // 投资进度：已投/总额（百分比，保留两位）
        BigDecimal amount = lend.getAmount() == null ? BigDecimal.ZERO : lend.getAmount();
        BigDecimal invested = lend.getInvestAmount() == null ? BigDecimal.ZERO : lend.getInvestAmount();
        BigDecimal progress = (amount.compareTo(BigDecimal.ZERO) > 0)
                ? invested.multiply(new BigDecimal(100)).divide(amount, 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;
        result.put("investProgress", progress);
        return result;
    }

    /**
     * 投资收益计算器：按还款方式路由到对应利息工具（Amount1-4 对应 returnMethod 1-4）
     */
    @Override
    public BigDecimal getInterestCount(BigDecimal invest, BigDecimal yearRate, int totalmonth, int returnMethod) {
        BigDecimal interest;
        switch (returnMethod) {
            case 1:  // 等额本息
                interest = Amount1Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
            case 2:  // 等额本金
                interest = Amount2Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
            case 3:  // 每月还息一次还本
                interest = Amount3Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
            case 4:  // 一次还本
                interest = Amount4Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
            default:
                throw new RuntimeException("不支持的还款方式：" + returnMethod);
        }
        return interest == null ? BigDecimal.ZERO : interest;
    }

    /**
     * 推荐标的：demo 简化实现（按投资人数降序 + 未满标优先取 topN）。
     * 说明：接口文档标注为 Item-CF 协同过滤，真实场景可按用户历史投标行为计算相似度；
     * 当前演示口径按热度推荐，答辩可说明为简化版。
     */
    @Override
    public List<Lend> getRecommendList(int topN) {
        return this.list(new LambdaQueryWrapper<Lend>()
                .eq(Lend::getDeleted, false)
                .ne(Lend::getStatus, 2)        // 未满标的才推荐
                .orderByDesc(Lend::getInvestNum)
                .last("limit " + topN));
    }

    /**
     * 放款：满标后同步调用银行放款接口。
     * 银行逻辑：解冻全部投资人资金 → 放款额 = Σ投资额 - mchFee → 划转给借款人托管协议(benefitBindCode)
     * → 该标投资记录置已放款。我方：借款人本地账户入账 + 标的置已放款(3)。
     * 幂等：标的已放款(3) 直接跳过；银行侧 status=1 亦幂等。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void makeLoan(Long lendId) {
        Lend lend = this.getById(lendId);
        if (lend == null) {
            throw new RuntimeException("标的不存在");
        }
        if (lend.getStatus() != null && lend.getStatus() == LEND_STATUS_LOANED) {
            log.info("标的已放款, 跳过: lendId={}, lendNo={}", lendId, lend.getLendNo());
            return;
        }
        if (lend.getStatus() == null || lend.getStatus() != LEND_STATUS_FULL) {
            throw new RuntimeException("标的未满标，不能放款, lendId=" + lendId + ", status=" + lend.getStatus());
        }
        // 借款人托管绑定（银行按 benefitBindCode 放款到账）
        UserBind borrowBind = userBindService.getOne(new LambdaQueryWrapper<UserBind>()
                .eq(UserBind::getUserId, lend.getUserId())
                .apply("is_deleted = 0"));
        if (borrowBind == null || borrowBind.getBindCode() == null || borrowBind.getBindCode().isEmpty()) {
            throw new RuntimeException("借款人未绑定托管账户，无法放款");
        }

        // 组装旺旺银行放款参数（同步接口，参数 key 对齐银行 AgreeAccountLendProject）
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("agentProjectCode", lend.getLendNo());
        paramMap.put("agentBillNo", LendNoUtils.getLoanNo());
        paramMap.put("mchFee", "0");
        paramMap.put("hyFee", "0");
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        paramMap.put("sign", RequestHelper.getSign(paramMap));

        String resp = HttpPostUtil.postForm(HfbConst.MAKE_LOAD_URL, paramMap);
        Map<String, Object> result = JSON.parseObject(resp);
        if (result == null || !"0000".equals(String.valueOf(result.get("resultCode")))) {
            log.error("银行放款失败, lendId={}, resp={}", lendId, resp);
            throw new RuntimeException("银行放款失败: " + resp);
        }
        BigDecimal loanAmt = new BigDecimal(String.valueOf(result.get("voteAmt")));
        String loanNo = String.valueOf(paramMap.get("agentBillNo"));

        // 借款人本地账户入账（与银行托管划转同步）
        creditBorrower(lend.getUserId(), loanAmt);

        // 埋点：放款流水 + 积分（1元=1分，幂等键=放款单号）
        transFlowService.addFlow(lend.getUserId(), 5, loanNo, loanAmt, "放款到账：" + lend.getTitle());
        userIntegralService.addIntegral(lend.getUserId(), loanAmt.intValue(), "放款" + loanNo);

        // 标的置已放款 + 记录实际放款额
        lend.setRealAmount(loanAmt);
        lend.setStatus(LEND_STATUS_LOANED);
        this.updateById(lend);
        log.info("放款成功, lendId={}, lendNo={}, loanAmt={}", lendId, lend.getLendNo(), loanAmt);
    }

    /** 借款人放款入账（本地账本镜像，无账户则新建） */
    private void creditBorrower(Long userId, BigDecimal amount) {
        UserAccount account = userAccountService.getOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUserId, userId));
        if (account == null) {
            account = new UserAccount()
                    .setUserId(userId)
                    .setAmount(amount)
                    .setFreezeAmount(BigDecimal.ZERO)
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now())
                    .setDeleted(false)
                    .setVersion(1);
        } else {
            BigDecimal old = account.getAmount() == null ? BigDecimal.ZERO : account.getAmount();
            account.setAmount(old.add(amount));
            account.setUpdateTime(LocalDateTime.now());
        }
        userAccountService.saveOrUpdate(account);
        log.info("借款人放款到账, userId={}, 入账={}, 当前余额={}", userId, amount, account.getAmount());
    }

    /** 标的满标 */
    private static final int LEND_STATUS_FULL = 2;
    /** 标的已放款 */
    private static final int LEND_STATUS_LOANED = 3;
}
