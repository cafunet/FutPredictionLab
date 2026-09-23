import { Match } from '../models/match.model';

/** Etiqueta de reloj en vivo para widget y admin. */
export function formatLiveBadge(match: Match): string {
  if (match.halftimeBreak) {
    return 'DESCANSO';
  }
  const minute = match.minute ?? 0;
  const stoppage = match.stoppageTime ?? 0;
  const clock = stoppage > 0 ? `${minute}' +${stoppage}` : `${minute}'`;
  return `EN VIVO · ${clock}`;
}

export function formatLiveClockShort(match: Match): string {
  if (match.halftimeBreak) {
    return 'Descanso';
  }
  const minute = match.minute ?? 0;
  const stoppage = match.stoppageTime ?? 0;
  return stoppage > 0 ? `${minute}' +${stoppage}` : `${minute}'`;
}
