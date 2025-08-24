package org.example.collector.dto;

import lombok.Data;

@Data
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

