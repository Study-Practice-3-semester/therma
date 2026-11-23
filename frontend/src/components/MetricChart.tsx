import { Badge, Card, Group, Text } from '@mantine/core';
import {
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { ChartRow } from '../types';
import { fmtNumber, fmtTime } from '../utils';

export interface ChartSeries {
  key: string;
  label: string;
  color: string;
  dashed?: boolean;
  axis?: 'left' | 'right';
}

export interface ChartRef {
  y: number;
  label: string;
  color: string;
  axis?: 'left' | 'right';
}

interface Props {
  title: string;
  rows: ChartRow[];
  series: ChartSeries[];
  refs?: ChartRef[];
  leftDomain?: [number | 'auto', number | 'auto'];
  rightDomain?: [number | 'auto', number | 'auto'];
  note?: string;
  unit?: string;
}

export function MetricChart({
  title,
  rows,
  series,
  refs = [],
  leftDomain,
  rightDomain,
  note,
  unit,
}: Props) {
  const hasRight =
    series.some((s) => s.axis === 'right') || refs.some((r) => r.axis === 'right');

  return (
    <Card withBorder padding="sm" h="100%" style={{ overflow: 'hidden' }}>
      <Group justify="space-between" gap={6} mb={4} wrap="nowrap">
        <Text fw={600} size="sm" lineClamp={1} title={title}>
          {title}
        </Text>
        {note && (
          <Badge size="xs" color="gray" variant="light" style={{ flexShrink: 0 }}>
            {note}
          </Badge>
        )}
      </Group>
      <ResponsiveContainer width="100%" height={218}>
        <ComposedChart data={rows} margin={{ top: 6, right: hasRight ? 4 : 14, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#ffffff14" />
          <XAxis
            dataKey="t"
            type="number"
            domain={['dataMin', 'dataMax']}
            tickFormatter={fmtTime}
            fontSize={10}
            minTickGap={45}
            stroke="#909296"
          />
          <YAxis
            yAxisId="left"
            domain={leftDomain ?? ['auto', 'auto']}
            fontSize={10}
            width={48}
            stroke="#909296"
            allowDataOverflow
            tickFormatter={(v: number) => fmtNumber(v, 0)}
          />
          {hasRight && (
            <YAxis
              yAxisId="right"
              orientation="right"
              domain={rightDomain ?? ['auto', 'auto']}
              fontSize={10}
              width={46}
              stroke="#909296"
              allowDataOverflow
              tickFormatter={(v: number) => fmtNumber(v, 0)}
            />
          )}
          <Tooltip
            labelFormatter={(v) => fmtTime(Number(v))}
            formatter={(value: unknown) =>
              typeof value === 'number' ? `${fmtNumber(value, 1)}${unit ? ` ${unit}` : ''}` : String(value ?? '')
            }
            contentStyle={{
              background: '#1a1b1e',
              border: '1px solid #373A40',
              borderRadius: 6,
              fontSize: 12,
            }}
          />
          <Legend wrapperStyle={{ fontSize: 10 }} iconSize={8} />
          {refs.map((r) => (
            <ReferenceLine
              key={`${r.label}-${r.y}`}
              yAxisId={r.axis ?? 'left'}
              y={r.y}
              stroke={r.color}
              strokeDasharray="6 4"
              strokeOpacity={0.8}
              label={{ value: r.label, fill: r.color, fontSize: 9, position: 'insideTopRight' }}
            />
          ))}
          {series.map((s) => (
            <Line
              key={s.key}
              yAxisId={s.axis ?? 'left'}
              type="monotone"
              dataKey={s.key}
              name={s.label}
              stroke={s.color}
              strokeWidth={1.8}
              strokeDasharray={s.dashed ? '5 4' : undefined}
              dot={false}
              connectNulls={false}
              isAnimationActive={false}
            />
          ))}
        </ComposedChart>
      </ResponsiveContainer>
    </Card>
  );
}
