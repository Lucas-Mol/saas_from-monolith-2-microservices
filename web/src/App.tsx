import { useEffect, useMemo, useRef } from 'react';
import { useAuth } from 'react-oidc-context';
import { NavLink, Navigate, Route, Routes } from 'react-router-dom';

import { createApiClient } from './api/client';
import { routingStore } from './api/routing';
import { backofficeUrl, identityFromAccessToken } from './auth/claims';
import { currentPath } from './auth/returnTo';
import { DebugFooter } from './components/DebugFooter';
import { Icon } from './components/Icon';
import { initials, tenantDisplayName } from './lib/format';
import { Agenda } from './pages/Agenda';
import { Customers } from './pages/Customers';
import { Invoices } from './pages/Invoices';
import { NewAppointment } from './pages/NewAppointment';
import { Professionals } from './pages/Professionals';

export function App() {
  const auth = useAuth();
  const token = useRef<string | undefined>(undefined);
  token.current = auth.user?.access_token;
  const api = useMemo(() => createApiClient(() => token.current, routingStore.set), []);

  useEffect(() => {
    if (!auth.isLoading && !auth.isAuthenticated && !auth.activeNavigator && !auth.error) {
      void auth.signinRedirect({ state: { returnTo: currentPath() } });
    }
  }, [auth]);


  useEffect(() => auth.events.addSilentRenewError(() => void auth.removeUser()), [auth]);

  if (auth.error) {
    return (
      <main className="gate">
        <div className="gate-card">
          <p className="eyebrow">Agenda</p>
          <h1>Sign-in did not complete</h1>
          <p>{auth.error.message}</p>
          <button
            type="button"
            className="button-primary"
            onClick={() => void auth.signinRedirect({ state: { returnTo: currentPath() } })}
          >
            Sign in again
          </button>
        </div>
      </main>
    );
  }
  if (!auth.isAuthenticated || !auth.user) {
    return (
      <main className="gate" aria-busy="true">
        <div className="gate-card">
          <p className="eyebrow">Agenda</p>
          <h1>Signing you in…</h1>
        </div>
      </main>
    );
  }

  const identity = identityFromAccessToken(auth.user.access_token);
  const isStaff = identity.roles.includes('ADMIN') || identity.roles.includes('STAFF');
  const isAdmin = identity.roles.includes('ADMIN');
  const clinicName = identity.tenantId ? tenantDisplayName(identity.tenantId) : 'Agenda';

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            <Icon name="calendar" />
          </span>
          <div>
            <strong>{clinicName}</strong>
            <span>Agenda</span>
          </div>
        </div>
        <nav className="nav" aria-label="Main">
          {isStaff && (
            <NavLink to="/" end>
              <Icon name="calendar" /> Agenda
            </NavLink>
          )}
          {isStaff && (
            <NavLink to="/appointments/new">
              <Icon name="plus" /> Book appointment
            </NavLink>
          )}
          {isStaff && (
            <NavLink to="/customers">
              <Icon name="people" /> Customers
            </NavLink>
          )}
          {isAdmin && (
            <NavLink to="/invoices">
              <Icon name="receipt" /> Invoices
            </NavLink>
          )}
          {!isStaff && (
            <NavLink to="/" end>
              <Icon name="stethoscope" /> Professionals
            </NavLink>
          )}
        </nav>
        <div className="account">
          {identity.tenantId && isStaff && (
            <a className="account-link" href={backofficeUrl(identity.tenantId)}>
              <Icon name="external" /> Open backoffice
            </a>
          )}
          <div className="account-user">
            <span className="avatar avatar-small" aria-hidden="true">
              {initials(identity.name ?? identity.email ?? '?')}
            </span>
            <span className="account-name">{identity.name ?? identity.email}</span>
          </div>
          <button type="button" className="account-link" onClick={() => void auth.signoutRedirect()}>
            <Icon name="logout" /> Log out
          </button>
        </div>
      </aside>
      <main className="content">
        {!identity.tenantId ? (
          <div className="callout callout-error" role="alert">
            <strong>This account is not linked to a clinic or salon.</strong>
            <span>Ask your administrator to add it to one, then sign in again.</span>
          </div>
        ) : (
          <Routes>
            <Route
              path="/"
              element={
                isStaff ? <Agenda api={api} clinicName={clinicName} /> : <Professionals api={api} clinicName={clinicName} />
              }
            />
            {isStaff && <Route path="/appointments/new" element={<NewAppointment api={api} />} />}
            {isStaff && <Route path="/customers" element={<Customers api={api} />} />}
            {isAdmin && <Route path="/invoices" element={<Invoices api={api} />} />}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        )}
      </main>
      <DebugFooter identity={identity} />
    </div>
  );
}
