package com.example.collab.config;

import com.example.collab.websocket.AuthHandshakeInterceptor;
import com.example.collab.websocket.CollabWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CollabWebSocketHandler collabWebSocketHandler;
    private final AuthHandshakeInterceptor authHandshakeInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public WebSocketConfig(CollabWebSocketHandler collabWebSocketHandler,
                           AuthHandshakeInterceptor authHandshakeInterceptor) {
        this.collabWebSocketHandler = collabWebSocketHandler;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(collabWebSocketHandler, "/ws")
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
