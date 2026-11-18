package com.campus.activity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园活动管理系统 启动类
 *
 * 启动方式：
 *   MySQL 模式：java -jar campus-activity.jar
 *   H2 演示模式：java -jar campus-activity.jar --spring.profiles.active=h2
 */
@SpringBootApplication
@MapperScan("com.campus.activity.mapper")
@EnableScheduling
public class CampusActivityApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusActivityApplication.class, args);
    }
}
