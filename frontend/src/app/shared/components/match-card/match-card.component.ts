import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Match } from '../../models';
import { formatLiveBadge } from '../../utils/live-time.util';

@Component({
  selector: 'app-match-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './match-card.component.html',
  styleUrl: './match-card.component.css'
})
export class MatchCardComponent {
  @Input({ required: true }) match!: Match;
  @Output() viewDetails = new EventEmitter<Match>();
  @Output() requestPrediction = new EventEmitter<Match>();
  @Output() teamClicked = new EventEmitter<string>();

  onViewDetails(): void {
    this.viewDetails.emit(this.match);
  }

  onRequestPrediction(): void {
    this.requestPrediction.emit(this.match);
  }

  onTeamClick(teamName: string): void {
    this.teamClicked.emit(teamName);
  }

  liveBadge(): string {
    return formatLiveBadge(this.match);
  }

  /** Retorna la hora del partido (HH:MM) a partir del campo date. */
  getMatchTime(): string {
    const raw = this.match.date ?? '';
    // Formato ISO: 2026-06-23T21:00:00Z → "21:00"
    const isoMatch = raw.match(/T(\d{2}:\d{2})/);
    if (isoMatch) return isoMatch[1];
    // Formato con espacio: "2026-06-23 21:00" → "21:00"
    const parts = raw.split(' ');
    if (parts.length >= 2) return parts[1].substring(0, 5);
    return 'Por Def.';
  }

  /** Retorna la fecha en formato legible: "Mié 3 Jun" */
  getFormattedDate(): string {
    const raw = this.match.date ?? '';
    const date = new Date(raw);
    if (isNaN(date.getTime())) return raw;

    return date.toLocaleDateString('es-CO', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      timeZone: 'UTC'
    });
  }
}
