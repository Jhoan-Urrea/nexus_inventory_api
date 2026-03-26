package com.example.nexus;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NexusInventoryApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NexusInventoryApiApplication.class, args);
	}

} 
