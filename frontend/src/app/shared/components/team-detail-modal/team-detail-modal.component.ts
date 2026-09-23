import { Component, Input, Output, EventEmitter, HostListener, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TeamInfo } from '../../models';
import { SportsDbService } from '../../../core/services/sportsdb.service';

@Component({
  selector: 'app-team-detail-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './team-detail-modal.component.html',
  styleUrl: './team-detail-modal.component.css'
})
export class TeamDetailModalComponent implements OnInit {
  @Input({ required: true }) team!: TeamInfo;
  @Output() closed = new EventEmitter<void>();

  private readonly sportsDb = inject(SportsDbService);

  isLoading = signal<boolean>(true);
  loadFailed = signal<boolean>(false);
  fromCache = signal<boolean>(false);

  ngOnInit(): void {
    this.sportsDb.getTeamSquad(this.team.name, this.team.pais).subscribe({
      next: (details) => {
        if (details) {
          this.team = {
            ...this.team,
            manager: details.manager,
            players: details.players
          };
          this.fromCache.set(!!details.fromCache);
          this.loadFailed.set(!details.players?.length);
        } else {
          this.loadFailed.set(true);
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.loadFailed.set(true);
        this.isLoading.set(false);
      }
    });
  }

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
    this.closed.emit();
  }
}
