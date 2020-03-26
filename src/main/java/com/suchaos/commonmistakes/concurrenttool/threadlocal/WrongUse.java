package com.suchaos.commonmistakes.concurrenttool.threadlocal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 线程重用导致用户信息错乱的 Bug
 * <p>
 * 程序运行在 Tomcat 中，执行程序的线程是 Tomcat 的工作线程，而 Tomcat 的工作线程是基于线程池的
 * <p>
 * 线程池会重用固定的几个线程，一旦线程重用，那么很可能首次从 ThreadLocal 获取的值是之前其他用户的请求遗留的值。
 * 这时，ThreadLocal 中的用户信息就是其他用户的信息
 * <p>
 * 使用类似 ThreadLocal 工具来存放一些数据时，需要特别注意在代码运行完后，显式地去清空设置的数据
 *
 * @author suchao
 * @date 2020/3/26
 */
@RestController
@RequestMapping("threadlocal")
public class WrongUse {

    private static final ThreadLocal<Integer> CURRENT_USER = ThreadLocal.withInitial(() -> null);

    @GetMapping("/wrong")
    public Map<String, String> wrong(@RequestParam("userId") Integer userId) {
        // 设置用户信息之前先查询一次ThreadLocal中的用户信息
        String before = Thread.currentThread().getName() + ":" + CURRENT_USER.get();
        CURRENT_USER.set(userId);
        try {
            //设置用户信息之后再查询一次ThreadLocal中的用户信息
            String after = Thread.currentThread().getName() + ":" + CURRENT_USER.get();
            // 汇总输出两次查询结果
            Map<String, String> result = new HashMap<>();
            result.put("before", before);
            result.put("after", after);
            return result;
        } finally {
            // 在finally代码块中删除ThreadLocal中的数据，确保数据不串
            CURRENT_USER.remove();
        }
    }
}
