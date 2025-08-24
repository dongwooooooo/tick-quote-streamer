package org.example.dataprocessor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KisOrderbookMessage {
    
    @JsonProperty("tr_id")
    private String trId;
    
    @JsonProperty("tr_key")
    private String trKey;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("sequence_number")
    private String sequenceNumber;
    
    // 매수호가 (1~10단계)
    @JsonProperty("bid_prices")
    private List<String> bidPrices;
    
    @JsonProperty("bid_volumes")
    private List<String> bidVolumes;
    
    // 매도호가 (1~10단계)
    @JsonProperty("ask_prices")
    private List<String> askPrices;
    
    @JsonProperty("ask_volumes")
    private List<String> askVolumes;
    
    @JsonProperty("total_bid_volume")
    private String totalBidVolume;
    
    @JsonProperty("total_ask_volume")
    private String totalAskVolume;
    
    // 편의 메서드들
    public Long getSequenceNumberAsLong() {
        return sequenceNumber != null ? Long.parseLong(sequenceNumber) : 0L;
    }
    
    public Long getTotalBidVolumeAsLong() {
        return totalBidVolume != null ? Long.parseLong(totalBidVolume) : 0L;
    }
    
    public Long getTotalAskVolumeAsLong() {
        return totalAskVolume != null ? Long.parseLong(totalAskVolume) : 0L;
    }
    
    public List<OrderbookLevelDto> getBidLevels() {
        List<OrderbookLevelDto> levels = new ArrayList<>();
        if (bidPrices != null && bidVolumes != null) {
            for (int i = 0; i < Math.min(bidPrices.size(), bidVolumes.size()); i++) {
                if (bidPrices.get(i) != null && bidVolumes.get(i) != null) {
                    levels.add(OrderbookLevelDto.builder()
                        .orderType("BID")
                        .priceLevel(i + 1)
                        .price(new BigDecimal(bidPrices.get(i)))
                        .volume(Long.parseLong(bidVolumes.get(i)))
                        .build());
                }
            }
        }
        return levels;
    }
    
    public List<OrderbookLevelDto> getAskLevels() {
        List<OrderbookLevelDto> levels = new ArrayList<>();
        if (askPrices != null && askVolumes != null) {
            for (int i = 0; i < Math.min(askPrices.size(), askVolumes.size()); i++) {
                if (askPrices.get(i) != null && askVolumes.get(i) != null) {
                    levels.add(OrderbookLevelDto.builder()
                        .orderType("ASK")
                        .priceLevel(i + 1)
                        .price(new BigDecimal(askPrices.get(i)))
                        .volume(Long.parseLong(askVolumes.get(i)))
                        .build());
                }
            }
        }
        return levels;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderbookLevelDto {
        private String orderType;
        private Integer priceLevel;
        private BigDecimal price;
        private Long volume;
    }
}

