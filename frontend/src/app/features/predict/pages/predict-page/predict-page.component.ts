import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Match, PredictionResult } from '../../../../shared/models';
import { PredictionService } from '../../../../core/services/prediction.service';
import { MatchService } from '../../../../core/services/match.service';

@Component({
  selector: 'app-predict-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './predict-page.component.html',
  styleUrl: './predict-page.component.css'
})
export class PredictPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly predictionService = inject(PredictionService);
  private readonly matchService = inject(MatchService);

  selectedMatch = signal<Match | null>(null);
  predictionResult = signal<PredictionResult | null>(null);
  isPredicting = signal<boolean>(false);
  predictionError = signal<string | null>(null);
  savedToHistory = signal(false);
  savingHistory = signal(false);

  ngOnInit(): void {
    const matchId = this.route.snapshot.queryParamMap.get('matchId');
    if (matchId) {
      this.loadMatchAndPredict(matchId);
    }
  }

  private loadMatchAndPredict(matchId: string): void {
    this.matchService.getMatchById(matchId).subscribe({
      next: (match) => {
        this.selectedMatch.set(match);
        this.generatePrediction(matchId);
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.predictionError.set(
          err.error?.message ?? err.message ?? 'No se encontró el partido seleccionado.'
        );
      }
    });
  }

  generatePrediction(matchId: string): void {
    this.isPredicting.set(true);
    this.predictionResult.set(null);
    this.predictionError.set(null);
    this.savedToHistory.set(false);

    this.predictionService.generateAndSavePrediction(matchId).subscribe({
      next: ({ result }) => {
        this.predictionResult.set(result);
        this.isPredicting.set(false);
        this.savedToHistory.set(true);
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.isPredicting.set(false);
        this.predictionError.set(
          err.error?.message ?? err.message ?? 'No se pudo generar la predicción. Verifica que el servicio LLM esté activo.'
        );
      }
    });
  }

  savePrediction(): void {
    if (this.savedToHistory()) {
      this.router.navigate(['/history']);
      return;
    }
    const match = this.selectedMatch();
    const result = this.predictionResult();
    if (!match || !result) return;

    this.savingHistory.set(true);
    this.predictionService.savePrediction({
      matchId: match.id,
      predictedResult: this.buildPredictedLabel(result, match),
      accuracy: result.confidence,
      justification: result.justification,
      localWinProbability: result.localWinProbability,
      drawProbability: result.drawProbability,
      visitorWinProbability: result.visitorWinProbability,
      predictedLocalScore: result.predictedLocalScore,
      predictedVisitorScore: result.predictedVisitorScore,
      modelVersion: result.modelVersion
    }).subscribe({
      next: () => {
        this.savingHistory.set(false);
        this.savedToHistory.set(true);
        this.router.navigate(['/history']);
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.savingHistory.set(false);
        this.predictionError.set(
          err.error?.message ?? err.message ?? 'No se pudo guardar la predicción.'
        );
      }
    });
  }

  private buildPredictedLabel(result: PredictionResult, match: Match): string {
    if (result.predictedWinner) {
      if (result.predictedWinner.toLowerCase() === match.local.toLowerCase()) {
        return `Gana ${match.local}`;
      }
      if (result.predictedWinner.toLowerCase() === match.visitor.toLowerCase()) {
        return `Gana ${match.visitor}`;
      }
      return `Gana ${result.predictedWinner}`;
    }
    const max = Math.max(
      result.localWinProbability,
      result.drawProbability,
      result.visitorWinProbability
    );
    if (max === result.drawProbability) return 'Empate';
    if (max === result.localWinProbability) return `Gana ${match.local}`;
    return `Gana ${match.visitor}`;
  }

  goBack(): void {
    this.router.navigate(['/']);
  }

  getFormattedDateTime(): string {
    const raw = this.selectedMatch()?.date;
    if (!raw) return '';
    const date = new Date(raw);
    if (isNaN(date.getTime())) return raw;

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
}
