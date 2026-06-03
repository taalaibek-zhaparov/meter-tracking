import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../shared/services/auth.service';
import { AdminService } from '../../shared/services/admin.service';
import { WebSocketService } from '../../shared/services/websocket.service';
import { User } from '../../shared/models/user.model';
import { Plan, CompletedTask, PlanRequest } from '../../shared/models/plan.model';
import { Subscription } from 'rxjs';

@Component({
  standalone: false,
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit, OnDestroy {

  activeTab = 'create-plan';

  users: User[] = [];
  plans: Plan[] = [];
  completedTasks: CompletedTask[] = [];
  searchPlans = '';
  searchCompleted = '';

  currentPlanPage = 1;
  planPageSize = 10;

  currentCompletedPage = 1;
  completedPageSize = 10;

  showCreateUserModal = false;
  showPasswordInModal = false;

  newUserData = {
    email: '',
    password: '',
    username: '',
    role: 'USER',
    region: ''
  };

 readonly regions = [
  'Манас РЭС', 'Кок-Арт РЭС', 'Сузак РЭС', 'Базар-Коргон РЭС',
  'Ноокен РЭС', 'Майлуу-Суу РЭС', 'Таш-Комур РЭС', 'Аксы РЭС',
  'Ала-Бука РЭС', 'Чаткал РЭС', 'Кара-Куль РЭС', 'Токтогул РЭС'
];

  planData: PlanRequest = {
    userId: 0,
    region: '',
    tp: '',
    licevoy: '',
    tip: '',
    nomerSchetchika: '',
    pokazaniya: 0,
    data: '',
    // НОВЫЕ ПОЛЯ
    fio: '',
    adres: '',
    documentType: ''
  };

  isLoadedFrom1C = false;
  errorMessage = '';
  successMessage = '';

  private planUpdatesSubscription?: Subscription;
  private taskUpdatesSubscription?: Subscription;
  private userUpdatesSubscription?: Subscription;

  constructor(
    private authService: AuthService,
    private adminService: AdminService,
    private webSocketService: WebSocketService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUsers();
    this.loadPlans();
    this.loadCompletedTasks();

    this.planUpdatesSubscription = this.webSocketService.planUpdates$.subscribe(() => {
      this.loadPlans();
    });
    this.taskUpdatesSubscription = this.webSocketService.taskUpdates$.subscribe(() => {
      this.loadCompletedTasks();
    });
    this.userUpdatesSubscription = this.webSocketService.userUpdates$.subscribe(() => {
      this.loadUsers();
    });
  }

  ngOnDestroy(): void {
    this.planUpdatesSubscription?.unsubscribe();
    this.taskUpdatesSubscription?.unsubscribe();
    this.userUpdatesSubscription?.unsubscribe();
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
  }

  loadUsers(): void {
    this.adminService.getAllUsers().subscribe({
      next: users => this.users = users,
      error: error => console.error('Error loading users:', error)
    });
  }

  loadPlans(): void {
    this.adminService.getAllPlans().subscribe({
      next: plans => this.plans = plans,
      error: error => console.error('Error loading plans:', error)
    });
  }

  loadCompletedTasks(): void {
    this.adminService.getAllCompletedTasks().subscribe({
      next: tasks => this.completedTasks = tasks,
      error: error => console.error('Error loading completed tasks:', error)
    });
  }

  // ── Пользователи ──

  openCreateUserModal(): void {
    this.showCreateUserModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeCreateUserModal(): void {
    this.showCreateUserModal = false;
    this.newUserData = { email: '', password: '', username: '', role: 'USER', region: '' };
    this.showPasswordInModal = false;
  }

  createUser(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.adminService.createUser(this.newUserData).subscribe({
      next: () => {
        this.successMessage = 'Пользователь успешно создан!';
        this.loadUsers();
        setTimeout(() => this.closeCreateUserModal(), 2000);
      },
      error: error => this.errorMessage = error.error?.error || 'Ошибка при создании пользователя'
    });
  }

  deleteUser(userId: number, username: string): void {
    if (confirm(`Вы уверены, что хотите удалить пользователя "${username}"?`)) {
      this.adminService.deleteUser(userId).subscribe({
        next: () => {
          this.successMessage = 'Пользователь успешно удалён!';
          this.loadUsers();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: error => {
          this.errorMessage = error.error?.error || 'Ошибка при удалении пользователя';
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    }
  }

  togglePasswordInModal(): void {
    this.showPasswordInModal = !this.showPasswordInModal;
  }

  // ── Планы ──

  onUserSelected(): void {
    const selectedUser = this.users.find(u => u.id === +this.planData.userId);
    if (selectedUser?.region) {
      this.planData.region = selectedUser.region;
    } else {
      this.planData.region = '';
    }
  }

  createPlan(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.adminService.createPlan(this.planData).subscribe({
      next: () => {
        this.successMessage = 'План успешно создан!';
        this.loadPlans();
        this.planData = {
          userId: 0, region: '', tp: '', licevoy: '', tip: '',
          nomerSchetchika: '', pokazaniya: 0, data: '',
          fio: '', adres: ''
        };
        this.onecData = null;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: error => this.errorMessage = error.error?.error || 'Ошибка при создании плана'
    });
  }

  deletePlan(planId: number): void {
    if (confirm('Вы уверены, что хотите удалить этот план?')) {
      this.adminService.deletePlan(planId).subscribe({
        next: () => {
          this.successMessage = 'План успешно удалён!';
          this.loadPlans();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: error => {
          this.errorMessage = error.error?.error || 'Ошибка при удалении плана';
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // ── Фильтры и пагинация ──

  get filteredPlans(): Plan[] {
    if (!this.searchPlans.trim()) return this.plans;
    const q = this.searchPlans.toLowerCase();
    return this.plans.filter(p =>
      p.region?.toLowerCase().includes(q) ||
      p.tp?.toLowerCase().includes(q) ||
      p.licevoy?.toLowerCase().includes(q) ||
      p.tip?.toLowerCase().includes(q) ||
      p.nomerSchetchika?.toLowerCase().includes(q) ||
      p.user?.username?.toLowerCase().includes(q) ||
      // НОВЫЕ ПОЛЯ в поиске
      p.fio?.toLowerCase().includes(q) ||
      p.adres?.toLowerCase().includes(q)
    );
  }

  get filteredCompleted(): CompletedTask[] {
    if (!this.searchCompleted.trim()) return this.completedTasks;
    const q = this.searchCompleted.toLowerCase();
    return this.completedTasks.filter(t =>
      t.region?.toLowerCase().includes(q) ||
      t.tp?.toLowerCase().includes(q) ||
      t.licevoy?.toLowerCase().includes(q) ||
      t.nomerSchetchika?.toLowerCase().includes(q) ||
      t.nomerPlomby?.toLowerCase().includes(q) ||
      t.nomerSimKarty?.toLowerCase().includes(q) ||
      t.nomerIccid?.toLowerCase().includes(q) ||
      t.user?.username?.toLowerCase().includes(q) ||
      // НОВЫЕ ПОЛЯ в поиске
      t.fio?.toLowerCase().includes(q) ||
      t.adres?.toLowerCase().includes(q)
    );
  }

  get paginatedPlans(): Plan[] {
    const start = (this.currentPlanPage - 1) * this.planPageSize;
    return this.filteredPlans.slice(start, start + this.planPageSize);
  }

  get totalPlanPages(): number {
    return Math.ceil(this.filteredPlans.length / this.planPageSize);
  }

  get visiblePlanPages(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(1, this.currentPlanPage - Math.floor(maxVisible / 2));
    let end = Math.min(this.totalPlanPages, start + maxVisible - 1);
    if (end - start < maxVisible - 1) start = Math.max(1, end - maxVisible + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  get paginatedCompleted(): CompletedTask[] {
    const start = (this.currentCompletedPage - 1) * this.completedPageSize;
    return this.filteredCompleted.slice(start, start + this.completedPageSize);
  }

  get totalCompletedPages(): number {
    return Math.ceil(this.filteredCompleted.length / this.completedPageSize);
  }

  get visibleCompletedPages(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(1, this.currentCompletedPage - Math.floor(maxVisible / 2));
    let end = Math.min(this.totalCompletedPages, start + maxVisible - 1);
    if (end - start < maxVisible - 1) start = Math.max(1, end - maxVisible + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  // ── 1С HTTP ──

  onecStatus = false;
  onecData: any = null;
  loadingOnec = false;

  fetchFromOnec(): void {
    if (!this.planData.licevoy) return;
    this.loadingOnec = true;
    this.onecData = null;
    this.adminService.getMeterFromOnec(this.planData.licevoy).subscribe({
      next: (data: any) => {
        this.onecData = data;
        this.planData.tp             = data.tp       || this.planData.tp;
        this.planData.tip            = data.meterType  || this.planData.tip;
        this.planData.nomerSchetchika = data.meterNumber || this.planData.nomerSchetchika;
        this.planData.pokazaniya     = data.reading   || this.planData.pokazaniya;
        this.planData.data           = data.date      || this.planData.data;
        // НОВЫЕ ПОЛЯ: ФИО и Адрес из 1С
        this.planData.fio   = data.fio   || '';
        this.planData.adres = data.adres || '';
        // Регион НЕ перезаписываем — берём от пользователя
        this.loadingOnec = false;
      },
      error: () => {
        this.errorMessage = 'Не удалось получить данные из 1С. Введите данные вручную.';
        this.loadingOnec = false;
      }
    });
  }

  // ── Excel ──

  downloadExcel(type: 'plans' | 'completed-tasks'): void {
    const obs = type === 'plans'
      ? this.adminService.exportPlans()
      : this.adminService.exportCompletedTasks();
    obs.subscribe((blob: Blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = type === 'plans' ? 'plans-report.xlsx' : 'completed-tasks.xlsx';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }
}