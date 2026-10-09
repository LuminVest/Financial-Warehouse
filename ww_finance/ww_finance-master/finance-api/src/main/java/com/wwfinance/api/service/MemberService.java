package com.wwfinance.api.service;

import java.util.Map;

/**
 * 管理后台-会员服务
 */
public interface MemberService {

    /**
     * 管理后台：会员分页列表（关键词+状态筛选，积分匹配等级）
     *
     * @return {list: [MemberAdminVO], total}
     */
    Map<String, Object> pageForAdmin(long pageNum, long pageSize, String keyword, Integer status);

    /**
     * 管理后台：新增会员（初始密码 123456）
     */
    void addForAdmin(String phone, String nickname, String realName, String idCard,
                     Integer gender, Integer score, String remark);

    /**
     * 管理后台：启用/禁用会员
     */
    void updateStatus(Long id, Integer status);
}
