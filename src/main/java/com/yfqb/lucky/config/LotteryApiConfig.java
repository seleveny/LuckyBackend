package com.yfqb.lucky.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 彩票开奖 API 配置
 * <p>
 * 默认使用中国福利彩票官方网站 JSON 接口（无需注册、无需 key），
 * 如需替换为其他 API 提供方，可通过配置文件覆盖。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "lottery.api")
public class LotteryApiConfig {

    /** 中国福彩网双色球开奖数据接口地址 */
    private String url = "https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice";

    /** 彩票种类，ssq=双色球 */
    private String name = "ssq";

    /** 系统类型，固定 PC */
    private String systemType = "PC";

    /** 每页条数（最大 100） */
    private int pageSize = 100;

    /** 同步任务 cron 表达式，默认每天 21:30 执行（双色球 21:15 开奖） */
    private String cron = "0 30 21 * * ?";

    /** 是否启用历史数据补全（首次启动时拉取全部历史数据） */
    private boolean historyEnable = true;
}
