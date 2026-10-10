export interface SigninState {
  returnTo: string;
}

export function currentPath(): string {
  return window.location.pathname + window.location.search;
}

export function safeReturnTo(state: unknown): string {
  const returnTo = (state as Partial<SigninState> | null | undefined)?.returnTo;
  if (typeof returnTo !== 'string' || !returnTo.startsWith('/') || returnTo.startsWith('//') || returnTo.includes('\\')) {
    return '/';
  }
  return returnTo;
}
