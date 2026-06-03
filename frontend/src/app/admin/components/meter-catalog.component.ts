import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';
import { MeterCatalogService, MeterModelDTO, MeterImportResponse } from '../../shared/services/meter-catalog.service';

@Component({
  standalone: false,
  selector: 'app-meter-catalog',
  templateUrl: './meter-catalog.component.html',
  styleUrls: ['./meter-catalog.component.css']
})
export class MeterCatalogComponent implements OnInit {

  @ViewChild('jsonFileInput') jsonFileInput!: ElementRef<HTMLInputElement>;

  // ── Данные ──────────────────────────────────────────────────────────
  allModels:      MeterModelDTO[] = [];
  filteredModels: MeterModelDTO[] = [];
  searchQuery = '';
  loading = false;

  // ── Пагинация ────────────────────────────────────────────────────────
  currentPage = 1;
  pageSize    = 15;

  // ── Импорт из 1С ─────────────────────────────────────────────────────
  showImportModal = false;
  importJsonText  = '';
  importLoading   = false;
  importResult:   MeterImportResponse | null = null;
  importError     = '';

  // ── Детали модели ─────────────────────────────────────────────────────
  selectedModel: MeterModelDTO | null = null;

  // ── Уведомления ──────────────────────────────────────────────────────
  successMessage = '';
  errorMessage   = '';

  // Пример JSON для подсказки в форме импорта
  readonly exampleJson = JSON.stringify({
    items: [
      { Код: 'МЕР-201-001', Наименование: 'Меркурий 201.5', Ампер: '5(80)А', Значность: 7, Фазность: 1, Напряжение: '220 В' },
      { Код: 'МЕР-206-003', Наименование: 'Меркурий 206',   Ампер: '100A',    Значность: 7, Фазность: 3, Напряжение: '3х220/380В' }
    ]
  }, null, 2);

  constructor(private catalogService: MeterCatalogService) {}

  ngOnInit(): void { this.loadModels(); }

  // ── Загрузка ──────────────────────────────────────────────────────────

  loadModels(): void {
    this.loading = true;
    this.catalogService.getAllModels().subscribe({
      next:  (models) => { this.allModels = models; this.applySearch(); this.loading = false; },
      error: ()       => { this.errorMessage = 'Ошибка загрузки справочника'; this.loading = false; }
    });
  }

  applySearch(): void {
    this.currentPage = 1;
    const q = this.searchQuery.trim().toLowerCase();
    this.filteredModels = !q
      ? [...this.allModels]
      : this.allModels.filter(m =>
          m.name.toLowerCase().includes(q) ||
          m.code.toLowerCase().includes(q)
        );
  }

  // ── Детали модели ─────────────────────────────────────────────────────

  openModel(model: MeterModelDTO): void {
    this.selectedModel = this.selectedModel?.id === model.id ? null : model;
  }

  // ── Импорт из 1С ─────────────────────────────────────────────────────

  openImportModal(): void {
    this.showImportModal = true;
    this.importJsonText  = '';
    this.importResult    = null;
    this.importError     = '';
  }

  closeImportModal(): void {
    this.showImportModal = false;
    if (this.importResult && this.importResult.modelsCreated > 0) {
      this.loadModels();
    }
  }

  loadExampleJson(): void { this.importJsonText = this.exampleJson; }

  triggerJsonFile(): void { this.jsonFileInput.nativeElement.click(); }

  onJsonFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => { this.importJsonText = e.target?.result as string; };
    reader.readAsText(file, 'UTF-8');
  }

  runImport(): void {
    this.importError  = '';
    this.importResult = null;

    let payload: any;
    try {
      payload = JSON.parse(this.importJsonText);
    } catch {
      this.importError = 'Неверный JSON. Проверьте формат.';
      return;
    }

    if (!payload.items || !Array.isArray(payload.items)) {
      this.importError = 'JSON должен содержать поле "items" — массив записей.';
      return;
    }

    this.importLoading = true;
    this.catalogService.importFromOnec(payload).subscribe({
      next: (result) => {
        this.importResult  = result;
        this.importLoading = false;
        if (result.modelsCreated > 0 || result.specsCreated > 0) {
          this.showSuccess(`Импорт завершён: +${result.modelsCreated} моделей, +${result.specsCreated} вариантов`);
        }
      },
      error: (e) => {
        this.importError   = e.error?.message || 'Ошибка импорта';
        this.importLoading = false;
      }
    });
  }

  // ── Геттеры ───────────────────────────────────────────────────────────

  get paginatedModels(): MeterModelDTO[] {
    const s = (this.currentPage - 1) * this.pageSize;
    return this.filteredModels.slice(s, s + this.pageSize);
  }

  get totalPages(): number { return Math.ceil(this.filteredModels.length / this.pageSize); }

  get totalSpecsCount(): number {
    return this.allModels.reduce((sum, m) => sum + (m.specs?.length || 0), 0);
  }

  // ── Утилиты ───────────────────────────────────────────────────────────

  formatSpec(spec: any): string {
    const phase = spec.phase === 1 ? '1ф' : '3ф';
    return `${phase} · ${spec.amp} · ${spec.digits}зн · ${spec.voltage}`;
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => this.successMessage = '', 5000);
  }
}
