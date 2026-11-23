import { useState } from 'react';
import {
  AppShell,
  Badge,
  Box,
  Button,
  Grid,
  Group,
  Notification,
  Select,
  Text,
  Title,
} from '@mantine/core';
import { useTherma } from './useTherma';
import { ChartsGrid } from './components/ChartsGrid';
import { RecommendationsPanel } from './components/RecommendationsPanel';
import { ControlsPanel } from './components/ControlsPanel';
import { cowperLabel } from './utils';

const SCENARIOS = [
  { value: '1', label: 'Норма' },
  { value: '2', label: 'Перегрев купола' },
  { value: '3', label: 'Обрыв связи' },
];

export default function App() {
  const therma = useTherma();
  const [scenario, setScenario] = useState('1');

  const active = therma.status === 'ACTIVE';
  const cowperOptions = [
    { value: 'ALL', label: 'Все' },
    ...therma.cowpers.map((c) => ({ value: c.id, label: cowperLabel(c.id, c.name) })),
  ];

  return (
    <AppShell header={{ height: 64 }} padding="sm">
      <AppShell.Header>
        <Group h="100%" px="md" gap="sm" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Title order={4} visibleFrom="md" style={{ whiteSpace: 'nowrap' }}>
              ЕвразТехника · Кауперы
            </Title>
            <Select
              size="xs"
              w={140}
              aria-label="Активный каупер"
              data={cowperOptions}
              value={therma.selected}
              onChange={(v) => v && therma.setSelected(v)}
              allowDeselect={false}
              comboboxProps={{ withinPortal: false }}
            />
            <Badge variant="dot" color={active ? 'teal' : 'yellow'}>
              {therma.status}
            </Badge>
            {therma.demo && (
              <Badge variant="light" color="grape">
                DEMO
              </Badge>
            )}
            {therma.connected && (
              <Badge variant="light" color="teal">
                LIVE
              </Badge>
            )}
          </Group>

          <Group gap="xs" wrap="nowrap">
            <Select
              size="xs"
              w={170}
              aria-label="Сценарий симулятора"
              data={SCENARIOS}
              value={scenario}
              onChange={(v) => v && setScenario(v)}
              allowDeselect={false}
              comboboxProps={{ withinPortal: false }}
            />
            <Button
              size="xs"
              onClick={() => void therma.startSim(Number(scenario))}
              disabled={!therma.demo && therma.simRunning}
            >
              Старт
            </Button>
            <Button
              size="xs"
              variant="light"
              color="red"
              onClick={() => void therma.stopSim()}
              disabled={!therma.simRunning}
            >
              Стоп
            </Button>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Main>
        <Grid gap="sm">
          <Grid.Col span={{ base: 12, lg: 9 }}>
            <ChartsGrid
              history={therma.history}
              cowpers={therma.cowpers}
              selected={therma.selected}
              status={therma.status}
              statusMessage={therma.statusMessage}
            />
            <ControlsPanel
              key={therma.selected}
              cowpers={therma.cowpers}
              selected={therma.selected}
              history={therma.history}
              recommendations={therma.recommendations}
              disabled={!active || !therma.simRunning}
              onApply={therma.submitControls}
            />
          </Grid.Col>

          <Grid.Col span={{ base: 12, lg: 3 }}>
            <RecommendationsPanel
              recommendations={therma.recommendations}
              selected={therma.selected}
              status={therma.status}
              onApply={(rec) => void therma.applyRec(rec)}
              onReject={(rec) => void therma.rejectRec(rec)}
            />
          </Grid.Col>
        </Grid>
      </AppShell.Main>

      {therma.lastAlert && (
        <Box pos="fixed" top={72} right={16} w={340} style={{ zIndex: 400 }}>
          <Notification
            color={
              therma.lastAlert.priority === 'CRITICAL'
                ? 'red'
                : therma.lastAlert.priority === 'WARNING'
                  ? 'yellow'
                  : 'blue'
            }
            title="Новая рекомендация"
            onClose={therma.clearLastAlert}
          >
            <Text size="xs" c="dimmed" mb={2}>
              {cowperLabel(therma.lastAlert.cowperId)} · {therma.lastAlert.priority}
            </Text>
            <Text size="sm">{therma.lastAlert.message}</Text>
          </Notification>
        </Box>
      )}
    </AppShell>
  );
}
