package org.example.collector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        try {
            // 모든 인증서를 신뢰하는 TrustManager (개발용)
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };

            // KIS API 도메인만 허용하는 호스트명 검증기
            HostnameVerifier kisHostnameVerifier = (hostname, session) -> {
                boolean isKisApiDomain = hostname.equals("openapivts.koreainvestment.com") ||
                                       hostname.equals("openapi.koreainvestment.com") ||
                                       hostname.equals("www.openapi.koreainvestment.com");
                System.out.println("Hostname verification for " + hostname + ": " + isKisApiDomain);
                return isKisApiDomain;
            };

            // SSL 컨텍스트 설정
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // 전역 SSL 설정
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(kisHostnameVerifier);

            System.out.println("SSL configuration applied for KIS API domains (trust all certs + hostname verification)");
            
        } catch (Exception e) {
            System.err.println("Failed to configure SSL for RestTemplate: " + e.getMessage());
            e.printStackTrace();
        }

        // SimpleClientHttpRequestFactory 사용
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        
        return new RestTemplate(factory);
    }
}
