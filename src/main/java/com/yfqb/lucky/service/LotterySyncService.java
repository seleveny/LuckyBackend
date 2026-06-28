package com.yfqb.lucky.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yfqb.lucky.config.LotteryApiConfig;
import com.yfqb.lucky.model.po.LotteryDoubleBall;
import com.yfqb.lucky.model.po.LotteryDoubleBallNumber;
import com.yfqb.lucky.repository.LotteryDoubleBallNumberRepository;
import com.yfqb.lucky.repository.LotteryDoubleBallRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 双色球开奖数据同步服务
 * <p>
 * 调用中国福利彩票官方网站 JSON 接口获取双色球开奖数据，写入数据库。
 * 支持单期查询、按期号查询、以及分页拉取全部历史数据。
 */
@Slf4j
@Service
public class LotterySyncService {

    @Resource
    private LotteryApiConfig lotteryApiConfig;
    @Resource
    private LotteryDoubleBallRepository ballRepository;
    @Resource
    private LotteryDoubleBallNumberRepository numberRepository;
    @Resource
    private WebClient cwlWebClient;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private TransactionalOperator transactionalOperator;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 同步最新一期双色球开奖数据
     * <p>
     * 请求 API 第 1 页取第 1 条数据，判断其开奖日期是否为当天。
     * 如果是则写入数据库（幂等），返回 true；否则返回 false（官网尚未更新）。
     * <p>
     * 注意：此方法只处理单期数据，不翻页。补全多期缺失数据请调用 {@link #syncHistory(int)}。
     *
     * @return true = 拉取到当天开奖数据并写入成功；false = 官网尚未更新到当天数据
     */
    public Mono<Boolean> syncLatest() {
        return fetchPage(1)
                .flatMap(pageData -> {
                    if (pageData.results.isEmpty()) {
                        log.warn("API 返回数据为空");
                        return Mono.just(false);
                    }
                    LotteryResult latest = pageData.results.get(0);
                    LocalDate today = LocalDate.now();

                    log.info("API 最新一期: {}（{}）", latest.period, latest.drawDate);

                    if (!today.equals(latest.drawDate)) {
                        log.info("API 最新一期日期 {} 不是今天 {}，官网尚未更新", latest.drawDate, today);
                        return Mono.just(false);
                    }

                    log.info("API 最新一期日期是今天，开始写入");
                    return saveLotteryData(latest).thenReturn(true);
                });
    }

    /**
     * 同步指定期号的双色球开奖数据
     *
     * @param period 期号，如 "2026072"
     */
    public Mono<Void> syncByPeriod(String period) {
        return fetchPage(1)
                .flatMapMany(pageData -> Flux.fromIterable(pageData.results))
                .filter(r -> period.equals(r.period))
                .next()
                .switchIfEmpty(Mono.error(new RuntimeException("未找到期号 " + period + " 的数据")))
                .flatMap(this::saveLotteryData)
                .then();
    }

    /**
     * 补全缺失的历史数据（智能增量同步）
     * <p>
     * - 如果数据库中没有数据，则拉取福彩网全部历史数据（2003年至今）
     * - 如果数据库中已有部分数据，则从数据库最新一期之后开始，拉取所有缺失的期数
     *   （防止服务宕机期间漏掉多期数据）
     * <p>
     * 已存在的期号自动跳过（幂等写入）。
     *
     * @param count 参数已废弃，保留仅用于兼容
     */
    /**
     * 双色球第一期的期号（2003 年第 1 期）
     */
    private static final String FIRST_PERIOD = "2003001";

    public Mono<Void> syncHistory(int count) {
        return ballRepository.findTopByOrderByDrawDateAsc()
                .defaultIfEmpty(new LotteryDoubleBall())
                .flatMap(oldest -> {
                    if (oldest.getPeriod() == null) {
                        // 数据库为空，拉取全部历史
                        log.info("数据库无历史数据，开始拉取全部双色球历史数据（2003年至今）...");
                        return syncAllHistory();
                    }

                    // 数据库已有数据，检查最小期号
                    if (!FIRST_PERIOD.equals(oldest.getPeriod())) {
                        // 最小期号不是 2003001，说明历史数据未拉全（可能上次中断），重新拉取全部
                        log.warn("数据库最小期号为 {}，不是第一期 {}，历史数据不完整，重新拉取全部历史数据",
                                oldest.getPeriod(), FIRST_PERIOD);
                        // 先清空已有数据，避免重复
                        return ballRepository.deleteAll()
                                .then(numberRepository.deleteAll())
                                .then(syncAllHistory());
                    }

                    // 最小期号是 2003001，历史数据完整，从最新一期之后拉取缺失数据
                    return ballRepository.findTopByOrderByDrawDateDesc()
                            .flatMap(latest -> {
                                log.info("数据库最新期号为 {}，开始拉取缺失数据...", latest.getPeriod());
                                return syncFromLatest(latest.getPeriod()).then();
                            });
                });
    }

    /**
     * 从 API 第1页（最新数据）开始逐页写入比数据库最新期号更新的数据，
     * 遇到已存在的期号则停止。
     * <p>
     * 每页 100 条（覆盖约 8 个月），绝大多数情况下只需请求第1页即可完成同步。
     * 写入顺序严格从新到旧。
     * <p>
     * 例如：数据库最新是 2026050，第1页返回 [2026072...2026001]
     * → takeWhile 写入 2026072 → ... → 2026051（比 2026050 新的）
     * → 遇到 2026050 → 停止
     *
     * @return true = 拉取到了新数据；false = 已是最新且 API 数据已就绪
     */
    private Mono<Boolean> syncFromLatest(String latestPeriod) {
        return syncPagesFrom(1, latestPeriod)
                .doOnSuccess(hasNew -> {
                    if (hasNew) {
                        log.info("缺失数据同步完成");
                    } else {
                        log.info("已是最新数据，无需同步");
                    }
                })
                .doOnError(e -> log.error("缺失数据同步失败: {}", e.getMessage()));
    }

    /**
     * 从指定页开始，逐页写入比 latestPeriod 更新的数据，遇到以下任一条件停止：
     * - 本页中 takeWhile 没有处理任何数据（说明后续页更旧，无需继续）
     * - 本页已包含目标期号
     * - 已到最后一页
     * <p>
     * 当 takeWhile 没有新数据时，会额外检查 API 最新一期是否接近当天日期。
     * 如果最新一期日期远早于今天（如 3 天前），说明官网尚未更新最新开奖数据，
     * 抛出异常让调用方继续重试。
     *
     * @return true = 拉取到了新数据；false = 已是最新且 API 数据已就绪
     */
    private Mono<Boolean> syncPagesFrom(int pageNo, String latestPeriod) {
        return fetchPage(pageNo)
                .flatMap(pageData -> {
                    List<LotteryResult> results = pageData.results;
                    log.info("处理第 {} 页数据", pageNo);

                    List<LotteryResult> toSave = results.stream()
                            .takeWhile(r -> r.period.compareTo(latestPeriod) > 0)
                            .toList();

                    if (toSave.isEmpty()) {
                        // takeWhile 一条都没处理，说明本页第一条已经 <= latestPeriod
                        // 但需要确认 API 数据是否已更新到最新一期
                        return handleNoNewData(results);
                    }

                    return Flux.fromIterable(toSave)
                            .concatMap(this::saveLotteryData)
                            .onErrorContinue((e, obj) ->
                                    log.error("保存期号 {} 数据失败: {}，跳过继续", obj, e.getMessage()))
                            .then(Mono.defer(() -> {
                                // 本页已包含目标期号，或已到最后一页，停止
                                boolean found = results.stream().anyMatch(r -> r.period.equals(latestPeriod));
                                if (found || pageNo >= pageData.totalPages) {
                                    return Mono.just(true);
                                }
                                // 继续翻下一页
                                return syncPagesFrom(pageNo + 1, latestPeriod);
                            }));
                })
                .onErrorResume(e -> {
                    log.error("处理第 {} 页失败: {}，跳过该页继续", pageNo, e.getMessage());
                    return syncPagesFrom(pageNo + 1, latestPeriod);
                });
    }

    /**
     * 当翻页遇到无新数据时，检查 API 最新一期是否接近当天日期。
     * <p>
     * 如果最新一期是当天或前一天，说明官网数据已就绪，只是数据库已是最新，返回 false（停止重试）。
     * 如果最新一期远早于今天（如 3 天前），说明官网尚未更新，抛异常让调用方继续重试。
     */
    private Mono<Boolean> handleNoNewData(List<LotteryResult> results) {
        if (results.isEmpty()) {
            log.warn("API 返回数据为空，无法判断是否已是最新");
            return Mono.error(new RuntimeException("API 返回数据为空"));
        }

        LotteryResult latestApi = results.get(0);
        LocalDate today = LocalDate.now();
        long daysDiff = today.toEpochDay() - latestApi.drawDate.toEpochDay();

        log.info("API 最新一期: {}（{}），距离今天 {} 天", latestApi.period, latestApi.drawDate, daysDiff);

        if (daysDiff <= 2) {
            // 最新一期是当天或前两天内（双色球周二四日开奖，间隔最多 2 天），说明 API 已就绪
            log.info("API 数据已更新到最新（{}），数据库已是最新，无需继续重试", latestApi.drawDate);
            return Mono.just(false);
        } else {
            // 最新一期远早于今天，说明官网还未更新最新开奖数据
            log.warn("API 最新一期日期 {} 距今天 {} 天，官网可能尚未更新最新开奖数据，需要继续重试",
                    latestApi.drawDate, daysDiff);
            return Mono.error(new RuntimeException(
                    "API 最新一期为 " + latestApi.period + "（" + latestApi.drawDate + "），尚未更新到最新"));
        }
    }

    /**
     * 拉取全部历史数据（分页）
     * <p>
     * 逐页拉取，某一页出错不会中断后续页的处理，仅记录错误日志后继续下一页。
     */
    private Mono<Void> syncAllHistory() {
        return fetchPage(1)
                .flatMap(firstPage -> {
                    int totalPages = firstPage.totalPages;
                    log.info("共 {} 页历史数据，开始逐页同步", totalPages);

                    // 先处理第1页
                    return processPageResults(firstPage.results)
                            .onErrorResume(e -> {
                                log.error("第 1/{} 页数据处理失败: {}", totalPages, e.getMessage());
                                return Mono.empty();
                            })
                            .then(Mono.defer(() -> {
                                // 再处理剩余页（第2页到最后一页）
                                if (totalPages <= 1) {
                                    log.info("全部历史数据同步完成");
                                    return Mono.empty();
                                }
                                return Flux.range(2, totalPages - 1)
                                        .concatMap(pageNo -> processSinglePage(pageNo, totalPages))
                                        .then();
                            }));
                })
                .doOnSuccess(v -> log.info("全部双色球历史数据同步完成"))
                .doOnError(e -> log.error("历史数据同步失败: {}", e.getMessage()));
    }

    /**
     * 处理单页数据，出错不中断后续页
     */
    private Mono<Void> processSinglePage(int pageNo, int totalPages) {
        return fetchPage(pageNo)
                .flatMap(pageData -> {
                    log.info("同步第 {}/{} 页数据（共 {} 条）", pageNo, totalPages, pageData.results.size());
                    return processPageResults(pageData.results);
                })
                .doOnSuccess(v -> log.info("第 {}/{} 页同步完成", pageNo, totalPages))
                .doOnError(e -> log.error("第 {}/{} 页同步失败: {}", pageNo, totalPages, e.getMessage()))
                .onErrorResume(e -> Mono.empty()); // 出错后继续下一页
    }

    /**
     * 批量处理一页的解析结果
     */
    private Mono<Void> processPageResults(List<LotteryResult> results) {
        return Flux.fromIterable(results)
                .concatMap(this::saveLotteryData)
                .then();
    }

    /**
     * 调用福彩网官方接口获取指定页的数据
     *
     * @param pageNo 页码，从1开始
     */
    private Mono<PageData> fetchPage(int pageNo) {
        return cwlWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("www.cwl.gov.cn")
                        .path("/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice")
                        .queryParam("name", lotteryApiConfig.getName())
                        .queryParam("pageNo", pageNo)
                        .queryParam("pageSize", lotteryApiConfig.getPageSize())
                        .queryParam("systemType", lotteryApiConfig.getSystemType())
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        int state = root.get("state").asInt();
                        if (state != 0) {
                            String msg = root.has("message") ? root.get("message").asText() : "未知错误";
                            log.error("福彩网接口返回错误: state={}, message={}", state, msg);
                            return Mono.error(new RuntimeException("福彩网接口返回错误: " + msg));
                        }

                        int total = root.get("total").asInt();
                        int pageSize = root.get("pageSize").asInt();
                        int totalPages = (int) Math.ceil((double) total / pageSize);
                        JsonNode resultArray = root.get("result");

                        List<LotteryResult> results = new ArrayList<>();
                        if (resultArray != null && resultArray.isArray()) {
                            for (JsonNode item : resultArray) {
                                try {
                                    results.add(parseCwlResult(item));
                                } catch (Exception e) {
                                    log.error("解析单条开奖数据失败: {}，跳过该条", e.getMessage());
                                }
                            }
                        }

                        return Mono.just(new PageData(results, totalPages));
                    } catch (Exception e) {
                        log.error("解析福彩网接口响应失败: {}", e.getMessage());
                        return Mono.error(new RuntimeException("解析福彩网接口响应失败", e));
                    }
                })
                .doOnError(e -> log.error("获取福彩网数据失败: {}", e.getMessage()));
    }

    /**
     * 解析福彩网官方接口返回的单条开奖数据
     * <p>
     * 福彩网返回格式：
     * {
     *   "name": "双色球",
     *   "code": "2026072",
     *   "date": "2026-06-25(四)",
     *   "red": "07,08,12,15,17,21",
     *   "blue": "01",
     *   "sales": "348744958",
     *   "poolmoney": "2182909574",
     *   "prizegrades": [
     *     {"type": 1, "typenum": "4", "typemoney": "8287457"},
     *     {"type": 2, "typenum": "143", "typemoney": "367827"},
     *     ...
     *   ]
     * }
     */
    private LotteryResult parseCwlResult(JsonNode item) {
        LotteryResult result = new LotteryResult();

        // 期号
        result.period = item.get("code").asText();

        // 开奖日期：格式 "2026-06-25(四)"，提取日期部分和星期
        String dateStr = item.get("date").asText();
        result.drawDate = LocalDate.parse(dateStr.substring(0, 10), DATE_FORMATTER);
        // 提取星期：括号内的内容，如 "四" → "星期四"
        int weekStart = dateStr.indexOf('(');
        int weekEnd = dateStr.indexOf(')');
        if (weekStart != -1 && weekEnd != -1) {
            String weekAbbr = dateStr.substring(weekStart + 1, weekEnd);
            result.weekday = convertWeekday(weekAbbr);
        } else {
            result.weekday = result.drawDate.getDayOfWeek().getDisplayName(
                    java.time.format.TextStyle.FULL, java.util.Locale.CHINESE);
        }

        // 红球号码：逗号分隔，如 "07,08,12,15,17,21"
        String redStr = item.get("red").asText();
        String[] redNumbers = redStr.split(",");
        result.redBalls = new int[6];
        for (int i = 0; i < 6; i++) {
            result.redBalls[i] = Integer.parseInt(redNumbers[i].trim());
        }

        // 蓝球号码
        result.blueBall = Integer.parseInt(item.get("blue").asText().trim());

        // 销售额（元）- 直接取原始字符串
        result.salesAmount = getStringField(item, "sales");
        // 奖池金额（元）
        result.poolAmount = getStringField(item, "poolmoney");

        // 从 prizegrades 中解析一等奖和二等奖
        result.firstPrizeCount = "0";
        result.firstPrizeAmount = "0";
        result.secondPrizeCount = "0";
        result.secondPrizeAmount = "0";

        if (item.has("prizegrades") && item.get("prizegrades").isArray()) {
            for (JsonNode grade : item.get("prizegrades")) {
                try {
                    int type = grade.get("type").asInt();
                    if (type == 1) {
                        result.firstPrizeCount = grade.get("typenum").asText();
                        result.firstPrizeAmount = grade.get("typemoney").asText();
                    } else if (type == 2) {
                        result.secondPrizeCount = grade.get("typenum").asText();
                        result.secondPrizeAmount = grade.get("typemoney").asText();
                    }
                } catch (Exception e) {
                    log.warn("解析奖级数据失败: {}", e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * 将解析后的数据写入数据库（幂等：已存在的期号自动跳过）
     * <p>
     * 每条数据写入都在独立事务中，确保主表和明细表同时写入或同时回滚。
     */
    private Mono<Void> saveLotteryData(LotteryResult result) {
        return ballRepository.findByPeriod(result.period)
                .flatMap(existing -> {
                    log.debug("期号 {} 数据已存在，跳过同步", result.period);
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("开始同步期号 {} 的开奖数据", result.period);
                    return saveNewLotteryData(result);
                }))
                .as(transactionalOperator::transactional)
                .then();
    }

    /**
     * 保存新的开奖数据
     */
    private Mono<Void> saveNewLotteryData(LotteryResult result) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 保存主表记录
        LotteryDoubleBall ball = new LotteryDoubleBall();
        ball.setPeriod(result.period);
        ball.setDrawDate(result.drawDate);
        ball.setWeekday(result.weekday);
        ball.setRedOne(result.redBalls[0]);
        ball.setRedTwo(result.redBalls[1]);
        ball.setRedThree(result.redBalls[2]);
        ball.setRedFour(result.redBalls[3]);
        ball.setRedFive(result.redBalls[4]);
        ball.setRedSix(result.redBalls[5]);
        ball.setBlue(result.blueBall);
        ball.setPoolAmount(result.poolAmount);
        ball.setFirstPrizeCount(result.firstPrizeCount);
        ball.setFirstPrizeAmount(result.firstPrizeAmount);
        ball.setSecondPrizeCount(result.secondPrizeCount);
        ball.setSecondPrizeAmount(result.secondPrizeAmount);
        ball.setSalesAmount(result.salesAmount);
        ball.setCreateTime(now);
        ball.setUpdateTime(now);

        return ballRepository.save(ball)
                .flatMap(savedBall -> {
                    // 2. 保存明细表记录（6个红球 + 1个蓝球）
                    List<LotteryDoubleBallNumber> numbers = new ArrayList<>(7);

                    // 红球
                    for (int i = 0; i < 6; i++) {
                        LotteryDoubleBallNumber num = new LotteryDoubleBallNumber();
                        num.setPeriod(result.period);
                        num.setDrawDate(result.drawDate);
                        num.setNumber(result.redBalls[i]);
                        num.setType(1); // 1=红球
                        num.setCreateTime(now);
                        numbers.add(num);
                    }

                    // 蓝球
                    LotteryDoubleBallNumber blueNum = new LotteryDoubleBallNumber();
                    blueNum.setPeriod(result.period);
                    blueNum.setDrawDate(result.drawDate);
                    blueNum.setNumber(result.blueBall);
                    blueNum.setType(2); // 2=蓝球
                    blueNum.setCreateTime(now);
                    numbers.add(blueNum);

                    return numberRepository.saveAll(numbers).then();
                })
                .doOnSuccess(v -> log.debug("期号 {} 数据同步完成", result.period))
                .doOnError(e -> log.error("期号 {} 数据同步失败: {}", result.period, e.getMessage()));
    }

    /**
     * 将福彩网返回的简写星期转换为完整中文星期
     */
    private String convertWeekday(String abbr) {
        return switch (abbr) {
            case "一" -> "星期一";
            case "二" -> "星期二";
            case "三" -> "星期三";
            case "四" -> "星期四";
            case "五" -> "星期五";
            case "六" -> "星期六";
            case "日", "天" -> "星期日";
            default -> "星期" + abbr;
        };
    }

    /**
     * 安全获取 JSON 字符串字段值，null 或缺失时返回 "0"
     */
    private String getStringField(JsonNode root, String field) {
        if (root.has(field) && !root.get(field).isNull()) {
            String val = root.get(field).asText();
            return val.isEmpty() ? "0" : val;
        }
        return "0";
    }

    /**
     * 一页数据的封装
     */
    private static class PageData {
        final List<LotteryResult> results;
        final int totalPages;

        PageData(List<LotteryResult> results, int totalPages) {
            this.results = results;
            this.totalPages = totalPages;
        }
    }

    /**
     * API 返回数据的内部解析结果
     */
    private static class LotteryResult {
        String period;
        LocalDate drawDate;
        String weekday;
        int[] redBalls;
        int blueBall;
        String poolAmount;
        String salesAmount;
        String firstPrizeCount;
        String firstPrizeAmount;
        String secondPrizeCount;
        String secondPrizeAmount;
    }
}
