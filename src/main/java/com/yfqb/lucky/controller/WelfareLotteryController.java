package com.yfqb.lucky.controller;

import com.yfqb.lucky.basic.IResult;
import com.yfqb.lucky.model.dto.SsqSearchDTO;
import com.yfqb.lucky.model.vo.LotteryDoubleBallVO;
import com.yfqb.lucky.model.vo.PageResult;
import com.yfqb.lucky.service.LotteryQueryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 福利彩票控制器
 * <p>
 * 提供双色球（SSQ）、福彩3D（FC3D）、七乐彩（QLC）等福利彩票的查询接口。
 *
 * @author xuchengcheng
 * @since 2026-04-23
 */
@RestController
@RequestMapping("/api/welfare")
public class WelfareLotteryController {

    @Resource
    private LotteryQueryService lotteryQueryService;

    // ==================== 双色球 ====================

    /**
     * 双色球开奖记录搜索
     * <p>
     * 支持按红球、蓝球号码筛选，以及分页查询。
     * 不传红球和蓝球时默认返回最新 100 条。
     *
     * @param dto 搜索参数（page、pageSize、redBall、blueBall）
     * @return 分页结果
     */
    @PostMapping("/ssq/search")
    public Mono<IResult<PageResult<LotteryDoubleBallVO>>> ssqSearch(@RequestBody SsqSearchDTO dto) {
        return lotteryQueryService.search(dto)
                .flatMap(IResult::success);
    }

    @GetMapping("/ssq/latest")
    public Mono<IResult<LotteryDoubleBallVO>> ssqLatest() {
        return lotteryQueryService.getLatest()
                .flatMap(IResult::success);
    }

    @GetMapping("/ssq/history")
    public Mono<IResult<String>> ssqHistory() {
        return IResult.success("双色球历史开奖");
    }

    @GetMapping("/ssq/period")
    public Mono<IResult<String>> ssqByPeriod() {
        return IResult.success("双色球按期号查询");
    }

    // ==================== 福彩3D ====================

    @GetMapping("/fc3d/latest")
    public Mono<IResult<String>> fc3dLatest() {
        return IResult.success("福彩3D最新开奖");
    }

    @GetMapping("/fc3d/history")
    public Mono<IResult<String>> fc3dHistory() {
        return IResult.success("福彩3D历史开奖");
    }

    // ==================== 七乐彩 ====================

    @GetMapping("/qlc/latest")
    public Mono<IResult<String>> qlcLatest() {
        return IResult.success("七乐彩最新开奖");
    }

    @GetMapping("/qlc/history")
    public Mono<IResult<String>> qlcHistory() {
        return IResult.success("七乐彩历史开奖");
    }
}
