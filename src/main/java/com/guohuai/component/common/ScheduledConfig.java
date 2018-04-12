package com.guohuai.component.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 
 * @ClassName: ScheduledConfig
 * @Description: spring定时任务.线程池,自定义多线程配置
 * @author chendonghui
 * @date 2018年1月16日 下午4:11:21
 *
 */
@Configuration
public class ScheduledConfig {

	//最大线程数，默认为5
    @Value("${spring.scheduled.maxPoolSize:5}")
    private String maxPoolSize;
    
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(Integer.parseInt(maxPoolSize));
        return taskScheduler;
    }
    
}
