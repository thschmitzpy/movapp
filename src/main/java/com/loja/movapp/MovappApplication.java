package com.loja.movapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MovappApplication {

	public static void main(String[] args) {
		SpringApplication.run(MovappApplication.class, args);
	}

}


