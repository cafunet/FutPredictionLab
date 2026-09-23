import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BracketMatch, BracketSimulation, BracketTeam } from '../../../../shared/models';
import { PredictionService } from '../../../../core/services/prediction.service';
import { AuthService } from '../../../../core/services/auth.service';
import { resolveFlagUrl } from '../../../../shared/utils/flag.util';

@Component({
  selector: 'app-bracket',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bracket.component.html',
  styleUrl: './bracket.component.css'
})
export class BracketComponent {
  private readonly predictionService = inject(PredictionService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly trophyUrl = '/world-cup-trophy.svg';
  readonly isAuthenticated = this.authService.isAuthenticated;
  readonly bracket = signal<BracketSimulation | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  predictChampion(): void {
    if (this.loading()) return;

    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/register'], {
        queryParams: { returnUrl: '/?tab=BRACKET' }
      });
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.predictionService.simulateBracket().subscribe({
      next: data => {
        this.bracket.set(data);
        this.loading.set(false);
      },
      error: (err: { status?: number; error?: { message?: string }; message?: string }) => {
        this.loading.set(false);
        if (err.status === 401 || err.status === 403) {
          this.router.navigate(['/auth/register'], {
            queryParams: { returnUrl: '/?tab=BRACKET' }
          });
          return;
        }
        this.error.set(
          err.error?.message ?? err.message ?? 'No se pudo simular la fase final. Intenta de nuevo.'
        );
      }
    });
  }

  flagUrl(team: BracketTeam): string {
    return resolveFlagUrl(team.banderaUrl, team.pais, team.name);
  }

  isWinner(team: BracketTeam, match: BracketMatch): boolean {
    return match.winner.name === team.name;
  }

  isChampion(team: BracketTeam, champion: BracketTeam | null): boolean {
    return champion?.name === team.name;
  }

  teamScore(team: BracketTeam, match: BracketMatch): number | null {
    if (match.homeScore == null || match.awayScore == null) return null;
    if (team.name === match.home.name) return match.homeScore;
    if (team.name === match.away.name) return match.awayScore;
    return null;
  }

  hasScores(match: BracketMatch): boolean {
    return match.homeScore != null && match.awayScore != null;
  }
}
