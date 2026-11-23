import {
  Badge,
  Button,
  Card,
  Divider,
  Group,
  ScrollArea,
  Stack,
  Text,
} from '@mantine/core';
import type { Priority, Recommendation, SystemStatusValue } from '../types';
import { cowperLabel } from '../utils';

const PRIORITY_COLOR: Record<Priority, string> = {
  INFO: 'blue',
  WARNING: 'yellow',
  CRITICAL: 'red',
};

const STATUS_COLOR: Record<string, string> = {
  APPLIED: 'green',
  REJECTED: 'gray',
  EXPIRED: 'gray',
};

interface Props {
  recommendations: Recommendation[];
  selected: string;
  status: SystemStatusValue;
  onApply: (rec: Recommendation) => void;
  onReject: (rec: Recommendation) => void;
}

export function RecommendationsPanel({ recommendations, selected, status, onApply, onReject }: Props) {
  const list = recommendations
    .filter((r) => selected === 'ALL' || r.cowperId === selected)
    .slice()
    .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt));

  return (
    <Card withBorder padding="sm" h="100%">
      <Group justify="space-between" mb="xs">
        <Text fw={600}>Рекомендации</Text>
        <Badge variant="light" color="gray">
          {list.filter((r) => r.status === 'PENDING').length} PENDING
        </Badge>
      </Group>

      <ScrollArea h="calc(100vh - 190px)" offsetScrollbars type="hover">
        <Stack gap="sm" pr={4}>
          {!list.length && (
            <Text size="sm" c="dimmed">
              Нет рекомендаций по выбранному воздухонагревателю.
            </Text>
          )}

          {list.map((rec) => {
            const blockApply = rec.status !== 'PENDING' || status !== 'ACTIVE';
            return (
              <Card
                key={rec.id}
                withBorder
                padding="sm"
                style={{
                  borderLeft: `4px solid var(--mantine-color-${PRIORITY_COLOR[rec.priority]}-6)`,
                }}
              >
                <Group justify="space-between" gap={6} mb={6} wrap="nowrap">
                  <Group gap={6} wrap="nowrap">
                    <Badge size="xs" color={PRIORITY_COLOR[rec.priority]}>
                      {rec.priority}
                    </Badge>
                    <Badge size="xs" variant="outline" color="gray">
                      {cowperLabel(rec.cowperId)}
                    </Badge>
                  </Group>
                  <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap' }}>
                    #{rec.id} · {new Date(rec.createdAt).toLocaleTimeString('ru-RU')}
                  </Text>
                </Group>

                <Text size="sm" fw={500} mb={6}>
                  {rec.message}
                </Text>

                <Group gap={6} mb={6}>
                  <Badge size="xs" variant="light" color="gray">
                    {rec.ruleCode}
                  </Badge>
                  {rec.status !== 'PENDING' && (
                    <Badge size="xs" color={STATUS_COLOR[rec.status] ?? 'gray'}>
                      {rec.status}
                    </Badge>
                  )}
                </Group>

                {rec.expectedEffect && (
                  <Group gap={6} mb={6}>
                    {rec.expectedEffect.tempDelta != null && (
                      <Badge
                        size="xs"
                        variant="light"
                        color={rec.expectedEffect.tempDelta >= 0 ? 'green' : 'orange'}
                      >
                        темп. {rec.expectedEffect.tempDelta >= 0 ? '+' : ''}
                        {rec.expectedEffect.tempDelta} °C
                      </Badge>
                    )}
                    {rec.expectedEffect.gasDelta != null && (
                      <Badge
                        size="xs"
                        variant="light"
                        color={rec.expectedEffect.gasDelta <= 0 ? 'green' : 'orange'}
                      >
                        газ {rec.expectedEffect.gasDelta >= 0 ? '+' : ''}
                        {rec.expectedEffect.gasDelta} м³/ч
                      </Badge>
                    )}
                  </Group>
                )}

                {!!rec.reasoning?.length && (
                  <Stack gap={2} mb="xs">
                    <Text size="xs" c="dimmed" fw={500}>
                      Объяснение
                    </Text>
                    {rec.reasoning.map((line, i) => (
                      <Text key={i} size="xs" c="dimmed">
                        • {line}
                      </Text>
                    ))}
                  </Stack>
                )}

                <Divider mb="xs" />
                <Group gap={6} grow>
                  <Button
                    size="compact-sm"
                    onClick={() => onApply(rec)}
                    disabled={blockApply}
                    title={status !== 'ACTIVE' ? 'Недостаточно данных' : undefined}
                  >
                    Применить
                  </Button>
                  <Button
                    size="compact-sm"
                    variant="light"
                    color="gray"
                    onClick={() => onReject(rec)}
                    disabled={rec.status !== 'PENDING'}
                  >
                    Отклонить
                  </Button>
                </Group>
              </Card>
            );
          })}
        </Stack>
      </ScrollArea>
    </Card>
  );
}
