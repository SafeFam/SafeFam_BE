package com.gold.safefam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SafefamApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafefamApplication.class, args);
	}

}
