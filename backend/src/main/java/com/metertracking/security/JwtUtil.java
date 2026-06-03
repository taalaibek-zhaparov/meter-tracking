package com.metertracking.security;

import com.metertracking.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Утилита для работы с JWT токенами.
 *
 * JWT (JSON Web Token) — это подписанная строка вида header.payload.signature.
 * Токен содержит: email пользователя, его роли, время создания и истечения.
 * Сервер не хранит токены — только проверяет подпись при каждом запросе.
 *
 * Оптимизации:
 * - signingKey создаётся ОДИН РАЗ при старте (@PostConstruct)
 *   Раньше Keys.hmacShaKeyFor() вызывался при каждом запросе — лишние CPU cycles
 * - generateToken() принимает User напрямую — не нужен запрос к БД за ролями
 * - JwtAuthenticationFilter читает роли из токена — не идёт в БД за авторизацией
 *
 * Срок жизни токена: jwt.expiration=86400000ms = 24 часа.
 * После истечения пользователь должен войти заново.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Ключ подписи — создаётся один раз при старте приложения.
     * HMAC-SHA256 ключ из секретной строки в application.properties.
     */
    private Key signingKey;

    @PostConstruct
    private void initKey() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Извлекает email из токена (поле sub = subject).
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Извлекает время истечения токена.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Универсальный метод извлечения поля из claims.
     * claimsResolver — лямбда, например: Claims::getSubject
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    /**
     * Парсит и верифицирует подпись токена.
     * Бросает исключение если токен невалидный или истёк.
     * Вызывается в JwtAuthenticationFilter на каждый HTTP запрос.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Генерирует JWT токен для пользователя.
     *
     * Содержимое токена (payload):
     * - sub: email пользователя
     * - roles: список ролей ["ROLE_USER"] или ["ROLE_ADMIN"]
     * - iat: время создания (issued at)
     * - exp: время истечения (expiration = iat + 24h)
     *
     * Принимает User напрямую — роли уже загружены, нет запроса к БД.
     */
    public String generateToken(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .collect(Collectors.toList());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("username", user.getUsername());

        return createToken(claims, user.getEmail());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiration))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Проверяет валидность токена: совпадение email и не истёк ли срок.
     * Используется в JwtAuthenticationFilter.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }
}