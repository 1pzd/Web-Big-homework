package com.iot.qa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 智能问答网站启动类。
 */
@SpringBootApplication
@MapperScan("com.iot.qa.mapper")
public class AiQaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiQaApplication.class, args);
        System.out.println("""

                ==================================================
                  AI 智能问答网站启动成功
                  前台地址: http://localhost:8080/
                  后台地址: http://localhost:8080/admin/login
                ==================================================
                """);
    }
}