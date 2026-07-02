package com.yfqb.lucky.repository;

import com.yfqb.lucky.model.po.LotteryDoubleBall;
import com.yfqb.lucky.model.po.LotteryDoubleBallNumber;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 双色球号码明细 Repository
 * <p>
 * 用于高效查询某个号码在最近 N 期中的出现次数。
 */
@Repository
public interface LotteryDoubleBallNumberRepository extends R2dbcRepository<LotteryDoubleBallNumber, Long> {

    /**
     * 查询某号码在指定日期范围内的出现次数
     */
    Mono<Integer> countByNumberAndTypeAndDrawDateBetween(Integer number, Integer type, LocalDate start, LocalDate end);

    /**
     * 查询某号码最近一次出现的记录
     */
    Mono<LotteryDoubleBallNumber> findTopByNumberAndTypeOrderByDrawDateDesc(Integer number, Integer type);

    /**
     * 查询某号码在指定日期范围内的明细
     */
    Flux<LotteryDoubleBallNumber> findByNumberAndTypeAndDrawDateBetweenOrderByDrawDateAsc(Integer number, Integer type, LocalDate start, LocalDate end);

    /**
     * 查询某期所有号码明细
     */
    Flux<LotteryDoubleBallNumber> findByPeriod(String period);

    /**
     * 删除某期的号码明细
     */
    Mono<Integer> deleteByPeriod(String period);

    // ==================== 筛选查询（JOIN 主表，一次返回完整数据） ====================

    /**
     * 查询包含指定红球号码的开奖记录（JOIN 主表，一次返回完整数据）
     * <p>
     * 利用 {@code idx_draw_date_number} 索引加速 {@code FIND_IN_SET} 筛选，
     * JOIN 主表直接返回 {@link LotteryDoubleBall} 完整记录，避免两步查询。
     */
    @Query("SELECT DISTINCT b.* FROM lottery_double_ball_number n " +
           "INNER JOIN lottery_double_ball b ON n.period = b.period " +
           "WHERE FIND_IN_SET(n.number, :redNumberList) AND n.type = 1 " +
           "GROUP BY n.period " +
           "HAVING COUNT(DISTINCT n.number) = :redCount " +
           "ORDER BY b.draw_date ASC, b.period ASC LIMIT :limit OFFSET :offset")
    Flux<LotteryDoubleBall> findJoinByRedBalls(String redNumberList, int redCount, int limit, int offset);

    /**
     * 统计包含指定红球号码的期数
     */
    @Query("SELECT COUNT(*) FROM (SELECT n.period FROM lottery_double_ball_number n " +
           "WHERE FIND_IN_SET(n.number, :redNumberList) AND n.type = 1 " +
           "GROUP BY n.period HAVING COUNT(DISTINCT n.number) = :redCount) t")
    Mono<Long> countByRedBalls(String redNumberList, int redCount);

    /**
     * 查询包含指定蓝球号码的开奖记录（JOIN 主表，一次返回完整数据）
     */
    @Query("SELECT DISTINCT b.* FROM lottery_double_ball_number n " +
           "INNER JOIN lottery_double_ball b ON n.period = b.period " +
           "WHERE n.number = :number AND n.type = 2 " +
           "ORDER BY b.draw_date ASC, b.period ASC LIMIT :limit OFFSET :offset")
    Flux<LotteryDoubleBall> findJoinByBlueBall(Integer number, int limit, int offset);

    /**
     * 统计包含指定蓝球号码的期数
     */
    @Query("SELECT COUNT(DISTINCT n.period) FROM lottery_double_ball_number n " +
           "WHERE n.number = :number AND n.type = 2")
    Mono<Long> countByBlueBall(Integer number);

    /**
     * 查询同时包含指定红球列表和蓝球号码的开奖记录（JOIN 主表，一次返回完整数据）
     */
    @Query("SELECT DISTINCT b.* FROM lottery_double_ball_number n " +
           "INNER JOIN lottery_double_ball_number nb ON n.period = nb.period AND nb.number = :blueBall AND nb.type = 2 " +
           "INNER JOIN lottery_double_ball b ON n.period = b.period " +
           "WHERE FIND_IN_SET(n.number, :redNumberList) AND n.type = 1 " +
           "GROUP BY n.period " +
           "HAVING COUNT(DISTINCT n.number) = :redCount " +
           "ORDER BY b.draw_date ASC, b.period ASC LIMIT :limit OFFSET :offset")
    Flux<LotteryDoubleBall> findJoinByRedBallsAndBlueBall(String redNumberList, int redCount, Integer blueBall, int limit, int offset);

    /**
     * 统计同时包含指定红球列表和蓝球号码的期数
     */
    @Query("SELECT COUNT(*) FROM (SELECT n.period FROM lottery_double_ball_number n " +
           "INNER JOIN lottery_double_ball_number nb ON n.period = nb.period AND nb.number = :blueBall AND nb.type = 2 " +
           "WHERE FIND_IN_SET(n.number, :redNumberList) AND n.type = 1 " +
           "GROUP BY n.period HAVING COUNT(DISTINCT n.number) = :redCount) t")
    Mono<Long> countByRedBallsAndBlueBall(String redNumberList, int redCount, Integer blueBall);
}
