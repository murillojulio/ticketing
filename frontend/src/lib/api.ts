import type {
  AuthResponse,
  EventAvailabilityResponse,
  EventResponse,
  OrderResponse,
  UUID
} from '../app/types';
import { httpJson } from './http';

export type LoginRequest = { email: string; password: string };
export type RegisterRequest = { email: string; password: string };

export type CreateEventRequest = {
  name: string;
  date: string; // ISO instant
  venue: string;
  totalCapacity: number;
};

export type CreateOrderRequest = {
  eventId: UUID;
  customerId: string;
  quantity: number;
};

export const api = {
  register: (body: RegisterRequest) =>
    httpJson<AuthResponse>('/api/auth/register', { method: 'POST', json: body, auth: false }),

  login: (body: LoginRequest) =>
    httpJson<AuthResponse>('/api/auth/login', { method: 'POST', json: body, auth: false }),

  listEvents: () => httpJson<EventResponse[]>('/api/events', { method: 'GET' }),

  createEvent: (body: CreateEventRequest) =>
    httpJson<EventResponse>('/api/events', { method: 'POST', json: body }),

  getAvailability: (eventId: UUID) =>
    httpJson<EventAvailabilityResponse>(`/api/events/${eventId}/availability`, { method: 'GET' }),

  createOrder: (body: CreateOrderRequest) =>
    httpJson<OrderResponse>('/api/orders', { method: 'POST', json: body }),

  getOrder: (orderId: UUID) =>
    httpJson<OrderResponse>(`/api/orders/${orderId}`, { method: 'GET' })
};

