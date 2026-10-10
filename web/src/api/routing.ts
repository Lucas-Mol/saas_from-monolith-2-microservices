let lastRoutedTo: string | null = null;
const listeners = new Set<() => void>();

export const routingStore = {
  get: (): string | null => lastRoutedTo,
  set: (value: string | null): void => {
    lastRoutedTo = value;
    listeners.forEach((listener) => listener());
  },
  subscribe: (listener: () => void): (() => void) => {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};
