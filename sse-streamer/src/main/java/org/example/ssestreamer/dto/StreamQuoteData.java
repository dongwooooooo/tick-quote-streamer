package org.example.ssestreamer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamQuoteData {
    
    @JsonProperty("stock_code")
    private String stockCode;
    
    @JsonProperty("stock_name")
    private String stockName;
    
    @JsonProperty("price")
    private BigDecimal price;
    
    @JsonProperty("volume")
    private Long volume;
    
    @JsonProperty("change_amount")
    private BigDecimal changeAmount;
    
    @JsonProperty("change_rate")
    private BigDecimal changeRate;
    
    @JsonProperty("high_price")
    private BigDecimal highPrice;
    
    @JsonProperty("low_price")
    private BigDecimal lowPrice;
    
    @JsonProperty("open_price")
    private BigDecimal openPrice;
    
    @JsonProperty("trade_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tradeTime;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("market_status")
    private String marketStatus;  // OPEN, CLOSE, PRE_MARKET, AFTER_MARKET
}

