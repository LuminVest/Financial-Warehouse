package com.wwfinance.api.task;

import com.wwfinance.api.service.LendReturnService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时还款任务：
 * 教学大纲阶段五要求"定时还款/回款"（原设计为 XXL-Job，本项目以 Spring @Scheduled 实现同等效果）。
 * 每分钟扫描到期未还的还款计划：
 *  - 借款人本地账户余额充足 → 自动代扣还款，同步回款分润给投资人（含流水+积分）
 *  - 余额不足 → 标记该期逾期
 */
@Slf4j
@Component
public class LendReturnTask {

    @Autowired
    private LendReturnService lendReturnService;

    /**
     * 每分钟触发一次（cron：秒 分 时 日 月 周）
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void autoRepay() {
        try {
            lendReturnService.autoProcessDuePlans();
        } catch (Exception e) {
            log.error("定时还款任务异常: {}", e.getMessage(), e);
        }
    }
}
