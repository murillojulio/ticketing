import {
  Alert,
  Badge,
  Button,
  Card,
  Container,
  Divider,
  Group,
  NumberInput,
  Select,
  SimpleGrid,
  Stack,
  Table,
  Text,
  TextInput,
  Title
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery } from '@tanstack/react-query';
import React, { useMemo, useState } from 'react';

import type { OrderResponse, UUID } from '../app/types';
import { api } from '../lib/api';
import type { HttpError } from '../lib/http';

function errMsg(err: unknown) {
  const e = err as Partial<HttpError>;
  return e?.message ?? 'Error inesperado';
}

function fmtDate(iso: string | null) {
  if (!iso) return '-';
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

export function OrdersPage() {
  const { data: events } = useQuery({ queryKey: ['events'], queryFn: api.listEvents });
  const eventOptions = useMemo(
    () =>
      (events ?? []).map((e) => ({
        value: e.id,
        label: `${e.name} (${e.venue})`
      })),
    [events]
  );

  const [eventId, setEventId] = useState<string | null>(null);
  const [customerId, setCustomerId] = useState('customer-123');
  const [quantity, setQuantity] = useState<number | ''>(2);

  const [lookupId, setLookupId] = useState('');
  const [lastOrder, setLastOrder] = useState<OrderResponse | null>(null);

  const createOrder = useMutation({
    mutationFn: () =>
      api.createOrder({
        eventId: (eventId ?? '') as UUID,
        customerId,
        quantity: typeof quantity === 'number' ? quantity : Number(quantity)
      }),
    onSuccess: (order) => {
      setLastOrder(order);
      setLookupId(order.id);
      notifications.show({ title: 'Orden creada', message: `ID: ${order.id}` });
    },
    onError: (err) => {
      notifications.show({ color: 'red', title: 'No se pudo crear la orden', message: errMsg(err) });
    }
  });

  const getOrder = useMutation({
    mutationFn: (id: string) => api.getOrder(id as UUID),
    onSuccess: (order) => {
      setLastOrder(order);
      notifications.show({ title: 'Orden actualizada', message: `Estado: ${order.state}` });
    },
    onError: (err) => {
      notifications.show({ color: 'red', title: 'No se pudo consultar', message: errMsg(err) });
    }
  });

  const canCreate =
    Boolean(eventId) && customerId.trim().length > 0 && (typeof quantity === 'number' ? quantity >= 1 : Number(quantity) >= 1);

  return (
    <Container size="xl">
      <Group justify="space-between" mb="md">
        <Title order={2}>Órdenes</Title>
      </Group>

      <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
        <Card withBorder>
          <Title order={4} mb="sm">
            Crear orden
          </Title>
          <Stack>
            <Select
              label="Evento"
              placeholder="Selecciona un evento"
              data={eventOptions}
              value={eventId}
              onChange={setEventId}
              searchable
              nothingFoundMessage="No hay eventos"
              required
            />
            <TextInput
              label="Customer ID"
              value={customerId}
              onChange={(e) => setCustomerId(e.currentTarget.value)}
              required
            />
            <NumberInput
              label="Cantidad"
              min={1}
              value={quantity}
              onChange={(value) => {
                if (typeof value === 'number') {
                  setQuantity(value);
                  return;
                }
                if (value === '') {
                  setQuantity('');
                  return;
                }
                const n = Number(value);
                setQuantity(Number.isFinite(n) ? n : '');
              }}
              required
            />
            <Group justify="flex-end">
              <Button onClick={() => createOrder.mutate()} disabled={!canCreate} loading={createOrder.isPending}>
                Crear
              </Button>
            </Group>
            <Text size="sm" c="dimmed">
              La creación devuelve la orden en estado inicial; el procesamiento final ocurre asíncronamente.
            </Text>
          </Stack>
        </Card>

        <Card withBorder>
          <Title order={4} mb="sm">
            Consultar orden
          </Title>
          <Stack>
            <TextInput
              label="Order ID"
              placeholder="UUID"
              value={lookupId}
              onChange={(e) => setLookupId(e.currentTarget.value)}
            />
            <Group justify="flex-end">
              <Button
                variant="light"
                onClick={() => getOrder.mutate(lookupId)}
                disabled={!lookupId.trim()}
                loading={getOrder.isPending}
              >
                Consultar
              </Button>
            </Group>
            {getOrder.error ? <Alert color="red">{errMsg(getOrder.error)}</Alert> : null}
          </Stack>
        </Card>
      </SimpleGrid>

      <Divider my="md" />

      <Card withBorder>
        <Group justify="space-between" mb="sm">
          <Title order={4}>Detalle</Title>
          {lastOrder ? (
            <Group gap="xs">
              <Badge variant="light">{lastOrder.state}</Badge>
              <Button variant="light" onClick={() => getOrder.mutate(lastOrder.id)} loading={getOrder.isPending}>
                Refrescar
              </Button>
            </Group>
          ) : null}
        </Group>

        {!lastOrder ? (
          <Text c="dimmed">Crea o consulta una orden para ver el detalle.</Text>
        ) : (
          <Stack gap="sm">
            <SimpleGrid cols={{ base: 1, md: 2 }}>
              <Text>
                <b>ID:</b> {lastOrder.id}
              </Text>
              <Text>
                <b>Event ID:</b> {lastOrder.eventId}
              </Text>
              <Text>
                <b>Customer:</b> {lastOrder.customerId}
              </Text>
              <Text>
                <b>Cantidad:</b> {lastOrder.quantity}
              </Text>
              <Text>
                <b>Creada:</b> {fmtDate(lastOrder.createdAt)}
              </Text>
              <Text>
                <b>Expira:</b> {fmtDate(lastOrder.expiresAt)}
              </Text>
            </SimpleGrid>

            <Title order={5}>Audit trail</Title>
            <Table striped highlightOnHover>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>De</Table.Th>
                  <Table.Th>A</Table.Th>
                  <Table.Th>Fecha</Table.Th>
                  <Table.Th>Razón</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {lastOrder.auditTrail.map((a, idx) => (
                  <Table.Tr key={idx}>
                    <Table.Td>{a.fromState ?? '-'}</Table.Td>
                    <Table.Td>{a.toState}</Table.Td>
                    <Table.Td>{fmtDate(a.changedAt)}</Table.Td>
                    <Table.Td>{a.reason}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Stack>
        )}
      </Card>
    </Container>
  );
}

