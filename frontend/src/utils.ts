import type { ChartRow, TelemetryPoint } from './types';

export const num = (v: unknown): number | null =>
  typeof v === 'number' && Number.isFinite(v) ? v : null;

/** Приводим разночтения контрактов к единому виду. */
export function normalizePoint(p: TelemetryPoint): TelemetryPoint {
  const bf = num(p.bfGasFlow) ?? 0;
  const nat = num(p.natGasFlow) ?? 0;
  return {
    ...p,
    hotBlastSetpoint: num(p.hotBlastSetpoint) ?? 1190,
    mixedGasFlow: num(p.mixedGasFlow) ?? (p.phase === 'BLOWING' ? null : bf + nat || null),
    wasteGasTemp: num(p.wasteGasTemp) ?? num(p.flueGasTemp),
  };
}

export const CHART_FIELDS = [
  'hotBlastTemp',
  'hotBlastSetpoint',
  'stoveOutletTemp',
  'mixingColdShare',
  'mixedGasFlow',
  'combustionAirFlow',
  'domeTemp',
  'wasteGasTemp',
  'flueO2',
  'flueCO',
] as const;

export function singleRows(points: TelemetryPoint[] | undefined): ChartRow[] {
  if (!points) return [];
  return points.map((raw) => {
    const p = normalizePoint(raw);
    const row: ChartRow = { t: Date.parse(p.recordedAt) };
    for (const f of CHART_FIELDS) row[f] = num(p[f]);
    return row;
  });
}

/** Для режима «Все»: единая временная сетка + последнее известное значение по каждому ВН. */
export function allRows(history: Record<string, TelemetryPoint[]>, ids: string[]): ChartRow[] {
  const series = ids.filter((id) => history[id]?.length);
  if (!series.length) return [];

  const times = new Set<number>();
  for (const id of series) {
    for (const p of history[id]) {
      const t = Date.parse(p.recordedAt);
      if (Number.isFinite(t)) times.add(t);
    }
  }
  const sorted = [...times].sort((a, b) => a - b);
  const ptr: Record<string, number> = {};
  const last: Record<string, TelemetryPoint | null> = {};

  return sorted.map((t) => {
    const row: ChartRow = { t };
    for (const id of series) {
      const arr = history[id];
      let i = ptr[id] ?? 0;
      while (i < arr.length && Date.parse(arr[i].recordedAt) <= t) {
        last[id] = arr[i];
        i += 1;
      }
      ptr[id] = i;
      const p = last[id];
      if (p) {
        const n = normalizePoint(p);
        for (const f of CHART_FIELDS) row[`${id}__${f}`] = num(n[f]);
      }
    }
    return row;
  });
}

export function cowperLabel(id: string, name?: string): string {
  if (/^VN-\d+$/i.test(id)) return id.toUpperCase();
  if (name) return name;
  const digits = id.match(/\d+/)?.[0];
  return digits ? `VN-${digits}` : id;
}

/** Прореживание точек перед передачей в Recharts: десятки тысяч SVG-узлов — главный источник лагов. */
export function downsample<T>(rows: T[], max: number): T[] {
  if (rows.length <= max) return rows;
  const step = rows.length / max;
  const out: T[] = [];
  for (let i = 0; i < max; i += 1) {
    out.push(rows[Math.min(rows.length - 1, Math.round(i * step))]);
  }
  return out;
}

export function latestPoint(points: TelemetryPoint[] | undefined): TelemetryPoint | undefined {
  return points && points.length ? points[points.length - 1] : undefined;
}

export const fmtTime = (t: number | string) =>
  new Date(Number(t)).toLocaleTimeString('ru-RU', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });

export function fmtNumber(v: unknown, digits = 1): string {
  const n = num(v);
  return n === null ? '—' : n.toLocaleString('ru-RU', { maximumFractionDigits: digits });
}
