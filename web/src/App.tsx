import { useEffect, useMemo, useRef } from 'react';
import { useAuth } from 'react-oidc-context';
import { NavLink, Navigate, Route, Routes } from 'react-router-dom';

import { createApiClient } from './api/client';
import { routingStore } from './api/routing';
import { backofficeUrl, identityFromAccessToken } from './auth/claims';
import { DebugFooter } from './components/DebugFooter';
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
      void auth.signinRedirect();
    }
  }, [auth]);

  useEffect(() => auth.events.addUserSignedOut(() => void auth.removeUser()), [auth]);
  useEffect(() => auth.events.addSilentRenewError(() => void auth.removeUser()), [auth]);

  if (auth.error) {
    return (
      <main className="center">
        <p className="error">Sign-in failed: {auth.error.message}</p>
        <button onClick={() => void auth.signinRedirect()}>Try again</button>
      </main>
    );
  }
  if (!auth.isAuthenticated || !auth.user) {
    return <main className="center">Signing in…</main>;
  }

  const identity = identityFromAccessToken(auth.user.access_token);
  const isStaff = identity.roles.includes('ADMIN') || identity.roles.includes('STAFF');
  const isAdmin = identity.roles.includes('ADMIN');

  return (
    <>
      <header className="top">
        <strong>Agenda</strong>
        <nav>
          {isStaff && <NavLink to="/">Agenda</NavLink>}
          {isStaff && <NavLink to="/appointments/new">New appointment</NavLink>}
          {isStaff && <NavLink to="/customers">Customers</NavLink>}
          {isAdmin && <NavLink to="/invoices">Invoices</NavLink>}
          {!isStaff && <NavLink to="/">Professionals</NavLink>}
        </nav>
        <div className="actions">
          {identity.tenantId && isStaff && <a href={backofficeUrl(identity.tenantId)}>Open backoffice</a>}
          <button onClick={() => void auth.signoutRedirect()}>Log out</button>
        </div>
      </header>
      <main>
        {!identity.tenantId ? (
          <p className="error">This account does not belong to a clinic or salon.</p>
        ) : (
          <Routes>
            <Route path="/" element={isStaff ? <Agenda api={api} /> : <Professionals api={api} />} />
            {isStaff && <Route path="/appointments/new" element={<NewAppointment api={api} />} />}
            {isStaff && <Route path="/customers" element={<Customers api={api} />} />}
            {isAdmin && <Route path="/invoices" element={<Invoices api={api} />} />}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        )}
      </main>
      <DebugFooter identity={identity} />
    </>
  );
}
