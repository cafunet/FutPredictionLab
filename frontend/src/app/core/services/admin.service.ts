import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User, Standing } from '../../shared/models';

export interface AdminCreateUserResponse {
  user: User;
  temporaryPassword?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/admin`;

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.API}/users`);
  }

  createUser(data: { name: string; email: string; password?: string; role: string }): Observable<User> {
    return this.http.post<User>(`${this.API}/users`, {
      name: data.name,
      email: data.email,
      password: data.password,
      role: data.role
    });
  }

  updateUser(userId: string, data: { name: string; email: string; password?: string; role: string }): Observable<User> {
    return this.http.put<User>(`${this.API}/users/${userId}`, {
      name: data.name,
      email: data.email,
      password: data.password,
      role: data.role
    });
  }

  updateUserRole(userId: string, role: string): Observable<User> {
    return this.http.put<User>(`${this.API}/users/${userId}/role`, { role });
  }

  deleteUser(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/users/${userId}`);
  }

  getPredictions(): Observable<AdminPredictionRow[]> {
    return this.http.get<AdminPredictionRow[]>(`${this.API}/predictions`);
  }

  getGroupStandings(group: string): Observable<Standing[]> {
    return this.http.get<Standing[]>(`${this.API}/standings/groups/${encodeURIComponent(group)}`);
  }

  updateStanding(teamId: string, payload: UpdateStandingPayload): Observable<Standing> {
    return this.http.put<Standing>(`${this.API}/standings/${teamId}`, payload);
  }

  resetStandingManual(teamId: string): Observable<Standing> {
    return this.http.delete<Standing>(`${this.API}/standings/${teamId}/manual`);
  }
}

export interface AdminPredictionRow {
  id: string;
  userName: string;
  userEmail: string;
  match: string;
  predicted: string;
  result: string;
  status: 'ACERTADA' | 'FALLIDA' | 'PENDIENTE';
  date: string;
  confidence: number;
}

export interface UpdateStandingPayload {
  pts: number;
  pj: number;
  pg: number;
  pe: number;
  pp: number;
  gf: number;
  gc: number;
}
