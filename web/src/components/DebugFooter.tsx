import { useSyncExternalStore } from 'react';

import { routingStore } from '../api/routing';
import type { Identity } from '../auth/claims';

export function DebugFooter({ identity }: { identity: Identity }) {
  const routedTo = useSyncExternalStore(routingStore.subscribe, routingStore.get);
  return (
    <footer className="debug">
      <span>
        tenant_id: <code>{identity.tenantId ?? '—'}</code>
      </span>
      <span>
        roles: <code>{identity.roles.join(', ') || '—'}</code>
      </span>
      <span>
        user: <code>{identity.email ?? '—'}</code>
      </span>
      <span>
        X-Routed-To: <code data-testid="routed-to">{routedTo ?? '—'}</code>
      </span>
    </footer>
  );
}
