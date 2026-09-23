import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Match, TeamInfo, Standing } from '../../../../shared/models';
import { AuthService } from '../../../../core/services/auth.service';
import { TeamService } from '../../../../core/services/team.service';
import { MatchService } from '../../../../core/services/match.service';
import { StandingsService } from '../../../../core/services/standings.service';
import { HeroSectionComponent } from '../../components/hero-section/hero-section.component';
import { LiveWidgetComponent } from '../../components/live-widget/live-widget.component';
import { GroupStandingsComponent } from '../../components/group-standings/group-standings.component';
import { BracketComponent } from '../../components/bracket/bracket.component';
import { MatchCardComponent } from '../../../../shared/components/match-card/match-card.component';
import { MatchDetailModalComponent } from '../../../../shared/components/match-detail-modal/match-detail-modal.component';
import { TeamDetailModalComponent } from '../../../../shared/components/team-detail-modal/team-detail-modal.component';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [
    CommonModule,
    HeroSectionComponent,
    LiveWidgetComponent,
    GroupStandingsComponent,
    BracketComponent,
    MatchCardComponent,
    MatchDetailModalComponent,
    TeamDetailModalComponent
  ],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.css'
})
export class HomePageComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly teamService = inject(TeamService);
  private readonly matchService = inject(MatchService);
  private readonly standingsService = inject(StandingsService);

  homeTab = signal<'CALENDAR' | 'GROUPS' | 'BRACKET'>('CALENDAR');

  selectedMatchDetails = signal<Match | null>(null);
  selectedTeamDetails = signal<TeamInfo | null>(null);

  readonly teams = signal<TeamInfo[]>([]);
  readonly allMatches = signal<Match[]>([]);
  readonly allStandings = signal<Record<string, Standing[]>>({});
  readonly loadError = signal(false);
  private matchesPollTimer: ReturnType<typeof setInterval> | null = null;

  /** Grupos distintos según equipos en BD (ordenados). */
  readonly groups = computed(() => {
    const set = new Set(this.teams().map(t => (t.group || '—').toUpperCase()));
    return [...set].sort((a, b) => a.localeCompare(b, 'es'));
  });

  readonly liveMatches = computed(() => this.allMatches().filter(m => m.status === 'EN_VIVO'));

  readonly calendarMatches = computed(() =>
    [...this.allMatches()].sort((a, b) => {
      const ta = Date.parse(a.date);
      const tb = Date.parse(b.date);
      return (Number.isNaN(ta) ? 0 : ta) - (Number.isNaN(tb) ? 0 : tb);
    })
  );

  ngOnInit(): void {
    const tab = this.route.snapshot.queryParamMap.get('tab');
    if (tab === 'BRACKET' || tab === 'GROUPS' || tab === 'CALENDAR') {
      this.homeTab.set(tab);
    }
    this.refreshData();
    this.matchesPollTimer = setInterval(() => this.refreshMatches(), 5_000);
  }

  ngOnDestroy(): void {
    if (this.matchesPollTimer) {
      clearInterval(this.matchesPollTimer);
    }
  }

  private refreshData(): void {
    forkJoin({
      teams: this.teamService.getTeams(),
      matches: this.matchService.getMatches(),
      standings: this.standingsService.getAllStandings()
    }).subscribe({
      next: ({ teams, matches, standings }) => {
        this.teams.set(teams);
        this.allMatches.set(matches);
        this.allStandings.set(standings);
        this.loadError.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.teams.set([]);
        this.allMatches.set([]);
      }
    });
  }

  private refreshMatches(): void {
    forkJoin({
      matches: this.matchService.getMatches(),
      standings: this.standingsService.getAllStandings()
    }).subscribe({
      next: ({ matches, standings }) => {
        this.allMatches.set(matches);
        this.allStandings.set(standings);
      },
      error: () => {}
    });
  }

  getStandingsForGroup(group: string): Standing[] {
    const rows = this.allStandings()[group.toUpperCase()] ?? this.allStandings()[group] ?? [];
    if (rows.length > 0) {
      return rows;
    }
    const g = group.toUpperCase();
    return this.teams()
      .filter(t => (t.group || '').toUpperCase() === g)
      .sort((a, b) => a.fifaRank - b.fifaRank)
      .map(t => ({
        teamId: t.id,
        name: t.name,
        flag: t.flag || t.pais || '🏳️',
        banderaUrl: t.banderaUrl || '',
        pj: 0,
        dg: 0,
        pts: 0
      }));
  }

  groupHasLiveStandings(group: string): boolean {
    return this.getStandingsForGroup(group).some(s => s.provisional);
  }

  openMatchDetails(match: Match): void {
    if (!match.details) {
      match.details = {
        stadium: 'Estadio Por Definir',
        capacity: 50000,
        referee: 'Por Asignar',
        var: 'Por Asignar',
        weather: 'Desconocido',
        city: 'Sede FIFA'
      };
    }
    this.selectedMatchDetails.set(match);
  }

  closeMatchDetails(): void {
    this.selectedMatchDetails.set(null);
  }

  /** Clic en equipo desde standings (tiene `teamId`). */
  openTeamFromStanding(standing: Standing): void {
    if (!standing.teamId) return;
    this.teamService.getTeamById(standing.teamId).subscribe({
      next: t => this.selectedTeamDetails.set(t)
    });
  }

  /** Clic en nombre desde partidos / widget (solo nombre). */
  openTeamDetails(teamName: string): void {
    const t = this.teams().find(x => x.name === teamName);
    if (t) {
      this.teamService.getTeamById(t.id).subscribe({
        next: team => this.selectedTeamDetails.set(team)
      });
    }
  }

  closeTeamDetails(): void {
    this.selectedTeamDetails.set(null);
  }

  requestPrediction(match: Match): void {
    this.selectedMatchDetails.set(null);
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.router.navigate(['/predict'], { queryParams: { matchId: match.id } });
  }
}
