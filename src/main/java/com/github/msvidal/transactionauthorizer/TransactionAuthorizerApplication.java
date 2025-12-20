package com.github.msvidal.transactionauthorizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class TransactionAuthorizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransactionAuthorizerApplication.class, args);
	}

}
