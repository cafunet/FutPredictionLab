import { Component, EventEmitter, HostListener, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TeamInfo } from '../../models';

@Component({
  selector: 'app-team-form-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './team-form-modal.component.html'
})
export class TeamFormModalComponent {
  @Input() team: TeamInfo | null = null;
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<{ name: string; pais: string; group: string; rankingRaw: string; banderaUrl: string }>();

  readonly validationError = signal<string | null>(null);

  @HostListener('window:keydown.Escape')
  onEscKey() {
    this.close();
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  close(): void {
    this.validationError.set(null);
    this.closed.emit();
  }

  save(name: string, pais: string, group: string, rankingRaw: string, banderaUrl: string): void {
    if (!name?.trim() || !group?.trim()) {
      this.validationError.set('El nombre y el grupo son obligatorios.');
      return;
    }
    this.validationError.set(null);
    this.saved.emit({ name, pais, group, rankingRaw, banderaUrl });
  }
}
