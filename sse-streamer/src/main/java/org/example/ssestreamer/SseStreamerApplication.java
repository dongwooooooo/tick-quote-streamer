package org.example.ssestreamer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

@Slf4j
@SpringBootApplication
@EnableKafka
@EnableCaching
public class SseStreamerApplication {

    public static void main(String[] args) {
        log.info("Starting SSE Streamer Application...");
        SpringApplication.run(SseStreamerApplication.class, args);
        log.info("SSE Streamer Application started successfully!");
    }
}