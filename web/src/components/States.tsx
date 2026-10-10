import type { ReactNode } from 'react';

export function ErrorCallout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="callout callout-error" role="alert">
      <strong>{title}</strong>
      <span>{children}</span>
    </div>
  );
}

export function SkeletonRows({ rows = 4 }: { rows?: number }) {
  return (
    <div className="skeleton" aria-busy="true" aria-label="Loading">
      {Array.from({ length: rows }, (_, index) => (
        <span key={index} className="skeleton-row" />
      ))}
    </div>
  );
}

export function PageHeader({ eyebrow, title, children }: { eyebrow?: string; title: string; children?: ReactNode }) {
  return (
    <header className="page-header">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
      </div>
      {children && <div className="page-actions">{children}</div>}
    </header>
  );
}
