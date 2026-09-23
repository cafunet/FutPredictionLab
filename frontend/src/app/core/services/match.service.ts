import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Match, MatchEvent } from '../../shared/models';

/** Respuesta GET del backend (`MatchViewDTO`). */
interface MatchViewDto {
  id: string;
  local: string;
  visitor: string;
  localFlag: string;
  visitorFlag: string;
  localBanderaUrl?: string;
  visitorBanderaUrl?: string;
  localScore: number;
  visitorScore: number;
  status: Match['status'];
  date: string;
  estadio?: string;
  fase?: string;
  liveMinute?: number;
  stoppageTime?: number;
  halftimeBreak?: boolean;
  scheduledBy?: string;
  events?: { id: string; minute: number; type: string; description: string }[];
}

/** Cuerpo POST `/matches` (`MatchCreateRequestDTO`). */
export interface MatchCreateRequest {
  idEquipoLocal: number;
  idEquipoVisitante: number;
  /** ISO-8601, fecha futura (validación `@Future` en el servidor). */
  fechaHora: string;
  estadio: string;
  fase: string;
}

interface MatchCreateResponseDto {
  id: number;
  idEquipoLocal: number;
  idEquipoVisitante: number;
  fechaHora: string;
  estadio: string;
  fase: string;
  estado: string;
}

@Injectable({ providedIn: 'root' })
export class MatchService {
  private readonly http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/matches`;

  /**
   * Lista de partidos (GET `/matches`). Filtro y paginación se aplican en cliente
   * porque el API no expone query params.
   */
  getMatches(status?: string): Observable<Match[]> {
    return this.http.get<MatchViewDto[]>(this.API).pipe(
      map(raw => {
        let list = raw.map(d => this.mapDto(d));
        if (status) {
          list = list.filter(m => m.status === status);
        }
        return list;
      })
    );
  }

  getMatchById(id: string): Observable<Match> {
    return this.http.get<MatchViewDto>(`${this.API}/${id}`).pipe(map(d => this.mapDto(d)));
  }

  getLiveMatches(): Observable<Match[]> {
    return this.http.get<MatchViewDto[]>(`${this.API}/live`).pipe(map(list => list.map(d => this.mapDto(d))));
  }

  /**
   * Oficializa resultado (PUT `/matches/{id}/result`, solo ADMIN).
   */
  officializeResult(id: string, localScore: number, visitorScore: number): Observable<Match> {
    return this.http
      .put<MatchViewDto>(`${this.API}/${id}/result`, {
        localScore,
        visitorScore
      })
      .pipe(map(d => this.mapDto(d)));
  }

  /**
   * Crear partido (POST `/matches`). Requiere payload alineado con `MatchCreateRequestDTO`.
   */
  createMatch(payload: MatchCreateRequest): Observable<Match> {
    return this.http.post<MatchCreateResponseDto>(this.API, payload).pipe(
      switchMap(res => this.getMatchById(String(res.id)))
    );
  }

  updateMatch(id: string, payload: MatchCreateRequest): Observable<Match> {
    return this.http.put<MatchViewDto>(`${this.API}/${id}`, payload).pipe(map(d => this.mapDto(d)));
  }

  deleteMatch(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }

  startLive(id: string): Observable<Match> {
    return this.http.post<MatchViewDto>(`${this.API}/${id}/start-live`, {}).pipe(map(d => this.mapDto(d)));
  }

  suspendMatch(id: string): Observable<Match> {
    return this.http.post<MatchViewDto>(`${this.API}/${id}/suspend`, {}).pipe(map(d => this.mapDto(d)));
  }

  resumeMatch(id: string): Observable<Match> {
    return this.http.post<MatchViewDto>(`${this.API}/${id}/resume`, {}).pipe(map(d => this.mapDto(d)));
  }

  updateLiveScore(id: string, localScore: number, visitorScore: number): Observable<Match> {
    return this.http
      .put<MatchViewDto>(`${this.API}/${id}/live-score`, { localScore, visitorScore })
      .pipe(map(d => this.mapDto(d)));
  }

  updateLiveClock(id: string, minute: number, stoppageTime: number): Observable<Match> {
    return this.http
      .put<MatchViewDto>(`${this.API}/${id}/live-clock`, { minute, stoppageTime })
      .pipe(map(d => this.mapDto(d)));
  }

  enterHalftime(id: string): Observable<Match> {
    return this.http.post<MatchViewDto>(`${this.API}/${id}/halftime`, {}).pipe(map(d => this.mapDto(d)));
  }

  resumeAfterHalftime(id: string): Observable<Match> {
    return this.http.post<MatchViewDto>(`${this.API}/${id}/resume-half`, {}).pipe(map(d => this.mapDto(d)));
  }

  addMatchEvent(
    id: string,
    payload: { type: 'goal' | 'yellow' | 'red'; minute: number; playerName: string; teamSide: 'local' | 'visitor' }
  ): Observable<Match> {
    return this.http.post<MatchViewDto>(`${this.API}/${id}/events`, payload).pipe(map(d => this.mapDto(d)));
  }

  deleteMatchEvent(matchId: string, eventId: string): Observable<Match> {
    return this.http
      .delete<MatchViewDto>(`${this.API}/${matchId}/events/${eventId}`)
      .pipe(map(d => this.mapDto(d)));
  }

  private mapDto(d: MatchViewDto): Match {
    return {
      id: d.id,
      local: d.local,
      visitor: d.visitor,
      localFlag: d.localFlag,
      visitorFlag: d.visitorFlag,
      localBanderaUrl: d.localBanderaUrl,
      visitorBanderaUrl: d.visitorBanderaUrl,
      localScore: d.localScore,
      visitorScore: d.visitorScore,
      status: d.status,
      date: d.date,
      estadio: d.estadio,
      fase: d.fase,
      minute: d.liveMinute ?? 0,
      stoppageTime: d.stoppageTime ?? 0,
      halftimeBreak: d.halftimeBreak ?? false,
      scheduledBy: d.scheduledBy,
      events: (d.events ?? []).map(e => ({
        id: e.id,
        minute: e.minute,
        text: e.description,
        type: this.mapEventType(e.type)
      }))
    };
  }

  private mapEventType(type: string): MatchEvent['type'] {
    switch (type?.toLowerCase()) {
      case 'goal':
        return 'goal';
      case 'yellow':
        return 'yellow';
      case 'red':
        return 'red';
      default:
        return 'info';
    }
  }
}
