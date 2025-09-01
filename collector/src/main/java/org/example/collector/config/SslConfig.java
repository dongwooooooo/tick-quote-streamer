package org.example.collector.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SslConfig {

    private final KisWebSocketProperties kisProperties;

    @Bean
    public SSLContext sslContext() {
        if (!kisProperties.getSsl().isTrustAllCertificates()) {
            log.info("표준 SSL 검증 사용");
            return null; // 기본 SSL Context 사용
        }

        try {
            log.warn("개발환경: SSL 인증서 검증 우회 (trust-all-certificates=true)");
            
            // 모든 인증서를 신뢰하는 TrustManager
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { 
                        return new X509Certificate[0]; 
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // 전역 SSL 설정 적용
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            
            return sslContext;
            
        } catch (Exception e) {
            log.error("SSL Context 생성 실패", e);
            throw new RuntimeException("SSL 설정 실패", e);
        }
    }

    @Bean
    public HostnameVerifier hostnameVerifier() {
        if (kisProperties.getSsl().isVerifyHostname()) {
            log.info("표준 호스트명 검증 사용");
            return null; // 기본 HostnameVerifier 사용
        }

        log.warn("개발환경: 호스트명 검증 우회 (verify-hostname=false)");
        
        HostnameVerifier allowAllHostnames = (hostname, session) -> {
            log.debug("호스트명 검증 우회: {}", hostname);
            return true;
        };
        
        // 전역 호스트명 검증 설정
        HttpsURLConnection.setDefaultHostnameVerifier(allowAllHostnames);
        
        return allowAllHostnames;
    }
}
