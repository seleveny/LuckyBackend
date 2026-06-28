-- ============================================
-- LuckyBackend 双色球中奖记录表
-- ============================================

-- 双色球开奖记录表
CREATE TABLE IF NOT EXISTS `lottery_double_ball` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `period` VARCHAR(20) NOT NULL COMMENT '期号，如 2024068',
    `draw_date` DATE NOT NULL COMMENT '开奖日期',
    `weekday` VARCHAR(10) NOT NULL COMMENT '星期，如 星期二',
    `red_one` INT NOT NULL COMMENT '红球1 (1-33)',
    `red_two` INT NOT NULL COMMENT '红球2 (1-33)',
    `red_three` INT NOT NULL COMMENT '红球3 (1-33)',
    `red_four` INT NOT NULL COMMENT '红球4 (1-33)',
    `red_five` INT NOT NULL COMMENT '红球5 (1-33)',
    `red_six` INT NOT NULL COMMENT '红球6 (1-33)',
    `blue` INT NOT NULL COMMENT '蓝球 (1-16)',
    `pool_amount` VARCHAR(50) DEFAULT '0' COMMENT '奖池金额（元）',
    `first_prize_count` VARCHAR(20) DEFAULT '0' COMMENT '一等奖注数',
    `first_prize_amount` VARCHAR(50) DEFAULT '0' COMMENT '一等奖单注金额（元）',
    `second_prize_count` VARCHAR(20) DEFAULT '0' COMMENT '二等奖注数',
    `second_prize_amount` VARCHAR(50) DEFAULT '0' COMMENT '二等奖单注金额（元）',
    `sales_amount` VARCHAR(50) DEFAULT '0' COMMENT '本期销售额（元）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_period` (`period`),
    KEY `idx_draw_date` (`draw_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='双色球开奖记录表';

-- 双色球号码明细表（每期 7 行：6 个红球 + 1 个蓝球）
-- 用于高效查询某个号码在最近 N 期中的出现次数
CREATE TABLE IF NOT EXISTS `lottery_double_ball_number` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `period` VARCHAR(20) NOT NULL COMMENT '期号',
    `draw_date` DATE NOT NULL COMMENT '开奖日期',
    `number` INT NOT NULL COMMENT '号码 (1-33 红球, 1-16 蓝球)',
    `type` TINYINT NOT NULL COMMENT '类型：1-红球，2-蓝球',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_period_number_type` (`period`, `number`, `type`) COMMENT '同一期同一个号码+类型只能出现一次',
    KEY `idx_number` (`number`),
    KEY `idx_draw_date_number` (`draw_date`, `number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='双色球号码明细表';
