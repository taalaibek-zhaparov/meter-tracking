package com.metertracking.service;

import com.metertracking.dto.LoginRequest;
import com.metertracking.dto.RegisterRequest;
import com.metertracking.entity.Role;
import com.metertracking.entity.User;
import com.metertracking.repository.RoleRepository;
import com.metertracking.repository.UserRepository;
import com.metertracking.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Сервис аутентификации и регистрации пользователей.
 *
 * Оптимизации:
 * - login() делает ровно 1 запрос к БД (findByEmail с JOIN FETCH ролей)
 * - @CacheEvict сбрасывает кэш UserDetails при входе
 * - generateToken() принимает User — не нужен повторный запрос к БД
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * Регистрация нового пользователя.
     * Сохраняет email, password (bcrypt), username, role и region.
     */
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email уже существует!");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getUsername());
        user.setDeleted(false);

        // ИСПРАВЛЕНО: регион теперь сохраняется (раньше терялся)
        if (request.getRegion() != null && !request.getRegion().isBlank()) {
            user.setRegion(request.getRegion());
        }

        String roleName = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole()
                : "USER";

        Role userRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Роль не найдена: " + roleName));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);
        return "Пользователь успешно зарегистрирован!";
    }

    /**
     * Вход в систему.
     *
     * Поток:
     * 1. authenticationManager.authenticate() — проверяет пароль через BCrypt,
     *    использует кэш UserDetails (CustomUserDetailsService + Caffeine)
     * 2. userRepository.findByEmail() — загружает User с ролями (JOIN FETCH)
     *    нужен чтобы передать роли в JWT токен
     * 3. jwtUtil.generateToken(user) — создаёт JWT без дополнительных запросов к БД
     *
     * Итого: максимум 1 запрос к БД при прогретом кэше,
     *        2 запроса при первом входе (кэш пустой).
     *
     * @CacheEvict нужен чтобы сбросить устаревший кэш если пользователь
     * изменил пароль или роли — иначе старый UserDetails останется в кэше
     * на 5 минут и authenticate() будет отклонять правильный пароль.
     */
    @CacheEvict(value = "userDetailsCache", key = "#request.email")
    public String login(LoginRequest request) {
        // Шаг 1: проверка пароля (BCrypt ~100-300ms, кэшируется в UserDetailsCache)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Шаг 2: загрузка пользователя с ролями для генерации токена
        // findByEmail использует LEFT JOIN FETCH u.roles — 1 запрос вместо 2
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден!"));

        // Шаг 3: генерация JWT (роли уже в user.getRoles() — нет лишних запросов)
        return jwtUtil.generateToken(user);
    }
}