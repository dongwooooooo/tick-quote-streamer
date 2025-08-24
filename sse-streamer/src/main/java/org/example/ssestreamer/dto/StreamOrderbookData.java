package org.example.ssestreamer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamOrderbookData {
    
    @JsonProperty("stock_code")
    private String stockCode;
    
    @JsonProperty("stock_name")
    private String stockName;
    
    @JsonProperty("quote_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime quoteTime;
    
    @JsonProperty("sequence_number")
    private Long sequenceNumber;
    
    @JsonProperty("total_bid_volume")
    private Long totalBidVolume;
    
    @JsonProperty("total_ask_volume")
    private Long totalAskVolume;
    
    @JsonProperty("bid_levels")
    private List<OrderbookLevelData> bidLevels;
    
    @JsonProperty("ask_levels")
    private List<OrderbookLevelData> askLevels;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderbookLevelData {
        @JsonProperty("level")
        private Integer level;
        
        @JsonProperty("price")
        private BigDecimal price;
        
        @JsonProperty("volume")
        private Long volume;
    }
}

