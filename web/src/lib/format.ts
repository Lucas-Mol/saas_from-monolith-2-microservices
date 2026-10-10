const currency = new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'BRL' });

export function formatMoney(amount: number): string {
  return currency.format(amount);
}

export function tenantDisplayName(tenantId: string): string {
  return tenantId
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export function initials(fullName: string): string {
  const words = fullName.replace(/^(dr|dra)\.?\s+/i, '').split(/\s+/).filter(Boolean);
  return ((words[0]?.[0] ?? '') + (words.length > 1 ? words[words.length - 1][0] : '')).toUpperCase();
}
