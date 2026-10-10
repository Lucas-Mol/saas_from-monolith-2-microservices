import type { ApiClient } from '../api/client';
import type { Customer } from '../api/types';
import { useAsync } from '../components/useAsync';

export function Customers({ api }: { api: ApiClient }) {
  const { data, error, loading } = useAsync(() => api.get<Customer[]>('/api/customers'), [api]);
  return (
    <section>
      <h1>Customers</h1>
      {loading && <p>Loading…</p>}
      {error && <p className="error">{error}</p>}
      {data && (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>E-mail</th>
              <th>Phone</th>
            </tr>
          </thead>
          <tbody>
            {data.map((c) => (
              <tr key={c.id}>
                <td>{c.fullName}</td>
                <td>{c.email ?? '—'}</td>
                <td>{c.phone ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
