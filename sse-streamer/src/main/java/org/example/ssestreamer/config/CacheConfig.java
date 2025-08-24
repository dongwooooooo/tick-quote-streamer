package org.example.ssestreamer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Value("${app.cache.recent-data-ttl:60}")
    private int recentDataTtl;
    
    @Value("${app.cache.max-cache-size:5000}")
    private int maxCacheSize;
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // SSE 스트리밍에 최적화된 캐시 설정 (짧은 TTL, 빠른 액세스)
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(maxCacheSize)
            .expireAfterWrite(recentDataTtl, TimeUnit.SECONDS)
            .recordStats());
        
        // 캐시 이름 설정
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "recentQuotes",
            "recentOrderbooks", 
            "stockInfo",
            "subscriptions"
        ));
        
        log.info("Cache Manager initialized for SSE streaming - TTL: {}s, Max Size: {}", recentDataTtl, maxCacheSize);
        
        return cacheManager;
    }
}

