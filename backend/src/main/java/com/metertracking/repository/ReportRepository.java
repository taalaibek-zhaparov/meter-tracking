package com.metertracking.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class ReportRepository {

    @PersistenceContext
    private EntityManager em;

    // ── KPI ──────────────────────────────────────────────────────────

    public Long totalPlans() {
        return (Long) em.createQuery("SELECT COUNT(p) FROM Plan p").getSingleResult();
    }

    public Long completedPlans() {
        return (Long) em.createQuery("SELECT COUNT(p) FROM Plan p WHERE p.completed = true").getSingleResult();
    }

    public Long activeMasters() {
        return (Long) em.createQuery(
                "SELECT COUNT(DISTINCT p.user) FROM Plan p WHERE p.user IS NOT NULL"
        ).getSingleResult();
    }

    public Long totalRegions() {
        return (Long) em.createQuery(
                "SELECT COUNT(DISTINCT p.region) FROM Plan p"
        ).getSingleResult();
    }

    // CAST вместо :: — Hibernate не экранирует CAST
    public Long completedToday() {
        Object r = em.createNativeQuery(
                "SELECT COUNT(*) FROM completed_task_data " +
                        "WHERE CAST(created_at AS DATE) = CURRENT_DATE"
        ).getSingleResult();
        return r != null ? ((Number) r).longValue() : 0L;
    }

    public Long completedThisWeek() {
        Object r = em.createNativeQuery(
                "SELECT COUNT(*) FROM completed_task_data " +
                        "WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'"
        ).getSingleResult();
        return r != null ? ((Number) r).longValue() : 0L;
    }

    public Long completedThisMonth() {
        Object r = em.createNativeQuery(
                "SELECT COUNT(*) FROM completed_task_data " +
                        "WHERE DATE_TRUNC('month', created_at) = DATE_TRUNC('month', CURRENT_DATE)"
        ).getSingleResult();
        return r != null ? ((Number) r).longValue() : 0L;
    }

    // ── ПО РЭС ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Object[]> regionStats() {
        return em.createNativeQuery(
                "SELECT " +
                        "  p.region, " +
                        "  COUNT(p.id) AS total_plans, " +
                        "  SUM(CASE WHEN p.completed THEN 1 ELSE 0 END) AS completed, " +
                        "  SUM(CASE WHEN NOT p.completed THEN 1 ELSE 0 END) AS pending " +
                        "FROM plan_data p " +
                        "GROUP BY p.region " +
                        "ORDER BY completed DESC"
        ).getResultList();
    }

    // ── ПО МАСТЕРАМ ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    // В ReportRepository.java замените masterStats():
    public List<Object[]> masterStats() {
        return em.createNativeQuery(
                "SELECT " +
                        "  u.username, " +
                        "  u.region, " +
                        "  COALESCE(plans.total, 0)     AS total, " +
                        "  COALESCE(plans.completed, 0) AS completed, " +
                        "  COALESCE(plans.pending, 0)   AS pending, " +
                        "  CAST(tasks.last_act AS DATE) AS last_activity " +
                        "FROM user_data u " +
                        // Планы — отдельный подзапрос
                        "LEFT JOIN ( " +
                        "  SELECT user_id, " +
                        "    COUNT(*) AS total, " +
                        "    SUM(CASE WHEN completed THEN 1 ELSE 0 END) AS completed, " +
                        "    SUM(CASE WHEN NOT completed THEN 1 ELSE 0 END) AS pending " +
                        "  FROM plan_data GROUP BY user_id " +
                        ") plans ON plans.user_id = u.id " +
                        // Последняя активность — отдельный подзапрос
                        "LEFT JOIN ( " +
                        "  SELECT user_id, MAX(created_at) AS last_act " +
                        "  FROM completed_task_data GROUP BY user_id " +
                        ") tasks ON tasks.user_id = u.id " +
                        "WHERE u.deleted = false " +
                        "  AND EXISTS ( " +
                        "    SELECT 1 FROM user_role ur " +
                        "    JOIN role_data r ON r.id = ur.role_id " +
                        "    WHERE ur.user_id = u.id AND r.name = 'USER' " +
                        "  ) " +
                        "ORDER BY completed DESC NULLS LAST"
        ).getResultList();
    }

    // ── ПО ТИПАМ СЧЁТЧИКОВ ───────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Object[]> meterTypeStats() {
        return em.createNativeQuery(
                "SELECT ct.tip, COUNT(*) AS cnt " +
                        "FROM completed_task_data ct " +
                        "WHERE ct.tip IS NOT NULL AND ct.tip <> '' " +
                        "GROUP BY ct.tip " +
                        "ORDER BY cnt DESC " +
                        "LIMIT 15"
        ).getResultList();
    }

    // ── ДИНАМИКА ПО ДНЯМ ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Object[]> dailyDynamics() {
        return em.createNativeQuery(
                "SELECT " +
                        "  CAST(ct.created_at AS DATE) AS day, " +
                        "  COUNT(*) AS cnt " +
                        "FROM completed_task_data ct " +
                        "WHERE ct.created_at >= CURRENT_DATE - INTERVAL '30 days' " +
                        "GROUP BY CAST(ct.created_at AS DATE) " +
                        "ORDER BY day"
        ).getResultList();
    }
}
