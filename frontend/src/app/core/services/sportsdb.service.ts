import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable, of, forkJoin } from 'rxjs';
import { map, catchError, switchMap, tap } from 'rxjs/operators';

export interface TeamSquadDetails {
  manager: string;
  players: { name: string; position: string; isStar: boolean; rating: number; photo: string | null }[];
  fromCache?: boolean;
}

interface SquadCacheEntry {
  manager: string;
  players: TeamSquadDetails['players'];
  cachedAt: string;
}

/** Nombres en BD → término de búsqueda en API-Football. */
const API_SEARCH_ALIASES: Record<string, string> = {
  'rd congo': 'Congo DR',
  'república democrática del congo': 'Congo DR',
  'republica democratica del congo': 'Congo DR',
  'rdc': 'Congo DR',
  'democratic republic of the congo': 'Congo DR',
  'uzbekistán': 'Uzbekistan',
  'uzbekistan': 'Uzbekistan',
  'eeuu': 'USA',
  'estados unidos': 'USA',
  'corea del sur': 'South Korea',
  'corea del norte': 'North Korea',
};

const CACHE_PREFIX = 'api_football_squad_';
const CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000;

@Injectable({
  providedIn: 'root'
})
export class SportsDbService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/football`;


  getTeamSquad(teamName: string, country?: string): Observable<TeamSquadDetails | null> {
    const cacheKey = this.buildCacheKey(teamName, country);
    const cached = this.readCache(cacheKey);
    if (cached) {
      return of({ ...cached, fromCache: true });
    }

    return this.resolveTeamId(teamName, country).pipe(
      switchMap(teamId => {
        if (!teamId) {
          return of(null);
        }
        return this.fetchSquadAndCoach(teamId);
      }),
      tap(details => {
        if (details) {
          this.writeCache(cacheKey, details);
        }
      }),
      catchError(err => {
        console.error('Error fetching from API-Football:', err);
        return of(null);
      })
    );
  }

  private resolveTeamId(teamName: string, country?: string): Observable<number | null> {
    const searchTerms = this.buildSearchTerms(teamName, country);
    return this.searchNationalTeamId(searchTerms, 0);
  }

  private searchNationalTeamId(terms: string[], index: number): Observable<number | null> {
    if (index >= terms.length) {
      return of(null);
    }
    const term = terms[index];
    const searchUrl = `${this.baseUrl}/teams?search=${encodeURIComponent(term)}`;

    return this.http.get<any>(searchUrl).pipe(
      switchMap(response => {
        const teamId = this.pickNationalTeamId(response?.response ?? []);
        if (teamId) {
          return of(teamId);
        }
        return this.searchNationalTeamId(terms, index + 1);
      }),
      catchError(() => this.searchNationalTeamId(terms, index + 1))
    );
  }

  private pickNationalTeamId(teams: { team: { id: number; name: string; national?: boolean; country?: string } }[]): number | null {
    if (!teams.length) {
      return null;
    }
    const national = teams.find(t => t.team?.national === true);
    return national?.team.id ?? teams[0]?.team?.id ?? null;
  }

  private buildSearchTerms(teamName: string, country?: string): string[] {
    const terms: string[] = [];
    const add = (value?: string) => {
      const trimmed = value?.trim();
      if (!trimmed) return;
      const normalized = this.normalizeKey(trimmed);
      const alias = API_SEARCH_ALIASES[normalized];
      if (alias && !terms.includes(alias)) {
        terms.push(alias);
      }
      if (!terms.includes(trimmed)) {
        terms.push(trimmed);
      }
      const withoutAccents = this.removeAccents(trimmed);
      if (withoutAccents !== trimmed && !terms.includes(withoutAccents)) {
        terms.push(withoutAccents);
        const aliasNoAccent = API_SEARCH_ALIASES[this.normalizeKey(withoutAccents)];
        if (aliasNoAccent && !terms.includes(aliasNoAccent)) {
          terms.push(aliasNoAccent);
        }
      }
    };

    add(teamName);
    add(country);
    return terms;
  }

  private fetchSquadAndCoach(teamId: number): Observable<TeamSquadDetails | null> {
    return forkJoin({
      squadRes: this.http.get<any>(`${this.baseUrl}/players/squads?team=${teamId}`),
      coachRes: this.http.get<any>(`${this.baseUrl}/coachs?team=${teamId}`)
    }).pipe(
      map(({ squadRes, coachRes }) => {
        let manager = 'No disponible';

        if (coachRes.response?.length > 0) {
          const currentCoach = coachRes.response.find((c: { career?: { team: { id: number }; end: string | null }[]; name: string }) =>
            c.career?.some(car => car.team.id === teamId && car.end === null)
          );
          manager = currentCoach?.name ?? coachRes.response[coachRes.response.length - 1].name;
        }

        const activePlayers: TeamSquadDetails['players'] = [];
        const playersData = squadRes.response?.[0]?.players;
        if (playersData?.length) {
          for (const p of playersData) {
            const positionMap: Record<string, string> = {
              Goalkeeper: 'POR',
              Defender: 'DEF',
              Midfielder: 'MED',
              Attacker: 'DEL'
            };
            const shortPos = positionMap[p.position] || p.position?.substring(0, 3).toUpperCase() || 'N/A';
            const pseudoRating = 70 + (p.name.length % 21);

            activePlayers.push({
              name: p.name,
              position: shortPos,
              isStar: pseudoRating >= 85,
              rating: pseudoRating,
              photo: p.photo || null
            });
          }
        }

        return { manager, players: activePlayers };
      })
    );
  }

  getCachedPlayerNames(teamName: string, country?: string): string[] {
    const cacheKey = this.buildCacheKey(teamName, country);
    const cached = this.readCache(cacheKey);
    if (!cached?.players?.length) {
      return [];
    }
    return cached.players.map(p => p.name).sort((a, b) => a.localeCompare(b, 'es'));
  }

  /** Precarga plantilla en caché si aún no existe (para el panel admin). */
  ensureSquadCached(teamName: string, country?: string): Observable<string[]> {
    const names = this.getCachedPlayerNames(teamName, country);
    if (names.length) {
      return of(names);
    }
    return this.getTeamSquad(teamName, country).pipe(
      map(details => details?.players.map(p => p.name) ?? [])
    );
  }

  private buildCacheKey(teamName: string, country?: string): string {
    return `${CACHE_PREFIX}${this.normalizeKey(teamName)}__${this.normalizeKey(country ?? '')}`;
  }

  private readCache(key: string): TeamSquadDetails | null {
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return null;
      const entry = JSON.parse(raw) as SquadCacheEntry;
      const age = Date.now() - new Date(entry.cachedAt).getTime();
      if (age > CACHE_TTL_MS) {
        localStorage.removeItem(key);
        return null;
      }
      return { manager: entry.manager, players: entry.players };
    } catch {
      return null;
    }
  }

  private writeCache(key: string, details: TeamSquadDetails): void {
    try {
      const entry: SquadCacheEntry = {
        manager: details.manager,
        players: details.players,
        cachedAt: new Date().toISOString()
      };
      localStorage.setItem(key, JSON.stringify(entry));
    } catch {
      // Quota exceeded or private mode — ignore silently
    }
  }

  private normalizeKey(value: string): string {
    return this.removeAccents(value).toLowerCase().trim();
  }

  private removeAccents(value: string): string {
    return value.normalize('NFD').replace(/\p{Diacritic}/gu, '');
  }
}
