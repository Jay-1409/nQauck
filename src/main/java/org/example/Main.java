package org.example;

import org.example.QueueAdaptor.RMQ.tut1.RmqProducer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @GetMapping("/")
    public String home() {
        return "Welcome to Pongpin Spring Boot Application!";
    }

    // Automatically executes when the main Spring Boot application finishes booting up
    @Bean
    public CommandLineRunner runner(RmqProducer rmqProducer) {
        return args -> {
            System.out.println("==================================================================");
            System.out.println("=== Spring Boot Main Application Started Successfully! ===");
            System.out.println("=== Triggering Automatic RabbitMQ Startup Message... ===");
            System.out.println("==================================================================");
            rmqProducer.sendMessage("Hello from Pongpin Spring Boot Main Application startup!");
        };
    }
}