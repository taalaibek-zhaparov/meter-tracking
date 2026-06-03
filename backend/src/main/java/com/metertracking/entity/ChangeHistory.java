package com.metertracking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность истории изменений.
 * @JsonIgnoreProperties защищает от LazyInit при сериализации.
 */
@Entity
@Table(name = "change_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_task_id")
    @JsonIgnoreProperties({"user", "plan", "hibernateLazyInitializer", "handler"})
    private CompletedTask completedTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnoreProperties({"roles", "password", "deleted", "deletedAt", "hibernateLazyInitializer", "handler"})
    private User user;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "change_time", nullable = false)
    private LocalDateTime changeTime;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "action_type", nullable = false)
    private String actionType;
}