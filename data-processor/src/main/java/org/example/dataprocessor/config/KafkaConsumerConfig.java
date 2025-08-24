package org.example.dataprocessor.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;
import org.springframework.kafka.listener.DefaultErrorHandler;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
    @Value("${spring.kafka.listener.concurrency:3}")
    private int concurrency;
    
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // 기본 설정
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // 성능 및 신뢰성 설정
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 수동 커밋
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        // 세션 및 하트비트 설정
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        
        log.info("Kafka Consumer configuration initialized with bootstrap servers: {}", bootstrapServers);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(concurrency);
        
        // 수동 커밋 설정
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setPollTimeout(3000);
        
        // 에러 핸들링 설정
        factory.setCommonErrorHandler(new DefaultErrorHandler());
        
        log.info("Kafka Listener Container Factory initialized with concurrency: {}", concurrency);
        
        return factory;
    }
    /*
    * RECORD (기본값):

동작 방식: 컨슈머가 메시지 한 건을 성공적으로 처리하고 난 뒤, 컨테이너가 자동으로 커밋합니다.

코드에 적용: @KafkaListener에 별도 설정이 없으면 이 모드가 기본으로 적용됩니다.

장점: 설정이 가장 간단하고 직관적입니다.

단점: 메시지 한 건마다 커밋을 수행하므로, 커밋 횟수가 많아져 성능에 약간의 오버헤드가 발생할 수 있습니다.

BATCH:

동작 방식: poll() 메서드로 가져온 **전체 배치(batch)**의 메시지를 모두 처리하고 난 뒤, 컨테이너가 한 번에 커밋합니다.

코드에 적용: containerProperties.setAckMode(AckMode.BATCH);

장점: 커밋 횟수가 줄어들어 성능 오버헤드를 낮출 수 있습니다.

단점: 배치 내의 메시지 중 하나라도 처리에 실패하면, 해당 배치 전체가 재처리될 수 있습니다. (메시지 처리 순서에 민감한 경우 주의)

MANUAL:

동작 방식: 리스너 메서드에서 acknowledgment.acknowledge()가 호출될 때, 컨테이너가 그 정보를 기억했다가 메서드 실행이 모두 끝난 후에 커밋합니다.

코드에 적용: containerProperties.setAckMode(AckMode.MANUAL);

장점: 개발자가 커밋 시점을 직접 제어할 수 있어 유연성이 높습니다. 배치 내에서 일부 메시지 처리가 성공했더라도, 원하는 시점까지 커밋을 지연시킬 수 있습니다.

주의: acknowledge() 호출이 없으면 커밋이 이루어지지 않습니다.

MANUAL_IMMEDIATE:

동작 방식: 리스너 메서드에서 acknowledgment.acknowledge()가 호출되는 즉시 컨테이너가 커밋을 수행합니다.

코드에 적용: containerProperties.setAckMode(AckMode.MANUAL_IMMEDIATE);

장점: 개발자가 정확한 커밋 타이밍을 지정할 수 있습니다.

단점: RECORD와 마찬가지로 커밋 횟수가 많아질 수 있습니다.*/
}

