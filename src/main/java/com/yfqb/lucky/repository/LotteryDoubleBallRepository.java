package com.yfqb.lucky.repository;

import com.yfqb.lucky.model.po.LotteryDoubleBall;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 双色球开奖记录 Repository
 */
@Repository
public interface LotteryDoubleBallRepository extends R2dbcRepository<LotteryDoubleBall, Long> {

    /**
     * 按期号查询
     */
    Mono<LotteryDoubleBall> findByPeriod(String period);

    /**
     * 按日期范围查询
     */
    Flux<LotteryDoubleBall> findByDrawDateBetween(LocalDate start, LocalDate end);

    /**
     * 查询最新一期
     */
    Mono<LotteryDoubleBall> findTopByOrderByDrawDateDesc();

    /**
     * 查询最早一期
     */
    Mono<LotteryDoubleBall> findTopByOrderByDrawDateAsc();

    /**
     * 查询最近 N 期，按开奖日期倒序（取最新的 N 条）
     */
    @Query("SELECT * FROM lottery_double_ball ORDER BY draw_date DESC, period DESC LIMIT :limit OFFSET :offset")
    Flux<LotteryDoubleBall> findLatestPage(int limit, int offset);

    /**
     * 查询最近 N 期的总数
     */
    @Query("SELECT COUNT(*) FROM lottery_double_ball")
    Mono<Long> countAll();

}
