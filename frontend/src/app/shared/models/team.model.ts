export interface TeamInfo {
  id: string;
  name: string;
  /** País (modelo ERD / API `pais`). */
  pais?: string;
  flag: string;
  banderaUrl?: string;
  group: string;
  fifaRank: number;
  manager: string;
  formation: string;
  players: Player[];
}

export interface Player {
  name: string;
  position: string;
  rating: number;
  isStar?: boolean;
  photo?: string | null;
}

export interface Standing {
  /** Id del equipo en API (para abrir detalle). */
  teamId?: string;
  name: string;
  flag: string;
  banderaUrl?: string;
  pj: number;
  pg?: number;
  pe?: number;
  pp?: number;
  gf?: number;
  gc?: number;
  dg: number;
  pts: number;
  provisional?: boolean;
  manualOverride?: boolean;
  modifiedBy?: string;
  sealSignature?: string;
  modifiedAt?: string;
}
