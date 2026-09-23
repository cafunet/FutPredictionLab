export interface PredictionResult {
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
  factors: PredictionFactor[];
  h2hComparison: H2HComparison;
}

export interface PredictionFactor {
  type: 'positive' | 'negative' | 'neutral';
  description: string;
}

export interface H2HComparison {
  localRank: number;
  visitorRank: number;
  localEfficiency: number;
  visitorEfficiency: number;
  localGoalsPerMatch: number;
  visitorGoalsPerMatch: number;
}

export interface PredictionHistory {
  id: string;
  match: string;
  localTeam?: string;
  visitorTeam?: string;
  predicted: string;
  result: string;
  status: PredictionStatus;
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

export type PredictionStatus = 'ACERTADA' | 'FALLIDA' | 'PENDIENTE';

export interface PredictionStats {
  totalPredictions: number;
  correct: number;
  failed: number;
  pending: number;
  accuracyRate: number;
}

export interface SavePredictionRequest {
  matchId: string;
  predictedResult: string;
  accuracy: number;
  justification: string;
  localWinProbability?: number;
  drawProbability?: number;
  visitorWinProbability?: number;
  predictedLocalScore?: number;
  predictedVisitorScore?: number;
  modelVersion?: string;
}
