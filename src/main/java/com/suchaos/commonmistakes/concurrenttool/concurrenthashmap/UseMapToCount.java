package com.suchaos.commonmistakes.concurrenttool.concurrenthashmap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 使用 Map 来统计 Key 出现次数
 * <p>
 * 1. 使用 ConcurrentHashMap 来统计，Key 的范围是 10。
 * <p>
 * 2. 使用最多 10 个并发，循环操作 1000 万次，每次操作累加随机的 Key。
 * <p>
 * 3. 如果 Key 不存在的话，首次设置值为 1。
 *
 * @author suchao
 * @date 2020/3/26
 */
@Slf4j
public class UseMapToCount {

    static int LOOP_COUNT = 10000000;

    static int THREAD_COUNT = 10;

    static int ITEM_COUNT = 10;

    public static Map<String, Long> normalUse() throws InterruptedException {
        ConcurrentHashMap<String, Long> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, LOOP_COUNT).parallel()
                .forEach(i -> {
                    // 获得一个随机的 key
                    String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                    //log.info(key);
                    synchronized (freqs) {
                        if (freqs.containsKey(key)) {
                            freqs.put(key, freqs.get(key) + 1);
                        } else {
                            freqs.put(key, 1L);
                        }
                    }
                })
        );
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        return freqs;
    }

    public static Map<String, Long> goodUse() throws InterruptedException {
        ConcurrentHashMap<String, LongAdder> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, LOOP_COUNT).parallel()
                .forEach(i -> {
                    // 获得一个随机的 key
                    String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                    // 利用computeIfAbsent()方法来实例化LongAdder，然后利用LongAdder来进行线程安全计数
                    // LongAdder 的官方文档就写了这个案例
                    freqs.computeIfAbsent(key, k -> new LongAdder()).increment();
                })
        );
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        // 因为我们的Value是LongAdder而不是Long，所以需要做一次转换才能返回
        return freqs.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().longValue()));
    }

    public static void main(String[] args) throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("normalUse");
        Map<String, Long> normalUse = UseMapToCount.normalUse();
        stopWatch.stop();
        // normalUse.forEach((key, value) -> System.out.println("key is " + key + ", and value is " + value));
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
    }
}
