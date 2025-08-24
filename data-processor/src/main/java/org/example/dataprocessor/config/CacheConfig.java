package org.example.dataprocessor.config;

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
    
    @Value("${app.cache.latest-data-ttl:300}")
    private int latestDataTtl;
    
    @Value("${app.cache.max-cache-size:10000}")
    private int maxCacheSize;
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 기본 캐시 설정
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(maxCacheSize)
            .expireAfterWrite(latestDataTtl, TimeUnit.SECONDS)
            .recordStats());
        
        // 캐시 이름 설정
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "latestQuotes",
            "latestOrderbooks",
            "stockInfo"
        ));
        
        log.info("Cache Manager initialized - TTL: {}s, Max Size: {}", latestDataTtl, maxCacheSize);
        
        return cacheManager;
    }
}
