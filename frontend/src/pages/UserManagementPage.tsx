import { ActionIcon, Badge, Button, Group, LoadingOverlay, Popover, Stack, Table, Text, Title, Modal, TextInput, PasswordInput, MultiSelect } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, Shield, Trash, UserPlus, Pencil } from 'lucide-react';
import React, { useState } from 'react';

import { api } from '../lib/api';
import { showNotification } from '@mantine/notifications';

export function UserManagementPage() {
    const queryClient = useQueryClient();

    const { data: users, isLoading, error } = useQuery({
        queryKey: ['users'],
        queryFn: api.listUsers
    });

    const [modalOpened, setModalOpened] = useState(false);
    const [editingUser, setEditingUser] = useState<any>(null);
    const [formData, setFormData] = useState({ email: '', password: '', roles: ['USER'] });

    const createMutation = useMutation({
        mutationFn: api.createUser,
        onSuccess: () => {
            showNotification({ title: 'Éxito', message: 'Usuario creado', color: 'green' });
            setModalOpened(false);
            queryClient.invalidateQueries({ queryKey: ['users'] });
        },
        onError: (err: any) => {
            showNotification({ title: 'Error', message: err.message, color: 'red' });
        }
    });

    const updateMutation = useMutation({
        mutationFn: ({ userId, body }: { userId: string, body: any }) => api.updateUser(userId, body),
        onSuccess: () => {
            showNotification({ title: 'Éxito', message: 'Usuario actualizado', color: 'green' });
            setModalOpened(false);
            queryClient.invalidateQueries({ queryKey: ['users'] });
        },
        onError: (err: any) => {
            showNotification({ title: 'Error', message: err.message, color: 'red' });
        }
    });

    const deleteMutation = useMutation({
        mutationFn: api.deleteUser,
        onSuccess: () => {
            showNotification({ title: 'Éxito', message: 'Usuario eliminado correctamente', color: 'green' });
            queryClient.invalidateQueries({ queryKey: ['users'] });
        },
        onError: (err: any) => {
            showNotification({ title: 'Error', message: err.message || 'No se pudo eliminar el usuario', color: 'red' });
        }
    });

    const assignRoleMutation = useMutation({
        mutationFn: ({ userId, role }: { userId: string, role: string }) => api.assignRoles(userId, { roles: [role] }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['users'] });
        },
        onError: (err: any) => {
            showNotification({ title: 'Error', message: err.message || 'Error al asignar rol', color: 'red' });
        }
    });

    const removeRoleMutation = useMutation({
        mutationFn: ({ userId, role }: { userId: string, role: string }) => api.removeRoles(userId, { roles: [role] }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['users'] });
        },
        onError: (err: any) => {
            showNotification({ title: 'Error', message: err.message || 'Error al remover rol', color: 'red' });
        }
    });

    const handleToggleAdmin = (userId: string, currentRoles: string[]) => {
        if (currentRoles.includes('ADMIN')) {
            removeRoleMutation.mutate({ userId, role: 'ADMIN' });
        } else {
            assignRoleMutation.mutate({ userId, role: 'ADMIN' });
        }
    };

    const openCreateModal = () => {
        setEditingUser(null);
        setFormData({ email: '', password: '', roles: ['USER'] });
        setModalOpened(true);
    };

    const openEditModal = (user: any) => {
        setEditingUser(user);
        setFormData({ email: user.email, password: '', roles: user.roles });
        setModalOpened(true);
    };

    const handleSubmit = (event: React.FormEvent) => {
        event.preventDefault();
        if (editingUser) {
            updateMutation.mutate({ userId: editingUser.id, body: { email: formData.email, roles: formData.roles } });
        } else {
            createMutation.mutate(formData);
        }
    };

    if (error) {
        return (
            <Stack align="center" mt="xl">
                <AlertCircle size={48} color="red" />
                <Text>Error al cargar usuarios. Asegúrate de ser administrador.</Text>
            </Stack>
        );
    }

    const rows = users?.map((user) => (
        <Table.Tr key={user.id}>
            <Table.Td>{user.email}</Table.Td>
            <Table.Td>
                <Group gap="xs">
                    {user.roles.map(r => (
                        <Badge key={r} color={r === 'ADMIN' ? 'red' : 'blue'} variant="light">
                            {r}
                        </Badge>
                    ))}
                </Group>
            </Table.Td>
            <Table.Td>
                <Group gap="xs">
                    <ActionIcon color="blue" variant="subtle" onClick={() => openEditModal(user)}>
                        <Pencil size={16} />
                    </ActionIcon>

                    <Button
                        size="xs"
                        variant={user.roles.includes('ADMIN') ? 'outline' : 'filled'}
                        color={user.roles.includes('ADMIN') ? 'gray' : 'grape'}
                        leftSection={<Shield size={14} />}
                        onClick={() => handleToggleAdmin(user.id, user.roles)}
                        loading={assignRoleMutation.isPending || removeRoleMutation.isPending}
                    >
                        {user.roles.includes('ADMIN') ? 'Quitar Admin' : 'Hacer Admin'}
                    </Button>

                    <Popover width={200} position="bottom" withArrow shadow="md">
                        <Popover.Target>
                            <ActionIcon color="red" variant="subtle">
                                <Trash size={16} />
                            </ActionIcon>
                        </Popover.Target>
                        <Popover.Dropdown>
                            <Text size="sm" mb="xs">¿Seguro que deseas eliminar el usuario <b>{user.email}</b>?</Text>
                            <Button color="red" fullWidth size="xs" onClick={() => deleteMutation.mutate(user.id)}>
                                Sí, eliminar
                            </Button>
                        </Popover.Dropdown>
                    </Popover>
                </Group>
            </Table.Td>
        </Table.Tr>
    ));

    return (
        <div style={{ position: 'relative' }}>
            <LoadingOverlay visible={isLoading || deleteMutation.isPending || createMutation.isPending || updateMutation.isPending} />
            <Group justify="space-between" mb="md">
                <Title order={2}>Gestión de Usuarios</Title>
                <Button leftSection={<UserPlus size={18} />} onClick={openCreateModal}>
                    Nuevo Usuario
                </Button>
            </Group>

            <Modal opened={modalOpened} onClose={() => setModalOpened(false)} title={editingUser ? 'Editar Usuario' : 'Nuevo Usuario'}>
                <form onSubmit={handleSubmit}>
                    <Stack>
                        <TextInput
                            label="Email"
                            required
                            value={formData.email}
                            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                        />
                        {!editingUser && (
                            <PasswordInput
                                label="Contraseña"
                                required
                                value={formData.password}
                                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                            />
                        )}
                        <MultiSelect
                            label="Roles"
                            data={['USER', 'ADMIN']}
                            value={formData.roles}
                            onChange={(val) => setFormData({ ...formData, roles: val })}
                            required
                        />
                        <Button type="submit" loading={createMutation.isPending || updateMutation.isPending}>
                            {editingUser ? 'Actualizar' : 'Crear'}
                        </Button>
                    </Stack>
                </form>
            </Modal>

            <Table striped highlightOnHover withTableBorder>
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Email</Table.Th>
                        <Table.Th>Roles</Table.Th>
                        <Table.Th>Acciones</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>{rows}</Table.Tbody>
            </Table>
        </div>
    );
}
