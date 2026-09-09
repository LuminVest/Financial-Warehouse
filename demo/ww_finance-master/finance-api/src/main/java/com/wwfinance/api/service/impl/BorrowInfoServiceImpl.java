package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.BorrowInfo;
import com.wwfinance.api.entity.Borrower;
import com.wwfinance.api.entity.Dict;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.vo.BorrowRecordAdminVO;
import com.wwfinance.api.enums.BorrowInfoStatusEnum;
import com.wwfinance.api.enums.BorrowerStatusEnum;
import com.wwfinance.common.exception.BusinessException;
import com.wwfinance.api.mapper.BorrowInfoMapper;
import com.wwfinance.api.mapper.BorrowerMapper;
import com.wwfinance.api.mapper.DictMapper;
import com.wwfinance.api.mapper.UserMapper;
import com.wwfinance.api.service.BorrowInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BorrowInfoServiceImpl extends ServiceImpl<BorrowInfoMapper, BorrowInfo> implements BorrowInfoService {

    @Autowired
    private BorrowerMapper borrowerMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DictMapper dictMapper;

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
     * 获取可借额度：
     * 借款人认证通过后，根据月收入档位（income）映射可借额度。
     * 说明：额度档位为演示口径（1→5千，2→1万，3→3万，4→5万），可按老师要求调整。
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
            case 1: amount = new BigDecimal(5000); break;
            case 2: amount = new BigDecimal(10000); break;
            case 3: amount = new BigDecimal(30000); break;
            case 4: amount = new BigDecimal(50000); break;
            default: amount = new BigDecimal(0);
        }
        return amount;
    }

    /**
     * 提交借款申请：补充 userId，状态置为「审核中」，保存
     */
    @Override
    public void saveBorrowInfo(BorrowInfo borrowInfo, Long userId) {
        borrowInfo.setUserId(userId);
        borrowInfo.setStatus(BorrowInfoStatusEnum.CHECK_RUN.getStatus()); // 1 审核中
        this.save(borrowInfo);
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
        this.updateById(borrowInfo);
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
        vo.setRate(info.getBorrowYearRate());
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
        String purpose = "";
        if (info.getMoneyUse() != null) {
            Dict dict = dictMapper.selectOne(new LambdaQueryWrapper<Dict>()
                    .eq(Dict::getDictCode, "moneyUse")
                    .eq(Dict::getValue, info.getMoneyUse())
                    .last("limit 1"));
            if (dict != null) {
                purpose = dict.getName();
            }
        }
        vo.setPurpose(purpose);
        vo.setRepayAmount(new BigDecimal(0));
        vo.setApplyTime(info.getCreateTime());
        vo.setAuditTime(info.getUpdateTime());
        vo.setRepayEndTime("");
        vo.setRejectReason("");
        return vo;
    }
}
