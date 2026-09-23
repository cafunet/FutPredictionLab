import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PredictionHistory } from '../../models';

@Component({
  selector: 'app-prediction-history-detail-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './prediction-history-detail-modal.component.html',
  styleUrl: './prediction-history-detail-modal.component.css'
})
export class PredictionHistoryDetailModalComponent {
  @Input({ required: true }) item!: PredictionHistory;
  @Output() closed = new EventEmitter<void>();

  @HostListener('window:keydown.Escape')
  onEscKey(): void {
    this.close();
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  close(): void {
    this.closed.emit();
  }

  getFormattedDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    return date.toLocaleDateString('es-CO', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      timeZone: 'UTC'
    });
  }
}
