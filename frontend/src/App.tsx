import { AppShell, Burger, Group, Title, Text, Button } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';

export default function App() {
  const [opened, { toggle }] = useDisclosure();

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 300, breakpoint: 'sm', collapsed: { mobile: !opened } }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md">
          <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" />
          <Title order={3}>ЕвразТехника: Кауперы</Title>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        <Text fw={500} mb="sm">Воздухонагреватели</Text>
        {/* Здесь будет навигация */}
        <Button variant="light" fullWidth mt="md">
          Симулятор: Старт
        </Button>
      </AppShell.Navbar>

      <AppShell.Main>
        <Title order={2} mb="md">Мониторинг телеметрии</Title>
        <Text c="dimmed">
          Здесь будут графики Recharts и лента рекомендаций...
        </Text>
      </AppShell.Main>
    </AppShell>
  );
}
