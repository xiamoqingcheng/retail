package com.retail.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 零售物品智能识别系统后端启动类。
 */
@SpringBootApplication
@EnableAsync
public class RetailServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetailServerApplication.class, args);
    }
}
