package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.LendItem;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.vo.InvestmentAdminVO;
import com.wwfinance.api.entity.vo.LoanProjectAdminVO;
import com.wwfinance.api.mapper.LendItemMapper;
import com.wwfinance.api.mapper.LendMapper;
import com.wwfinance.api.mapper.UserMapper;
import com.wwfinance.api.service.LendItemReturnService;
import com.wwfinance.api.service.LendReturnService;
import com.wwfinance.api.service.LendService;
import com.wwfinance.api.service.LoanProjectService;
import com.wwfinance.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LoanProjectServiceImpl implements LoanProjectService {

    /** 状态：已下架 */
    private static final int STATUS_OFFLINE = 4;

    @Autowired
    private LendMapper lendMapper;

    @Autowired
    private LendService lendService;

    @Autowired
    private LendReturnService lendReturnService;

    @Autowired
    private LendItemReturnService lendItemReturnService;

    @Autowired
    private LendItemMapper lendItemMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Map<String, Object> pageForAdmin(long pageNum, long pageSize, String keyword, Integer status) {
        Page<Lend> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Lend> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("is_deleted = 0");
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            // 关键词匹配：标的名称 或 借款人姓名（user表关联）
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.apply("is_deleted = 0").like(User::getName, kw);
            List<Long> userIds = userMapper.selectList(userWrapper).stream()
                    .map(User::getId).collect(Collectors.toList());
            wrapper.and(w -> {
                w.like(Lend::getTitle, kw);
                if (!userIds.isEmpty()) {
                    w.or().in(Lend::getUserId, userIds);
                }
            });
        }
        if (status != null) {
            wrapper.eq(Lend::getStatus, status);
        }
        wrapper.orderByDesc(Lend::getCreateTime);
        Page<Lend> result = lendMapper.selectPage(pageParam, wrapper);

        // 关联 user 表取借款人姓名
        List<Long> userIds = result.getRecords().stream()
                .map(Lend::getUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords().stream()
                .map(l -> toAdminVO(l, userMap.get(l.getUserId())))
                .collect(Collectors.toList()));
        data.put("total", result.getTotal());
        return data;
    }

    @Override
    public void publishForAdmin(String title, Long borrowerId, BigDecimal amount,
                                BigDecimal rate, Integer term, String purpose, Integer riskLevel) {
        if (borrowerId == null) {
            throw new BusinessException("借款人ID不能为空");
        }
        User user = userMapper.selectById(borrowerId);
        if (user == null) {
            throw new BusinessException("借款人不存在");
        }
        Lend lend = new Lend();
        lend.setUserId(borrowerId);
        lend.setLendNo(generateLendNo());
        lend.setTitle(title);
        lend.setAmount(amount);
        lend.setPeriod(term);
        lend.setLendYearRate(rate == null ? null
                : rate.divide(BigDecimal.valueOf(100)));
        lend.setLendInfo(purpose);
        lend.setStatus(1);
        lend.setInvestAmount(BigDecimal.ZERO);
        lend.setInvestNum(0);
        lend.setPublishDate(LocalDateTime.now());
        lend.setDeleted(false);
        lendMapper.insert(lend);
        log.info("管理后台发布标的: id={}, title={}, borrowerId={}", lend.getId(), title, borrowerId);
    }

    @Override
    public void offlineByAdmin(Long id) {
        Lend lend = lendMapper.selectById(id);
        if (lend == null) {
            throw new BusinessException("标的不存在");
        }
        Lend update = new Lend();
        update.setId(id);
        update.setStatus(STATUS_OFFLINE);
        lendMapper.updateById(update);
        log.info("管理后台下架标的: id={}", id);
    }

    @Override
    public void loanByAdmin(Long id) {
        Lend lend = lendMapper.selectById(id);
        if (lend == null) {
            throw new BusinessException("标的不存在");
        }
        if (lend.getStatus() == null || lend.getStatus() != 2) {
            throw new BusinessException("仅满标状态的标的可以放款");
        }
        // 补齐满标后处理（均幂等）：还款计划 → 回款明细 → 放款
        lendReturnService.generateReturnPlan(lend);
        lendItemReturnService.generateReturnDetail(lend);
        lendService.makeLoan(id);
        log.info("管理后台放款成功: id={}", id);
    }

    @Override
    public List<Object> listInvestments(Long projectId) {        LambdaQueryWrapper<LendItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("is_deleted = 0").eq(LendItem::getLendId, projectId)
                .orderByDesc(LendItem::getInvestTime);
        List<LendItem> items = lendItemMapper.selectList(wrapper);
        // 关联 lend 取期限
        Lend lend = lendMapper.selectById(projectId);
        Integer period = lend == null ? null : lend.getPeriod();
        return items.stream().map(item -> toInvestVO(item, period)).collect(Collectors.toList());
    }

    private LoanProjectAdminVO toAdminVO(Lend l, User user) {
        BigDecimal amount = l.getAmount() == null ? BigDecimal.ZERO : l.getAmount();
        BigDecimal invested = l.getInvestAmount() == null ? BigDecimal.ZERO : l.getInvestAmount();
        int progress = amount.compareTo(BigDecimal.ZERO) > 0
                ? invested.multiply(BigDecimal.valueOf(100)).divide(amount, 0, RoundingMode.HALF_UP).intValue()
                : 0;
        // lend_year_rate 存小数(0.12=12%)，前端按百分数展示 ×100
        BigDecimal rate = l.getLendYearRate() == null ? null
                : l.getLendYearRate().multiply(BigDecimal.valueOf(100));
        return new LoanProjectAdminVO()
                .setId(l.getId())
                .setTitle(l.getTitle())
                .setBorrowerName(user == null ? "" : user.getName())
                .setBorrowerId(l.getUserId())
                .setAmount(amount)
                .setRate(rate)
                .setTerm(l.getPeriod())
                .setRaisedAmount(invested)
                .setProgress(progress)
                .setStatus(l.getStatus())
                .setPurpose(l.getLendInfo())
                .setRiskLevel(null)
                .setPublishTime(l.getPublishDate())
                .setEndTime(l.getLendEndDate())
                .setCreateTime(l.getCreateTime())
                .setRemark("");
    }

    private InvestmentAdminVO toInvestVO(LendItem item, Integer period) {
        // 后端状态(0默认 1已支付 2已还款) → 前端(0投资中 1持有中 2已退出 3已收益)
        Integer frontStatus;
        if (item.getStatus() == null) {
            frontStatus = 0;
        } else if (item.getStatus() == 2) {
            frontStatus = 3;
        } else {
            frontStatus = item.getStatus();
        }
        return new InvestmentAdminVO()
                .setId(item.getId())
                .setProjectId(item.getLendId())
                .setInvestorName(item.getInvestName())
                .setInvestorId(item.getInvestUserId())
                .setAmount(item.getInvestAmount())
                .setExpectedReturn(item.getExpectAmount())
                .setTerm(period)
                .setStatus(frontStatus)
                .setInvestTime(item.getInvestTime());
    }

    private String generateLendNo() {
        return "LEND" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format(Locale.ROOT, "%03d", (int) (Math.random() * 1000));
    }
}
