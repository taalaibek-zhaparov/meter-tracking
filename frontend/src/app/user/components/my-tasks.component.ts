import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { UserService } from '../../shared/services/user.service';
import { WebSocketService } from '../../shared/services/websocket.service';
import { Plan, CompletedTaskRequest } from '../../shared/models/plan.model';
import { PdfProtocolService } from '../../shared/services/pdf-protocol.service';
import { Subscription } from 'rxjs';

@Component({
  standalone: false,
  selector: 'app-my-tasks',
  templateUrl: './my-tasks.component.html',
  styleUrls: ['./my-tasks.component.css']
})
export class MyTasksComponent implements OnInit, OnDestroy {
  tasks: Plan[] = [];

  currentPage = 1;
  pageSize = 20;
  searchQuery = '';
  loading = false;
  selectedTask: Plan | null = null;
  showModal = false;

  completedTaskData: CompletedTaskRequest = {
    planId: 0, region: '', tp: '', licevoy: '', tip: '',
    oldNomerSchetchika: '', oldPokazaniya: 0,
    nomerSchetchika: '', newPokazaniya: 0, pokazaniya: 0,
    data: '', plombaGos: '', nomerPlomby: '',
    naKryshke: '', naYashike: '', nomerSimKarty: '', nomerIccid: '',
    newPhases: null, newAmperage: null,
    fio: '', adres: '',
    documentType: ''
  };

  plombaGosPrefix = '';
  signatureAbonent: string | null = null;
  signatureMaster:  string | null = null;
  showSignatureWarning = false;

  @ViewChild('scanVideoEl') scanVideoEl!: ElementRef<HTMLVideoElement>;
  showMeterScanner = false;
  meterScannerError = '';
  private scanStream: MediaStream | null = null;
  private scanInterval: any = null;

  errorMessage = '';
  successMessage = '';
  private planUpdatesSubscription?: Subscription;
  meterSearchResults: any[] = [];
  meterSelectedFromDb = false;

  constructor(
    private userService: UserService,
    private webSocketService: WebSocketService,
    private pdfService: PdfProtocolService
  ) {}

  ngOnInit(): void {
    this.loadTasks();
    this.planUpdatesSubscription = this.webSocketService.planUpdates$.subscribe(() => this.loadTasks());
  }

  ngOnDestroy(): void {
    this.planUpdatesSubscription?.unsubscribe();
    this.closeMeterScanner();
  }

  loadTasks(): void {
    this.loading = true;
    this.userService.getMyTasks().subscribe({
      next: (tasks) => { this.tasks = tasks; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  openCompleteModal(task: Plan): void {
    this.selectedTask = task;
    this.completedTaskData = {
      planId: task.id,
      region: task.region,
      tp: task.tp,
      licevoy: task.licevoy,
      tip: task.tip,
      oldNomerSchetchika: task.nomerSchetchika,
      oldPokazaniya: task.pokazaniya,
      nomerSchetchika: '',
      newPokazaniya: 0,
      pokazaniya: task.pokazaniya,
      data: task.data,
      plombaGos: '',
      nomerPlomby: '',
      naKryshke: '',
      naYashike: '',
      nomerSimKarty: '',
      nomerIccid: '',
      newPhases: null,
      newAmperage: null,
      fio: task.fio || '',
      adres: task.adres || '',
      documentType: task.documentType || ''
    };

    this.plombaGosPrefix = '';
    this.signatureAbonent = null;
    this.signatureMaster  = null;
    this.showSignatureWarning = false;
    this.meterSelectedFromDb = false;
    this.meterSearchResults = [];
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedTask = null;
    this.plombaGosPrefix = '';
    this.signatureAbonent = null;
    this.signatureMaster  = null;
    this.showSignatureWarning = false;

    this.completedTaskData = {
      planId: 0, region: '', tp: '', licevoy: '', tip: '',
      oldNomerSchetchika: '', oldPokazaniya: 0,
      nomerSchetchika: '', newPokazaniya: 0, pokazaniya: 0,
      data: '', plombaGos: '', nomerPlomby: '',
      naKryshke: '', naYashike: '', nomerSimKarty: '', nomerIccid: '',
      newPhases: null, newAmperage: null,
      fio: '', adres: '',
      documentType: ''
    };
  }

  updatePlombaGos(): void {
    this.completedTaskData.plombaGos = this.plombaGosPrefix;
  }

  onAbonentSignature(data: string | null): void {
    this.signatureAbonent = data;
    this.showSignatureWarning = false;
  }

  onMasterSignature(data: string | null): void {
    this.signatureMaster = data;
    this.showSignatureWarning = false;
  }

  // УПРОЩЕНО: просто сохраняем в БД
  // PDF скачивается отдельно из списка "Выполненные задачи" — там уже с номером документа
  completeTask(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const request = {
      ...this.completedTaskData,
      signatureAbonent: this.signatureAbonent || undefined,
      signatureMaster:  this.signatureMaster  || undefined,
      documentType:     this.completedTaskData.documentType
    };

    this.userService.completeTask(request).subscribe({
      next: (response: any) => {
        this.successMessage = 'Данные успешно записались в БД!';

        // Только после успешного сохранения — помечаем счётчик занятым
        if (this.meterSelectedFromDb && this.completedTaskData.nomerSchetchika) {
          this.userService.markMeterBusy(this.completedTaskData.nomerSchetchika).subscribe({
            next: () => console.log('Счётчик помечен как Занят:', this.completedTaskData.nomerSchetchika),
            error: (e: any) => console.warn('Не удалось обновить статус счётчика:', e)
          });
        }

        setTimeout(() => {
          this.closeModal();
          this.loadTasks();
        }, 2000);
      },
      error: (error) => {
        this.errorMessage = error.error?.error || 'Ошибка при сохранении';
      }
    });
  }

  getTodayDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  onNewMeterNumberInput(): void {
    const q = this.completedTaskData.nomerSchetchika;
    this.meterSelectedFromDb = false;

    if (q && q.length >= 8) {
      this.userService.searchMeters(q).subscribe(r => {
        this.meterSearchResults = r.filter((m: any) => m.available === true);
      });
    } else {
      this.meterSearchResults = [];
    }
  }

  // markMeterBusy убран — вызывается только после сохранения в БД
  selectMeterFromSearch(meter: any): void {
    this.completedTaskData.nomerSchetchika = meter.meterNumber;
    this.completedTaskData.tip             = meter.meterType     || this.completedTaskData.tip;
    this.completedTaskData.nomerSimKarty   = meter.simCardNumber || '';
    this.completedTaskData.nomerIccid      = meter.iccidNumber   || '';
    this.completedTaskData.newPhases       = meter.phases    ?? null;
    this.completedTaskData.newAmperage     = meter.amperage  ?? null;
    this.completedTaskData.znch            = meter.znch      ?? null;
    this.meterSearchResults  = [];
    this.meterSelectedFromDb = true;
  }

  downloadExcel(type: 'plans' | 'completed'): void {
    const obs = type === 'plans'
      ? this.userService.exportMyPlans()
      : this.userService.exportMyCompletedTasks();

    obs.subscribe((blob: Blob) => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = type === 'plans' ? 'my-plans.xlsx' : 'my-completed-tasks.xlsx';
      a.click();
    });
  }

  async openMeterScanner(): Promise<void> {
    if (this.scanStream) return;

    if (location.protocol !== 'https:' && location.hostname !== 'localhost') {
      this.meterScannerError = 'Сканер работает только по HTTPS';
      return;
    }

    this.showMeterScanner = true;
    this.meterScannerError = '';

    await new Promise(r => setTimeout(r, 150));

    try {
      this.scanStream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: { ideal: 'environment' },
          width:  { ideal: 1280 },
          height: { ideal: 720 }
        }
      });

      const video = this.scanVideoEl.nativeElement;
      video.srcObject = this.scanStream;
      video.setAttribute('playsinline', 'true');
      await video.play();

      if ((window as any).BarcodeDetector) {
        const detector = new (window as any).BarcodeDetector({
          formats: ['code_128', 'code_39', 'ean_13', 'ean_8', 'qr_code', 'data_matrix']
        });

        const scanFrame = async () => {
          if (!this.showMeterScanner) return;
          try {
            const b = await detector.detect(video);
            if (b.length > 0) {
              if (navigator.vibrate) navigator.vibrate(200);
              this.completedTaskData.nomerSchetchika = b[0].rawValue;
              this.onNewMeterNumberInput();
              this.closeMeterScanner();
              return;
            }
          } catch {}
          requestAnimationFrame(scanFrame);
        };

        requestAnimationFrame(scanFrame);
      } else {
        this.meterScannerError = 'Сканер не поддерживается. Используйте Chrome на Android.';
      }

    } catch (err: any) {
      this.meterScannerError = 'Нет доступа к камере: ' + (err.message || err);
    }
  }

  closeMeterScanner(): void {
    this.showMeterScanner = false;

    if (this.scanInterval) {
      clearInterval(this.scanInterval);
      this.scanInterval = null;
    }

    if (this.scanStream) {
      this.scanStream.getTracks().forEach(t => t.stop());
      this.scanStream = null;
    }

    if (this.scanVideoEl?.nativeElement) {
      this.scanVideoEl.nativeElement.pause();
      this.scanVideoEl.nativeElement.srcObject = null;
    }
  }

  onManualScanInput(event: Event): void {
    const code = (event.target as HTMLInputElement).value;
    if (code.length >= 3) {
      this.completedTaskData.nomerSchetchika = code;
      this.onNewMeterNumberInput();
      if (this.meterSearchResults.length === 1) {
        this.selectMeterFromSearch(this.meterSearchResults[0]);
        this.closeMeterScanner();
      }
    }
  }

  get filteredTasks(): Plan[] {
    const sorted = [...this.tasks].sort((a, b) => (b.id || 0) - (a.id || 0));
    const q = this.searchQuery.toLowerCase().trim();
    if (!q) return sorted;
    return sorted.filter((t: any) =>
      (t.licevoy         || '').toLowerCase().includes(q) ||
      (t.nomerSchetchika || '').toLowerCase().includes(q) ||
      (t.fio             || '').toLowerCase().includes(q) ||
      (t.tip             || '').toLowerCase().includes(q)
    );
  }

  get paginatedTasks(): Plan[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredTasks.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredTasks.length / this.pageSize));
  }

  get visiblePages(): number[] {
    const pages: number[] = [], max = 5;
    let start = Math.max(1, this.currentPage - Math.floor(max / 2));
    let end   = Math.min(this.totalPages, start + max - 1);
    if (end - start < max - 1) start = Math.max(1, end - max + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  onSearch(): void { this.currentPage = 1; }
}