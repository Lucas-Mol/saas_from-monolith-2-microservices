import { useEffect, useState } from 'react';

export interface AsyncState<T> {
  data: T | null;
  error: string | null;
  loading: boolean;
}

export function useAsync<T>(load: () => Promise<T>, deps: unknown[]): AsyncState<T> {
  const [state, setState] = useState<AsyncState<T>>({ data: null, error: null, loading: true });

  useEffect(() => {
    let current = true;
    setState({ data: null, error: null, loading: true });
    load().then(
      (data) => current && setState({ data, error: null, loading: false }),
      (error: unknown) =>
        current && setState({ data: null, error: error instanceof Error ? error.message : String(error), loading: false }),
    );
    return () => {
      current = false;
    };
  }, deps);

  return state;
}
