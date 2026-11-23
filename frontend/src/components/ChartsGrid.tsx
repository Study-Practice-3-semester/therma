import { Box, SimpleGrid, Text } from '@mantine/core';
import type { ChartRef, ChartSeries } from './MetricChart';
import { MetricChart } from './MetricChart';
import type { Cowper, Phase, SystemStatusValue, TelemetryPoint } from '../types';
import { allRows, cowperLabel, latestPoint, singleRows } from '../utils';

const COLORS = ['#4dabf7', '#51cf66', '#ffa94d', '#da77f2', '#f783ac', '#22b8cf'];

interface SeriesDef {
  field: string;
  label: string;
  color: string;
  dashed?: boolean;
  axis?: 'left' | 'right';
}

interface ChartDef {
  title: string;
  series: SeriesDef[];
  refs?: ChartRef[];
  leftDomain?: [number, number];
  rightDomain?: [number, number];
  phase?: Phase;
}

const CHARTS: ChartDef[] = [
  {
    title: 'Температура горячего дутья, °C',
    series: [
      { field: 'hotBlastTemp', label: 'hotBlastTemp', color: '#4dabf7' },
      { field: 'hotBlastSetpoint', label: 'Уставка', color: '#e9ecef', dashed: true },
    ],
    refs: [
      { y: 1140, label: '1140', color: '#ffd43b' },
      { y: 1210, label: '1210', color: '#ffd43b' },
    ],
    leftDomain: [1100, 1230],
  },
  {
    title: 'Выход ВН и подмес холодного дутья',
    series: [
      { field: 'stoveOutletTemp', label: 'Выход ВН, °C', color: '#4dabf7' },
      { field: 'mixingColdShare', label: 'Подмес, %', color: '#ffa94d', dashed: true, axis: 'right' },
    ],
    refs: [
      { y: 1180, label: '1180', color: '#ffd43b' },
      { y: 1300, label: '1300', color: '#ffd43b' },
    ],
    leftDomain: [1150, 1320],
    rightDomain: [0, 12],
    phase: 'BLOWING',
  },
  {
    title: 'Расходы на нагреве, м³/ч',
    series: [
      { field: 'mixedGasFlow', label: 'Смеш. газ', color: '#51cf66' },
      { field: 'combustionAirFlow', label: 'Воздух', color: '#4dabf7', dashed: true },
    ],
    refs: [
      { y: 35000, label: '35 000', color: '#ffd43b' },
      { y: 65000, label: '65 000', color: '#ffd43b' },
    ],
    leftDomain: [30000, 70000],
    phase: 'HEATING',
  },
  {
    title: 'Температура купола на газе, °C',
    series: [{ field: 'domeTemp', label: 'domeTemp', color: '#51cf66' }],
    refs: [
      { y: 1390, label: 'R1 1390', color: '#ff922b' },
      { y: 1400, label: 'R1 1400', color: '#fa5252' },
    ],
    leftDomain: [1100, 1470],
  },
  {
    title: 'Температура отходящих дымов, °C',
    series: [{ field: 'wasteGasTemp', label: 'wasteGasTemp', color: '#ffa94d' }],
    refs: [
      { y: 380, label: 'R2 380', color: '#ff922b' },
      { y: 400, label: 'R2 400', color: '#fa5252' },
    ],
    leftDomain: [150, 480],
  },
  {
    title: 'Показатели горения: O₂ / CO',
    series: [
      { field: 'flueO2', label: 'O₂, %', color: '#4dabf7' },
      { field: 'flueCO', label: 'CO, ppm', color: '#f783ac', dashed: true, axis: 'right' },
    ],
    refs: [
      { y: 0.3, label: 'R3 0.3%', color: '#fa5252' },
      { y: 1000, label: 'R3 1000ppm', color: '#fa5252', axis: 'right' },
    ],
    leftDomain: [0, 3],
    rightDomain: [0, 5000],
  },
];

interface Props {
  history: Record<string, TelemetryPoint[]>;
  cowpers: Cowper[];
  selected: string;
  status: SystemStatusValue;
  statusMessage?: string;
}

export function ChartsGrid({ history, cowpers, selected, status, statusMessage }: Props) {
  const single = selected !== 'ALL';
  const rows = single ? singleRows(history[selected]) : allRows(history, cowpers.map((c) => c.id));
  const phase = single ? latestPoint(history[selected])?.phase : undefined;

  return (
    <Box pos="relative">
      <SimpleGrid cols={{ base: 1, md: 2, xl: 3 }} spacing="sm" verticalSpacing="sm">
        {CHARTS.map((cfg) => {
          const active = !cfg.phase || !single || phase === cfg.phase;
          const series: ChartSeries[] = single
            ? cfg.series.map((s) => ({ ...s, key: s.field }))
            : cowpers.flatMap((c, idx) =>
                cfg.series.map((s) => ({
                  key: `${c.id}__${s.field}`,
                  label:
                    cfg.series.length > 1
                      ? `${cowperLabel(c.id, c.name)} · ${s.label}`
                      : cowperLabel(c.id, c.name),
                  color: COLORS[idx % COLORS.length],
                  dashed: s.dashed,
                  axis: s.axis,
                })),
              );
          return (
            <MetricChart
              key={cfg.title}
              title={cfg.title}
              rows={active ? rows : []}
              series={series}
              refs={cfg.refs}
              leftDomain={cfg.leftDomain}
              rightDomain={cfg.rightDomain}
              note={!active ? `Фаза: ${phase ?? '—'}` : undefined}
            />
          );
        })}
      </SimpleGrid>

      {status === 'INSUFFICIENT_DATA' && (
        <Box
          pos="absolute"
          inset={0}
          style={{
            background: 'rgba(20, 21, 23, 0.72)',
            backdropFilter: 'blur(2px)',
            borderRadius: 8,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 5,
          }}
        >
          <Box ta="center" px="lg">
            <Text fw={700} size="lg">
              Нет актуальной телеметрии
            </Text>
            <Text c="dimmed" size="sm">
              Выдача рекомендаций приостановлена
            </Text>
            {statusMessage && (
              <Text c="dimmed" size="xs" mt={4}>
                {statusMessage}
              </Text>
            )}
          </Box>
        </Box>
      )}
    </Box>
  );
}
