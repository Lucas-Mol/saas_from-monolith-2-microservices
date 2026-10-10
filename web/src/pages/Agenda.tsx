import { Link, useNavigate, useSearchParams } from 'react-router-dom';

import type { ApiClient } from '../api/client';
import type { Appointment, Customer, Professional } from '../api/types';
import { Icon } from '../components/Icon';
import { ErrorCallout, PageHeader, SkeletonRows } from '../components/States';
import { useAsync } from '../components/useAsync';
import { addDays, formatLongDate, isValidIsoDate, todayIso } from '../lib/time';
import { DayBook } from './DayBook';

export function Agenda({ api, clinicName }: { api: ApiClient; clinicName: string }) {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const requested = params.get('date');
  const date = isValidIsoDate(requested) ? requested : todayIso();
  const { data, error, loading } = useAsync(
    () =>
      Promise.all([
        api.get<Appointment[]>(`/api/appointments?from=${date}&to=${date}`),
        api.get<Customer[]>('/api/customers'),
        api.get<Professional[]>('/api/professionals'),
      ]),
    [api, date],
  );

  const goTo = (day: string) => setParams({ date: day });
  const [appointments, customers, professionals] = data ?? [[], [], []];
  const active = appointments.filter((a) => a.status !== 'CANCELLED');
  const confirmed = appointments.filter((a) => a.status === 'CONFIRMED').length;
  const cancelled = appointments.length - active.length;

  return (
    <section className="page">
      <PageHeader eyebrow={clinicName} title={formatLongDate(date)}>
        <div className="day-nav" role="group" aria-label="Change day">
          <button type="button" className="button-icon" onClick={() => goTo(addDays(date, -1))} aria-label="Previous day">
            <Icon name="chevronLeft" />
          </button>
          <button type="button" className="button-quiet" onClick={() => goTo(todayIso())} disabled={date === todayIso()}>
            Today
          </button>
          <button type="button" className="button-icon" onClick={() => goTo(addDays(date, 1))} aria-label="Next day">
            <Icon name="chevronRight" />
          </button>
          <input
            type="date"
            className="date-input"
            aria-label="Pick a day"
            value={date}
            onChange={(e) => isValidIsoDate(e.target.value) && goTo(e.target.value)}
          />
        </div>
        <Link className="button-primary" to={`/appointments/new?date=${date}`}>
          <Icon name="plus" /> Book appointment
        </Link>
      </PageHeader>

      {error && <ErrorCallout title="The agenda could not be loaded.">{error}</ErrorCallout>}
      {loading && <SkeletonRows rows={8} />}
      {data && (
        <>
          <p className="summary">
            <span>
              <strong className="mono">{active.length}</strong> {active.length === 1 ? 'appointment' : 'appointments'}
            </span>
            <span>
              <strong className="mono">{confirmed}</strong> confirmed
            </span>
            {cancelled > 0 && (
              <span>
                <strong className="mono">{cancelled}</strong> cancelled
              </span>
            )}
            {appointments.length === 0 && <span className="summary-hint">Pick a free slot below to book the first one.</span>}
          </p>
          <DayBook
            date={date}
            appointments={appointments}
            customers={customers}
            professionals={professionals}
            onBook={(professionalId, start) =>
              navigate(`/appointments/new?date=${date}&professionalId=${professionalId}&start=${start}`)
            }
          />
        </>
      )}
    </section>
  );
}
