import { useEffect, useState } from 'react';

import type { Appointment, Customer, Professional } from '../api/types';
import { formatMinutes, todayIso, toMinutes } from '../lib/time';

const SLOT = 15;
const BOOKABLE_STEP = 30;

interface DayBookProps {
  date: string;
  appointments: Appointment[];
  customers: Customer[];
  professionals: Professional[];
  onBook: (professionalId: number, start: string) => void;
}

export function DayBook({ date, appointments, customers, professionals, onBook }: DayBookProps) {
  const now = useNow();
  const { start, end } = visibleRange(professionals, appointments);
  const rows = (end - start) / SLOT;
  const rowOf = (minutes: number) => 2 + Math.floor((minutes - start) / SLOT);
  const nowMinutes = now.getHours() * 60 + now.getMinutes();
  const showNow = date === todayIso(now) && nowMinutes >= start && nowMinutes < end;
  const customerName = (id: number) => customers.find((c) => c.id === id)?.fullName ?? `Customer #${id}`;

  return (
    <div className="daybook-scroll">
      <div
        className="daybook"
        style={{
          gridTemplateColumns: `4rem repeat(${professionals.length}, minmax(12rem, 1fr))`,
          gridTemplateRows: `auto repeat(${rows}, var(--slot-height))`,
        }}
      >
        <div className="daybook-corner" />
        {professionals.map((p, column) => (
          <div key={p.id} className="daybook-head" style={{ gridColumn: column + 2 }}>
            <strong>{p.fullName}</strong>
            <span>
              {p.specialty} · <span className="mono">{p.workStart.slice(0, 5)}–{p.workEnd.slice(0, 5)}</span>
            </span>
          </div>
        ))}

        {Array.from({ length: (end - start) / 60 }, (_, hour) => (
          <div key={hour} className="daybook-hour" style={{ gridRow: `${2 + (hour * 60) / SLOT} / span ${60 / SLOT}` }}>
            <span className="mono">{formatMinutes(start + hour * 60)}</span>
          </div>
        ))}

        {professionals.map((p, column) => {
          const shiftStart = toMinutes(p.workStart);
          const shiftEnd = toMinutes(p.workEnd);
          return Array.from({ length: (end - start) / BOOKABLE_STEP }, (_, index) => {
            const slotStart = start + index * BOOKABLE_STEP;
            const placement = { gridColumn: column + 2, gridRow: `${rowOf(slotStart)} / span ${BOOKABLE_STEP / SLOT}` };
            const onShift = slotStart >= shiftStart && slotStart + BOOKABLE_STEP <= shiftEnd;
            const key = `${p.id}-${slotStart}`;
            return onShift ? (
              <button
                key={key}
                type="button"
                className={slotStart % 60 === 0 ? 'slot slot-hour' : 'slot'}
                style={placement}
                aria-label={`Book ${p.fullName} at ${formatMinutes(slotStart)}`}
                onClick={() => onBook(p.id, formatMinutes(slotStart))}
              >
                <span className="slot-hint mono">+ {formatMinutes(slotStart)}</span>
              </button>
            ) : (
              <div key={key} className={slotStart % 60 === 0 ? 'slot-off slot-hour' : 'slot-off'} style={placement} />
            );
          });
        })}

        {appointments.map((a) => {
          const column = professionals.findIndex((p) => p.id === a.professionalId);
          if (column < 0) {
            return null;
          }
          const from = toMinutes(a.startTime);
          const to = toMinutes(a.endTime);
          return (
            <article
              key={a.id}
              className={`booking booking-${a.status.toLowerCase()}`}
              style={{ gridColumn: column + 2, gridRow: `${rowOf(from)} / span ${Math.max(1, (to - from) / SLOT)}` }}
              title={a.notes ?? undefined}
            >
              <span className="booking-time mono">
                {a.startTime.slice(11, 16)}–{a.endTime.slice(11, 16)}
              </span>
              <strong className="booking-name">{customerName(a.customerId)}</strong>
              <span className="booking-status">{a.status.toLowerCase().replace('_', ' ')}</span>
            </article>
          );
        })}

        {showNow && (
          <div
            className="now-line"
            style={{
              gridRow: rowOf(nowMinutes),
              transform: `translateY(calc(var(--slot-height) * ${((nowMinutes - start) % SLOT) / SLOT}))`,
            }}
            aria-label={`Now, ${formatMinutes(nowMinutes)}`}
          >
            <span className="mono">{formatMinutes(nowMinutes)}</span>
          </div>
        )}
      </div>
    </div>
  );
}


function visibleRange(professionals: Professional[], appointments: Appointment[]) {
  const starts = [...professionals.map((p) => toMinutes(p.workStart)), ...appointments.map((a) => toMinutes(a.startTime))];
  const ends = [...professionals.map((p) => toMinutes(p.workEnd)), ...appointments.map((a) => toMinutes(a.endTime))];
  const start = starts.length ? Math.floor(Math.min(...starts) / 60) * 60 : 8 * 60;
  const end = ends.length ? Math.ceil(Math.max(...ends) / 60) * 60 : 18 * 60;
  return { start, end: Math.max(end, start + 60) };
}

function useNow(): Date {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 60_000);
    return () => window.clearInterval(timer);
  }, []);
  return now;
}
