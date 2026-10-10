import { describe, expect, it } from 'vitest';

import { backofficeUrl, identityFromAccessToken, tenantOf } from './claims';

function token(payload: object): string {
  const json = new TextEncoder().encode(JSON.stringify(payload));
  const base64url = btoa(String.fromCharCode(...json)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `header.${base64url}.signature`;
}

describe('tenantOf', () => {
  it('reads the Keycloak organization list and plain strings', () => {
    expect(tenantOf(['acme-clinic'])).toBe('acme-clinic');
    expect(tenantOf('bella-salon')).toBe('bella-salon');
  });

  it('rejects missing, ambiguous and malformed tenants', () => {
    expect(tenantOf(undefined)).toBeNull();
    expect(tenantOf([])).toBeNull();
    expect(tenantOf(['acme-clinic', 'bella-salon'])).toBeNull();
    expect(tenantOf(['Acme_Clinic'])).toBeNull();
    expect(tenantOf([42])).toBeNull();
  });
});

describe('identityFromAccessToken', () => {
  it('extracts tenant, realm roles and profile, including non-ASCII names', () => {
    const identity = identityFromAccessToken(
      token({ email: 'staff@acme.test', name: 'José Ação', tenant_id: ['acme-clinic'], realm_access: { roles: ['STAFF'] } }),
    );

    expect(identity).toEqual({ email: 'staff@acme.test', name: 'José Ação', tenantId: 'acme-clinic', roles: ['STAFF'] });
  });

  it('returns an empty identity for unreadable tokens', () => {
    expect(identityFromAccessToken('not-a-jwt')).toEqual({ email: null, name: null, tenantId: null, roles: [] });
    expect(identityFromAccessToken('a.!!!.c').roles).toEqual([]);
  });
});

describe('backofficeUrl', () => {
  it('points at the tenant subdomain of the gateway', () => {
    expect(backofficeUrl('acme-clinic', 'http://localhost:8080')).toBe('http://acme-clinic.localhost:8080/backoffice');
  });

  it('refuses tenant ids that could change the host', () => {
    expect(() => backofficeUrl('evil.com/x', 'http://localhost:8080')).toThrow();
  });
});
