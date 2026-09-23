import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TeamInfo, Standing } from '../../shared/models';

/** Respuesta del backend (`TeamResponseDTO`). */
interface TeamApiDto {
  id: number;
  nombre: string;
  pais: string;
  grupo: string;
  rankingFifa: number;
  banderaUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class TeamService {
  private readonly http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/teams`;

  /**
   * Lista de equipos (GET `/teams`). El parámetro `group` filtra en cliente por `grupo`.
   */
  getTeams(group?: string): Observable<TeamInfo[]> {
    return this.http.get<TeamApiDto[]>(this.API).pipe(
      map(list => {
        let mapped = list.map(t => this.mapTeam(t));
        if (group?.trim()) {
          const g = group.trim().toUpperCase();
          mapped = mapped.filter(t => t.group.toUpperCase() === g);
        }
        return mapped;
      })
    );
  }

  getTeamById(id: string): Observable<TeamInfo> {
    return this.http.get<TeamApiDto>(`${this.API}/${id}`).pipe(map(t => this.mapTeam(t)));
  }

  getTeamByName(name: string): Observable<TeamInfo> {
    return this.getTeams().pipe(
      map(teams => {
        const found = teams.find(t => t.name.toLowerCase() === name.trim().toLowerCase());
        if (!found) {
          throw new Error(`No se encontró el equipo "${name}"`);
        }
        return found;
      })
    );
  }

  getStandings(group: string): Observable<Standing[]> {
    return of([]);
  }

  getAllStandings(): Observable<Record<string, Standing[]>> {
    return of({});
  }

  createTeam(team: Partial<TeamInfo> & { pais?: string, banderaUrl?: string }): Observable<TeamInfo> {
    const body = this.buildTeamCreateBody(team);
    return this.http.post<TeamApiDto>(this.API, body).pipe(map(t => this.mapTeam(t)));
  }

  updateTeam(id: string, team: Partial<TeamInfo> & { pais?: string, banderaUrl?: string }): Observable<TeamInfo> {
    const body = this.buildTeamCreateBody(team);
    return this.http.put<TeamApiDto>(`${this.API}/${id}`, body).pipe(map(t => this.mapTeam(t)));
  }

  deleteTeam(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }

  private buildTeamCreateBody(team: Partial<TeamInfo> & { pais?: string, banderaUrl?: string }): {
    nombre: string;
    pais: string;
    grupo: string;
    rankingFifa: number;
    banderaUrl?: string;
  } {
    const nombre = team.name?.trim() ?? '';
    const pais = team.pais?.trim() || 'Sin definir';
    const grupo = (team.group?.trim() || 'A').toUpperCase().slice(0, 10);
    const rankingFifa = typeof team.fifaRank === 'number' && !Number.isNaN(team.fifaRank) ? team.fifaRank : 0;
    return { nombre, pais, grupo, rankingFifa, banderaUrl: team.banderaUrl };
  }

  private mapTeam(dto: TeamApiDto): TeamInfo {
    return {
      id: String(dto.id),
      name: dto.nombre,
      pais: dto.pais,
      flag: dto.pais,
      banderaUrl: dto.banderaUrl,
      group: dto.grupo,
      fifaRank: dto.rankingFifa,
      manager: '',
      formation: '',
      players: []
    };
  }
}
