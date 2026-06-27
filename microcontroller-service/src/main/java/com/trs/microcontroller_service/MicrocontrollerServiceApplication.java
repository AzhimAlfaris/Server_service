package com.trs.microcontroller_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableRabbit
@EnableDiscoveryClient
@SpringBootApplication
public class MicrocontrollerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicrocontrollerServiceApplication.class, args);
	}

}
