export interface Match {
  id: string;
  local: string;
  visitor: string;
  localFlag: string;
  visitorFlag: string;
  localBanderaUrl?: string;
  visitorBanderaUrl?: string;
  localScore: number;
  visitorScore: number;
  status: MatchStatus;
  date: string;
  estadio?: string;
  fase?: string;
  minute?: number;
  stoppageTime?: number;
  halftimeBreak?: boolean;
  scheduledBy?: string;
  events?: MatchEvent[];
  details?: MatchDetails;
}

export type MatchStatus = 'PROGRAMADO' | 'EN_VIVO' | 'SUSPENDIDO' | 'OFICIAL';

export interface MatchEvent {
  id?: string;
  minute: number;
  text: string;
  type: 'goal' | 'yellow' | 'red' | 'card' | 'info';
}

export interface MatchDetails {
  stadium: string;
  capacity: number;
  referee: string;
  var: string;
  weather: string;
  city: string;
}
