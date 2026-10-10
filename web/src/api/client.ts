export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

export interface ApiClient {
  get<T>(path: string): Promise<T>;
  post<T>(path: string, body: unknown): Promise<T>;
}

export function createApiClient(
  getToken: () => string | undefined,
  onRoutedTo: (routedTo: string | null) => void,
  fetchImpl: typeof fetch = (input, init) => fetch(input, init),
): ApiClient {
  async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers);
    headers.set('Accept', 'application/json');
    const token = getToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
    if (init.body !== undefined) {
      headers.set('Content-Type', 'application/json');
    }
    const response = await fetchImpl(path, { ...init, headers });
    onRoutedTo(response.headers.get('X-Routed-To'));
    const body = parseJson(await response.text());
    if (!response.ok) {
      throw new ApiError(response.status, messageOf(body) ?? `Request failed with status ${response.status}`);
    }
    return body as T;
  }

  return {
    get: (path) => request(path),
    post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  };
}

function parseJson(text: string): unknown {
  if (!text) {
    return undefined;
  }
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

function messageOf(body: unknown): string | null {
  if (typeof body !== 'object' || body === null) {
    return null;
  }
  const { message, detail } = body as { message?: unknown; detail?: unknown };
  if (typeof message === 'string') {
    return message;
  }
  return typeof detail === 'string' ? detail : null;
}
