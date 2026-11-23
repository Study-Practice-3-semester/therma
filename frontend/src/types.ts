export type Phase = 'HEATING' | 'BLOWING' | 'CHANGING' | 'ISOLATED';
export type SystemStatusValue = 'ACTIVE' | 'INSUFFICIENT_DATA';
export type Priority = 'INFO' | 'WARNING' | 'CRITICAL';
export type RecommendationStatus = 'PENDING' | 'APPLIED' | 'REJECTED' | 'EXPIRED';

export interface Cowper {
  id: string;
  name: string;
  status?: Phase | 'RESERVE' | 'MAINTENANCE';
}

export interface TelemetryPoint {
  cowperId: string;
  recordedAt: string;
  phase?: Phase;
  hotBlastTemp?: number | null;
  hotBlastSetpoint?: number | null;
  stoveOutletTemp?: number | null;
  mixingColdShare?: number | null;
  mixedGasFlow?: number | null;
  combustionAirFlow?: number | null;
  domeTemp?: number | null;
  wasteGasTemp?: number | null;
  flueGasTemp?: number | null;
  flueO2?: number | null;
  flueCO?: number | null;
  airGasRatio?: number | null;
  bfGasFlow?: number | null;
  natGasFlow?: number | null;
}

export interface ExpectedEffect {
  tempDelta?: number;
  gasDelta?: number;
}

export interface Recommendation {
  id: number;
  cowperId: string;
  createdAt: string;
  ruleCode: string;
  message: string;
  priority: Priority;
  status: RecommendationStatus;
  expectedEffect?: ExpectedEffect;
  reasoning?: string[];
  targetParameter?: string;
  suggestedValueMin?: number;
  suggestedValueMax?: number;
}

export interface SystemStatus {
  status: SystemStatusValue;
  timestamp?: string;
  lastUpdate?: string;
  message?: string;
}

export interface ControlsBody {
  mixedGasFlow: number;
  alpha: number;
  airGasRatio: number;
}

export interface ChartRow {
  t: number;
  [key: string]: number | string | null;
}
