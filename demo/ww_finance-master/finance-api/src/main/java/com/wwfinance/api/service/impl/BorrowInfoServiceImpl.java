package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.BorrowInfo;
import com.wwfinance.api.entity.Borrower;
import com.wwfinance.api.entity.BorrowerAttach;
import com.wwfinance.api.entity.Dict;
import com.wwfinance.api.entity.IntegralGrade;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.LendReturn;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.vo.BorrowRecordAdminVO;
import com.wwfinance.api.enums.BorrowInfoStatusEnum;
import com.wwfinance.api.enums.BorrowerStatusEnum;
import com.wwfinance.common.exception.BusinessException;
import com.wwfinance.api.mapper.BorrowInfoMapper;
import com.wwfinance.api.mapper.BorrowerAttachMapper;
import com.wwfinance.api.mapper.BorrowerMapper;
import com.wwfinance.api.mapper.DictMapper;
import com.wwfinance.api.mapper.IntegralGradeMapper;
import com.wwfinance.api.mapper.LendMapper;
import com.wwfinance.api.mapper.LendReturnMapper;
import com.wwfinance.api.mapper.UserMapper;
import com.wwfinance.api.service.BorrowInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BorrowInfoServiceImpl extends ServiceImpl<BorrowInfoMapper, BorrowInfo> implements BorrowInfoService {

    @Autowired
    private BorrowerMapper borrowerMapper;

    @Autowired
    private BorrowerAttachMapper borrowerAttachMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DictMapper dictMapper;

    @Autowired
    private LendMapper lendMapper;

    @Autowired
    private LendReturnMapper lendReturnMapper;

    @Autowired
    private IntegralGradeMapper integralGradeMapper;

    /**
     * 获取借款申请审批状态：查该用户最近一条借款信息的 status
     */
    @Override
    public Integer getStatusByUserId(Long userId) {
        BorrowInfo borrowInfo = this.getOne(new LambdaQueryWrapper<BorrowInfo>()
                .eq(BorrowInfo::getUserId, userId)
                .orderByDesc(BorrowInfo::getId)
                .last("limit 1"));
        if (borrowInfo == null) {
            return BorrowInfoStatusEnum.NO_AUTH.getStatus(); // 0 未提交
        }
        return borrowInfo.getStatus();
    }

    /**
     * 获取最近一次借款申请的审核意见（拒绝原因）
     */
    @Override
    public String getAuditRemarkByUserId(Long userId) {
        BorrowInfo borrowInfo = this.getOne(new LambdaQueryWrapper<BorrowInfo>()
                .eq(BorrowInfo::getUserId, userId)
                .orderByDesc(BorrowInfo::getId)
                .last("limit 1"));
        return borrowInfo == null ? null : borrowInfo.getAuditRemark();
    }

    /**
     * 获取可借额度 = 收入档位额度 × 积分等级系数
     * 收入档位（月收入×12，单位：元）：
     *      1→3千×12=3.6万，2→8千×12=9.6万，3→2万×12=24万，4→4万×12=48万，
     *      5→8万×12=96万，6→15万×12=180万；
     *      7/8 为企业主专属档（大额经营贷，不按个人月收入×12）：
     *      7→500万，8→2000万。
     * 积分等级系数（integral_grade.borrow_coefficient，管理端可配置）：
     *      等级越高系数越大 → 可借额度越高（标准1.0 / 银卡1.1 / 金卡1.2 / 白金1.3 / 钻石1.5 / 黑金2.0）
     */
    @Override
    public BigDecimal getBorrowAmount(Long userId) {
        Borrower borrower = borrowerMapper.selectOne(new LambdaQueryWrapper<Borrower>()
                .eq(Borrower::getUserId, userId));
        if (borrower == null || borrower.getStatus() == null
                || borrower.getStatus() != BorrowerStatusEnum.AUTH_OK.getStatus()) {
            // 未认证或未认证通过：额度为 0
            return new BigDecimal(0);
        }
        BigDecimal amount = new BigDecimal(0);
        Integer income = borrower.getIncome() == null ? 0 : borrower.getIncome();
        switch (income) {
            case 1: amount = new BigDecimal(36000); break;
            case 2: amount = new BigDecimal(96000); break;
            case 3: amount = new BigDecimal(240000); break;
            case 4: amount = new BigDecimal(480000); break;
            case 5: amount = new BigDecimal(960000); break;
            case 6: amount = new BigDecimal(1800000); break;
            case 7: amount = new BigDecimal(5000000); break;
            case 8: amount = new BigDecimal(20000000); break;
            default: amount = new BigDecimal(0);
        }
        // 积分等级系数加成：等级越高额度越高
        BigDecimal coefficient = getGradeCoefficient(userId);
        if (coefficient != null) {
            amount = amount.multiply(coefficient).setScale(0, RoundingMode.HALF_UP);
        }
        return amount;
    }

    /**
     * 查询用户当前积分等级的可借额度系数（查不到按 1.0 处理）
     */
    private BigDecimal getGradeCoefficient(Long userId) {
        IntegralGrade grade = getCurrentGrade(userId);
        return grade == null || grade.getBorrowCoefficient() == null
                ? BigDecimal.ONE : grade.getBorrowCoefficient();
    }

    /**
     * 查询用户当前积分等级（按 User.integral 总积分匹配 integral_grade 区间）
     */
    private IntegralGrade getCurrentGrade(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null || user.getIntegral() == null) {
                return null;
            }
            long integral = user.getIntegral();
            List<IntegralGrade> grades = integralGradeMapper.selectList(
                    new LambdaQueryWrapper<IntegralGrade>().apply("is_deleted = 0"));
            if (grades != null) {
                for (IntegralGrade g : grades) {
                    if (g.getIntegralStart() != null && g.getIntegralEnd() != null
                            && integral >= g.getIntegralStart() && integral <= g.getIntegralEnd()) {
                        return g;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询用户积分等级失败: userId={}, err={}", userId, e.getMessage());
        }
        return null;
    }

    /**
     * 提交借款申请：校验可借额度 → 补充 userId，状态置为「审核中」，保存
     */
    @Override
    public void saveBorrowInfo(BorrowInfo borrowInfo, Long userId) {
        // 手册规则：仅借款人身份（userType=2）可申请借款，投资人无法借款
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getUserType() != null && user.getUserType() != 2) {
            throw new BusinessException("仅借款人身份可申请借款，投资人无法借款");
        }
        // 查重：已有审核中的借款申请时禁止重复提交（审核通过后可继续发起新借款）
        Integer pendingCount = this.baseMapper.selectCount(new LambdaQueryWrapper<BorrowInfo>()
                .eq(BorrowInfo::getUserId, userId)
                .eq(BorrowInfo::getStatus, BorrowInfoStatusEnum.CHECK_RUN.getStatus()));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("已有审核中的借款申请，请等待审核结果");
        }
        // 校验可借额度：收入档位 × 积分等级系数（未认证通过额度为 0）
        BigDecimal limit = getBorrowAmount(userId);
        if (borrowInfo.getAmount() == null || borrowInfo.getAmount().compareTo(limit) > 0) {
            throw new BusinessException("借款金额超过可借额度 " + limit + " 元");
        }
        // 利率下限：积分等级越高最低利率越低，申请利率不得低于等级利率
        BigDecimal gradeRate = getGradeRate(userId);
        if (gradeRate != null) {
            if (borrowInfo.getBorrowYearRate() == null) {
                borrowInfo.setBorrowYearRate(gradeRate);
            } else if (borrowInfo.getBorrowYearRate().compareTo(gradeRate) < 0) {
                borrowInfo.setBorrowYearRate(gradeRate);
            }
        }
        borrowInfo.setUserId(userId);
        borrowInfo.setStatus(BorrowInfoStatusEnum.CHECK_RUN.getStatus()); // 1 审核中
        this.save(borrowInfo);
    }

    /**
     * 查询用户当前积分等级的最低年利率（小数，查不到返回 null）
     */
    private BigDecimal getGradeRate(Long userId) {
        IntegralGrade grade = getCurrentGrade(userId);
        return grade == null ? null : grade.getBorrowRate();
    }

    // ==================== 管理后台 ====================

    @Override
    public Map<String, Object> pageForAdmin(long pageNum, long pageSize, String keyword, Integer status) {
        Page<BorrowInfo> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BorrowInfo> wrapper = new LambdaQueryWrapper<>();
        // 关键字：按借款人姓名过滤（关联 user 表取姓名，先按 userId 匹配）
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getName, kw))
                    .stream().map(User::getId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("list", java.util.Collections.emptyList());
                empty.put("total", 0L);
                return empty;
            }
            wrapper.in(BorrowInfo::getUserId, userIds);
        }
        // 状态过滤：前端 0-待审核 2-还款中 3-已结清 4-已拒绝 → 后端 0/1 审核中、2 通过、-1 失败
        if (status != null) {
            if (status == 4) {
                wrapper.eq(BorrowInfo::getStatus, -1);
            } else if (status == 2 || status == 3) {
                wrapper.eq(BorrowInfo::getStatus, 2);
            } else {
                wrapper.in(BorrowInfo::getStatus, 0, 1);
            }
        }
        wrapper.orderByDesc(BorrowInfo::getId);
        Page<BorrowInfo> result = this.page(pageParam, wrapper);
        List<BorrowRecordAdminVO> voList = result.getRecords().stream()
                .map(this::toAdminVO)
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", voList);
        data.put("total", result.getTotal());
        return data;
    }

    @Override
    public void auditByAdmin(Long id, Integer status, String rejectReason) {
        BorrowInfo borrowInfo = this.getById(id);
        if (borrowInfo == null) {
            throw new BusinessException("借款记录不存在");
        }
        // 前端 status：1-通过 4-拒绝 → 后端：2 通过 / -1 失败
        Integer targetStatus;
        if (status != null && status == 1) {
            targetStatus = 2;
        } else if (status != null && status == 4) {
            targetStatus = -1;
        } else {
            throw new BusinessException("非法的审核状态");
        }
        borrowInfo.setStatus(targetStatus);
        // 拒绝原因落库（管理端驳回时必填，用户端可查看）
        if (targetStatus == -1) {
            borrowInfo.setAuditRemark(rejectReason);
        }
        this.updateById(borrowInfo);
        // 审核通过 → 自动生成可投标的标的（BorrowInfo → Lend），进入募集中
        if (targetStatus == 2) {
            createLendByBorrowInfo(borrowInfo);
        }
    }

    /**
     * 审核通过后自动生成标的：
     *  - 同一借款申请只生成一次（按 borrow_info_id 幂等）
     *  - 金额/期限/利率/还款方式从借款申请带出
     *  - 借款用途经字典转中文存入 lend_info
     */
    private void createLendByBorrowInfo(BorrowInfo borrowInfo) {
        Integer exist = lendMapper.selectCount(new LambdaQueryWrapper<Lend>()
                .eq(Lend::getBorrowInfoId, borrowInfo.getId())
                .apply("is_deleted = 0"));
        if (exist != null && exist > 0) {
            log.info("借款申请已生成标的, 跳过: borrowInfoId={}", borrowInfo.getId());
            return;
        }
        User user = userMapper.selectById(borrowInfo.getUserId());
        Lend lend = new Lend();
        lend.setUserId(borrowInfo.getUserId());
        lend.setBorrowInfoId(borrowInfo.getId());
        lend.setLendNo("LEND" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format(Locale.ROOT, "%03d", (int) (Math.random() * 1000)));
        lend.setTitle((user == null ? "" : user.getName()) + "的借款标的");
        lend.setAmount(borrowInfo.getAmount());
        lend.setPeriod(borrowInfo.getPeriod());
        lend.setLendYearRate(borrowInfo.getBorrowYearRate());
        lend.setReturnMethod(borrowInfo.getReturnMethod());
        // 平台服务费率 1%（满标放款时按借款金额一次性扣除）
        lend.setServiceRate(new BigDecimal("0.01"));
        lend.setLendInfo(convertMoneyUse(borrowInfo.getMoneyUse()));
        // 风险等级：借款人评分基础档 + 借款金额修正（评分高/金额低 → 低风险）
        lend.setRiskLevel(calcRiskLevel(borrowInfo.getUserId(), borrowInfo.getAmount()));
        lend.setStatus(1); // 募集中
        lend.setInvestAmount(BigDecimal.ZERO);
        lend.setInvestNum(0);
        lend.setPublishDate(LocalDateTime.now());
        // 募集起止日期：发布日起始，+30 天募集截止
        LocalDate publishDate = LocalDate.now();
        lend.setLendStartDate(publishDate);
        lend.setLendEndDate(publishDate.plusDays(30));
        lend.setDeleted(false);
        lendMapper.insert(lend);
        log.info("审核通过自动生成标的: borrowInfoId={}, lendId={}, title={}",
                borrowInfo.getId(), lend.getId(), lend.getTitle());
    }

    /**
     * 风险等级评估：借款人认证评分基础档 + 借款金额修正
     *  基础档：评分≥190 低(1)、130~189 中(2)、&lt;130 高(3)
     *  金额修正：≤100万 +0、100万~1000万 +1、≥1000万 +2
     *  最终等级封顶 3（高风险）
     */
    private Integer calcRiskLevel(Long userId, BigDecimal amount) {
        int score = 0;
        try {
            Borrower borrower = borrowerMapper.selectOne(new LambdaQueryWrapper<Borrower>()
                    .eq(Borrower::getUserId, userId)
                    .last("limit 1"));
            if (borrower != null) {
                // 基本信息齐全 +30
                if (borrower.getAge() != null && borrower.getEducation() != null
                        && borrower.getIndustry() != null && borrower.getIncome() != null
                        && borrower.getReturnSource() != null) {
                    score += 30;
                }
                // 附件：身份证 +30、车辆 +60、房产 +100
                List<BorrowerAttach> attaches = borrowerAttachMapper.selectList(
                        new LambdaQueryWrapper<BorrowerAttach>()
                                .eq(BorrowerAttach::getBorrowerId, borrower.getId())
                                .eq(BorrowerAttach::getDeleted, false));
                boolean idCard = false, car = false, house = false;
                if (attaches != null) {
                    for (BorrowerAttach attach : attaches) {
                        String type = attach.getImageType();
                        if ("idCard1".equals(type) || "idCard2".equals(type)) {
                            idCard = true;
                        } else if ("car".equals(type)) {
                            car = true;
                        } else if ("house".equals(type)) {
                            house = true;
                        }
                    }
                }
                if (idCard) {
                    score += 30;
                }
                if (car) {
                    score += 60;
                }
                if (house) {
                    score += 100;
                }
            }
        } catch (Exception e) {
            log.warn("评估标的风险等级失败, userId={}, err={}", userId, e.getMessage());
        }
        // 基础档
        int base = score >= 190 ? 1 : (score >= 130 ? 2 : 3);
        // 金额修正：≤100万 +0；100万~1000万 +1；≥1000万 +2
        int up = 0;
        if (amount != null) {
            if (amount.compareTo(new BigDecimal("10000000")) >= 0) {
                up = 2;
            } else if (amount.compareTo(new BigDecimal("1000000")) > 0) {
                up = 1;
            }
        }
        int level = Math.min(base + up, 3);
        log.info("风险等级评估: userId={}, score={}, amount={}, level={}", userId, score, amount, level);
        return level;
    }

    /** 借款用途数字 → 字典中文（dict 表：父项 dict_code=moneyUse，子项按 parent_id+value 匹配） */    private String convertMoneyUse(Integer moneyUse) {
        if (moneyUse == null) {
            return "";
        }
        // 先按 dict_code 找父项（父项 value 为 NULL）
        Dict parent = dictMapper.selectOne(new LambdaQueryWrapper<Dict>()
                .eq(Dict::getDictCode, "moneyUse")
                .isNull(Dict::getValue)
                .last("limit 1"));
        if (parent == null) {
            return "";
        }
        // 再按 parent_id + value 找子项名称
        Dict child = dictMapper.selectOne(new LambdaQueryWrapper<Dict>()
                .eq(Dict::getParentId, parent.getId())
                .eq(Dict::getValue, moneyUse)
                .last("limit 1"));
        return child == null ? "" : child.getName();
    }

    /**
     * 借款记录 → 管理后台 VO（字段对齐 ww_finance_admin 前端 BorrowRecord 类型）
     */
    private BorrowRecordAdminVO toAdminVO(BorrowInfo info) {
        BorrowRecordAdminVO vo = new BorrowRecordAdminVO();
        vo.setId(info.getId());
        vo.setBorrowerId(info.getUserId());
        User user = userMapper.selectById(info.getUserId());
        vo.setBorrowerName(user == null ? "" : user.getName());
        vo.setAmount(info.getAmount());
        vo.setTerm(info.getPeriod());
        // 利率口径：库中存小数（0.08=8%），接口统一返回百分数（8），前端直接拼 "%"
        vo.setRate(info.getBorrowYearRate() == null ? null
                : info.getBorrowYearRate().multiply(new BigDecimal(100)));
        // 后端状态 → 前端状态
        Integer st = info.getStatus();
        if (st != null && st == 2) {
            vo.setStatus(1);            // 审核通过
        } else if (st != null && st == -1) {
            vo.setStatus(4);            // 已拒绝
        } else {
            vo.setStatus(0);            // 待审核（0 未提交 / 1 审核中）
        }
        // 资金用途：字典 dict_code=moneyUse 转中文
        vo.setPurpose(convertMoneyUse(info.getMoneyUse()));
        // 已还金额：该借款申请关联标的的还款计划中已还本金累计（status=1 表示已还）
        BigDecimal repaid = BigDecimal.ZERO;
        Lend relatedLend = lendMapper.selectOne(new LambdaQueryWrapper<Lend>()
                .eq(Lend::getBorrowInfoId, info.getId())
                .apply("is_deleted = 0")
                .last("limit 1"));
        if (relatedLend != null) {
            List<LendReturn> repaidReturns = lendReturnMapper.selectList(
                    new LambdaQueryWrapper<LendReturn>()
                            .eq(LendReturn::getLendId, relatedLend.getId())
                            .eq(LendReturn::getStatus, 1)
                            .apply("is_deleted = 0"));
            for (LendReturn r : repaidReturns) {
                if (r.getPrincipal() != null) {
                    repaid = repaid.add(r.getPrincipal());
                }
            }
        }
        vo.setRepayAmount(repaid);
        vo.setApplyTime(info.getCreateTime());
        vo.setAuditTime(info.getUpdateTime());
        // 应还清时间：关联标的还款计划中最后一期的还款日（按 return_date 取最大）
        String repayEndTime = "";
        if (relatedLend != null) {
            LendReturn lastReturn = lendReturnMapper.selectOne(new LambdaQueryWrapper<LendReturn>()
                    .eq(LendReturn::getLendId, relatedLend.getId())
                    .apply("is_deleted = 0")
                    .orderByDesc(LendReturn::getReturnDate)
                    .last("limit 1"));
            if (lastReturn != null && lastReturn.getReturnDate() != null) {
                repayEndTime = lastReturn.getReturnDate().toString();
            }
        }
        vo.setRepayEndTime(repayEndTime);
        vo.setRejectReason(info.getAuditRemark() == null ? "" : info.getAuditRemark());
        return vo;
    }
}
