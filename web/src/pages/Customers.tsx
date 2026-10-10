import { useState } from 'react';

import type { ApiClient } from '../api/client';
import type { Customer } from '../api/types';
import { Icon } from '../components/Icon';
import { ErrorCallout, PageHeader, SkeletonRows } from '../components/States';
import { useAsync } from '../components/useAsync';
import { initials } from '../lib/format';

export function Customers({ api }: { api: ApiClient }) {
  const { data, error, loading } = useAsync(() => api.get<Customer[]>('/api/customers'), [api]);
  const [query, setQuery] = useState('');
  const needle = query.trim().toLowerCase();
  const visible = (data ?? []).filter((c) =>
    [c.fullName, c.email ?? '', c.phone ?? ''].some((value) => value.toLowerCase().includes(needle)),
  );

  return (
    <section className="page">
      <PageHeader eyebrow={data ? `${data.length} on file` : undefined} title="Customers">
        <label className="search">
          <Icon name="search" />
          <span className="visually-hidden">Search customers</span>
          <input type="search" placeholder="Search name, e-mail, phone" value={query} onChange={(e) => setQuery(e.target.value)} />
        </label>
      </PageHeader>
      {error && <ErrorCallout title="Customers could not be loaded.">{error}</ErrorCallout>}
      {loading && <SkeletonRows />}
      {data && visible.length === 0 && <p className="empty">No customer matches “{query}”.</p>}
      {visible.length > 0 && (
        <ul className="people">
          {visible.map((c) => (
            <li key={c.id} className="person">
              <span className="avatar" aria-hidden="true">
                {initials(c.fullName)}
              </span>
              <strong>{c.fullName}</strong>
              <span className="person-detail">{c.email ?? 'No e-mail'}</span>
              <span className="person-detail mono">{c.phone ?? '—'}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
