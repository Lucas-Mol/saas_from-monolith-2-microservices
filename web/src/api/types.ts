export type AppointmentStatus = 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW';

export interface Appointment {
  id: number;
  customerId: number;
  professionalId: number;
  startTime: string;
  endTime: string;
  status: AppointmentStatus;
  notes: string | null;
}

export interface Customer {
  id: number;
  fullName: string;
  email: string | null;
  phone: string | null;
}

export interface Professional {
  id: number;
  fullName: string;
  specialty: string;
  workStart: string;
  workEnd: string;
}

export interface Invoice {
  id: number;
  customerId: number;
  status: 'OPEN' | 'PAID' | 'VOID';
  total: number;
  issuedAt: string;
  paidAt: string | null;
}
