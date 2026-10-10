import { type FormEvent, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';

import type { ApiClient } from '../api/client';
import type { Customer, Professional } from '../api/types';
import { ErrorCallout, PageHeader, SkeletonRows } from '../components/States';
import { useAsync } from '../components/useAsync';
import { formatLongDate, formatMinutes, isValidIsoDate, todayIso, toMinutes } from '../lib/time';

const CLOCK = /^\d{2}:\d{2}$/;

export function NewAppointment({ api }: { api: ApiClient }) {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const options = useAsync(
    () => Promise.all([api.get<Customer[]>('/api/customers'), api.get<Professional[]>('/api/professionals')]),
    [api],
  );

  const initialStart = CLOCK.test(params.get('start') ?? '') ? (params.get('start') as string) : '09:00';
  const [date, setDate] = useState(isValidIsoDate(params.get('date')) ? (params.get('date') as string) : todayIso());
  const [start, setStart] = useState(initialStart);
  const [end, setEnd] = useState(formatMinutes(Math.min(toMinutes(initialStart) + 60, 23 * 60 + 45)));
  const [professionalId, setProfessionalId] = useState(params.get('professionalId') ?? '');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSaving(true);
    setError(null);
    try {
      await api.post('/api/appointments', {
        customerId: Number(form.get('customerId')),
        professionalId: Number(professionalId),
        startTime: `${date}T${start}:00`,
        endTime: `${date}T${end}:00`,
        notes: String(form.get('notes') ?? '').trim() || null,
      });
      navigate(`/?date=${date}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }

  if (options.loading) {
    return (
      <section className="page">
        <PageHeader title="Book appointment" />
        <SkeletonRows rows={5} />
      </section>
    );
  }
  if (options.error || !options.data) {
    return (
      <section className="page">
        <PageHeader title="Book appointment" />
        <ErrorCallout title="Customers and professionals could not be loaded.">{options.error}</ErrorCallout>
      </section>
    );
  }

  const [customers, professionals] = options.data;
  const selected = professionals.find((p) => String(p.id) === professionalId) ?? professionals[0];
  const duration = toMinutes(end) - toMinutes(start);

  return (
    <section className="page">
      <PageHeader eyebrow="Agenda" title="Book appointment" />
      <form onSubmit={submit} className="booking-form">
        <fieldset>
          <legend>Who</legend>
          <label className="field">
            <span>Customer</span>
            <select name="customerId" required>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.fullName}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Professional</span>
            <select value={selected ? String(selected.id) : ''} onChange={(e) => setProfessionalId(e.target.value)} required>
              {professionals.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.fullName} · {p.specialty}
                </option>
              ))}
            </select>
            {selected && (
              <small className="field-hint">
                Works <span className="mono">{selected.workStart.slice(0, 5)}–{selected.workEnd.slice(0, 5)}</span>
              </small>
            )}
          </label>
        </fieldset>

        <fieldset>
          <legend>When</legend>
          <label className="field">
            <span>Day</span>
            <input type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
          </label>
          <div className="field-row">
            <label className="field">
              <span>Starts</span>
              <input type="time" step={900} value={start} onChange={(e) => setStart(e.target.value)} required />
            </label>
            <label className="field">
              <span>Ends</span>
              <input type="time" step={900} value={end} onChange={(e) => setEnd(e.target.value)} required />
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>Notes</legend>
          <label className="field">
            <span className="visually-hidden">Notes</span>
            <textarea name="notes" rows={3} maxLength={2000} placeholder="Optional, e.g. first visit" />
          </label>
        </fieldset>

        <aside className="booking-preview" aria-live="polite">
          <span className="eyebrow">You are booking</span>
          <strong>{isValidIsoDate(date) ? formatLongDate(date) : 'Pick a day'}</strong>
          <span className="mono">
            {start}–{end}
            {duration > 0 ? ` · ${duration} min` : ''}
          </span>
          {selected && <span>with {selected.fullName}</span>}
        </aside>

        {error && <ErrorCallout title="The appointment was not booked.">{error}</ErrorCallout>}

        <div className="form-actions">
          <Link className="button-quiet" to={`/?date=${date}`}>
            Back to agenda
          </Link>
          <button type="submit" className="button-primary" disabled={saving}>
            {saving ? 'Booking…' : 'Book appointment'}
          </button>
        </div>
      </form>
    </section>
  );
}
