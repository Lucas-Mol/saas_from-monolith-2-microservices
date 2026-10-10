import type { ApiClient } from '../api/client';
import type { Customer, Invoice } from '../api/types';
import { useAsync } from '../components/useAsync';

export function Invoices({ api }: { api: ApiClient }) {
  const { data, error, loading } = useAsync(
    () => Promise.all([api.get<Invoice[]>('/api/invoices'), api.get<Customer[]>('/api/customers')]),
    [api],
  );
  const [invoices, customers] = data ?? [[], []];
  return (
    <section>
      <h1>Invoices</h1>
      {loading && <p>Loading…</p>}
      {error && <p className="error">{error}</p>}
      {!loading && !error && (
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Customer</th>
              <th>Total</th>
              <th>Status</th>
              <th>Issued</th>
            </tr>
          </thead>
          <tbody>
            {invoices.map((i) => (
              <tr key={i.id}>
                <td>{i.id}</td>
                <td>{customers.find((c) => c.id === i.customerId)?.fullName ?? `#${i.customerId}`}</td>
                <td>{i.total.toFixed(2)}</td>
                <td>{i.status}</td>
                <td>{i.issuedAt.slice(0, 10)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
