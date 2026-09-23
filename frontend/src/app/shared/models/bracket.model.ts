export interface BracketTeam {
  name: string;
  pais: string;
  banderaUrl: string;
  fifaRank: number;
}

export interface BracketMatch {
  home: BracketTeam;
  away: BracketTeam;
  winner: BracketTeam;
  homeScore?: number | null;
  awayScore?: number | null;
}

export interface BracketSimulation {
  quarterFinalsLeft: BracketMatch[];
  quarterFinalsRight: BracketMatch[];
  semiFinalLeft: BracketMatch | null;
  semiFinalRight: BracketMatch | null;
  finalMatch: BracketMatch | null;
  champion: BracketTeam | null;
  reasoning: string;
}
