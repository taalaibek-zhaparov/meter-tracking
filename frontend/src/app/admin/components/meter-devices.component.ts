import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { AdminService } from '../../shared/services/admin.service';
import { MeterCatalogService, MeterModelDTO, MeterSpecDTO } from '../../shared/services/meter-catalog.service';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';

interface MeterDevice {
  id: number;
  meterNumber: string;
  meterType: string;
  simCardNumber: string;
  iccidNumber: string;
  phases: number | null;
  amperage: number | null;
  znch: number | null;
  voltage: string;
  available: boolean;
  meterModelCode?: string;
  meterModelName?: string;
}

interface NewDeviceForm {
  meterNumber: string;
  simCardNumber: string;
  iccidNumber: string;
  available: boolean;
  // Из справочника (readonly)
  meterType: string;
  phases: number | null;
  amperage: number | null;
  znch: number | null;
  voltage: string;
  meterModelCode: string;
  selectedSpecId: number | null;
}

interface EditForm {
  id: number;
  meterNumber: string;
  simCardNumber: string;
  iccidNumber: string;
  available: boolean;
}

@Component({
  standalone: false,
  selector: 'app-meter-devices',
  templateUrl: './meter-devices.component.html',
  styleUrls: ['./meter-devices.component.css'],
})
export class MeterDevicesComponent implements OnInit, OnDestroy {

  @ViewChild('videoEl')  videoEl!:  ElementRef<HTMLVideoElement>;
  @ViewChild('csvInput') csvInput!: ElementRef<HTMLInputElement>;

  currentPage = 1;
  pageSize    = 10;

  allDevices:      MeterDevice[] = [];
  filteredDevices: MeterDevice[] = [];
  searchQuery = '';
  loading = false;

  // ── Форма добавления ─────────────────────────────────────────────
  showAddForm = false;
  newDevice: NewDeviceForm = this.emptyForm();

  // ── Форма редактирования (только разрешённые поля) ───────────────
  editingDevice: EditForm | null = null;
  editingOriginal: MeterDevice | null = null;

  // ── Поиск по справочнику ─────────────────────────────────────────
  catalogQuery        = '';
  catalogResults:     MeterModelDTO[] = [];
  selectedModel:      MeterModelDTO | null = null;
  showCatalogDropdown = false;
  catalogLoading      = false;
  private catalogSearch$ = new Subject<string>();

  // ── Сканер ───────────────────────────────────────────────────────
  showScanner  = false;
  scannerError = '';
  private stream:       MediaStream | null = null;
  private scanInterval: any               = null;
  scanResult = '';

  // ── Импорт CSV ───────────────────────────────────────────────────
  importError   = '';
  importSuccess = '';
  importPreview: any[] = [];
  showImportPreview = false;

  successMessage = '';
  errorMessage   = '';

  constructor(
    private adminService:   AdminService,
    private catalogService: MeterCatalogService
  ) {}

  ngOnInit(): void {
    this.loadDevices();
    this.catalogSearch$.pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(q => this.doSearchCatalog(q));
  }

  ngOnDestroy(): void { this.stopScanner(); this.catalogSearch$.complete(); }

  private emptyForm(): NewDeviceForm {
    return {
      meterNumber: '', simCardNumber: '', iccidNumber: '', available: true,
      meterType: '', phases: null, amperage: null, znch: null,
      voltage: '', meterModelCode: '', selectedSpecId: null
    };
  }

  loadDevices(): void {
    this.loading = true;
    this.adminService.getAllMeterDevices().subscribe({
      next: (devices: any[]) => { this.allDevices = devices; this.applySearch(); this.loading = false; },
      error: () => { this.errorMessage = 'Ошибка загрузки счётчиков'; this.loading = false; }
    });
  }

  applySearch(): void {
    this.currentPage = 1;
    const q = this.searchQuery.trim().toLowerCase();
    this.filteredDevices = !q ? [...this.allDevices]
      : this.allDevices.filter(d =>
          d.meterNumber.toLowerCase().includes(q) ||
          d.meterType.toLowerCase().includes(q) ||
          (d.simCardNumber || '').toLowerCase().includes(q) ||
          (d.meterModelCode || '').toLowerCase().includes(q)
        );
  }

  openAddForm(): void {
    this.newDevice      = this.emptyForm();
    this.selectedModel  = null;
    this.catalogQuery   = '';
    this.catalogResults = [];
    this.showAddForm    = true;
    this.editingDevice  = null;
  }

  // ── Справочник 1С ────────────────────────────────────────────────

  onCatalogQueryInput(): void {
    this.showCatalogDropdown = true;
    this.catalogSearch$.next(this.catalogQuery);
  }

  private doSearchCatalog(q: string): void {
    if (!q.trim()) { this.catalogResults = []; return; }
    this.catalogLoading = true;
    this.catalogService.searchModelsAdmin(q).subscribe({
      next: (r) => { this.catalogResults = r; this.catalogLoading = false; },
      error: ()  => { this.catalogLoading = false; }
    });
  }

  selectModel(model: MeterModelDTO): void {
    this.selectedModel       = model;
    this.catalogQuery        = model.name;
    this.showCatalogDropdown = false;
    this.catalogResults      = [];
    this.newDevice.meterModelCode = model.code;
    this.newDevice.meterType      = model.name;
    this.newDevice.selectedSpecId = null;
    this.newDevice.phases   = null;
    this.newDevice.amperage = null;
    this.newDevice.znch     = null;
    this.newDevice.voltage  = '';
    if (model.specs && model.specs.length === 1) this.selectSpec(model.specs[0]);
  }

  selectSpec(spec: MeterSpecDTO): void {
    this.newDevice.selectedSpecId = spec.id;
    this.newDevice.phases         = spec.phase;
    this.newDevice.amperage       = this.parseAmperage(spec.amp);
    this.newDevice.znch           = spec.digits;
    this.newDevice.voltage        = spec.voltage;
  }

  clearModel(): void {
    this.selectedModel  = null;
    this.catalogQuery   = '';
    this.newDevice = { ...this.newDevice,
      meterType: '', meterModelCode: '', selectedSpecId: null,
      phases: null, amperage: null, znch: null, voltage: ''
    };
  }

  closeCatalogDropdown(): void { setTimeout(() => { this.showCatalogDropdown = false; }, 200); }

  get isFormValid(): boolean {
    return !!(this.newDevice.meterNumber.trim() && this.selectedModel && this.newDevice.selectedSpecId != null);
  }

  // ── Сохранение нового счётчика ────────────────────────────────────

  saveDevice(): void {
    if (!this.newDevice.meterNumber.trim())    { this.showError('Номер счётчика обязателен!'); return; }
    if (!this.selectedModel)                   { this.showError('Выберите модель из справочника 1С!'); return; }
    if (this.newDevice.selectedSpecId == null) { this.showError('Выберите вариант исполнения!'); return; }

    const payload = {
      meterNumber:   this.newDevice.meterNumber.trim(),
      meterType:     this.newDevice.meterType,
      simCardNumber: this.newDevice.simCardNumber.trim(),
      iccidNumber:   this.newDevice.iccidNumber.trim(),
      phases:        this.newDevice.phases,
      amperage:      this.newDevice.amperage,
      znch:          this.newDevice.znch,
      voltage:       this.newDevice.voltage,
      available:     this.newDevice.available,
    };

    this.adminService.createMeterDevice(payload).subscribe({
      next: (created: any) => {
        this.catalogService.linkDeviceToModel(
          created.id, this.newDevice.meterModelCode, this.newDevice.selectedSpecId!
        ).subscribe({
          next:  () => { this.showSuccess('Счётчик добавлен и привязан к справочнику!'); this.showAddForm = false; this.loadDevices(); },
          error: () => { this.showSuccess('Счётчик добавлен!'); this.showAddForm = false; this.loadDevices(); }
        });
      },
            error: (e: any) => {
        const msg = e.error?.message || e.error?.error || '';
        if (msg.toLowerCase().includes('уже существует') || 
            msg.toLowerCase().includes('unique') || 
            e.status === 409) {
          this.showError(`Счётчик с номером "${this.newDevice.meterNumber}" уже существует в базе!`);
        } else {
          this.showError(msg || 'Ошибка сохранения');
        }
      }
    });
  }

  // ── Редактирование (только номер, SIM, ICCID, статус) ────────────

  startEdit(device: MeterDevice): void {
    this.editingOriginal = device;
    this.editingDevice = {
      id:            device.id,
      meterNumber:   device.meterNumber,
      simCardNumber: device.simCardNumber || '',
      iccidNumber:   device.iccidNumber   || '',
      available:     device.available
    };
    this.showAddForm = false;
  }

  saveEdit(): void {
    if (!this.editingDevice) return;
    const payload = {
      meterNumber:   this.editingDevice.meterNumber,
      meterType:     this.editingOriginal?.meterType || '',
      simCardNumber: this.editingDevice.simCardNumber,
      iccidNumber:   this.editingDevice.iccidNumber,
      phases:        this.editingOriginal?.phases,
      amperage:      this.editingOriginal?.amperage,
      znch:          this.editingOriginal?.znch,
      voltage:       this.editingOriginal?.voltage,
      available:     this.editingDevice.available,
    };
    this.adminService.updateMeterDevice(this.editingDevice.id, payload).subscribe({
      next: () => { this.showSuccess('Счётчик обновлён!'); this.editingDevice = null; this.editingOriginal = null; this.loadDevices(); },
      error: (e: any) => this.showError(e.error?.error || 'Ошибка обновления')
    });
  }

  cancelEdit(): void { this.editingDevice = null; this.editingOriginal = null; }

  deleteDevice(id: number): void {
    if (!confirm('Удалить счётчик из базы?')) return;
    this.adminService.deleteMeterDevice(id).subscribe({
      next: () => { this.showSuccess('Счётчик удалён!'); this.loadDevices(); },
      error: (e: any) => this.showError(e.error?.error || 'Ошибка удаления')
    });
  }

  // ── Сканер ───────────────────────────────────────────────────────

  async openScanner(): Promise<void> {
    this.showScanner = true; this.scannerError = ''; this.scanResult = '';
    await new Promise(r => setTimeout(r, 200));
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } } });
      const video = this.videoEl.nativeElement;
      video.srcObject = this.stream; await video.play();
      if ('BarcodeDetector' in window) {
        const detector = new (window as any).BarcodeDetector({ formats: ['code_128','code_39','ean_13','ean_8','qr_code','data_matrix'] });
        this.scanInterval = setInterval(async () => {
          try { const b = await detector.detect(video); if (b.length > 0) this.onBarcodeDetected(b[0].rawValue); } catch {}
        }, 500);
      } else { this.scannerError = 'BarcodeDetector не поддерживается.'; }
    } catch (err: any) { this.scannerError = 'Нет доступа к камере: ' + (err.message || err); }
  }

  private onBarcodeDetected(code: string): void {
    this.stopScanner(); this.scanResult = code; this.searchQuery = code; this.applySearch();
    if      (this.filteredDevices.length === 1) this.showSuccess(`✅ Найден: ${this.filteredDevices[0].meterNumber}`);
    else if (this.filteredDevices.length === 0) this.showError(`Счётчик "${code}" не найден`);
  }

  stopScanner(): void {
    this.showScanner = false;
    if (this.scanInterval) { clearInterval(this.scanInterval); this.scanInterval = null; }
    if (this.stream) { this.stream.getTracks().forEach(t => t.stop()); this.stream = null; }
  }

  onManualCode(event: Event): void {
    const code = (event.target as HTMLInputElement).value;
    if (code.length >= 3) { this.searchQuery = code; this.applySearch(); }
  }

  // ── Импорт CSV ───────────────────────────────────────────────────

  triggerCsvImport(): void { this.csvInput.nativeElement.click(); }

  onCsvFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.importError = ''; this.importSuccess = '';
    const ext = file.name.split('.').pop()?.toLowerCase();
    if (ext === 'csv') this.parseCsv(file);
    else this.importError = 'Поддерживаются только .csv файлы';
  }

  private parseCsv(file: File): void {
    const reader = new FileReader();
    reader.onload = (e) => {
      const lines = (e.target?.result as string).split('\n').filter(l => l.trim());
      this.showImportPreviewModal(this.processCsvLines(lines));
    };
    reader.readAsText(file, 'UTF-8');
  }

 private processCsvLines(lines: string[]): any[] {
  const rows: any[] = [];
  const dataLines = lines[0].toLowerCase().includes('номер') || lines[0].toLowerCase().includes('meter')
    ? lines.slice(1) : lines;
  for (const line of dataLines) {
    const cols = line.split(';').map(c => c.trim().replace(/^"|"$/g, ''));
    if (cols.length >= 2 && cols[0]) {
      rows.push({
        meterNumber:    cols[0] || '',
        meterModelCode: cols[1] || '',   // ← Код 1С (новая колонка)
        meterType:      cols[2] || '',
        simCardNumber:  cols[3] || '',
        iccidNumber:    cols[4] || '',
        phases:         cols[5] ? parseInt(cols[5]) || null : null,
        amperage:       cols[6] ? parseInt(cols[6]) || null : null,
        znch:           cols[7] ? parseInt(cols[7]) || null : null,
        voltage:        cols[8] || '',
        available: true
      });
    }
  }
  return rows;
}

  private showImportPreviewModal(rows: any[]): void {
    if (rows.length === 0) { this.importError = 'Файл пустой или неправильный формат'; return; }
    this.importPreview = rows; this.showImportPreview = true;
  }

  confirmImport(): void {
    let saved = 0, errors = 0;
    const total = this.importPreview.length;
    this.importPreview.forEach(device => {
      this.adminService.createMeterDevice(device).subscribe({
        next:  () => { saved++;  if (saved + errors === total) this.finishImport(saved, errors); },
        error: () => { errors++; if (saved + errors === total) this.finishImport(saved, errors); }
      });
    });
  }

  private finishImport(saved: number, errors: number): void {
    this.showImportPreview = false; this.importPreview = []; this.loadDevices();
    this.importSuccess = `Импортировано: ${saved}${errors > 0 ? `, ошибок: ${errors}` : ''}`;
    setTimeout(() => this.importSuccess = '', 5000);
    if (this.csvInput) this.csvInput.nativeElement.value = '';
  }

  cancelImport(): void {
    this.showImportPreview = false; this.importPreview = [];
    if (this.csvInput) this.csvInput.nativeElement.value = '';
  }

  exportSql(): void {
    const lines = this.allDevices.map(d =>
      `('${d.meterNumber}','${d.meterType}','${d.simCardNumber||''}','${d.iccidNumber||''}',${d.phases??'NULL'},${d.amperage??'NULL'},${d.znch??'NULL'},'${d.voltage||''}',${d.available})`
    );
    const sql = `INSERT INTO meter_devices (meter_number,meter_type,sim_card_number,iccid_number,phases,amperage,znch,voltage,available)\nVALUES\n${lines.join(',\n')}\nON CONFLICT (meter_number) DO NOTHING;\n`;
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([sql], { type: 'text/plain' }));
    a.download = 'meter_devices_export.sql'; a.click();
  }

  downloadCsvTemplate(): void {
  const header = 'Номер счётчика;Код 1С;Тип счётчика;SIM-карта;ICCID;Фазность;Ампер;Значность;Напряжение';
  const example1 = '084253002990;519;DTS27;996556996633;89999556562663200;3;100;6;3х220/380В';
  const example2 = '0842519930505;500;HXE-110;996777050505;89999656968979636;1;5;6;220В';
  const example3 = '084253000631;506;HXF-300 100B KUK;996501323232;89999026262626116;3;5;6;3х220/380В';
  const notes = [
    '# ИНСТРУКЦИЯ:',
    '# Разделитель: точка с запятой (;)',
    '# Код 1С: числовой код модели из справочника (500=HXE-110, 501=HXE-310, 506=HXF-300 KUK...)',
    '# Фазность: 1 (однофазный) или 3 (трёхфазный)',
    '# Ампер: целое число (5, 10, 60, 100)',
    '# Значность: целое число (6, 7)',
    '# Напряжение: 220В или 3х220/380В',
    '# Первая строка — заголовок, не удаляйте её',
    '# Кодировка: UTF-8'
  ].join('\n');
  const csv = notes + '\n' + header + '\n' + example1 + '\n' + example2 + '\n' + example3 + '\n';
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' }));
  a.download = 'шаблон_счётчики.csv'; a.click();
}

  private parseAmperage(amp: string): number | null {
    if (!amp) return null;
    const match = amp.match(/\d+/);
    return match ? parseInt(match[0]) : null;
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg; this.errorMessage = '';
    setTimeout(() => this.successMessage = '', 4000);
  }
  private showError(msg: string): void {
    this.errorMessage = msg; setTimeout(() => this.errorMessage = '', 5000);
  }

  get availableCount(): number { return this.allDevices.filter(d => d.available).length; }
  get totalCount():     number { return this.allDevices.length; }
  get paginatedDevices(): MeterDevice[] {
    const s = (this.currentPage - 1) * this.pageSize;
    return this.filteredDevices.slice(s, s + this.pageSize);
  }
  get totalPages(): number { return Math.ceil(this.filteredDevices.length / this.pageSize); }
}
