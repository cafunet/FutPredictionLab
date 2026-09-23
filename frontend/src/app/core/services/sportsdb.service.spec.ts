import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { SportsDbService, TeamSquadDetails } from './sportsdb.service';

describe('SportsDbService: consultas mediante el backend', () => {
  it('consulta equipos, jugadores y entrenador sin enviar una clave de API-Sports', () => {
    TestBed.configureTestingModule({
      providers: [SportsDbService, provideHttpClient(), provideHttpClientTesting()],
    });
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(SportsDbService);
    const name = 'Equipo de prueba del proxy';
    const cacheKey = 'api_football_squad_equipo de prueba del proxy__';
    localStorage.removeItem(cacheKey);
    let details: TeamSquadDetails | null | undefined;

    try {
      service.getTeamSquad(name).subscribe(value => details = value);
      const search = http.expectOne(`${environment.apiUrl}/football/teams?search=${encodeURIComponent(name)}`);
      expect(search.request.headers.has('x-apisports-key')).toBe(false);
      search.flush({ response: [{ team: { id: 29, name, national: true } }] });

      const squad = http.expectOne(`${environment.apiUrl}/football/players/squads?team=29`);
      const coach = http.expectOne(`${environment.apiUrl}/football/coachs?team=29`);
      expect(squad.request.headers.has('x-apisports-key')).toBe(false);
      expect(coach.request.headers.has('x-apisports-key')).toBe(false);
      squad.flush({ response: [{ players: [{ name: 'Jugador de prueba', position: 'Attacker', photo: null }] }] });
      coach.flush({ response: [{ name: 'Entrenador de prueba', career: [{ team: { id: 29 }, end: null }] }] });

      expect(details?.manager).toBe('Entrenador de prueba');
      expect(details?.players[0].name).toBe('Jugador de prueba');
      http.verify();
    } finally {
      localStorage.removeItem(cacheKey);
    }
  });
});
