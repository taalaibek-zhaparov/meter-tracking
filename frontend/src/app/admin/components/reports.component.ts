import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../shared/services/admin.service';

@Component({
  standalone: false,
  selector: 'app-reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.css']
})
export class ReportsComponent implements OnInit {

  loading = false;
  dashboard: any = null;
  errorMessage = '';
  lastUpdated = '';

  // Активная вкладка в блоке детализации
  activeDetail: 'regions' | 'masters' | 'meters' = 'regions';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.adminService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard    = data;
        this.lastUpdated  = new Date().toLocaleTimeString('ru-RU');
        this.loading      = false;
      },
      error: () => {
        this.errorMessage = 'Ошибка загрузки отчёта';
        this.loading      = false;
      }
    });
  }

  // ── Геттеры ──────────────────────────────────────────────────────

  get kpi()         { return this.dashboard?.kpi || {}; }
  get byRegion()    { return this.dashboard?.byRegion || []; }
  get byMaster()    { return this.dashboard?.byMaster || []; }
  get byMeterType() { return this.dashboard?.byMeterType || []; }
  get daily()       { return this.dashboard?.dailyDynamics || []; }
  get problems()    { return this.dashboard?.problems || []; }
  get top()         { return this.dashboard?.top || {}; }

  // ── Цвет статуса РЭС ─────────────────────────────────────────────
  statusClass(status: string): string {
    if (status === 'GREEN')  return 'status-green';
    if (status === 'YELLOW') return 'status-yellow';
    return 'status-red';
  }

  statusLabel(status: string): string {
    if (status === 'GREEN')  return ' Норма';
    if (status === 'YELLOW') return ' Умеренно';
    return '🔴 Критично';
  }

  // ── Цвет прогресс-бара ───────────────────────────────────────────
  barColor(rate: number): string {
    if (rate >= 80) return '#22c55e';
    if (rate >= 50) return '#f59e0b';
    return '#ef4444';
  }

  // ── Спарклайн — мини-гистограмма из CSS ──────────────────────────
  get maxDailyCount(): number {
    return Math.max(...this.daily.map((d: any) => d.count), 1);
  }

  barHeight(count: number): number {
    return Math.round((count / this.maxDailyCount) * 60);
  }

  // ── Ширина полосы для типов счётчиков ────────────────────────────
  meterBarWidth(percent: number): number {
    return Math.max(percent, 2);
  }

  // ── Цвет мастера ─────────────────────────────────────────────────
  masterRateClass(rate: number): string {
    if (rate >= 80) return 'rate-good';
    if (rate >= 40) return 'rate-mid';
    return 'rate-bad';
  }
}
