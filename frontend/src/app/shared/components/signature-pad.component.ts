import {
  Component, ElementRef, ViewChild, AfterViewInit,
  Output, EventEmitter, Input, OnDestroy
} from '@angular/core';

/**
 * Компонент цифровой подписи.
 * Использует Canvas API для рисования подписи мышью или пальцем.
 * Экспортирует подпись как base64 PNG строку.
 */
@Component({
  standalone: false,
  selector: 'app-signature-pad',
  template: `
    <div class="signature-wrapper">
      <div class="signature-label">{{ label }}</div>
      <div class="canvas-container" [class.signed]="isSigned">
        <canvas #sigCanvas
          (mousedown)="startDrawing($event)"
          (mousemove)="draw($event)"
          (mouseup)="stopDrawing()"
          (mouseleave)="stopDrawing()"
          (touchstart)="onTouchStart($event)"
          (touchmove)="onTouchMove($event)"
          (touchend)="stopDrawing()">
        </canvas>
        <div class="sig-placeholder" *ngIf="!isSigned">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#b0b8d0" stroke-width="1.5">
            <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>
          </svg>
          <span>Нарисуйте подпись здесь</span>
        </div>
      </div>
      <div class="sig-actions">
        <button type="button" class="btn-clear-sig" (click)="clear()" [disabled]="!isSigned">
          Очистить
        </button>
        <span class="sig-status" *ngIf="isSigned">✓ Подпись поставлена</span>
      </div>
    </div>
  `,
  styles: [`
    .signature-wrapper { display: flex; flex-direction: column; gap: 6px; }

    .signature-label {
      font-size: 11px;
      font-weight: 700;
      color: #667eea;
      text-transform: uppercase;
      letter-spacing: 0.07em;
    }

    .canvas-container {
      position: relative;
      border: 2px dashed #d0d5e8;
      border-radius: 10px;
      background: #fafbff;
      transition: border-color 0.2s;
      overflow: hidden;
    }
    .canvas-container:hover { border-color: #667eea; }
    .canvas-container.signed { border-style: solid; border-color: #667eea; background: #fff; }

    canvas {
      display: block;
      width: 100%;
      height: 120px;
      cursor: crosshair;
      touch-action: none;
    }

    .sig-placeholder {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 6px;
      pointer-events: none;
      color: #b0b8d0;
      font-size: 12px;
    }

    .sig-actions {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 22px;
    }

    .btn-clear-sig {
      font-size: 11px;
      color: #e05470;
      background: none;
      border: none;
      cursor: pointer;
      padding: 0;
      font-family: inherit;
      font-weight: 600;
    }
    .btn-clear-sig:disabled { opacity: 0.3; cursor: not-allowed; }
    .btn-clear-sig:not(:disabled):hover { text-decoration: underline; }

    .sig-status {
      font-size: 11px;
      color: #16a34a;
      font-weight: 600;
    }
  `]
})
export class SignaturePadComponent implements AfterViewInit, OnDestroy {
  @ViewChild('sigCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  @Input() label = 'Подпись';
  @Output() signatureChange = new EventEmitter<string | null>();

  private ctx!: CanvasRenderingContext2D;
  private isDrawing = false;
  isSigned = false;

  ngAfterViewInit(): void {
    const canvas = this.canvasRef.nativeElement;
    // Устанавливаем реальный размер canvas
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width || 400;
    canvas.height = 120;

    this.ctx = canvas.getContext('2d')!;
    this.ctx.strokeStyle = '#1a1a2e';
    this.ctx.lineWidth = 2;
    this.ctx.lineCap = 'round';
    this.ctx.lineJoin = 'round';
  }

  ngOnDestroy(): void {}

  private getPos(event: MouseEvent | Touch): { x: number; y: number } {
    const canvas = this.canvasRef.nativeElement;
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    return {
      x: (event.clientX - rect.left) * scaleX,
      y: (event.clientY - rect.top) * scaleY
    };
  }

  startDrawing(event: MouseEvent): void {
    this.isDrawing = true;
    const pos = this.getPos(event);
    this.ctx.beginPath();
    this.ctx.moveTo(pos.x, pos.y);
  }

  draw(event: MouseEvent): void {
    if (!this.isDrawing) return;
    const pos = this.getPos(event);
    this.ctx.lineTo(pos.x, pos.y);
    this.ctx.stroke();
    this.isSigned = true;
  }

  stopDrawing(): void {
    if (this.isDrawing && this.isSigned) {
      this.isDrawing = false;
      this.signatureChange.emit(this.getDataUrl());
    }
    this.isDrawing = false;
  }

  onTouchStart(event: TouchEvent): void {
    event.preventDefault();
    const touch = event.touches[0];
    this.isDrawing = true;
    const pos = this.getPos(touch);
    this.ctx.beginPath();
    this.ctx.moveTo(pos.x, pos.y);
  }

  onTouchMove(event: TouchEvent): void {
    event.preventDefault();
    if (!this.isDrawing) return;
    const touch = event.touches[0];
    const pos = this.getPos(touch);
    this.ctx.lineTo(pos.x, pos.y);
    this.ctx.stroke();
    this.isSigned = true;
  }

  clear(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx.clearRect(0, 0, canvas.width, canvas.height);
    this.isSigned = false;
    this.signatureChange.emit(null);
  }

  getDataUrl(): string {
    return this.canvasRef.nativeElement.toDataURL('image/png');
  }
}