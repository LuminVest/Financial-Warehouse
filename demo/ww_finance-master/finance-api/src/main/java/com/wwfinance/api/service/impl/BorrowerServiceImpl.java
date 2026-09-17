package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.Borrower;
import com.wwfinance.api.entity.BorrowerAttach;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.dto.BorrowerDTO;
import com.wwfinance.api.entity.vo.BorrowerAdminVO;
import com.wwfinance.api.enums.BorrowerStatusEnum;
import com.wwfinance.common.exception.BusinessException;
import com.wwfinance.api.mapper.BorrowerAttachMapper;
import com.wwfinance.api.mapper.BorrowerMapper;
import com.wwfinance.api.mapper.UserMapper;
import com.wwfinance.api.service.BorrowerService;
import com.wwfinance.api.service.UserIntegralService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BorrowerServiceImpl extends ServiceImpl<BorrowerMapper, Borrower> implements BorrowerService {

    @Autowired
    private BorrowerAttachMapper borrowerAttachMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserIntegralService userIntegralService;

    /**
     * 借款人认证提交：
     * 1. 添加 borrower（user_id 唯一索引：已存在则更新，否则新增）
     * 2. 添加 borrower_attach（附件）
     * 3. 更新 user 表（borrow_auth_status -> 认证中）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBorrowerVOByUserId(BorrowerDTO borrowerDTO, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 1. 添加 borrower：姓名/身份证/手机号取 user 表，其余认证信息来自 DTO
        Borrower borrower = new Borrower();
        BeanUtils.copyProperties(borrowerDTO, borrower);
        borrower.setUserId(user.getId());
        borrower.setName(user.getName());
        borrower.setIdCard(user.getIdCard());
        borrower.setMobile(user.getMobile());
        borrower.setStatus(BorrowerStatusEnum.AUTH_RUNNING.getStatus()); // 1 认证中
        // user_id 唯一：先查已存在记录
        Borrower exist = this.getOne(new LambdaQueryWrapper<Borrower>().eq(Borrower::getUserId, user.getId()));
        if (exist != null) {
            borrower.setId(exist.getId());
            this.updateById(borrower);
        } else {
            this.save(borrower);
        }

        // 2. 添加 borrower_attach（附件：身份证正反面、房产证、车等）
        List<BorrowerAttach> attachList = borrowerDTO.getBorrowerAttachList();
        if (attachList != null && !attachList.isEmpty()) {
            for (BorrowerAttach attach : attachList) {
                attach.setBorrowerId(borrower.getId());
                borrowerAttachMapper.insert(attach);
            }
        }

        // 3. 更新 user 表：借款人认证状态置为「认证中」
        user.setBorrowAuthStatus(BorrowerStatusEnum.AUTH_RUNNING.getStatus());
        userMapper.updateById(user);
    }

    @Override
    public Integer getStatusByUserId(Long userId) {
        LambdaQueryWrapper<Borrower> borrowerQueryWrapper = new LambdaQueryWrapper<>();
        borrowerQueryWrapper.select(Borrower::getStatus).eq(Borrower::getUserId, userId);
        Borrower borrower = this.getOne(borrowerQueryWrapper);
        return borrower == null ? null : borrower.getStatus();
    }

    // ==================== 管理后台 ====================

    @Override
    public Map<String, Object> pageForAdmin(long pageNum, long pageSize, String keyword, Integer auditStatus) {
        Page<Borrower> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Borrower> wrapper = new LambdaQueryWrapper<>();
        // 关键字：姓名/身份证/手机号 模糊匹配
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Borrower::getName, kw)
                    .or().like(Borrower::getIdCard, kw)
                    .or().like(Borrower::getMobile, kw));
        }
        // 审核状态：前端 0-待审核 1-通过 2-拒绝 → 后端 0/1 认证中、2 通过、-1 失败
        if (auditStatus != null) {
            if (auditStatus == 1) {
                wrapper.eq(Borrower::getStatus, 2);
            } else if (auditStatus == 2) {
                wrapper.eq(Borrower::getStatus, -1);
            } else {
                wrapper.in(Borrower::getStatus, 0, 1);
            }
        }
        Page<Borrower> result = this.page(pageParam, wrapper);
        List<BorrowerAdminVO> voList = result.getRecords().stream()
                .map(this::toAdminVO)
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", voList);
        data.put("total", result.getTotal());
        return data;
    }

    @Override
    public int auditByAdmin(Long id, Integer auditStatus, String remark) {
        Borrower borrower = this.getById(id);
        if (borrower == null) {
            throw new BusinessException("借款人不存在");
        }
        // 前端 auditStatus：1-通过 2-拒绝 → 后端 status：2 通过 / -1 失败
        Integer targetStatus;
        if (auditStatus != null && auditStatus == 1) {
            targetStatus = 2;
        } else if (auditStatus != null && auditStatus == 2) {
            targetStatus = -1;
        } else {
            throw new BusinessException("非法的审核状态");
        }
        // 审批前原始状态（用于积分防重判断，必须在 setStatus 之前取值）
        Integer oldStatus = borrower.getStatus();
        boolean needIntegral = targetStatus == 2 && (oldStatus == null || oldStatus != 2);
        borrower.setStatus(targetStatus);
        this.updateById(borrower);
        // 同步 user 表借款人认证状态
        User user = userMapper.selectById(borrower.getUserId());
        if (user != null) {
            user.setBorrowAuthStatus(targetStatus);
            userMapper.updateById(user);
        }
        // 审批通过 → 积分回写（状态机防重：已是通过状态不再加分）
        // 积分规则：基本信息 30 + 身份证 30 + 车辆 30 + 房产 100
        int score = 0;
        if (needIntegral) {
            score = calcBorrowerScore(id);
            if (score > 0) {
                userIntegralService.addIntegral(borrower.getUserId(), score, "借款人认证审批通过-" + id);
                log.info("借款人审批通过积分回写, borrowerId={}, userId={}, score={}", id, borrower.getUserId(), score);
            }
        }
        return score;
    }

    /**
     * 借款人认证积分计算：
     * 基本信息完整 30 + 身份证材料(idCard1/idCard2) 30 + 车辆材料(car) 30 + 房产材料(house) 100
     */
    private int calcBorrowerScore(Long borrowerId) {
        Borrower borrower = this.getById(borrowerId);
        int score = 0;
        if (borrower != null) {
            // 基本信息：年龄/学历/行业/月收入/还款来源 齐全
            if (borrower.getAge() != null && borrower.getEducation() != null
                    && borrower.getIndustry() != null && borrower.getIncome() != null
                    && borrower.getReturnSource() != null) {
                score += 30;
            }
            // 材料类：按附件 image_type 加分
            List<BorrowerAttach> attaches = borrowerAttachMapper.selectList(
                    new LambdaQueryWrapper<BorrowerAttach>().eq(BorrowerAttach::getBorrowerId, borrowerId));
            boolean idCardOk = false, carOk = false, houseOk = false;
            if (attaches != null) {
                for (BorrowerAttach attach : attaches) {
                    String type = attach.getImageType();
                    if ("idCard1".equals(type) || "idCard2".equals(type)) {
                        idCardOk = true;
                    } else if ("car".equals(type)) {
                        carOk = true;
                    } else if ("house".equals(type)) {
                        houseOk = true;
                    }
                }
            }
            if (idCardOk) {
                score += 30;
            }
            if (carOk) {
                score += 30;
            }
            if (houseOk) {
                score += 100;
            }
        }
        return score;
    }

    /**
     * 借款人记录 → 管理后台 VO（字段对齐 ww_finance_admin 前端 Borrower 类型）
     */
    private BorrowerAdminVO toAdminVO(Borrower borrower) {
        BorrowerAdminVO vo = new BorrowerAdminVO();
        vo.setId(borrower.getId());
        vo.setMemberId(borrower.getUserId());
        vo.setRealName(borrower.getName());
        vo.setIdCard(borrower.getIdCard());
        vo.setPhone(borrower.getMobile());
        vo.setGender(borrower.getSex());
        // 后端状态 → 前端审核状态
        Integer status = borrower.getStatus();
        if (status != null && status == 2) {
            vo.setAuditStatus(1);        // 通过
        } else if (status != null && status == -1) {
            vo.setAuditStatus(2);        // 拒绝
        } else {
            vo.setAuditStatus(0);        // 待审核（0 未认证 / 1 认证中）
        }
        // demo：额度体系/工作单位/月收入暂无，返回 0 或空
        vo.setCreditLimit(0);
        vo.setUsedLimit(0);
        vo.setBankCard("");
        vo.setEmployer("");
        vo.setMonthlyIncome(0);
        vo.setCreateTime(borrower.getCreateTime());
        vo.setAuditTime(borrower.getUpdateTime());
        vo.setRemark("");
        return vo;
    }
}
