package com.metertracking.config;

import com.metertracking.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSocketMessageBroker   // ← оставить здесь, раз WebSocketConfig удалён
@RequiredArgsConstructor
@Slf4j
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    // ── ИЗ УДАЛЁННОГО WebSocketConfig ────────────────────────────────

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    // ── JWT ПРОВЕРКА ПРИ CONNECT ──────────────────────────────────────

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) return message;

                StompCommand command = accessor.getCommand();

                if (StompCommand.CONNECT.equals(command)) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        try {
                            String jwt = authHeader.substring(7);
                            Claims claims = jwtUtil.extractAllClaims(jwt);
                            String email = claims.getSubject();
                            List<String> roles = claims.get("roles", List.class);
                            var authorities = roles.stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());
                            var userDetails = User.builder()
                                    .username(email).password("")
                                    .authorities(authorities).build();
                            var auth = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, authorities);
                            accessor.setUser(auth);
                            log.debug("[WS] CONNECT: {}", email);
                        } catch (Exception e) {
                            log.debug("[WS] CONNECT без токена или истёк");
                        }
                    }
                }

                if (StompCommand.DISCONNECT.equals(command)) {
                    log.debug("[WS] DISCONNECT");
                }

                return message;
            }
        });
    }
}