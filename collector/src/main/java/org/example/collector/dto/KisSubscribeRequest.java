package org.example.collector.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KisSubscribeRequest {
    private Header header;
    private Body body;

    @Data
    @Builder
    public static class Header {
        private String approval_key;
        private String custtype;
        private String tr_type;
        private String content_type;
    }

    @Data
    @Builder
    public static class Body {
        private Input input;
    }

    @Data
    @Builder
    public static class Input {
        private String tr_id;
        private String tr_key;
    }
}

