import type { ApiClient } from '../api/client';
import type { Professional } from '../api/types';
import { ErrorCallout, PageHeader, SkeletonRows } from '../components/States';
import { useAsync } from '../components/useAsync';
import { initials } from '../lib/format';

export function Professionals({ api, clinicName }: { api: ApiClient; clinicName: string }) {
  const { data, error, loading } = useAsync(() => api.get<Professional[]>('/api/professionals'), [api]);
  return (
    <section className="page">
      <PageHeader eyebrow={clinicName} title="Our professionals" />
      {error && <ErrorCallout title="Professionals could not be loaded.">{error}</ErrorCallout>}
      {loading && <SkeletonRows rows={3} />}
      <ul className="people">
        {data?.map((p) => (
          <li key={p.id} className="person">
            <span className="avatar" aria-hidden="true">
              {initials(p.fullName)}
            </span>
            <strong>{p.fullName}</strong>
            <span className="person-detail">{p.specialty}</span>
            <span className="person-detail mono">
              {p.workStart.slice(0, 5)}–{p.workEnd.slice(0, 5)}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}
