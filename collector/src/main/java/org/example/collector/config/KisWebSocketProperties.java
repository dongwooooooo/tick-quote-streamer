package org.example.collector.config;

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
    private List<String> targetStocks = List.of(
        "005930", // 삼성전자
        "000660", // SK하이닉스  
        "373220", // LG에너지솔루션
        "207940", // 삼성바이오로직스
        "005935", // 삼성전자우
        "012450", // 한화에어로스페이스
        "005380", // 현대차
        "329180", // HD현대중공업
        "034020", // 두산에너빌리티
        "105560"  // KB금융
    );

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
