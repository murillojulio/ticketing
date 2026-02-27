import { AppShell, Burger, Button, Group, NavLink, Text } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import React from 'react';
import { BrowserRouter, Link, Navigate, Route, Routes, useLocation } from 'react-router-dom';

import { useAuth } from './AuthContext';
import { ProtectedRoute } from './ProtectedRoute';
import { EventsPage } from '../pages/EventsPage';
import { LoginPage } from '../pages/LoginPage';
import { OrdersPage } from '../pages/OrdersPage';

function ShellRoutes() {
  const [opened, { toggle }] = useDisclosure();
  const { auth, logout } = useAuth();
  const location = useLocation();

  return (
    <AppShell
      header={{ height: 56 }}
      navbar={{ width: 260, breakpoint: 'sm', collapsed: { mobile: !opened } }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group>
            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" />
            <Text fw={700}>Ticketing Admin</Text>
          </Group>
          <Group gap="sm">
            <Text size="sm" c="dimmed">
              {auth?.email} ({auth?.role})
            </Text>
            <Button variant="subtle" onClick={logout}>
              Salir
            </Button>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        <NavLink
          label="Eventos"
          component={Link}
          to="/events"
          active={location.pathname.startsWith('/events')}
        />
        <NavLink
          label="Órdenes"
          component={Link}
          to="/orders"
          active={location.pathname.startsWith('/orders')}
        />
      </AppShell.Navbar>

      <AppShell.Main>
        <Routes>
          <Route path="/" element={<Navigate to="/events" replace />} />
          <Route path="/events" element={<EventsPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="*" element={<Navigate to="/events" replace />} />
        </Routes>
      </AppShell.Main>
    </AppShell>
  );
}

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <ShellRoutes />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

