import { describe, expect, it } from 'vitest';

import { addDays, formatLongDate, formatMinutes, isValidIsoDate, toMinutes } from './time';

describe('time helpers', () => {
  it('moves across month and year boundaries', () => {
    expect(addDays('2026-10-31', 1)).toBe('2026-11-01');
    expect(addDays('2026-01-01', -1)).toBe('2025-12-31');
  });

  it('reads clock times and ISO date-times as minutes', () => {
    expect(toMinutes('08:30:00')).toBe(510);
    expect(toMinutes('2026-10-12T14:45:00')).toBe(885);
    expect(formatMinutes(885)).toBe('14:45');
  });

  it('formats the day heading and validates query dates', () => {
    expect(formatLongDate('2026-10-12')).toBe('Monday, 12 October 2026');
    expect(isValidIsoDate('2026-10-12')).toBe(true);
    expect(isValidIsoDate('12/10/2026')).toBe(false);
  });
});
