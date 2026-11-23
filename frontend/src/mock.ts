import type { Cowper, Phase, Recommendation, TelemetryPoint } from './types';

export const FALLBACK_COWPERS: Cowper[] = [
  { id: 'STOVE_1', name: 'VN-1', status: 'BLOWING' },
  { id: 'STOVE_2', name: 'VN-2', status: 'HEATING' },
  { id: 'STOVE_3', name: 'VN-3', status: 'HEATING' },
  { id: 'STOVE_4', name: 'VN-4', status: 'BLOWING' },
];

const PHASES: Phase[] = ['BLOWING', 'HEATING', 'HEATING', 'BLOWING'];

const wave = (t: number, periodMs: number, phase: number) => Math.sin((t / periodMs) * Math.PI * 2 + phase);

export function mockPoint(index: number, at: number): TelemetryPoint {
  const id = FALLBACK_COWPERS[index % FALLBACK_COWPERS.length].id;
  const phase = PHASES[index % PHASES.length];
  const blowing = phase === 'BLOWING';
  const w1 = wave(at, 180_000, index);
  const w2 = wave(at, 95_000, index * 1.7);
  const w3 = wave(at, 47_000, index * 2.3);

  return {
    cowperId: id,
    recordedAt: new Date(at).toISOString(),
    phase,
    hotBlastTemp: blowing ? 1175 + 24 * w1 : 0,
    hotBlastSetpoint: 1190,
    stoveOutletTemp: blowing ? 1240 + 42 * w2 : 880 + 15 * w2,
    mixingColdShare: blowing ? 6 + 3.4 * w3 : 0,
    mixedGasFlow: blowing ? null : 48_000 + 8_500 * w1,
    combustionAirFlow: blowing ? null : 52_000 + 7_500 * w2,
    airGasRatio: 1.16 + 0.1 * w3,
    bfGasFlow: blowing ? 0 : 41_000 + 7_000 * w1,
    natGasFlow: blowing ? 0 : 7_000 + 1_500 * w2,
    domeTemp: blowing ? 1180 + 22 * w2 : 1325 + 48 * w1,
    wasteGasTemp: blowing ? 235 + 22 * w3 : 300 + 55 * w2,
    flueGasTemp: blowing ? 235 + 22 * w3 : 300 + 55 * w2,
    flueO2: blowing ? 0.9 + 0.35 * w3 : 1.25 + 0.85 * w2,
    flueCO: blowing ? 280 + 180 * w3 : 950 + 650 * w2,
  };
}

export function mockHistory(minutes = 15, now = Date.now()): Record<string, TelemetryPoint[]> {
  const result: Record<string, TelemetryPoint[]> = {};
  FALLBACK_COWPERS.forEach((c) => (result[c.id] = []));
  const points = minutes * 60;
  for (let i = points; i > 0; i -= 1) {
    const at = now - i * 1000;
    FALLBACK_COWPERS.forEach((_, idx) => {
      const p = mockPoint(idx, at);
      result[p.cowperId].push(p);
    });
  }
  return result;
}

export function mockRecommendations(now = new Date()): Recommendation[] {
  const iso = (offsetSec: number) => new Date(now.getTime() - offsetSec * 1000).toISOString();
  return [
    {
      id: 1042,
      cowperId: 'STOVE_2',
      createdAt: iso(120),
      ruleCode: 'ERR_DOME_OVERHEAT',
      message: 'КРИТИЧЕСКАЯ ТЕМПЕРАТУРА КУПОЛА (1420 °C). Немедленно снизьте расход газа!',
      priority: 'CRITICAL',
      status: 'PENDING',
      expectedEffect: { tempDelta: -15.5, gasDelta: -500 },
      reasoning: ['domeTemp > 1400.0 (Текущее: 1420.0)', 'Тренд роста +6 °C/мин'],
      targetParameter: 'mixedGasFlow',
      suggestedValueMin: 42_000,
      suggestedValueMax: 44_000,
    },
    {
      id: 1041,
      cowperId: 'STOVE_3',
      createdAt: iso(260),
      ruleCode: 'ERR_WASTE_GAS_TEMP',
      message: 'Температура отходящих дымов приближается к порогу R2 (400 °C).',
      priority: 'WARNING',
      status: 'PENDING',
      expectedEffect: { tempDelta: -8, gasDelta: -200 },
      reasoning: ['wasteGasTemp > 380.0 (Текущее: 392.0)'],
      targetParameter: 'airGasRatio',
      suggestedValueMin: 1.08,
      suggestedValueMax: 1.12,
    },
    {
      id: 1040,
      cowperId: 'STOVE_2',
      createdAt: iso(420),
      ruleCode: 'INFO_O2_LOW',
      message: 'Пониженный остаточный кислород в дымовых газах (0.24 %).',
      priority: 'INFO',
      status: 'PENDING',
      expectedEffect: { tempDelta: 2, gasDelta: 0 },
      reasoning: ['flueO2 < 0.3 (Текущее: 0.24)'],
      targetParameter: 'airGasRatio',
      suggestedValueMin: 1.18,
      suggestedValueMax: 1.2,
    },
  ];
}
