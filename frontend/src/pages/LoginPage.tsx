import { Alert, Button, Container, Paper, PasswordInput, Stack, Text, TextInput, Title } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import React, { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';

import { api } from '../lib/api';
import type { HttpError } from '../lib/http';
import { useAuth } from '../app/AuthContext';

function errorMessage(err: unknown) {
  const e = err as Partial<HttpError>;
  return e?.message ?? 'Error inesperado';
}

export function LoginPage() {
  const { isAuthenticated, setFromAuthResponse } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/events';

  const [email, setEmail] = useState('admin@example.com');
  const [password, setPassword] = useState('password123');
  const [loading, setLoading] = useState(false);
  const [fatal, setFatal] = useState<string | null>(null);

  if (isAuthenticated) {
    return <Navigate to={from} replace />;
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFatal(null);
    setLoading(true);
    try {
      const res = await api.login({ email, password });
      setFromAuthResponse(res);
      notifications.show({ title: 'Sesión iniciada', message: `Bienvenido ${res.email}` });
      navigate(from, { replace: true });
    } catch (err) {
      setFatal(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Container size={420} py={80}>
      <Title ta="center">Ticketing Admin</Title>
      <Text c="dimmed" size="sm" ta="center" mt="xs">
        Inicia sesión con un usuario existente.
      </Text>

      <Paper withBorder shadow="sm" p="lg" radius="md" mt="xl">
        <form onSubmit={onSubmit}>
          <Stack gap="md">
            {fatal ? <Alert color="red" title="No se pudo iniciar sesión">{fatal}</Alert> : null}
            <TextInput
              label="Email"
              placeholder="user@example.com"
              value={email}
              onChange={(e) => setEmail(e.currentTarget.value)}
              required
            />
            <PasswordInput
              label="Password"
              placeholder="Tu password"
              value={password}
              onChange={(e) => setPassword(e.currentTarget.value)}
              required
            />
            <Button type="submit" loading={loading} fullWidth>
              Entrar
            </Button>
            <Text size="xs" c="dimmed">
              Nota: este panel consume los endpoints existentes del backend (eventos y órdenes).
            </Text>
          </Stack>
        </form>
      </Paper>
    </Container>
  );
}

