import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Match } from '../../models';

@Component({
  selector: 'app-match-detail-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './match-detail-modal.component.html',
  styleUrl: './match-detail-modal.component.css'
})
export class MatchDetailModalComponent {
  @Input({ required: true }) match!: Match;
  @Output() closed = new EventEmitter<void>();
  @Output() requestPrediction = new EventEmitter<Match>();

  close(): void {
    this.closed.emit();
  }

  onRequestPrediction(): void {
    this.requestPrediction.emit(this.match);
  }

  getFormattedDate(): string {
    const raw = this.match.date ?? '';
    const date = new Date(raw);
    if (isNaN(date.getTime())) return raw;
    return date.toLocaleDateString('es-CO', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC'
    });
  }

  getMatchTime(): string {
    const raw = this.match.date ?? '';
    const isoMatch = raw.match(/T(\d{2}:\d{2})/);
    if (isoMatch) return isoMatch[1];
    const parts = raw.split(' ');
    if (parts.length >= 2) return parts[1].substring(0, 5);
    return 'Por Definir';
  }
}
