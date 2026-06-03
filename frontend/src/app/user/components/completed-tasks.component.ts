import { Component, OnInit } from '@angular/core';
import { UserService } from '../../shared/services/user.service';
import { CompletedTask, UpdateCompletedTaskRequest, ChangeHistory } from '../../shared/models/plan.model';
import { PdfProtocolService } from '../../shared/services/pdf-protocol.service';

@Component({
  standalone: false,
  selector: 'app-completed-tasks',
  templateUrl: './completed-tasks.component.html',
  styleUrls: ['./completed-tasks.component.css'],
  host: { style: 'display: block; width: 100%; background: #f7f8fa;' }
})
export class CompletedTasksComponent implements OnInit {
  completedTasks: CompletedTask[] = [];
  loading = false;

  searchText = ''; filterRegion = ''; filterDateFrom = ''; filterDateTo = '';
  regions: string[] = [];
  currentPage = 1; pageSize = 10;

  showEditModal = false;
  selectedTask: CompletedTask | null = null;

  editTaskData: UpdateCompletedTaskRequest & {
    newPhases?: number | null;
    newAmperage?: number | null;
    fio?: string;
    adres?: string;
  } = {
    completedTaskId: 0, region: '', tp: '', licevoy: '', tip: '',
    nomerSchetchika: '', pokazaniya: 0, data: '',
    nomerPlomby: '', nomerSimKarty: '', nomerIccid: '',
    naKryshke: '', naYashike: '', plombaGos: '',
    newPhases: null, newAmperage: null,
    fio: '', adres: ''
  };

  editPlombaGosPrefix = '';
  showHistoryModal = false; taskHistory: ChangeHistory[] = []; loadingHistory = false;
  errorMessage = ''; successMessage = '';

  constructor(
    private userService: UserService,
    private pdfService: PdfProtocolService
  ) {}

  ngOnInit(): void { this.loadCompletedTasks(); }

  loadCompletedTasks(): void {
    this.loading = true;
    this.userService.getMyCompletedTasks().subscribe({
      next: (tasks) => { this.completedTasks = tasks; this.extractRegions(); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  extractRegions(): void {
    this.regions = [...new Set(this.completedTasks.map(t => t.region))].sort();
  }

  get filteredTasks(): CompletedTask[] {
    let f = [...this.completedTasks].sort((a, b) => (b.id || 0) - (a.id || 0));
    if (this.searchText) {
      const s = this.searchText.toLowerCase();
      f = f.filter(t =>
        t.region.toLowerCase().includes(s)          ||
        t.tp.toLowerCase().includes(s)              ||
        t.licevoy.toLowerCase().includes(s)         ||
        t.nomerSchetchika.toLowerCase().includes(s) ||
        t.nomerPlomby.toLowerCase().includes(s)     ||
        (t.fio   || '').toLowerCase().includes(s)   ||
        (t.adres || '').toLowerCase().includes(s)
      );
    }
    if (this.filterRegion)   f = f.filter(t => t.region === this.filterRegion);
    if (this.filterDateFrom) f = f.filter(t => t.data >= this.filterDateFrom);
    if (this.filterDateTo)   f = f.filter(t => t.data <= this.filterDateTo);
    return f;
  }

  get paginatedTasks(): CompletedTask[] {
    return this.filteredTasks.slice(
      (this.currentPage - 1) * this.pageSize,
       this.currentPage      * this.pageSize
    );
  }

  get totalPages(): number {
    return Math.ceil(this.filteredTasks.length / this.pageSize);
  }

  get visiblePages(): number[] {
    const p: number[] = [], mv = 5;
    let s = Math.max(1, this.currentPage - Math.floor(mv / 2));
    let e = Math.min(this.totalPages, s + mv - 1);
    if (e - s < mv - 1) s = Math.max(1, e - mv + 1);
    for (let i = s; i <= e; i++) p.push(i);
    return p;
  }

  goToPage(p: number): void { this.currentPage = p; }
  previousPage(): void { if (this.currentPage > 1) this.currentPage--; }
  nextPage(): void { if (this.currentPage < this.totalPages) this.currentPage++; }

  resetFilters(): void {
    this.searchText = ''; this.filterRegion = '';
    this.filterDateFrom = ''; this.filterDateTo = '';
    this.currentPage = 1;
  }

  exportToExcel(): void {
    this.userService.exportMyCompletedTasks().subscribe((blob: Blob) => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'my-completed-tasks.xlsx';
      a.click();
    });
  }

  openEditModal(task: CompletedTask): void {
    this.selectedTask = task;
    this.editTaskData = {
      completedTaskId: task.id,
      region:          task.region,
      tp:              task.tp,
      licevoy:         task.licevoy,
      tip:             task.tip,
      nomerSchetchika: task.nomerSchetchika,
      pokazaniya:      task.pokazaniya,
      data:            task.data,
      nomerPlomby:     task.nomerPlomby,
      nomerSimKarty:   task.nomerSimKarty,
      nomerIccid:      task.nomerIccid,
      naKryshke:       task.naKryshke || '',
      naYashike:       task.naYashike || '',
      plombaGos:       task.plombaGos || '',
      newPhases:       task.phases   ?? null,
      newAmperage:     task.amperage ?? null,
      fio:             task.fio   || '',
      adres:           task.adres || ''
    };
    this.editPlombaGosPrefix = task.plombaGos || '';
    this.showEditModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedTask = null;
    this.editPlombaGosPrefix = '';
  }

  updateEditPlombaGos(): void {
    this.editTaskData.plombaGos = this.editPlombaGosPrefix;
  }

  updateTask(): void {
    this.errorMessage = ''; this.successMessage = '';
    this.userService.updateTask(this.editTaskData).subscribe({
      next: () => {
        this.successMessage = 'Задача успешно обновлена!';
        setTimeout(() => { this.closeEditModal(); this.loadCompletedTasks(); }, 2000);
      },
      error: (e) => { this.errorMessage = e.error?.error || 'Ошибка при обновлении'; }
    });
  }

  // ИСПРАВЛЕНО: async, generateProtocol теперь возвращает Promise<boolean>
  async downloadProtocol(task: any): Promise<void> {
    await this.pdfService.generateProtocol({
      id:                 task.id,
      region:             task.region,
      tp:                 task.tp,
      licevoy:            task.licevoy,
      fio:                task.fio   || '',
      adres:              task.adres || '',
      tip:                task.tip,
      oldNomerSchetchika: task.oldNomerSchetchika || task.nomerSchetchika,
      oldMeterType:       task.oldMeterType       || task.tip,
      oldPokazaniya:      task.oldPokazaniya      ?? task.pokazaniya,
      nomerSchetchika:    task.nomerSchetchika,
      newMeterType:       task.newMeterType        || task.tip,
      newPokazaniya:      task.newPokazaniya       ?? 0,
      newPhases:          task.phases   ?? null,
      newAmperage:        task.amperage ?? null,
      pokazaniya:         task.pokazaniya,
      data:               task.data,
      plombaGos:          task.plombaGos,
      nomerPlomby:        task.nomerPlomby,
      naKryshke:          task.naKryshke,
      naYashike:          task.naYashike,
      masterName:         task.user?.username,
      documentNumber:     task.documentNumber,
      znch:               task.znch,
      signatureAbonent:   task.signatureAbonent || undefined,
      signatureMaster:    task.signatureMaster  || undefined,
      documentType:       task.documentType,
    });
  }

  openHistoryModal(taskId: number): void {
    this.showHistoryModal = true;
    this.loadingHistory = true;
    this.taskHistory = [];
    this.userService.getTaskHistory(taskId).subscribe({
      next: (h) => { this.taskHistory = h; this.loadingHistory = false; },
      error: () => { this.loadingHistory = false; }
    });
  }

  closeHistoryModal(): void {
    this.showHistoryModal = false;
    this.taskHistory = [];
  }

  getTodayDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  getFieldNameRu(f: string): string {
    const m: { [k: string]: string } = {
      region:          'Регион',
      tp:              'ТП',
      licevoy:         'Лицевой счет',
      tip:             'Тип счетчика',
      nomerSchetchika: 'Номер счетчика',
      pokazaniya:      'Показания',
      data:            'Дата',
      nomerPlomby:     'Одноразовая пломба',
      nomerSimKarty:   'Номер SIM-карты',
      nomerIccid:      'Номер ICCID',
      naKryshke:       'На крышке',
      naYashike:       'На ящике',
      plombaGos:       'Пломба гос.',
      fio:             'ФИО',
      adres:           'Адрес',
      'Задача создана':'Задача создана'
    };
    return m[f] || f;
  }
}