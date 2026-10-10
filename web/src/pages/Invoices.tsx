import type { ApiClient } from '../api/client';
import type { Customer, Invoice } from '../api/types';
import { ErrorCallout, PageHeader, SkeletonRows } from '../components/States';
import { useAsync } from '../components/useAsync';
import { formatMoney } from '../lib/format';

export function Invoices({ api }: { api: ApiClient }) {
  const { data, error, loading } = useAsync(
    () => Promise.all([api.get<Invoice[]>('/api/invoices'), api.get<Customer[]>('/api/customers')]),
    [api],
  );
  const [invoices, customers] = data ?? [[], []];
  const open = invoices.filter((i) => i.status === 'OPEN');
  const paid = invoices.filter((i) => i.status === 'PAID');
  const sum = (list: Invoice[]) => list.reduce((total, i) => total + i.total, 0);

  return (
    <section className="page">
      <PageHeader eyebrow="Billing" title="Invoices" />
      {error && <ErrorCallout title="Invoices could not be loaded.">{error}</ErrorCallout>}
      {loading && <SkeletonRows />}
      {data && (
        <>
          <dl className="totals">
            <div>
              <dt>Awaiting payment</dt>
              <dd className="mono">{formatMoney(sum(open))}</dd>
              <dd className="totals-count">{open.length} open</dd>
            </div>
            <div>
              <dt>Received</dt>
              <dd className="mono">{formatMoney(sum(paid))}</dd>
              <dd className="totals-count">{paid.length} paid</dd>
            </div>
          </dl>
          {invoices.length === 0 ? (
            <p className="empty">No invoices yet. Invoices are issued from completed appointments in the backoffice.</p>
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th scope="col">Invoice</th>
                    <th scope="col">Customer</th>
                    <th scope="col">Issued</th>
                    <th scope="col">Status</th>
                    <th scope="col" className="numeric">
                      Total
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {invoices.map((i) => (
                    <tr key={i.id}>
                      <td className="mono">#{i.id}</td>
                      <td>{customers.find((c) => c.id === i.customerId)?.fullName ?? `Customer #${i.customerId}`}</td>
                      <td className="mono">{i.issuedAt.slice(0, 10)}</td>
                      <td>
                        <span className={`pill pill-${i.status.toLowerCase()}`}>{i.status.toLowerCase()}</span>
                      </td>
                      <td className="numeric mono">{formatMoney(i.total)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </section>
  );
}
