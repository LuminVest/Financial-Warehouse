package com.wwfinance.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwfinance.api.entity.Lend;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 标的 Mapper
 */
public interface LendMapper extends BaseMapper<Lend> {

    /**
     * 原子累加已投金额/人数（防并发超投）：
     * 仅当 当前已投 + 本次 <= 标的总额 时更新；满标时同步置状态为已满标(2)。
     * 返回 0 表示剩余额度不足（并发竞争时同样安全）。
     */
    @Update("UPDATE lend SET invest_amount = invest_amount + #{amount}, " +
            "invest_num = invest_num + 1, " +
            "status = IF(invest_amount + #{amount} >= amount, 2, status), " +
            "update_time = NOW() " +
            "WHERE id = #{lendId} AND is_deleted = 0 AND invest_amount + #{amount} <= amount")
    int addInvest(@Param("lendId") Long lendId, @Param("amount") BigDecimal amount);

}
