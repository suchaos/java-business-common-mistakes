package com.suchaos.commonmistakes.concurrenttool.concurrenthashmap;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * putIfAbsent vs computeIfAbsent
 *
 * ans:https://stackoverflow.com/questions/48183999/what-is-the-difference-between-putifabsent-and-computeifabsent-in-java-8-map
 *
 * @author suchao
 * @date 2020/3/26
 */
@Slf4j
public class PiaCia {

    public static void main(String[] args) {
        test(new HashMap<>());
        System.out.println();
        test(new ConcurrentHashMap<>());
    }

    private static void test(Map<String, String> map) {
        log.info("class : {}", map.getClass().getName());

        try {
            log.info("putIfAbsent null value : {}", map.putIfAbsent("test1", null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("test containsKey after putIfAbsent : {}", map.containsKey("test1"));

        log.info("computeIfAbsent null value : {}", map.computeIfAbsent("test2", k -> null));
        log.info("test containsKey after putIfAbsent : {}", map.containsKey("test2"));

        log.info("putIfAbsent non-null value : {}", map.putIfAbsent("test3", "test3"));
        log.info("computeIfAbsent non-null value : {}", map.computeIfAbsent("test4", __ -> "test4"));

        log.info("putIfAbsent expensive value : {}", map.putIfAbsent("test4", getValue()));
        log.info("computeIfAbsent expensive value : {}", map.computeIfAbsent("test4", __ -> getValue()));
    }

    private static String getValue() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return UUID.randomUUID().toString();
    }

}
