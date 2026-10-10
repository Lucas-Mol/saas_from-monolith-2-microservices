import { act, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { routingStore } from '../api/routing';
import { DebugFooter } from './DebugFooter';

describe('DebugFooter', () => {
  it('shows the tenant and follows the last X-Routed-To value', () => {
    render(<DebugFooter identity={{ email: 'staff@acme.test', name: null, tenantId: 'acme-clinic', roles: ['STAFF'] }} />);

    expect(screen.getByText('acme-clinic')).toBeTruthy();
    act(() => routingStore.set('scheduling-service'));
    expect(screen.getByTestId('routed-to').textContent).toBe('scheduling-service');
  });
});
