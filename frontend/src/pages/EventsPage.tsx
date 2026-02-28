import {
  ActionIcon,
  Badge,
  Button,
  Card,
  Container,
  Group,
  Modal,
  NumberInput,
  SimpleGrid,
  Stack,
  Table,
  Text,
  TextInput,
  Title
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import React, { useMemo, useState } from 'react';

import type { EventAvailabilityResponse, EventResponse } from '../app/types';
import { api, type CreateEventRequest } from '../lib/api';
import type { HttpError } from '../lib/http';

function fmtDate(iso: string) {
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

function errMsg(err: unknown) {
  const e = err as Partial<HttpError>;
  return e?.message ?? 'Error inesperado';
}

export function EventsPage() {
  const qc = useQueryClient();
  const { data: events, isLoading, error } = useQuery({
    queryKey: ['events'],
    queryFn: api.listEvents
  });

  const [createOpened, setCreateOpened] = useState(false);
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);

  const selectedEvent = useMemo<EventResponse | undefined>(() => {
    if (!events || !selectedEventId) return undefined;
    return events.find((e) => e.id === selectedEventId);
  }, [events, selectedEventId]);

  const availabilityQuery = useQuery<EventAvailabilityResponse>({
    queryKey: ['availability', selectedEventId],
    queryFn: () => api.getAvailability(selectedEventId!),
    enabled: Boolean(selectedEventId)
  });

  const createMutation = useMutation({
    mutationFn: (body: CreateEventRequest) => api.createEvent(body),
    onSuccess: async (created) => {
      notifications.show({ title: 'Evento creado', message: created.name });
      setCreateOpened(false);
      await qc.invalidateQueries({ queryKey: ['events'] });
    },
    onError: (err) => {
      notifications.show({ color: 'red', title: 'No se pudo crear', message: errMsg(err) });
    }
  });

  return (
    <Container size="xl">
      <Group justify="space-between" mb="md">
        <Title order={2}>Eventos</Title>
        <Button onClick={() => setCreateOpened(true)}>Crear evento</Button>
      </Group>

      <Modal opened={createOpened} onClose={() => setCreateOpened(false)} title="Crear evento" centered>
        <CreateEventForm
          loading={createMutation.isPending}
          onSubmit={(body) => createMutation.mutate(body)}
        />
      </Modal>

      {error ? (
        <Card withBorder>
          <Text c="red">Error: {errMsg(error)}</Text>
        </Card>
      ) : null}

      <Card withBorder>
        <Stack gap="md">
          <Text c="dimmed" size="sm">
            {isLoading ? 'Cargando eventos...' : `Eventos: ${events?.length ?? 0}`}
          </Text>

          <Table highlightOnHover striped>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Nombre</Table.Th>
                <Table.Th>Fecha</Table.Th>
                <Table.Th>Venue</Table.Th>
                <Table.Th ta="right">Capacidad</Table.Th>
                <Table.Th>Estado</Table.Th>
                <Table.Th ta="right">Acciones</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {(events ?? []).map((ev) => (
                <Table.Tr key={ev.id}>
                  <Table.Td>{ev.name}</Table.Td>
                  <Table.Td>{fmtDate(ev.date)}</Table.Td>
                  <Table.Td>{ev.venue}</Table.Td>
                  <Table.Td ta="right">{ev.totalCapacity}</Table.Td>
                  <Table.Td>
                    <Group gap="xs">
                      <Badge variant="light">Avail {ev.availableTickets}</Badge>
                      <Badge color="yellow" variant="light">
                        Res {ev.reservedTickets}
                      </Badge>
                      <Badge color="green" variant="light">
                        Sold {ev.soldTickets}
                      </Badge>
                    </Group>
                  </Table.Td>
                  <Table.Td ta="right">
                    <ActionIcon
                      variant={selectedEventId === ev.id ? 'filled' : 'light'}
                      onClick={() => setSelectedEventId(ev.id)}
                      aria-label="Ver disponibilidad"
                    >
                      i
                    </ActionIcon>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Stack>
      </Card>

      <SimpleGrid cols={{ base: 1, md: 2 }} mt="md">
        <Card withBorder>
          <Title order={4} mb="xs">
            Disponibilidad
          </Title>
          {selectedEvent ? (
            <Stack gap="xs">
              <Text fw={600}>{selectedEvent.name}</Text>
              {availabilityQuery.isLoading ? <Text c="dimmed">Cargando...</Text> : null}
              {availabilityQuery.error ? (
                <Text c="red">Error: {errMsg(availabilityQuery.error)}</Text>
              ) : null}
              {availabilityQuery.data ? (
                <Group gap="xs" wrap="wrap">
                  <Badge variant="light">Total {availabilityQuery.data.totalCapacity}</Badge>
                  <Badge variant="light">Avail {availabilityQuery.data.availableTickets}</Badge>
                  <Badge color="yellow" variant="light">
                    Res {availabilityQuery.data.reservedTickets}
                  </Badge>
                  <Badge color="green" variant="light">
                    Sold {availabilityQuery.data.soldTickets}
                  </Badge>
                  <Badge color="grape" variant="light">
                    Comp {availabilityQuery.data.complimentaryTickets}
                  </Badge>
                </Group>
              ) : (
                <Text c="dimmed">Selecciona un evento en la tabla.</Text>
              )}
            </Stack>
          ) : (
            <Text c="dimmed">Selecciona un evento en la tabla para ver su disponibilidad.</Text>
          )}
        </Card>

        <Card withBorder>
          <Title order={4} mb="xs">
            Tips
          </Title>
          <Text size="sm" c="dimmed">
            El backend es reactivo y procesa las órdenes de forma asíncrona. Verás cambios de estado al consultar una orden.
          </Text>
        </Card>
      </SimpleGrid>
    </Container>
  );
}

function CreateEventForm({
  loading,
  onSubmit
}: {
  loading: boolean;
  onSubmit: (body: CreateEventRequest) => void;
}) {
  const [name, setName] = useState('Rock Festival');
  const [venue, setVenue] = useState('National Stadium');
  const [dateIso, setDateIso] = useState('2026-12-01T20:00:00Z');
  const [totalCapacity, setTotalCapacity] = useState<number | ''>(100);

  function submit(e: React.FormEvent) {
    e.preventDefault();
    const capacity = typeof totalCapacity === 'number' ? totalCapacity : Number(totalCapacity);
    onSubmit({ name, venue, date: dateIso, totalCapacity: capacity });
  }

  return (
    <form onSubmit={submit}>
      <Stack>
        <TextInput label="Nombre" value={name} onChange={(e) => setName(e.currentTarget.value)} required />
        <TextInput label="Venue" value={venue} onChange={(e) => setVenue(e.currentTarget.value)} required />
        <TextInput
          label="Fecha (ISO)"
          description="Ej: 2026-12-01T20:00:00Z"
          value={dateIso}
          onChange={(e) => setDateIso(e.currentTarget.value)}
          required
        />
        <NumberInput
          label="Capacidad total"
          min={1}
          value={totalCapacity}
          onChange={(value) => {
            if (typeof value === 'number') {
              setTotalCapacity(value);
              return;
            }
            if (value === '') {
              setTotalCapacity('');
              return;
            }
            const n = Number(value);
            setTotalCapacity(Number.isFinite(n) ? n : '');
          }}
          required
        />
        <Group justify="flex-end">
          <Button type="submit" loading={loading}>
            Crear
          </Button>
        </Group>
      </Stack>
    </form>
  );
}

