import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PredictionHistory, PredictionStats } from '../../../../shared/models';
import { PredictionService } from '../../../../core/services/prediction.service';
import { AuthService } from '../../../../core/services/auth.service';
import { StatCardComponent } from '../../../../shared/components/stat-card/stat-card.component';
import { PredictionHistoryDetailModalComponent } from '../../../../shared/components/prediction-history-detail-modal/prediction-history-detail-modal.component';

@Component({
  selector: 'app-history-page',
  standalone: true,
  imports: [CommonModule, StatCardComponent, PredictionHistoryDetailModalComponent],
  templateUrl: './history-page.component.html',
  styleUrl: './history-page.component.css'
})
export class HistoryPageComponent implements OnInit {
  private readonly predictionService = inject(PredictionService);
  readonly auth = inject(AuthService);

  history = signal<PredictionHistory[]>([]);
  stats = signal<PredictionStats | null>(null);
  selectedItem = signal<PredictionHistory | null>(null);

  ngOnInit(): void {
    this.loadHistory();
    this.loadStats();
  }

  private loadHistory(): void {
    this.predictionService.getHistory().subscribe({
      next: (data) => this.history.set(data)
    });
  }

  private loadStats(): void {
    this.predictionService.getStats().subscribe({
      next: (data) => this.stats.set(data)
    });
  }

  openDetail(item: PredictionHistory): void {
    this.selectedItem.set(item);
  }

  closeDetail(): void {
    this.selectedItem.set(null);
  }

  getFormattedDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;

    const datePart = date.toLocaleDateString('es-CO', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC'
    });

    const timePart = date.toLocaleTimeString('es-CO', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
      timeZone: 'UTC'
    });

    const capitalizedDate = datePart.charAt(0).toUpperCase() + datePart.slice(1);
    return `${capitalizedDate} - ${timePart}`;
  }

  deletePrediction(id: string): void {
    this.predictionService.deletePrediction(id).subscribe({
      next: () => {
        this.loadHistory();
        this.loadStats();
      }
    });
  }
}
