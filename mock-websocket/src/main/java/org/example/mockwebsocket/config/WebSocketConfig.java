package org.example.mockwebsocket.config;

import org.example.mockwebsocket.handler.KisWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final KisWebSocketHandler kisWebSocketHandler;

    public WebSocketConfig(KisWebSocketHandler kisWebSocketHandler) {
        this.kisWebSocketHandler = kisWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(kisWebSocketHandler, "/kis-mock")
                .setAllowedOrigins("*");
    }
}

