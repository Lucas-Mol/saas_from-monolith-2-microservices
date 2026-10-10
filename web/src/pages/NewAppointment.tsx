import { type FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import type { ApiClient } from '../api/client';
import type { Customer, Professional } from '../api/types';
import { useAsync } from '../components/useAsync';
import { todayIso } from './Agenda';

export function NewAppointment({ api }: { api: ApiClient }) {
  const navigate = useNavigate();
  const options = useAsync(
    () => Promise.all([api.get<Customer[]>('/api/customers'), api.get<Professional[]>('/api/professionals')]),
    [api],
  );
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const date = String(form.get('date'));
    setSaving(true);
    setError(null);
    try {
      await api.post('/api/appointments', {
        customerId: Number(form.get('customerId')),
        professionalId: Number(form.get('professionalId')),
        startTime: `${date}T${String(form.get('start'))}:00`,
        endTime: `${date}T${String(form.get('end'))}:00`,
        notes: String(form.get('notes') ?? '') || null,
      });
      navigate(`/?date=${date}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }

  if (options.loading) {
    return <p>Loading…</p>;
  }
  if (options.error || !options.data) {
    return <p className="error">{options.error}</p>;
  }
  const [customers, professionals] = options.data;

  return (
    <section>
      <h1>New appointment</h1>
      <form onSubmit={submit} className="form">
        <label>
          Customer
          <select name="customerId" required>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.fullName}
              </option>
            ))}
          </select>
        </label>
        <label>
          Professional
          <select name="professionalId" required>
            {professionals.map((p) => (
              <option key={p.id} value={p.id}>
                {p.fullName} ({p.specialty})
              </option>
            ))}
          </select>
        </label>
        <label>
          Day <input type="date" name="date" defaultValue={todayIso()} required />
        </label>
        <label>
          Start <input type="time" name="start" defaultValue="09:00" required />
        </label>
        <label>
          End <input type="time" name="end" defaultValue="10:00" required />
        </label>
        <label>
          Notes <input type="text" name="notes" maxLength={2000} />
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={saving}>
          Schedule
        </button>
      </form>
    </section>
  );
}
