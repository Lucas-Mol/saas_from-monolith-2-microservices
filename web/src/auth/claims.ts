const TENANT_ID = /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/;

export interface Identity {
  email: string | null;
  name: string | null;
  tenantId: string | null;
  roles: string[];
}

export function tenantOf(claim: unknown): string | null {
  const values = typeof claim === 'string' ? [claim] : Array.isArray(claim) ? claim : [];
  if (values.length !== 1 || typeof values[0] !== 'string' || !TENANT_ID.test(values[0])) {
    return null;
  }
  return values[0];
}

export function identityFromAccessToken(token: string): Identity {
  const claims = decodePayload(token);
  const realmAccess = claims.realm_access as { roles?: unknown } | undefined;
  const roles = Array.isArray(realmAccess?.roles)
    ? realmAccess.roles.filter((role): role is string => typeof role === 'string')
    : [];
  return {
    email: typeof claims.email === 'string' ? claims.email : null,
    name: typeof claims.name === 'string' ? claims.name : null,
    tenantId: tenantOf(claims.tenant_id),
    roles,
  };
}

export function backofficeUrl(tenantId: string, gatewayUrl = import.meta.env.VITE_GATEWAY_URL ?? 'http://localhost:8080'): string {
  if (!TENANT_ID.test(tenantId)) {
    throw new Error('Invalid tenant id');
  }
  const url = new URL(gatewayUrl);
  url.hostname = `${tenantId}.${url.hostname}`;
  url.pathname = '/backoffice';
  return url.toString();
}

function decodePayload(token: string): Record<string, unknown> {
  const payload = token.split('.')[1];
  if (!payload) {
    return {};
  }
  const base64 = payload.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(payload.length / 4) * 4, '=');
  try {
    const bytes = Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
    const parsed: unknown = JSON.parse(new TextDecoder().decode(bytes));
    return typeof parsed === 'object' && parsed !== null ? (parsed as Record<string, unknown>) : {};
  } catch {
    return {};
  }
}
