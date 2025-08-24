package org.example.ssestreamer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KisQuoteMessage {
    
    @JsonProperty("tr_id")
    private String trId;
    
    @JsonProperty("tr_key")
    private String trKey;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("price")
    private String price;
    
    @JsonProperty("volume")
    private String volume;
    
    @JsonProperty("change_amount")
    private String changeAmount;
    
    @JsonProperty("change_rate")
    private String changeRate;
    
    @JsonProperty("high_price")
    private String highPrice;
    
    @JsonProperty("low_price")
    private String lowPrice;
    
    @JsonProperty("open_price")
    private String openPrice;
    
    // 편의 메서드들
    public BigDecimal getPriceAsBigDecimal() {
        return price != null ? new BigDecimal(price) : BigDecimal.ZERO;
    }
    
    public Long getVolumeAsLong() {
        return volume != null ? Long.parseLong(volume) : 0L;
    }
    
    public BigDecimal getChangeAmountAsBigDecimal() {
        return changeAmount != null ? new BigDecimal(changeAmount) : BigDecimal.ZERO;
    }
    
    public BigDecimal getChangeRateAsBigDecimal() {
        return changeRate != null ? new BigDecimal(changeRate) : BigDecimal.ZERO;
    }
    
    public BigDecimal getHighPriceAsBigDecimal() {
        return highPrice != null ? new BigDecimal(highPrice) : BigDecimal.ZERO;
    }
    
    public BigDecimal getLowPriceAsBigDecimal() {
        return lowPrice != null ? new BigDecimal(lowPrice) : BigDecimal.ZERO;
    }
    
    public BigDecimal getOpenPriceAsBigDecimal() {
        return openPrice != null ? new BigDecimal(openPrice) : BigDecimal.ZERO;
    }
}

