package org.example.notificationservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@SpringBootApplication
@EnableKafka
@EnableRetry
@EnableTransactionManagement
public class NotificationServiceApplication {

    public static void main(String[] args) {
        log.info("Starting Notification Service Application...");
        SpringApplication.run(NotificationServiceApplication.class, args);
        log.info("Notification Service Application started successfully!");
    }
}