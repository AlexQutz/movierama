package com.movierama;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MovieramaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MovieramaApplication.class, args);
	}

}
