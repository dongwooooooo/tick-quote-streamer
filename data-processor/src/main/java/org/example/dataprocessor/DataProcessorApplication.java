package org.example.dataprocessor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@SpringBootApplication
@EnableKafka
@EnableCaching
@EnableTransactionManagement
public class DataProcessorApplication {

    public static void main(String[] args) {
        log.info("Starting Data Processor Application...");
        SpringApplication.run(DataProcessorApplication.class, args);
        log.info("Data Processor Application started successfully!");
    }
}