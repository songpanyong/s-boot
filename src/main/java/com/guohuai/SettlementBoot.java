package com.guohuai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启动类
 * @author xueyunlong
 * @date 2018/1/27 16:36
 */
@SpringBootApplication
@EnableAutoConfiguration
@EnableJpaRepositories
@ComponentScan(basePackages = {"com.guohuai"})
@EnableAsync
public class SettlementBoot {
    public static void main(String[] args) {
        SpringApplication.run(SettlementBoot.class, args);
    }
}
