package com.ztq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableTransactionManagement
public class PtxzApplication implements WebMvcConfigurer {


    public static void main(String[] args) {
        SpringApplication.run(PtxzApplication.class, args);
    }

    //允许7070的所有路径的跨域访问 并允许携带cookie
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("http://localhost:7070").allowCredentials(true);
    }
}
