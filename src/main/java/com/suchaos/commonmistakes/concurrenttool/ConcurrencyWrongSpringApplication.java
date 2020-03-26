package com.suchaos.commonmistakes.concurrenttool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBootApplication
 *
 * @author suchao
 * @date 2020/3/26
 */
@SpringBootApplication
public class ConcurrencyWrongSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcurrencyWrongSpringApplication.class, args);
    }
}
