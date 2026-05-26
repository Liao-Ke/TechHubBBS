package com.techhub;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.techhub.mapper")
@EnableScheduling
@EnableAsync
@EnableFileStorage
public class TechHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechHubApplication.class, args);
    }
}
