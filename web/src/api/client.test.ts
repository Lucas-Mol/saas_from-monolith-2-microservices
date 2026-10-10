import { describe, expect, it, vi } from 'vitest';

import { ApiError, createApiClient } from './client';

function respond(status: number, body: string, headers: Record<string, string> = {}) {
  return vi.fn<typeof fetch>().mockResolvedValue(new Response(body || null, { status, headers }));
}

describe('createApiClient', () => {
  it('sends the bearer token and reports which backend answered', async () => {
    const fetchImpl = respond(200, '[{"id":1}]', { 'X-Routed-To': 'legacy' });
    const onRoutedTo = vi.fn();
    const api = createApiClient(() => 'token-123', onRoutedTo, fetchImpl);

    await expect(api.get('/api/customers')).resolves.toEqual([{ id: 1 }]);

    const headers = new Headers(fetchImpl.mock.calls[0][1]?.headers);
    expect(headers.get('Authorization')).toBe('Bearer token-123');
    expect(onRoutedTo).toHaveBeenCalledWith('legacy');
  });

  it('posts JSON bodies', async () => {
    const fetchImpl = respond(201, '{"id":7}');
    const api = createApiClient(() => 'token', () => undefined, fetchImpl);

    await api.post('/api/appointments', { customerId: 1 });

    const init = fetchImpl.mock.calls[0][1];
    expect(init?.method).toBe('POST');
    expect(init?.body).toBe('{"customerId":1}');
    expect(new Headers(init?.headers).get('Content-Type')).toBe('application/json');
  });

  it('surfaces legacy error messages and problem details', async () => {
    const legacy = createApiClient(() => 'token', () => undefined,
      respond(422, '{"status":422,"message":"Appointment must start in the future"}'));
    const gateway = createApiClient(() => 'token', () => undefined,
      respond(403, '{"status":403,"detail":"Conflicting tenant identifiers"}', { 'Content-Type': 'application/problem+json' }));

    await expect(legacy.get('/api/appointments')).rejects.toEqual(new ApiError(422, 'Appointment must start in the future'));
    await expect(gateway.get('/api/customers')).rejects.toThrow('Conflicting tenant identifiers');
  });

  it('copes with empty error bodies', async () => {
    const api = createApiClient(() => undefined, () => undefined, respond(401, ''));

    await expect(api.get('/api/customers')).rejects.toMatchObject({ status: 401 });
  });
});
