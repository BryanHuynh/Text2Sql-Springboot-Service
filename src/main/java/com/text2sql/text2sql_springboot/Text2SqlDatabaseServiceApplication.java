package com.text2sql.text2sql_springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Text2SqlDatabaseServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(Text2SqlDatabaseServiceApplication.class, args);
	}

}
