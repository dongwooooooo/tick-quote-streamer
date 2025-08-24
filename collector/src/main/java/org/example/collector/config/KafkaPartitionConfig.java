package org.example.collector.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class KafkaPartitionConfig implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        // 종목 코드를 기반으로 파티션 결정
        if (key == null) {
            return 0; // 기본 파티션
        }
        
        String stockCode = key.toString();
        int partitionCount = cluster.partitionCountForTopic(topic);
        
        // 특정 인기 종목들을 균등하게 분산
        switch (stockCode) {
            case "005930": // 삼성전자
                return 0;
            case "000660": // SK하이닉스  
                return 1;
            case "035420": // NAVER
                return 2;
            case "035720": // 카카오
                return 3;
            default:
                // 나머지 종목들은 해시 기반 분산
                return Math.abs(stockCode.hashCode()) % partitionCount;
        }
    }

    @Override
    public void close() {
        // No resources to clean up
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // No configuration needed
    }
}

