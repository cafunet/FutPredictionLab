import { Component, Input, Output, EventEmitter, signal, computed, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Match } from '../../../../shared/models';
import { formatLiveBadge } from '../../../../shared/utils/live-time.util';

@Component({
  selector: 'app-live-widget',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './live-widget.component.html',
  styleUrl: './live-widget.component.css'
})
export class LiveWidgetComponent {
  @ViewChild('track') track?: ElementRef<HTMLElement>;

  @Input({ required: true }) set matches(value: Match[] | null) {
    this._matches.set((value ?? []).filter(m => m.status === 'EN_VIVO'));
  }
  @Output() teamClicked = new EventEmitter<string>();

  private readonly _matches = signal<Match[]>([]);

  readonly matchList = computed(() => this._matches());
  readonly hasMatches = computed(() => this._matches().length > 0);
  readonly total = computed(() => this._matches().length);

  scroll(direction: -1 | 1): void {
    const el = this.track?.nativeElement;
    if (!el) return;
    const slide = el.querySelector<HTMLElement>('.live-slider__slide');
    const gap = 12;
    const step = slide ? slide.offsetWidth + gap : el.clientWidth * 0.85;
    el.scrollBy({ left: direction * step, behavior: 'smooth' });
  }

  liveBadge(match: Match): string {
    return formatLiveBadge(match);
  }

  onTeamClick(teamName: string): void {
    this.teamClicked.emit(teamName);
  }

  eventIcon(type: string): string {
    switch (type) {
      case 'goal':
        return '⚽';
      case 'yellow':
        return '🟨';
      case 'red':
        return '🟥';
      default:
        return '•';
    }
  }
}
