package com.xy6f.fiva_assistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.xy6f.fiva_assistant.mapper")
public class FivaAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(FivaAssistantApplication.class, args);
    }

}
