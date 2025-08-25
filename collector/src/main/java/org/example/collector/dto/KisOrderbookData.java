package org.example.collector.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KisOrderbookData {
    private String tr_id;
    private String tr_key;
    private String timestamp;
    private String bid_price_1;
    private String bid_volume_1;
    private String ask_price_1;
    private String ask_volume_1;
    private String bid_price_2;
    private String bid_volume_2;
    private String ask_price_2;
    private String ask_volume_2;
    private String bid_price_3;
    private String bid_volume_3;
    private String ask_price_3;
    private String ask_volume_3;
    private String total_bid_volume;
    private String total_ask_volume;
}

