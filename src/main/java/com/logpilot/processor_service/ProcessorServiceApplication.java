package com.logpilot.processor_service;

import com.logpilot.processor_service.config.ProcessorConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ProcessorConfiguration.class)
public class ProcessorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProcessorServiceApplication.class, args);
	}

}
