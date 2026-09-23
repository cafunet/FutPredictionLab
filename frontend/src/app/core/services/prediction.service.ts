import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PredictionResult,
  PredictionHistory,
  PredictionStats,
  SavePredictionRequest,
  BracketSimulation
} from '../../shared/models';

interface PredictionResultDto {
  matchId: string;
  localWinProbability: number;
  drawProbability: number;
  visitorWinProbability: number;
  justification: string;
  confidence: number;
  modelVersion: string;
  predictedWinner?: string;
  predictedLocalScore?: number;
  predictedVisitorScore?: number;
  factors: { type: 'positive' | 'negative' | 'neutral'; description: string }[];
  h2hComparison: PredictionResult['h2hComparison'];
}

interface PredictionHistoryDto {
  id: string;
  match: string;
  localTeam?: string;
  visitorTeam?: string;
  predicted: string;
  result: string;
  status: PredictionHistory['status'];
  date: string;
  accuracy: number;
  justification?: string;
  localWinProbability?: number;
  drawProbability?: number;
  visitorWinProbability?: number;
  predictedLocalScore?: number;
  predictedVisitorScore?: number;
  modelVersion?: string;
}

interface GenerateAndSaveDto {
  prediction: PredictionResultDto;
  history: PredictionHistoryDto;
}

interface PredictionStatsDto {
  totalPredictions: number;
  correct: number;
  failed: number;
  pending: number;
  accuracyRate: number;
}

@Injectable({ providedIn: 'root' })
export class PredictionService {
  private readonly http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/predictions`;

  /** Genera predicción y la guarda en PostgreSQL (historial del usuario). */
  generateAndSavePrediction(matchId: string): Observable<{ result: PredictionResult; history: PredictionHistory }> {
    return this.http
      .post<GenerateAndSaveDto>(`${this.API}/matches/${matchId}/generate-and-save`, {})
      .pipe(
        map(res => ({
          result: this.mapResult(res.prediction),
          history: res.history
        }))
      );
  }

  generatePrediction(matchId: string): Observable<PredictionResult> {
    return this.http
      .post<PredictionResultDto>(`${this.API}/matches/${matchId}/generate`, {})
      .pipe(map(dto => this.mapResult(dto)));
  }

  savePrediction(prediction: SavePredictionRequest): Observable<PredictionHistory> {
    return this.http.post<PredictionHistoryDto>(this.API, {
      matchId: prediction.matchId,
      predictedResult: prediction.predictedResult,
      accuracy: prediction.accuracy,
      justification: prediction.justification,
      localWinProbability: prediction.localWinProbability,
      drawProbability: prediction.drawProbability,
      visitorWinProbability: prediction.visitorWinProbability,
      predictedLocalScore: prediction.predictedLocalScore,
      predictedVisitorScore: prediction.predictedVisitorScore,
      modelVersion: prediction.modelVersion
    });
  }

  getHistory(): Observable<PredictionHistory[]> {
    return this.http.get<PredictionHistoryDto[]>(`${this.API}/history`).pipe(
      map(items => items.map(item => this.mapHistory(item)))
    );
  }

  deletePrediction(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }

  getStats(): Observable<PredictionStats> {
    return this.http.get<PredictionStatsDto>(`${this.API}/stats`);
  }

  simulateBracket(): Observable<BracketSimulation> {
    return this.http.post<BracketSimulation>(`${this.API}/bracket/simulate`, {});
  }

  private mapResult(dto: PredictionResultDto): PredictionResult {
    return {
      matchId: dto.matchId,
      localWinProbability: dto.localWinProbability,
      drawProbability: dto.drawProbability,
      visitorWinProbability: dto.visitorWinProbability,
      justification: dto.justification,
      confidence: dto.confidence,
      modelVersion: dto.modelVersion,
      predictedWinner: dto.predictedWinner,
      predictedLocalScore: dto.predictedLocalScore,
      predictedVisitorScore: dto.predictedVisitorScore,
      factors: dto.factors ?? [],
      h2hComparison: dto.h2hComparison
    };
  }

  private mapHistory(dto: PredictionHistoryDto): PredictionHistory {
    return {
      id: dto.id,
      match: dto.match,
      localTeam: dto.localTeam,
      visitorTeam: dto.visitorTeam,
      predicted: dto.predicted,
      result: dto.result,
      status: dto.status,
      date: dto.date,
      accuracy: dto.accuracy,
      justification: dto.justification,
      localWinProbability: dto.localWinProbability,
      drawProbability: dto.drawProbability,
      visitorWinProbability: dto.visitorWinProbability,
      predictedLocalScore: dto.predictedLocalScore,
      predictedVisitorScore: dto.predictedVisitorScore,
      modelVersion: dto.modelVersion
    };
  }
}
