package org.example.mockwebsocket.dto;

import lombok.Data;

@Data
public class KisSubscribeMessage {
    private Header header;
    private Body body;

    @Data
    public static class Header {
        private String approval_key;
        private String custtype;
        private String tr_type;
        private String content_type;
    }

    @Data
    public static class Body {
        private Input input;
    }

    @Data
    public static class Input {
        private String tr_id;
        private String tr_key;
    }
}

