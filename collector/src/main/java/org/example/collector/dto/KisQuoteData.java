package org.example.collector.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KisQuoteData {
    private String tr_id;
    private String tr_key;
    private String timestamp;
    private String price;
    private String volume;
    private String change_amount;
    private String change_rate;
    private String high_price;
    private String low_price;
    private String open_price;
}

