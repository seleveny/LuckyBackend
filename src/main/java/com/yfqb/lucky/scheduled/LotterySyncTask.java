package com.yfqb.lucky.scheduled;

import com.yfqb.lucky.config.LotteryApiConfig;
import com.yfqb.lucky.service.LotterySyncService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 双色球开奖数据定时同步任务
 * <p>
 * 双色球每周二、四、日开奖（21:15），定时任务在 21:30 拉取最新数据。
 * 如果官网尚未更新，则每分钟重试一次，最多重试 30 次（到 22:00）。
 * <p>
 * 首次启动时自动拉取全部历史数据（2003年至今）。
 * 服务器宕机恢复后，{@link #initHistory()} 会自动补全缺失期数。
 */
@Slf4j
@Component
public class LotterySyncTask {

    /** 双色球开奖日：周二、周四、周日 */
    private static final Set<DayOfWeek> DRAW_DAYS = Set.of(
            DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY
    );

    @Resource
    private LotterySyncService lotterySyncService;

    @Resource
    private LotteryApiConfig lotteryApiConfig;

    /** 重试最大次数 */
    private static final int MAX_RETRIES = 30;

    /** 标记当天是否已拉取到最新开奖数据 */
    private final AtomicBoolean todaySynced = new AtomicBoolean(false);

    /** 当前重试次数 */
    private int retryCount = 0;

    /** 标记 initHistory 是否正在执行，防止与定时任务并发 */
    private final AtomicBoolean historyRunning = new AtomicBoolean(false);

    /**
     * 开奖日 21:30 执行首次同步
     * <p>
     * 只在周二、四、日执行。如果 21:30 官网尚未更新，由 {@link #syncLatestRetry()} 每分钟重试。
     */
    @Scheduled(cron = "0 30 21 * * 2,4,7")
    public void syncLatestFirst() {
        if (!isDrawDay()) {
            return;
        }
        // 如果历史数据正在补全中，跳过本次同步
        if (historyRunning.get()) {
            log.info("历史数据正在补全中，跳过 21:30 首次同步");
            return;
        }
        log.info("开奖日 21:30 首次同步开始");
        doSync();
    }

    /**
     * 开奖日 21:31~22:00 每分钟重试（最多 {@link #MAX_RETRIES} 次），直到拉取到当天数据
     */
    @Scheduled(cron = "0 31-59 21 * * 2,4,7")
    public void syncLatestRetry() {
        // 已同步成功或已过重试窗口（22:00）或已达重试上限，跳过
        if (todaySynced.get() || LocalTime.now().getHour() >= 22 || retryCount >= MAX_RETRIES) {
            return;
        }
        // 如果历史数据正在补全中，跳过本次重试
        if (historyRunning.get()) {
            log.info("历史数据正在补全中，跳过本次重试");
            return;
        }
        retryCount++;
        log.info("重试同步（第 {}/{} 次）：尝试获取当天开奖数据", retryCount, MAX_RETRIES);
        doSync();
    }

    /**
     * 执行一次同步，成功则标记当天已完成
     */
    private void doSync() {
        lotterySyncService.syncLatest()
                .doOnSuccess(found -> {
                    if (found) {
                        log.info("成功拉取到当天双色球开奖数据");
                        todaySynced.set(true);
                    } else {
                        log.info("官网尚未更新当天数据，稍后重试");
                    }
                })
                .doOnError(e -> {
                    log.error("同步失败: {}，稍后重试", e.getMessage());
                    retryCount++; // 网络故障也算一次重试，防止无限重试
                })
                .subscribe();
    }

    /**
     * 判断当天是否为双色球开奖日
     */
    private boolean isDrawDay() {
        return DRAW_DAYS.contains(LocalDate.now().getDayOfWeek());
    }

    /**
     * 首次启动时补全全部历史数据（2003年至今）
     * <p>
     * 如果数据库已有部分数据，则自动从最新一期之后拉取所有缺失期数。
     * 这保证了服务器宕机后重启能自动补上遗漏的开奖数据。
     */
    @PostConstruct
    public void initHistory() {
        if (!lotteryApiConfig.isHistoryEnable()) {
            return;
        }

        log.info("首次启动，开始拉取全部双色球历史数据（2003年至今）...");
        historyRunning.set(true);
        lotterySyncService.syncHistory(0)
                .doOnSuccess(v -> {
                    log.info("全部历史数据补全完成");
                    historyRunning.set(false);
                })
                .doOnError(e -> {
                    log.error("历史数据补全失败: {}", e.getMessage());
                    historyRunning.set(false);
                })
                .subscribe();
    }
}
