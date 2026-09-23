import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { AdminService, AdminPredictionRow } from '../../../../core/services/admin.service';
import { TeamService } from '../../../../core/services/team.service';
import { MatchService } from '../../../../core/services/match.service';
import { SportsDbService } from '../../../../core/services/sportsdb.service';
import { Match, TeamInfo, User, UserRole, MatchEvent, Standing } from '../../../../shared/models';
import { formatLiveBadge, formatLiveClockShort } from '../../../../shared/utils/live-time.util';
import { MatchCreateRequest } from '../../../../core/services/match.service';
import { UserFormModalComponent } from '../../../../shared/components/user-form-modal/user-form-modal.component';
import { TeamFormModalComponent } from '../../../../shared/components/team-form-modal/team-form-modal.component';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

interface PendingConfirm extends ConfirmDialogData {
  onConfirm: () => void;
  onCancel?: () => void;
}

interface ToastMessage {
  message: string;
  type: 'success' | 'error';
}

interface ScheduleForm {
  idLocal: string;
  idVisitor: string;
  fechaHora: string;
  estadio: string;
  fase: string;
}

interface OfficializeForm {
  matchId: string;
  localScore: number;
  visitorScore: number;
}

interface EventSimulationForm {
  type: 'goal' | 'yellow' | 'red';
  minute: string;
  teamSide: 'local' | 'visitor';
  playerName: string;
}

interface StandingRowSnapshot {
  pts: number;
  pj: number;
  pg: number;
  pe: number;
  pp: number;
  gf: number;
  gc: number;
}

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule, FormsModule, UserFormModalComponent, TeamFormModalComponent, ConfirmDialogComponent],
  templateUrl: './admin-page.component.html',
  styleUrl: './admin-page.component.css'
})
export class AdminPageComponent implements OnInit, OnDestroy {
  readonly auth = inject(AuthService);
  private readonly adminService = inject(AdminService);
  private readonly teamService = inject(TeamService);
  private readonly matchService = inject(MatchService);
  private readonly sportsDb = inject(SportsDbService);

  readonly users = signal<User[]>([]);
  readonly loadingUsers = signal(false);
  readonly roles: UserRole[] = ['USER', 'ADMIN'];
  readonly matches = signal<Match[]>([]);
  readonly matchesToOfficialize = computed(() =>
    this.matches().filter(m => m.status === 'EN_VIVO')
  );
  readonly liveMatches = computed(() => this.matches().filter(m => m.status === 'EN_VIVO'));
  readonly sortedMatches = computed(() =>
    [...this.matches()].sort((a, b) => {
      const ta = Date.parse(a.date);
      const tb = Date.parse(b.date);
      return (Number.isNaN(ta) ? 0 : ta) - (Number.isNaN(tb) ? 0 : tb);
    })
  );

  readonly teams = signal<TeamInfo[]>([]);
  readonly editingUser = signal<User | null>(null);
  readonly isCreatingUser = signal(false);
  readonly editingTeam = signal<TeamInfo | null>(null);
  readonly isCreatingTeam = signal(false);
  readonly isSchedulingMatch = signal(false);
  readonly isOfficializingMatch = signal(false);
  readonly editingMatch = signal<Match | null>(null);
  readonly savingMatch = signal(false);
  readonly toast = signal<ToastMessage | null>(null);
  readonly confirmDialog = signal<PendingConfirm | null>(null);
  readonly scheduleFormError = signal<string | null>(null);
  readonly savingLiveScore = signal(false);
  readonly savingLiveClock = signal(false);
  readonly officializingSimulation = signal(false);
  readonly addingEvent = signal(false);
  readonly loadingPlayers = signal(false);
  simulationMatchId = '';
  simulationLocalScore = '0';
  simulationVisitorScore = '0';
  simulationMinute = '0';
  simulationStoppage = '0';
  localPlayers: string[] = [];
  visitorPlayers: string[] = [];
  eventForm: EventSimulationForm = this.emptyEventForm();
  readonly userPredictions = signal<AdminPredictionRow[]>([]);
  readonly loadingPredictions = signal(false);
  readonly groupStandingsRows = signal<Standing[]>([]);
  readonly savingStanding = signal(false);
  readonly standingsDirty = signal(false);
  private readonly standingsSnapshot = new Map<string, StandingRowSnapshot>();

  readonly adminGroups = computed(() => {
    const set = new Set(this.teams().map(t => (t.group || 'A').toUpperCase()));
    return [...set].sort((a, b) => a.localeCompare(b, 'es'));
  });

  standingsGroup = 'A';

  scheduleForm: ScheduleForm = this.emptyScheduleForm();
  officializeForm: OfficializeForm = this.emptyOfficializeForm();
  private matchesPollTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.loadUsers();
    this.loadMatches();
    this.loadTeams();
    this.loadUserPredictions();
    this.matchesPollTimer = setInterval(() => this.loadMatches(), 5_000);
  }

  ngOnDestroy(): void {
    if (this.matchesPollTimer) {
      clearInterval(this.matchesPollTimer);
    }
  }

  private emptyOfficializeForm(): OfficializeForm {
    return { matchId: '', localScore: 0, visitorScore: 0 };
  }

  private emptyEventForm(): EventSimulationForm {
    return { type: 'goal', minute: '0', teamSide: 'local', playerName: '' };
  }

  selectedSimulationMatch(): Match | null {
    if (!this.simulationMatchId) return null;
    return this.matches().find(m => m.id === this.simulationMatchId) ?? null;
  }

  get eventPlayerOptions(): string[] {
    return this.eventForm.teamSide === 'local' ? this.localPlayers : this.visitorPlayers;
  }

  eventTypeIcon(type: MatchEvent['type']): string {
    switch (type) {
      case 'goal':
        return '⚽';
      case 'yellow':
        return '🟨';
      case 'red':
        return '🟥';
      default:
        return '•';
    }
  }

  simulationLiveLabel(match: Match): string {
    return formatLiveBadge(match);
  }

  simulationClockShort(match: Match): string {
    return formatLiveClockShort(match);
  }

  openLiveSimulation(match: Match): void {
    this.simulationMatchId = match.id;
    this.syncSimulationFormFromMatch(match);
    this.loadPlayersForSimulation();
    setTimeout(
      () => document.getElementById('live-simulation')?.scrollIntoView({ behavior: 'smooth', block: 'start' }),
      80
    );
  }

  onSimulationMatchChange(): void {
    const match = this.selectedSimulationMatch();
    if (match) {
      this.syncSimulationFormFromMatch(match);
      this.loadPlayersForSimulation();
    }
  }

  private syncSimulationFormFromMatch(match: Match): void {
    this.simulationLocalScore = String(match.localScore);
    this.simulationVisitorScore = String(match.visitorScore);
    this.simulationMinute = String(match.minute ?? 0);
    this.simulationStoppage = String(match.stoppageTime ?? 0);
    this.eventForm.minute = String(match.minute ?? 0);
    this.eventForm.playerName = '';
  }

  loadPlayersForSimulation(): void {
    const match = this.selectedSimulationMatch();
    if (!match) return;

    const localTeam = this.teams().find(t => t.name === match.local);
    const visitorTeam = this.teams().find(t => t.name === match.visitor);
    this.loadingPlayers.set(true);

    forkJoin([
      this.sportsDb.ensureSquadCached(match.local, localTeam?.pais),
      this.sportsDb.ensureSquadCached(match.visitor, visitorTeam?.pais)
    ]).subscribe({
      next: ([localP, visitorP]) => {
        this.localPlayers = localP;
        this.visitorPlayers = visitorP;
        this.loadingPlayers.set(false);
      },
      error: () => this.loadingPlayers.set(false)
    });
  }

  saveSimulationScore(): void {
    const match = this.selectedSimulationMatch();
    if (!match || this.savingLiveScore()) return;

    const localScore = Number(this.simulationLocalScore);
    const visitorScore = Number(this.simulationVisitorScore);
    if (!Number.isFinite(localScore) || !Number.isFinite(visitorScore) || localScore < 0 || visitorScore < 0) {
      this.showToast('Marcador inválido.', 'error');
      return;
    }

    this.savingLiveScore.set(true);
    this.matchService.updateLiveScore(match.id, localScore, visitorScore).subscribe({
      next: () => {
        this.savingLiveScore.set(false);
        this.loadMatches();
        this.showToast('Marcador actualizado en la home.');
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.savingLiveScore.set(false);
        this.showToast(err.error?.message ?? err.message ?? 'No se pudo guardar el marcador.', 'error');
      }
    });
  }

  saveSimulationClock(): void {
    const match = this.selectedSimulationMatch();
    if (!match || this.savingLiveClock()) return;

    const minute = Number(this.simulationMinute);
    const stoppageTime = Number(this.simulationStoppage);
    if (!Number.isFinite(minute) || minute < 0 || minute > 120) {
      this.showToast('Minuto de juego inválido (0–120).', 'error');
      return;
    }
    if (!Number.isFinite(stoppageTime) || stoppageTime < 0 || stoppageTime > 30) {
      this.showToast('Tiempo de adición inválido (0–30).', 'error');
      return;
    }

    this.savingLiveClock.set(true);
    this.matchService.updateLiveClock(match.id, minute, stoppageTime).subscribe({
      next: () => {
        this.savingLiveClock.set(false);
        this.loadMatches();
        this.showToast('Reloj actualizado.');
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.savingLiveClock.set(false);
        this.showToast(err.error?.message ?? err.message ?? 'No se pudo guardar el reloj.', 'error');
      }
    });
  }

  startHalftimeSimulation(): void {
    const match = this.selectedSimulationMatch();
    if (!match) return;

    this.matchService.enterHalftime(match.id).subscribe({
      next: () => {
        this.loadMatches();
        this.showToast('Descanso activado. El reloj queda en pausa.');
      },
      error: (err: { error?: { message?: string }; message?: string }) =>
        this.showToast(err.error?.message ?? err.message ?? 'No se pudo marcar descanso.', 'error')
    });
  }

  resumeHalftimeSimulation(): void {
    const match = this.selectedSimulationMatch();
    if (!match) return;

    this.matchService.resumeAfterHalftime(match.id).subscribe({
      next: () => {
        this.loadMatches();
        this.showToast('Segundo tiempo iniciado.');
      },
      error: (err: { error?: { message?: string }; message?: string }) =>
        this.showToast(err.error?.message ?? err.message ?? 'No se pudo reanudar.', 'error')
    });
  }

  officializeSimulationMatch(): void {
    const match = this.selectedSimulationMatch();
    if (!match || this.officializingSimulation()) return;

    const localScore = Number(this.simulationLocalScore);
    const visitorScore = Number(this.simulationVisitorScore);
    if (!Number.isFinite(localScore) || !Number.isFinite(visitorScore) || localScore < 0 || visitorScore < 0) {
      this.showToast('Marcador inválido para oficializar.', 'error');
      return;
    }

    this.openConfirm({
      title: 'Oficializar partido',
      message: `¿Cerrar ${match.local} vs ${match.visitor} con resultado ${localScore} - ${visitorScore}? Esta acción es irreversible.`,
      confirmLabel: 'Oficializar',
      variant: 'warning',
      onConfirm: () => {
        this.officializingSimulation.set(true);
        this.matchService.officializeResult(match.id, localScore, visitorScore).subscribe({
          next: () => {
            this.officializingSimulation.set(false);
            this.loadMatches();
            this.showToast('Partido oficializado.');
          },
          error: (err: { error?: { message?: string }; message?: string }) => {
            this.officializingSimulation.set(false);
            this.showToast(err.error?.message ?? err.message ?? 'No se pudo oficializar.', 'error');
          }
        });
      }
    });
  }

  addSimulationEvent(): void {
    const match = this.selectedSimulationMatch();
    if (!match || this.addingEvent()) return;

    const minute = Number(this.eventForm.minute);
    if (!this.eventForm.playerName?.trim()) {
      this.showToast('Selecciona un jugador.', 'error');
      return;
    }
    if (!Number.isFinite(minute) || minute < 0 || minute > 120) {
      this.showToast('Minuto inválido (0–120).', 'error');
      return;
    }

    this.addingEvent.set(true);
    this.matchService
      .addMatchEvent(match.id, {
        type: this.eventForm.type,
        minute,
        playerName: this.eventForm.playerName.trim(),
        teamSide: this.eventForm.teamSide
      })
      .subscribe({
        next: () => {
          this.addingEvent.set(false);
          this.eventForm.playerName = '';
          this.loadMatches();
          this.syncSimulationFormFromMatch(this.selectedSimulationMatch()!);
          this.showToast('Evento registrado. Visible en el widget en vivo.');
        },
        error: (err: { error?: { message?: string }; message?: string }) => {
          this.addingEvent.set(false);
          this.showToast(err.error?.message ?? err.message ?? 'No se pudo registrar el evento.', 'error');
        }
      });
  }

  removeSimulationEvent(eventId: string): void {
    const match = this.selectedSimulationMatch();
    if (!match || !eventId) return;

    this.openConfirm({
      title: 'Eliminar evento',
      message: '¿Eliminar este evento del partido en vivo?',
      confirmLabel: 'Eliminar',
      variant: 'danger',
      onConfirm: () => {
        this.matchService.deleteMatchEvent(match.id, eventId).subscribe({
          next: () => {
            this.loadMatches();
            const updated = this.selectedSimulationMatch();
            if (updated) this.syncSimulationFormFromMatch(updated);
            this.showToast('Evento eliminado.');
          },
          error: (err: { error?: { message?: string }; message?: string }) =>
            this.showToast(err.error?.message ?? err.message ?? 'No se pudo eliminar.', 'error')
        });
      }
    });
  }

  loadUserPredictions(): void {
    this.loadingPredictions.set(true);
    this.adminService.getPredictions().subscribe({
      next: (list) => {
        this.userPredictions.set(list);
        this.loadingPredictions.set(false);
      },
      error: () => {
        this.userPredictions.set([]);
        this.loadingPredictions.set(false);
      }
    });
  }

  predictionStatusLabel(status: AdminPredictionRow['status']): string {
    switch (status) {
      case 'ACERTADA':
        return 'Acertada';
      case 'FALLIDA':
        return 'Fallida';
      default:
        return 'Pendiente';
    }
  }

  private emptyScheduleForm(): ScheduleForm {
    return { idLocal: '', idVisitor: '', fechaHora: '', estadio: '', fase: '' };
  }

  showToast(message: string, type: 'success' | 'error' = 'success'): void {
    this.toast.set({ message, type });
    setTimeout(() => this.toast.set(null), 5500);
  }

  private openConfirm(data: PendingConfirm): void {
    this.confirmDialog.set(data);
  }

  onConfirmDialogAccepted(): void {
    const dialog = this.confirmDialog();
    this.confirmDialog.set(null);
    dialog?.onConfirm();
  }

  onConfirmDialogCancelled(): void {
    const dialog = this.confirmDialog();
    this.confirmDialog.set(null);
    dialog?.onCancel?.();
  }

  private showScheduleError(message: string): void {
    this.scheduleFormError.set(this.humanizeScheduleError(message));
  }

  private humanizeScheduleError(message: string): string {
    const raw = (message ?? '').trim();
    if (!raw) {
      return 'No se pudo guardar el partido. Revisa los datos e intenta de nuevo.';
    }
    if (raw.includes('fecha futura') || raw.includes('fecha debe')) {
      return 'La fecha y hora deben ser posteriores al momento actual. Elige un horario futuro.';
    }
    if (raw.startsWith('Error de validacion:')) {
      return raw.replace(/^Error de validacion:\s*/i, '').trim() || raw;
    }
    return raw;
  }

  /** Hora programada tal como se guardó (sin conversión de zona horaria). */
  formatMatchDateTime(iso: string): string {
    const m = iso.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    if (!m) return '—';
    const months = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
    const day = Number(m[3]);
    const month = months[Number(m[2]) - 1] ?? m[2];
    const year = m[1];
    const hour24 = Number(m[4]);
    const minute = m[5];
    const h12 = hour24 % 12 || 12;
    const ampm = hour24 >= 12 ? 'p. m.' : 'a. m.';
    return `${day} ${month} ${year}, ${h12}:${minute} ${ampm}`;
  }

  loadTeams(): void {
    this.teamService.getTeams().subscribe({
      next: (list) => {
        this.teams.set([...list].sort((a, b) => a.name.localeCompare(b.name, 'es')));
        if (!this.adminGroups().includes(this.standingsGroup)) {
          this.standingsGroup = this.adminGroups()[0] ?? 'A';
        }
        this.loadGroupStandings();
      },
      error: () => this.teams.set([])
    });
  }

  loadMatches(): void {
    this.matchService.getMatches().subscribe({
      next: (list) => {
        this.matches.set(list);
        const live = list.filter(m => m.status === 'EN_VIVO');
        if (live.length === 0) {
          this.simulationMatchId = '';
          return;
        }
        if (!this.simulationMatchId || !live.some(m => m.id === this.simulationMatchId)) {
          this.simulationMatchId = live[0].id;
          this.syncSimulationFormFromMatch(live[0]);
          this.loadPlayersForSimulation();
        } else {
          const current = live.find(m => m.id === this.simulationMatchId);
          if (current) this.syncSimulationFormFromMatch(current);
        }
      },
      error: () => this.matches.set([])
    });
  }

  loadUsers(): void {
    this.loadingUsers.set(true);
    this.adminService.getUsers().subscribe({
      next: (list) => {
        this.users.set(list);
        this.loadingUsers.set(false);
      },
      error: (err: { error?: { message?: string; detail?: string }; message?: string }) => {
        this.loadingUsers.set(false);
        this.showToast(
          `No se pudo cargar usuarios: ${err.error?.message ?? err.error?.detail ?? err.message ?? 'Error'}`,
          'error'
        );
      }
    });
  }

  createUser(name: string, email: string, role: string, password?: string): void {
    if (!name || !email || !password) {
      this.showToast('Nombre, email y contraseña son obligatorios.', 'error');
      return;
    }
    this.adminService.createUser({ name, email, role, password }).subscribe({
      next: (user) => {
        this.showToast(`Usuario creado: ${user.email}`);
        this.closeUserModal();
        this.loadUsers();
      },
      error: (err: { error?: { message?: string; detail?: string }; message?: string }) =>
        this.showToast(
          err.error?.message ?? err.error?.detail ?? err.message ?? 'No se pudo crear el usuario.',
          'error'
        )
    });
  }

  setEditingUser(user: User | null): void {
    this.editingUser.set(user);
    if (user) {
      this.isCreatingUser.set(false);
    }
  }

  openCreateUserModal(): void {
    this.editingUser.set(null);
    this.isCreatingUser.set(true);
  }

  closeUserModal(): void {
    this.editingUser.set(null);
    this.isCreatingUser.set(false);
  }

  handleUserSave(data: { name: string; email: string; role: string; password?: string }): void {
    if (this.editingUser()) {
      this.updateUser(data.name, data.email, data.role, data.password);
    } else {
      this.createUser(data.name, data.email, data.role, data.password);
    }
  }

  updateUser(name: string, email: string, role: string, password?: string): void {
    const user = this.editingUser();
    if (!user || !name || !email) return;

    this.adminService.updateUser(user.id, { name, email, role, password }).subscribe({
      next: () => {
        this.showToast('Usuario actualizado correctamente.');
        this.closeUserModal();
        this.loadUsers();
      },
      error: (err: { error?: { message?: string; detail?: string }; message?: string }) =>
        this.showToast(
          err.error?.message ?? err.error?.detail ?? err.message ?? 'No se pudo actualizar el usuario.',
          'error'
        )
    });
  }

  onRoleChange(user: User, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const newRole = select.value;
    if (newRole === user.role) return;

    this.openConfirm({
      title: 'Cambiar rol de usuario',
      message: `¿Cambiar el rol de ${user.email} a ${newRole}?`,
      confirmLabel: 'Confirmar',
      variant: 'primary',
      onConfirm: () => {
        this.adminService.updateUserRole(user.id, newRole).subscribe({
          next: () => this.loadUsers(),
          error: (err: { error?: { message?: string; detail?: string }; message?: string }) => {
            select.value = user.role;
            this.showToast(
              err.error?.message ?? err.error?.detail ?? err.message ?? 'No se pudo actualizar el rol.',
              'error'
            );
          }
        });
      },
      onCancel: () => {
        select.value = user.role;
      }
    });
  }

  removeUser(user: User): void {
    this.openConfirm({
      title: 'Eliminar usuario',
      message: `¿Eliminar definitivamente a ${user.email}?\n\nEsta acción no se puede deshacer.`,
      confirmLabel: 'Eliminar',
      variant: 'danger',
      onConfirm: () => {
        this.adminService.deleteUser(user.id).subscribe({
          next: () => this.loadUsers(),
          error: (err: { error?: { message?: string; detail?: string }; message?: string }) =>
            this.showToast(
              err.error?.message ?? err.error?.detail ?? err.message ?? 'No se pudo eliminar el usuario.',
              'error'
            )
        });
      }
    });
  }

  isCurrentUser(user: User): boolean {
    const me = this.auth.currentUser();
    return !!me && me.id === user.id;
  }

  createTeam(name: string, pais: string, group: string, rankingRaw: string, banderaUrl: string): void {
    if (!name?.trim() || !group?.trim()) return;
    const fifaRank = rankingRaw === '' || rankingRaw == null ? 0 : Number(rankingRaw);
    this.teamService
      .createTeam({
        name: name.trim(),
        pais: pais?.trim() || 'Sin definir',
        group: group.trim(),
        fifaRank: Number.isFinite(fifaRank) ? Math.max(0, Math.floor(fifaRank)) : 0,
        banderaUrl: banderaUrl?.trim() || ''
      })
      .subscribe({
        next: () => {
          this.showToast('Equipo registrado exitosamente.');
          this.closeTeamModal();
          this.loadTeams();
        },
        error: (err: { error?: { message?: string }; message?: string }) =>
          this.showToast(err.error?.message ?? err.message ?? 'No se pudo registrar el equipo.', 'error')
      });
  }

  setEditingTeam(team: TeamInfo | null): void {
    this.editingTeam.set(team);
    if (team) {
      this.isCreatingTeam.set(false);
    }
  }

  openCreateTeamModal(): void {
    this.editingTeam.set(null);
    this.isCreatingTeam.set(true);
  }

  closeTeamModal(): void {
    this.editingTeam.set(null);
    this.isCreatingTeam.set(false);
  }

  openScheduleMatchModal(): void {
    this.editingMatch.set(null);
    this.scheduleForm = this.emptyScheduleForm();
    this.scheduleFormError.set(null);
    this.isSchedulingMatch.set(true);
  }

  openEditMatchModal(match: Match): void {
    const localTeam = this.teams().find(t => t.name === match.local);
    const visitorTeam = this.teams().find(t => t.name === match.visitor);
    this.editingMatch.set(match);
    this.scheduleForm = {
      idLocal: localTeam?.id ?? '',
      idVisitor: visitorTeam?.id ?? '',
      fechaHora: this.toDatetimeLocal(match.date),
      estadio: match.estadio ?? '',
      fase: match.fase ?? ''
    };
    this.scheduleFormError.set(null);
    this.isSchedulingMatch.set(true);
  }

  closeScheduleMatchModal(): void {
    this.isSchedulingMatch.set(false);
    this.editingMatch.set(null);
    this.savingMatch.set(false);
    this.scheduleFormError.set(null);
    this.scheduleForm = this.emptyScheduleForm();
  }

  openOfficializeMatchModal(match?: Match): void {
    this.loadMatches();
    if (match) {
      this.officializeForm = {
        matchId: match.id,
        localScore: match.localScore,
        visitorScore: match.visitorScore
      };
    } else {
      this.officializeForm = this.emptyOfficializeForm();
    }
    this.isOfficializingMatch.set(true);
  }

  closeOfficializeMatchModal(): void {
    this.isOfficializingMatch.set(false);
    this.officializeForm = this.emptyOfficializeForm();
  }


  startLiveNow(match: Match): void {
    this.openConfirm({
      title: 'Iniciar partido en vivo',
      message: `¿Iniciar ahora ${match.local} vs ${match.visitor} en vivo?`,
      confirmLabel: 'Iniciar ya',
      variant: 'warning',
      onConfirm: () => {
        this.matchService.startLive(match.id).subscribe({
          next: () => {
            this.loadMatches();
            this.showToast('Partido en vivo. Aparecerá en la página de inicio.');
          },
          error: (err: { error?: { message?: string; detail?: string }; message?: string }) =>
            this.showToast(
              err.error?.message ?? err.error?.detail ?? err.message ?? 'No se pudo iniciar el partido.',
              'error'
            )
        });
      }
    });
  }

  suspendMatchNow(match: Match): void {
    const message =
      match.status === 'EN_VIVO'
        ? `¿Suspender ${match.local} vs ${match.visitor}?\n\nEl reloj se congelará en ${match.minute ?? 0}'.`
        : `¿Marcar como suspendido ${match.local} vs ${match.visitor}?\n\nNo aparecerá en la home hasta reanudarlo.`;

    this.openConfirm({
      title: 'Suspender partido',
      message,
      confirmLabel: 'Suspender',
      variant: 'warning',
      onConfirm: () => {
        this.matchService.suspendMatch(match.id).subscribe({
          next: () => {
            if (this.simulationMatchId === match.id) {
              this.simulationMatchId = '';
              this.localPlayers = [];
              this.visitorPlayers = [];
              this.eventForm = this.emptyEventForm();
            }
            this.loadMatches();
            this.showToast('Partido suspendido.');
          },
          error: (err: { error?: { message?: string; detail?: string }; message?: string }) =>
            this.showToast(
              err.error?.message ?? err.error?.detail ?? err.message ?? 'No se pudo suspender el partido.',
              'error'
            )
        });
      }
    });
  }

  resumeMatchNow(match: Match): void {
    this.openConfirm({
      title: 'Reanudar partido',
      message: `¿Reanudar ${match.local} vs ${match.visitor}?`,
      confirmLabel: 'Reanudar',
      variant: 'primary',
      onConfirm: () => {
        this.matchService.resumeMatch(match.id).subscribe({
          next: () => {
            this.loadMatches();
            this.showToast('Partido reanudado.');
          },
          error: (err: { error?: { message?: string; detail?: string }; message?: string }) =>
            this.showToast(
              err.error?.message ?? err.error?.detail ?? err.message ?? 'No se pudo reanudar el partido.',
              'error'
            )
        });
      }
    });
  }

  handleTeamSave(data: { name: string; pais: string; group: string; rankingRaw: string; banderaUrl: string }): void {
    if (this.editingTeam()) {
      this.updateTeam(data.name, data.pais, data.group, data.rankingRaw, data.banderaUrl);
    } else {
      this.createTeam(data.name, data.pais, data.group, data.rankingRaw, data.banderaUrl);
    }
  }

  updateTeam(name: string, pais: string, group: string, rankingRaw: string, banderaUrl: string): void {
    const team = this.editingTeam();
    if (!team || !name?.trim() || !group?.trim()) return;

    const fifaRank = rankingRaw === '' || rankingRaw == null ? 0 : Number(rankingRaw);
    this.teamService
      .updateTeam(team.id, {
        name: name.trim(),
        pais: pais?.trim() || 'Sin definir',
        group: group.trim(),
        fifaRank: Number.isFinite(fifaRank) ? Math.max(0, Math.floor(fifaRank)) : 0,
        banderaUrl: banderaUrl?.trim() || ''
      })
      .subscribe({
        next: () => {
          this.showToast('Equipo actualizado exitosamente.');
          this.closeTeamModal();
          this.loadTeams();
        },
        error: (err: { error?: { message?: string }; message?: string }) =>
          this.showToast(err.error?.message ?? err.message ?? 'No se pudo actualizar el equipo.', 'error')
      });
  }

  removeTeam(team: TeamInfo): void {
    this.openConfirm({
      title: 'Eliminar equipo',
      message: `¿Eliminar definitivamente a ${team.name}?\n\nEsta acción no se puede deshacer.`,
      confirmLabel: 'Eliminar',
      variant: 'danger',
      onConfirm: () => {
        this.teamService.deleteTeam(team.id).subscribe({
          next: () => this.loadTeams(),
          error: (err: { error?: { message?: string; detail?: string }; message?: string }) =>
            this.showToast(
              err.error?.message ?? err.error?.detail ?? err.message ?? 'No se pudo eliminar el equipo.',
              'error'
            )
        });
      }
    });
  }

  submitScheduleForm(): void {
    if (this.savingMatch()) return;
    this.scheduleFormError.set(null);

    const { idLocal, idVisitor, fechaHora, estadio, fase } = this.scheduleForm;
    if (!idLocal || !idVisitor || idLocal === idVisitor) {
      this.showScheduleError('Elige dos equipos distintos.');
      return;
    }
    if (!fechaHora?.trim()) {
      this.showScheduleError('Indica fecha y hora del partido.');
      return;
    }
    if (!estadio?.trim() || !fase?.trim()) {
      this.showScheduleError('Completa estadio y fase.');
      return;
    }

    const scheduledAt = this.parseDatetimeLocal(fechaHora.trim());
    if (!scheduledAt) {
      this.showScheduleError('La fecha y hora no son válidas.');
      return;
    }
    if (scheduledAt.getTime() <= Date.now()) {
      this.showScheduleError(
        'La fecha y hora deben ser posteriores al momento actual. Elige un horario futuro.'
      );
      return;
    }

    let iso = fechaHora.trim();
    if (iso.length === 16) {
      iso += ':00';
    }

    const payload: MatchCreateRequest = {
      idEquipoLocal: Number(idLocal),
      idEquipoVisitante: Number(idVisitor),
      fechaHora: iso,
      estadio: estadio.trim(),
      fase: fase.trim()
    };

    const editing = this.editingMatch();
    this.savingMatch.set(true);

    const request$ = editing
      ? this.matchService.updateMatch(editing.id, payload)
      : this.matchService.createMatch(payload);

    request$.subscribe({
      next: () => {
        this.savingMatch.set(false);
        this.closeScheduleMatchModal();
        this.loadMatches();
        this.showToast(editing ? 'Partido actualizado correctamente.' : 'Partido programado correctamente.');
      },
      error: (err: { error?: { message?: string; detail?: string }; message?: string }) => {
        this.savingMatch.set(false);
        const msg =
          err.error?.message ??
          err.error?.detail ??
          err.message ??
          (editing
            ? 'No se pudo actualizar el partido.'
            : 'No se pudo crear el partido. La fecha y hora deben ser futuras.');
        this.showScheduleError(msg);
      }
    });
  }

  removeMatch(match: Match): void {
    if (match.status === 'OFICIAL') {
      this.showToast('No se puede eliminar un partido ya oficializado.', 'error');
      return;
    }

    const message =
      match.status === 'EN_VIVO' || match.status === 'SUSPENDIDO'
        ? `¿Cancelar y eliminar el partido ${match.local} vs ${match.visitor}?\n\nSe borrarán marcador, eventos y dejará de mostrarse en la home. Esta acción no se puede deshacer.`
        : `¿Eliminar el partido ${match.local} vs ${match.visitor}?\n\nEsta acción no se puede deshacer.`;

    this.openConfirm({
      title: 'Eliminar partido',
      message,
      confirmLabel: 'Eliminar',
      variant: 'danger',
      onConfirm: () => {
        this.matchService.deleteMatch(match.id).subscribe({
          next: () => {
            if (this.simulationMatchId === match.id) {
              this.simulationMatchId = '';
              this.localPlayers = [];
              this.visitorPlayers = [];
              this.eventForm = this.emptyEventForm();
            }
            this.loadMatches();
            this.showToast(
              match.status === 'EN_VIVO' || match.status === 'SUSPENDIDO'
                ? 'Partido eliminado. La simulación se detuvo.'
                : 'Partido eliminado correctamente.'
            );
          },
          error: (err: { error?: { message?: string; detail?: string }; message?: string }) =>
            this.showToast(
              err.error?.message ?? err.error?.detail ?? err.message ?? 'No se pudo eliminar el partido.',
              'error'
            )
        });
      }
    });
  }

  matchStatusLabel(status: Match['status']): string {
    switch (status) {
      case 'EN_VIVO':
        return 'En vivo';
      case 'SUSPENDIDO':
        return 'Suspendido';
      case 'OFICIAL':
        return 'Oficial';
      default:
        return 'Programado';
    }
  }

  officializeMatch(): void {
    const { matchId, localScore, visitorScore } = this.officializeForm;
    if (!matchId?.trim()) {
      this.showToast('Selecciona un partido en vivo.', 'error');
      return;
    }
    if (!Number.isFinite(localScore) || !Number.isFinite(visitorScore) || localScore < 0 || visitorScore < 0) {
      this.showToast('Introduce goles válidos (enteros ≥ 0) para local y visitante.', 'error');
      return;
    }
    this.matchService.officializeResult(matchId.trim(), localScore, visitorScore).subscribe({
      next: () => {
        this.closeOfficializeMatchModal();
        this.loadMatches();
        this.showToast('Resultado oficializado correctamente.');
      },
      error: (err: { error?: { message?: string; detail?: string }; message?: string }) =>
        this.showToast(
          err.error?.message ?? err.error?.detail ?? err.message ?? 'No se pudo oficializar el partido.',
          'error'
        )
    });
  }

  private toDatetimeLocal(iso: string): string {
    const m = iso.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    if (m) {
      return `${m[1]}-${m[2]}-${m[3]}T${m[4]}:${m[5]}`;
    }
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}T${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}`;
  }

  private parseDatetimeLocal(value: string): Date | null {
    const normalized = value.length === 16 ? `${value}:00` : value;
    const d = new Date(normalized);
    return Number.isNaN(d.getTime()) ? null : d;
  }

  loadGroupStandings(): void {
    if (!this.standingsGroup?.trim()) return;
    this.adminService.getGroupStandings(this.standingsGroup.trim()).subscribe({
      next: rows => {
        this.groupStandingsRows.set(rows.map(r => ({ ...r })));
        this.rebuildStandingsSnapshot(rows);
        this.standingsDirty.set(false);
      },
      error: () => {
        this.groupStandingsRows.set([]);
        this.standingsSnapshot.clear();
      }
    });
  }

  standingRowHasChanges(row: Standing): boolean {
    if (!row.teamId) return false;
    const snap = this.standingsSnapshot.get(row.teamId);
    if (!snap) return false;
    const current = this.snapshotFromRow(row);
    return (
      snap.pts !== current.pts ||
      snap.pj !== current.pj ||
      snap.pg !== current.pg ||
      snap.pe !== current.pe ||
      snap.pp !== current.pp ||
      snap.gf !== current.gf ||
      snap.gc !== current.gc
    );
  }

  markStandingsDirty(): void {
    this.standingsDirty.set(this.groupStandingsRows().some(r => this.standingRowHasChanges(r)));
  }

  private rebuildStandingsSnapshot(rows: Standing[]): void {
    this.standingsSnapshot.clear();
    for (const row of rows) {
      if (row.teamId) {
        this.standingsSnapshot.set(row.teamId, this.snapshotFromRow(row));
      }
    }
  }

  private snapshotFromRow(row: Standing): StandingRowSnapshot {
    return {
      pts: row.pts ?? 0,
      pj: row.pj ?? 0,
      pg: row.pg ?? 0,
      pe: row.pe ?? 0,
      pp: row.pp ?? 0,
      gf: row.gf ?? 0,
      gc: row.gc ?? 0
    };
  }

  onStandingsGroupChange(): void {
    this.standingsDirty.set(false);
    this.loadGroupStandings();
  }

  saveStandingRow(row: Standing): void {
    if (!row.teamId || this.savingStanding() || !this.standingRowHasChanges(row)) return;
    this.savingStanding.set(true);
    this.adminService.updateStanding(row.teamId, {
      pts: row.pts,
      pj: row.pj,
      pg: row.pg ?? 0,
      pe: row.pe ?? 0,
      pp: row.pp ?? 0,
      gf: row.gf ?? 0,
      gc: row.gc ?? 0
    }).subscribe({
      next: () => {
        this.savingStanding.set(false);
        this.loadGroupStandings();
        this.showToast('Clasificación manual guardada.');
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.savingStanding.set(false);
        this.showToast(err.error?.message ?? err.message ?? 'No se pudo guardar.', 'error');
      }
    });
  }
}
