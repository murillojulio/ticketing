export type UUID = string;

export type AuthResponse = {
  userId: UUID;
  email: string;
  roles: string[];
  token: string;
  tokenType: string;
};

export type UserResponse = {
  id: UUID;
  email: string;
  roles: string[];
};

export type RoleAssignRequest = {
  roles: string[];
};

export type TicketState =
  | 'AVAILABLE'
  | 'RESERVED'
  | 'PENDING_CONFIRMATION'
  | 'SOLD'
  | 'COMPLIMENTARY';

export type EventResponse = {
  id: UUID;
  name: string;
  date: string; // ISO
  venue: string;
  totalCapacity: number;
  availableTickets: number;
  reservedTickets: number;
  soldTickets: number;
  complimentaryTickets: number;
};

export type EventAvailabilityResponse = {
  eventId: UUID;
  totalCapacity: number;
  availableTickets: number;
  reservedTickets: number;
  soldTickets: number;
  complimentaryTickets: number;
};

export type AuditEntryResponse = {
  fromState: TicketState | null;
  toState: TicketState;
  changedAt: string; // ISO
  reason: string;
};

export type OrderResponse = {
  id: UUID;
  eventId: UUID;
  customerId: string;
  quantity: number;
  state: TicketState;
  createdAt: string;
  expiresAt: string | null;
  auditTrail: AuditEntryResponse[];
};

