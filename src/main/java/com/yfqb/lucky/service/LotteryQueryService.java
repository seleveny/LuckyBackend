package com.yfqb.lucky.service;

import com.yfqb.lucky.model.dto.SsqSearchDTO;
import com.yfqb.lucky.model.po.LotteryDoubleBall;
import com.yfqb.lucky.model.vo.LotteryDoubleBallVO;
import com.yfqb.lucky.model.vo.PageResult;
import com.yfqb.lucky.repository.LotteryDoubleBallNumberRepository;
import com.yfqb.lucky.repository.LotteryDoubleBallRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 双色球开奖数据查询服务
 * <p>
 * 提供双色球开奖记录的分页搜索功能，支持按红球列表、蓝球号码筛选。
 * <p>
 * 优化说明：筛选查询使用 JOIN 方式一次返回完整开奖记录，避免先查期号再查主表的两步开销。
 */
@Slf4j
@Service
public class LotteryQueryService {

    @Resource
    private LotteryDoubleBallRepository ballRepository;

    @Resource
    private LotteryDoubleBallNumberRepository numberRepository;

    /**
     * 搜索双色球开奖记录
     * <p>
     * - 传了 redBalls 或 blueBall：通过号码明细表 JOIN 主表，一次查询返回完整数据
     * - 都不传：默认返回最新 N 条
     *
     * @param dto 搜索参数
     * @return 分页结果
     */
    public Mono<PageResult<LotteryDoubleBallVO>> search(SsqSearchDTO dto) {
        int page = dto.getPage() != null && dto.getPage() > 0 ? dto.getPage() : 1;
        int pageSize = dto.getPageSize() != null && dto.getPageSize() > 0 ? dto.getPageSize() : 100;
        int offset = (page - 1) * pageSize;

        List<Integer> redBalls = dto.getRedBalls();
        Integer blueBall = dto.getBlueBall();

        if ((redBalls != null && !redBalls.isEmpty()) || blueBall != null) {
            return searchWithFilter(redBalls, blueBall, pageSize, offset);
        } else {
            return searchLatest(pageSize, offset);
        }
    }

    /**
     * 按号码筛选查询（JOIN 方式，一次查询返回完整数据）
     */
    @SuppressWarnings("null")
    private Mono<PageResult<LotteryDoubleBallVO>> searchWithFilter(List<Integer> redBalls, Integer blueBall, int pageSize, int offset) {
        boolean hasRed = redBalls != null && !redBalls.isEmpty();
        boolean hasBlue = blueBall != null;

        Mono<List<LotteryDoubleBall>> recordsMono;
        Mono<Long> countMono;

        if (hasRed && hasBlue) {
            String redNumberList = toCommaSeparated(redBalls);
            int redCount = redBalls.size();
            recordsMono = numberRepository.findJoinByRedBallsAndBlueBall(redNumberList, redCount, blueBall, pageSize, offset)
                    .collectList();
            countMono = numberRepository.countByRedBallsAndBlueBall(redNumberList, redCount, blueBall);
        } else if (hasRed) {
            String redNumberList = toCommaSeparated(redBalls);
            int redCount = redBalls.size();
            recordsMono = numberRepository.findJoinByRedBalls(redNumberList, redCount, pageSize, offset)
                    .collectList();
            countMono = numberRepository.countByRedBalls(redNumberList, redCount);
        } else {
            recordsMono = numberRepository.findJoinByBlueBall(blueBall, pageSize, offset)
                    .collectList();
            countMono = numberRepository.countByBlueBall(blueBall);
        }

        return Mono.zip(recordsMono, countMono)
                .map(tuple -> new PageResult<>(1, pageSize, tuple.getT2(),
                        tuple.getT1().stream().map(this::toVO).toList()));
    }

    /**
     * 获取最新一期开奖记录
     *
     * @return 最新一期开奖记录，可能为空
     */
    public Mono<LotteryDoubleBallVO> getLatest() {
        return ballRepository.findTopByOrderByDrawDateDesc()
                .map(this::toVO);
    }

    /**
     * 查询最新 N 条（按期号从小到大排列）
     */
    private Mono<PageResult<LotteryDoubleBallVO>> searchLatest(int pageSize, int offset) {
        return Mono.zip(
                ballRepository.findLatestPage(pageSize, offset)
                        .map(this::toVO)
                        .collectSortedList((a, b) -> a.getPeriod().compareTo(b.getPeriod())),
                ballRepository.countAll()
        ).map(tuple -> new PageResult<>(1, pageSize, tuple.getT2(), tuple.getT1()));
    }

    /**
     * PO 转 VO
     */
    private LotteryDoubleBallVO toVO(LotteryDoubleBall po) {
        LotteryDoubleBallVO vo = new LotteryDoubleBallVO();
        BeanUtils.copyProperties(po, vo);
        return vo;
    }

    /**
     * 将 Integer 列表转为逗号分隔字符串
     */
    private static String toCommaSeparated(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
