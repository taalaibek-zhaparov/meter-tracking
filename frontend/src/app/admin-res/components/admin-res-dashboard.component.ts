import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../shared/services/auth.service';
import { AdminResService } from '../admin-res.service';

@Component({
  standalone: false,
  selector: 'app-admin-res-dashboard',
  templateUrl: './admin-res-dashboard.component.html',
  styleUrls: ['./admin-res-dashboard.component.css']
})
export class AdminResDashboardComponent implements OnInit {

  activeTab: 'create-plan' | 'plans' | 'completed' = 'plans';

  me: any = null;
  users: any[] = [];
  plans: any[] = [];
  completedTasks: any[] = [];
  loading = false;

  // Поиск
  searchPlans = '';
  searchCompleted = '';

  // Пагинация
  planPage = 1; planPageSize = 10;
  completedPage = 1; completedPageSize = 10;

  // Форма создания плана
  planData: any = {
    userId: 0, region: '', tp: '', licevoy: '', tip: '',
    nomerSchetchika: '', pokazaniya: 0, data: '', fio: '', adres: '',
    documentType: ''
  };
  loadingOnec = false;
  onecData: any = null;
  successMessage = '';
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private adminResService: AdminResService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadMe();
    this.loadUsers();
    this.loadPlans();
    this.loadCompleted();
  }

  loadMe(): void {
    this.adminResService.getMe().subscribe({
      next: (me) => {
        this.me = me;
        this.planData.region = me.region;
      }
    });
  }

  loadUsers(): void {
    this.adminResService.getUsers().subscribe({
      next: (u) => this.users = u,
      error: () => {}
    });
  }

  loadPlans(): void {
    this.loading = true;
    this.adminResService.getPlans().subscribe({
      next: (p) => { this.plans = p; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  loadCompleted(): void {
    this.adminResService.getCompletedTasks().subscribe({
      next: (c) => this.completedTasks = c,
      error: () => {}
    });
  }

  setTab(tab: 'create-plan' | 'plans' | 'completed'): void {
    this.activeTab = tab;
    this.successMessage = '';
    this.errorMessage = '';
  }

  // ── Создание плана ────────────────────────────────────────────────

  onUserSelected(): void {
    const user = this.users.find(u => u.id === +this.planData.userId);
    if (user) this.planData.region = user.region || this.me?.region || '';
  }

  fetchFromOnec(): void {
    if (!this.planData.licevoy) return;
    this.loadingOnec = true;
    this.adminResService.getMeterFromOnec(this.planData.licevoy).subscribe({
      next: (data) => {
        this.onecData = data;
        this.planData.tp             = data.tp || '';
        this.planData.tip            = data.meterType || '';
        this.planData.nomerSchetchika= data.meterNumber || '';
        this.planData.pokazaniya     = data.reading ?? 0;
        this.planData.data           = data.date || '';
        this.planData.fio            = data.fio || '';
        this.planData.adres          = data.adres || '';
        this.loadingOnec = false;
      },
      error: () => { this.loadingOnec = false; this.onecData = null; }
    });
  }

  createPlan(): void {
    this.errorMessage = ''; this.successMessage = '';
    if (!this.planData.userId || this.planData.userId === 0) {
      this.errorMessage = 'Выберите пользователя!'; return;
    }
    const req = { ...this.planData, userId: +this.planData.userId };
    this.adminResService.createPlan(req).subscribe({
      next: () => {
        this.successMessage = 'План успешно создан!';
        this.loadPlans();
        this.planData = { userId: 0, region: this.me?.region || '', tp: '', licevoy: '', tip: '', nomerSchetchika: '', pokazaniya: 0, data: '', fio: '', adres: '', documentType: '' };
        this.onecData = null;
        setTimeout(() => this.successMessage = '', 4000);
      },
      error: (e) => this.errorMessage = e.error?.error || 'Ошибка создания плана'
    });
  }

  deletePlan(planId: number): void {
    if (!confirm('Удалить план?')) return;
    this.adminResService.deletePlan(planId).subscribe({
      next: () => { this.successMessage = 'План удалён!'; this.loadPlans(); setTimeout(() => this.successMessage = '', 3000); },
      error: (e) => this.errorMessage = e.error?.error || 'Ошибка удаления'
    });
  }

  // ── Фильтрация и пагинация ────────────────────────────────────────

  get filteredPlans(): any[] {
    const valid = this.plans
      .filter(p => p && p.licevoy)
      .sort((a, b) => (b.id || 0) - (a.id || 0)); // новые первыми
    const q = this.searchPlans.toLowerCase().trim();
    return !q ? valid : valid.filter(p =>
      (p.licevoy || '').toLowerCase().includes(q) ||
      (p.nomerSchetchika || '').toLowerCase().includes(q) ||
      (p.user?.username || '').toLowerCase().includes(q) ||
      (p.tip || '').toLowerCase().includes(q)
    );
  }

  get visiblePlanPages(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(1, this.planPage - Math.floor(maxVisible / 2));
    let end   = Math.min(this.totalPlanPages, start + maxVisible - 1);
    if (end - start < maxVisible - 1) start = Math.max(1, end - maxVisible + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  get paginatedPlans(): any[] {
    const s = (this.planPage - 1) * this.planPageSize;
    return this.filteredPlans.slice(s, s + this.planPageSize);
  }

  get totalPlanPages(): number { return Math.ceil(this.filteredPlans.length / this.planPageSize); }

  get filteredCompleted(): any[] {
    const sorted = [...this.completedTasks]
      .sort((a, b) => (b.id || 0) - (a.id || 0)); // новые первыми
    const q = this.searchCompleted.toLowerCase();
    return !q ? sorted : sorted.filter(t =>
      (t.licevoy || '').toLowerCase().includes(q) ||
      (t.nomerSchetchika || '').toLowerCase().includes(q) ||
      (t.user?.username || '').toLowerCase().includes(q)
    );
  }

  get visibleCompletedPages(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(1, this.completedPage - Math.floor(maxVisible / 2));
    let end   = Math.min(this.totalCompletedPages, start + maxVisible - 1);
    if (end - start < maxVisible - 1) start = Math.max(1, end - maxVisible + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  get paginatedCompleted(): any[] {
    const s = (this.completedPage - 1) * this.completedPageSize;
    return this.filteredCompleted.slice(s, s + this.completedPageSize);
  }

  get totalCompletedPages(): number { return Math.ceil(this.filteredCompleted.length / this.completedPageSize); }

    get completedCount(): number {
    if (!this.filteredPlans.length) return 0;
    return this.filteredPlans.filter(p => p.completed === true).length;
  }

  get pendingCount(): number {
    if (!this.filteredPlans.length) return 0;
    return this.filteredPlans.filter(p => p.completed === false).length;
  }

  get todayCompleted(): number {
    const today = new Date().toDateString();
    return this.completedTasks.filter(t => new Date(t.createdAt).toDateString() === today).length;
  }

  logout(): void { this.authService.logout(); this.router.navigate(['/login']); }
}
