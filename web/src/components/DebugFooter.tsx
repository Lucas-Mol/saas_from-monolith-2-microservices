import { useSyncExternalStore } from 'react';

import { routingStore } from '../api/routing';
import type { Identity } from '../auth/claims';
import { Icon } from './Icon';

export function DebugFooter({ identity }: { identity: Identity }) {
  const routedTo = useSyncExternalStore(routingStore.subscribe, routingStore.get);
  return (
    <footer className="routing-strip" aria-label="Request routing">
      <span className="routing-label">Routing</span>
      <code className="routing-tenant">{identity.tenantId ?? 'no tenant'}</code>
      <Icon name="arrow" className="routing-arrow" />
      {/* Re-keyed on every change so the highlight replays when a call lands on a different backend. */}
      <code key={routedTo ?? 'none'} className="routing-target" data-testid="routed-to">
        {routedTo ?? '—'}
      </code>
      <span className="routing-meta">
        {identity.email ?? 'unknown user'} · {identity.roles.join(', ') || 'no roles'}
      </span>
    </footer>
  );
}
