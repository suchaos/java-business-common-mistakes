package com.suchaos.commonmistakes.concurrenttool.concurrenthashmap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * 使用了线程安全的并发工具，并不代表解决了所有线程安全问题
 * <p>
 * ConcurrentHashMap 只能保证提供的原子性读写操作是线程安全的
 *
 * @author suchao
 * @date 2020/3/26
 */
@RestController
@RequestMapping("hashmap")
@Slf4j
public class ConcurrentHashMapWrong {

    private static int THREAD_COUNT = 10;

    private static int ITEM_COUNT = 1000;

    // 帮助方法，用来获得一个指定原色数量模拟数据的 ConcurrentHashMap
    private ConcurrentHashMap<String, Long> getData(int count) {
        return LongStream.rangeClosed(1, count)
                .boxed()
                .collect(Collectors.toConcurrentMap(i -> UUID.randomUUID().toString(),
                        Function.identity(), (o1, o2) -> o1, ConcurrentHashMap::new));
    }

    @GetMapping("wrong")
    public String wrong() throws InterruptedException {
        ConcurrentHashMap<String, Long> concurrentHashMap = getData(ITEM_COUNT - 100);
        // 初始化 900 个元素
        log.info("init size: {}", concurrentHashMap.size());

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        // 使用线程池并发处理逻辑
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, 10).parallel()
                .forEach(i -> {
                    // 查询还需要补充多少个元素 -- 注意：这是错误示范
                    int gap = ITEM_COUNT - concurrentHashMap.size();
                    log.info("gap size: {}", gap);
                    // 补充元素
                    concurrentHashMap.putAll(getData(gap));
                })
        );

        // 等待所有任务完成
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        // 最后元素个数是 1000 吗？
        log.info("finish size: {}", concurrentHashMap.size());
        return "OK";
    }

    @GetMapping("old-right")
    public String oldRight() throws InterruptedException {
        ConcurrentHashMap<String, Long> concurrentHashMap = getData(ITEM_COUNT - 100);
        // 初始化 900 个元素
        log.info("init size: {}", concurrentHashMap.size());

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        // 使用线程池并发处理逻辑
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, 10).parallel()
                .forEach(i -> {
                    synchronized (concurrentHashMap) {
                        // 查询还需要补充多少个元素 -- 注意：这是错误示范
                        int gap = ITEM_COUNT - concurrentHashMap.size();
                        log.info("gap size: {}", gap);
                        // 补充元素
                        concurrentHashMap.putAll(getData(gap));
                    }
                })
        );

        // 等待所有任务完成
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        // 最后元素个数是 1000 吗？
        log.info("finish size: {}", concurrentHashMap.size());
        return "OK";
    }

    @GetMapping("right")
    public String goodWay() throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("normalUse");
        Map<String, Long> normalUse = UseMapToCount.normalUse();
        stopWatch.stop();
        // 检验元素数量
        Assert.isTrue(normalUse.size() == UseMapToCount.ITEM_COUNT, "normalUse size error");
        // 校验累计总数
        Assert.isTrue(normalUse.values().stream().reduce((long) 0, Long::sum) == UseMapToCount.LOOP_COUNT,
                "normalUse count error");

        stopWatch.start("goodUse");
        Map<String, Long> goodUse = UseMapToCount.goodUse();
        stopWatch.stop();
        Assert.isTrue(goodUse.size() == UseMapToCount.ITEM_COUNT, "goodUse size error");
        Assert.isTrue(goodUse.values().stream()
                .mapToLong(l -> l).reduce(0, Long::sum) == UseMapToCount.LOOP_COUNT, "goodUse count error");

        log.info(stopWatch.prettyPrint());
        return "OK";
    }


    public static void main(String[] args) {
        ConcurrentHashMapWrong map = new ConcurrentHashMapWrong();
        ConcurrentHashMap<String, Long> data = map.getData(10);
        data.forEach((key, value) -> System.out.println("key is " + key + ", and value is " + value));
        System.out.println();
        data.putAll(map.getData(10));
        data.forEach((key, value) -> System.out.println("key is " + key + ", and value is " + value));
    }
}
