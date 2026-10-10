import { describe, expect, it } from 'vitest';

import { initials, tenantDisplayName } from './format';

describe('format helpers', () => {
  it('turns tenant ids into display names', () => {
    expect(tenantDisplayName('acme-clinic')).toBe('Acme Clinic');
  });

  it('builds initials, skipping professional titles', () => {
    expect(initials('Alice Martins')).toBe('AM');
    expect(initials('Dr. Daniel Rocha')).toBe('DR');
    expect(initials('Carla')).toBe('C');
  });
});
