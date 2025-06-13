package com.example.corebanking_service;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubbo
@SpringBootApplication
public class CorebankingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CorebankingServiceApplication.class, args);
	}

}
