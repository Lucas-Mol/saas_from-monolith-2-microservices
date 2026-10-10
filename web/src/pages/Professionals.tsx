import type { ApiClient } from '../api/client';
import type { Professional } from '../api/types';
import { useAsync } from '../components/useAsync';

export function Professionals({ api }: { api: ApiClient }) {
  const { data, error, loading } = useAsync(() => api.get<Professional[]>('/api/professionals'), [api]);
  return (
    <section>
      <h1>Our professionals</h1>
      {loading && <p>Loading…</p>}
      {error && <p className="error">{error}</p>}
      <ul>
        {data?.map((p) => (
          <li key={p.id}>
            {p.fullName} — {p.specialty}
          </li>
        ))}
      </ul>
    </section>
  );
}
