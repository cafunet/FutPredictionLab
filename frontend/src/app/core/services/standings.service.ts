import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Standing } from '../../shared/models';

interface StandingDto {
  teamId: string;
  name: string;
  pais: string;
  banderaUrl: string;
  pts: number;
  pj: number;
  pg: number;
  pe: number;
  pp: number;
  gf: number;
  gc: number;
  dg: number;
  provisional: boolean;
  manualOverride: boolean;
  modifiedBy?: string;
  sealSignature?: string;
  modifiedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class StandingsService {
  private readonly http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/standings`;

  getAllStandings(): Observable<Record<string, Standing[]>> {
    return this.http.get<Record<string, StandingDto[]>>(this.API).pipe(
      map(data => {
        const result: Record<string, Standing[]> = {};
        for (const [group, rows] of Object.entries(data)) {
          result[group] = rows.map(r => this.mapStanding(r));
        }
        return result;
      })
    );
  }

  getStandingsForGroup(group: string): Observable<Standing[]> {
    return this.http
      .get<StandingDto[]>(`${this.API}/groups/${encodeURIComponent(group)}`)
      .pipe(map(rows => rows.map(r => this.mapStanding(r))));
  }

  private mapStanding(dto: StandingDto): Standing {
    return {
      teamId: dto.teamId,
      name: dto.name,
      flag: dto.pais,
      banderaUrl: dto.banderaUrl,
      pts: dto.pts,
      pj: dto.pj,
      pg: dto.pg,
      pe: dto.pe,
      pp: dto.pp,
      gf: dto.gf,
      gc: dto.gc,
      dg: dto.dg,
      provisional: dto.provisional,
      manualOverride: dto.manualOverride,
      modifiedBy: dto.modifiedBy,
      sealSignature: dto.sealSignature,
      modifiedAt: dto.modifiedAt
    };
  }
}
