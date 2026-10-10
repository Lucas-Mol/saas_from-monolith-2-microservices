import { useSearchParams } from 'react-router-dom';

import type { ApiClient } from '../api/client';
import type { Appointment, Customer, Professional } from '../api/types';
import { useAsync } from '../components/useAsync';

export function todayIso(): string {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
}

export function Agenda({ api }: { api: ApiClient }) {
  const [params, setParams] = useSearchParams();
  const date = params.get('date') ?? todayIso();
  const { data, error, loading } = useAsync(
    () =>
      Promise.all([
        api.get<Appointment[]>(`/api/appointments?from=${date}&to=${date}`),
        api.get<Customer[]>('/api/customers'),
        api.get<Professional[]>('/api/professionals'),
      ]),
    [api, date],
  );

  const [appointments, customers, professionals] = data ?? [[], [], []];
  const customerName = (id: number) => customers.find((c) => c.id === id)?.fullName ?? `#${id}`;
  const professionalName = (id: number) => professionals.find((p) => p.id === id)?.fullName ?? `#${id}`;

  return (
    <section>
      <h1>Agenda</h1>
      <label>
        Day <input type="date" value={date} onChange={(e) => e.target.value && setParams({ date: e.target.value })} />
      </label>
      {loading && <p>Loading…</p>}
      {error && <p className="error">{error}</p>}
      {!loading && !error && appointments.length === 0 && <p>No appointments on this day.</p>}
      {appointments.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Time</th>
              <th>Customer</th>
              <th>Professional</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {appointments.map((a) => (
              <tr key={a.id}>
                <td>
                  {a.startTime.slice(11, 16)}–{a.endTime.slice(11, 16)}
                </td>
                <td>{customerName(a.customerId)}</td>
                <td>{professionalName(a.professionalId)}</td>
                <td>{a.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
