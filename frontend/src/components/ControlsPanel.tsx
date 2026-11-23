import { useMemo, useState } from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  NumberInput,
  Stack,
  Text,
} from '@mantine/core';
import type { ControlsBody, Cowper, Recommendation, TelemetryPoint } from '../types';
import { cowperLabel, latestPoint } from '../utils';

interface Props {
  cowpers: Cowper[];
  selected: string;
  history: Record<string, TelemetryPoint[]>;
  recommendations: Recommendation[];
  disabled: boolean;
  onApply: (body: ControlsBody) => Promise<{ ok: boolean; message: string }>;
}

export function ControlsPanel({
  cowpers,
  selected,
  history,
  recommendations,
  disabled,
  onApply,
}: Props) {
  const [mixed, setMixed] = useState<number | string>(() => {
    const m = latestPoint(history[selected])?.mixedGasFlow;
    return m == null ? 48000 : Math.round(m / 1000) * 1000;
  });
  const [alpha, setAlpha] = useState<number | string>(() => {
    const a = latestPoint(history[selected])?.airGasRatio;
    return a == null ? 1.15 : Math.round(a * 100) / 100;
  });
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<{ ok: boolean; message: string } | null>(null);

  const isAll = selected === 'ALL';
  const cowper = cowpers.find((c) => c.id === selected);

  const activeRec = useMemo(
    () =>
      recommendations.find(
        (r) =>
          r.status === 'PENDING' &&
          (isAll || r.cowperId === selected) &&
          (r.targetParameter === 'mixedGasFlow' || r.targetParameter === 'airGasRatio'),
      ),
    [recommendations, selected, isAll],
  );

  const fillFromRecommendation = () => {
    if (!activeRec) return;
    const value = activeRec.suggestedValueMin ?? activeRec.suggestedValueMax;
    if (value == null) return;
    if (activeRec.targetParameter === 'airGasRatio') setAlpha(value);
    else setMixed(value);
  };

  const submit = async () => {
    const m = Number(mixed);
    const a = Number(alpha);
    if (!Number.isFinite(m) || !Number.isFinite(a)) return;
    setBusy(true);
    const res = await onApply({ mixedGasFlow: m, alpha: a, airGasRatio: a });
    setResult(res);
    setBusy(false);
  };

  return (
    <Card withBorder padding="md" mt="sm">
      <Group justify="space-between" mb="xs">
        <Text fw={600}>Управляющие воздействия</Text>
        <Badge variant="light" color={isAll ? 'gray' : 'blue'}>
          {isAll ? 'Выберите ВН' : cowperLabel(selected, cowper?.name)}
        </Badge>
      </Group>

      <Group align="flex-end" gap="md" wrap="wrap">
        <NumberInput
          label="Расход смешанного газа, м³/ч"
          value={mixed}
          onChange={setMixed}
          min={25000}
          max={65000}
          step={1000}
          decimalScale={0}
          w={240}
          disabled={isAll || disabled}
        />
        <NumberInput
          label="Коэффициент избытка воздуха α"
          value={alpha}
          onChange={setAlpha}
          min={1.02}
          max={1.4}
          step={0.02}
          decimalScale={2}
          w={220}
          disabled={isAll || disabled}
        />
        <Button onClick={submit} loading={busy} disabled={isAll || disabled}>
          Применить параметры
        </Button>
        <Button variant="light" onClick={fillFromRecommendation} disabled={!activeRec || isAll}>
          Подставить из рекомендации
        </Button>
      </Group>

      <Stack gap={4} mt="xs">
        {isAll && (
          <Text size="xs" c="dimmed">
            Ручной ввод доступен для конкретного воздухонагревателя.
          </Text>
        )}
        {!isAll && !activeRec && (
          <Text size="xs" c="dimmed">
            Активной рекомендации для этого ВН нет.
          </Text>
        )}
        {activeRec && (
          <Text size="xs" c="dimmed">
            Рекомендация #{activeRec.id}: {activeRec.targetParameter} ={' '}
            {activeRec.suggestedValueMin ?? '—'}…{activeRec.suggestedValueMax ?? '—'}
          </Text>
        )}
        {result && (
          <Alert color={result.ok ? 'green' : 'red'} variant="light" py={6}>
            {result.message}
          </Alert>
        )}
      </Stack>
    </Card>
  );
}
