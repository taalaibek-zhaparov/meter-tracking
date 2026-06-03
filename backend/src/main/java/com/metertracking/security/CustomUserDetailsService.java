package com.metertracking.security;

import com.metertracking.entity.User;
import com.metertracking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Реализация UserDetailsService для Spring Security.
 *
 * Вызывается на КАЖДЫЙ HTTP запрос с JWT токеном — Spring Security
 * должен загрузить UserDetails чтобы проверить что пользователь существует
 * и получить его роли для авторизации.
 *
 * БЕЗ КЭША (как было раньше):
 * - 100 HTTP запросов в минуту = 100 SELECT к user_data через сеть
 * - Каждый запрос: 192.168.145.252 → 192.168.144.180 = +50-150ms латентность
 * - При 10 пользователях одновременно = 1000 запросов/мин к БД
 *
 * С КЭШЕМ Caffeine (как сейчас):
 * - Первый запрос пользователя: 1 SELECT к БД (~150ms)
 * - Следующие 5 минут: данные из памяти (~0.1ms)
 * - Экономия: 99% запросов к БД для авторизации
 *
 * Кэш сбрасывается:
 * - Автоматически через 300 секунд (expireAfterWrite=300s)
 * - Вручную через @CacheEvict в AuthService.login() при входе пользователя
 *
 * Настройка кэша в application.properties:
 *   spring.cache.type=caffeine
 *   spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=300s
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Загружает UserDetails по email для Spring Security.
     *
     * @Cacheable кэширует результат в "userDetailsCache" с ключом = email.
     * При повторном вызове с тем же email — возвращает из кэша без запроса к БД.
     *
     * UserRepository.findByEmail() использует LEFT JOIN FETCH u.roles —
     * роли загружаются в одном запросе, не вызывают LazyInitializationException.
     */
    @Cacheable(value = "userDetailsCache", key = "#email")
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Пользователь не найден: " + email
                ));

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}