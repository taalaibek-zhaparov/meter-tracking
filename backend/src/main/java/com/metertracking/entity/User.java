package com.metertracking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Сущность пользователя системы.
 *
 * Soft delete: вместо физического удаления строки из БД
 * @SQLDelete устанавливает deleted=true.
 * @Where фильтрует удалённых при обычных запросах.
 *
 * Безопасность:
 * @JsonIgnore на password — пароль никогда не попадает в JSON ответ.
 * roles — LAZY, загружается только при явном JOIN FETCH.
 */
@Entity
@Table(name = "user_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE user_data SET deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String username;

    @JsonIgnore
    @Column(name = "deleted")
    private Boolean deleted = false;

    @JsonIgnore
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "region")
    private String region;

    /**
     * Роли пользователя. LAZY — загружаются только при JOIN FETCH.
     * @JsonIgnoreProperties защищает от циклических ссылок Role→User.
     * cascade = MERGE — при сохранении User роли обновляются, но не удаляются.
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<Role> roles = new HashSet<>();
}