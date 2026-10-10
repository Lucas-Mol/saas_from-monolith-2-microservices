import { describe, expect, it } from 'vitest';

import { safeReturnTo } from './returnTo';

describe('safeReturnTo', () => {
  it('restores the page the user was on', () => {
    expect(safeReturnTo({ returnTo: '/customers' })).toBe('/customers');
    expect(safeReturnTo({ returnTo: '/?date=2026-10-12' })).toBe('/?date=2026-10-12');
  });

  it('falls back to the home page for anything that is not a local path', () => {
    expect(safeReturnTo(undefined)).toBe('/');
    expect(safeReturnTo({})).toBe('/');
    expect(safeReturnTo({ returnTo: 'https://evil.test/' })).toBe('/');
    expect(safeReturnTo({ returnTo: '//evil.test/' })).toBe('/');
    expect(safeReturnTo({ returnTo: '/\\evil.test' })).toBe('/');
  });
});
