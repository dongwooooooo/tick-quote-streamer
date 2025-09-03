package org.example.collector.config;

import java.util.ArrayList;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "kis")
public class KisWebSocketProperties {
    private WebSocket websocket = new WebSocket();
    private App app = new App();
    private Ssl ssl = new Ssl();
    private Rest rest = new Rest();
    private List<String> targetStockNames = new ArrayList<>();

    @Data
    public static class App {
        private String key;
        private String secret;
    }

    @Data
    public static class WebSocket {
        private String domain;
        private String executionUrl;
        private String quotationUrl;
        private String tradeId;
    }

    @Data
    public static class Rest {
        private String domain;
        private String oauthAccessUrl;
        private String oauthTokenUrl;

        public String tokenAPIUrl() {
            return this.domain + this.oauthTokenUrl;
        }

        public String accessAPIUrl() {
            return this.domain + this.oauthAccessUrl;
        }
    }

    @Data
    public static class Ssl {
        private boolean trustAllCertificates = false;
        private boolean verifyHostname = true;
    }
}
