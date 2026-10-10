import { InMemoryWebStorage, type User, WebStorageStateStore } from 'oidc-client-ts';
import type { AuthProviderProps } from 'react-oidc-context';

import { safeReturnTo } from './returnTo';

const authority = import.meta.env.VITE_OIDC_AUTHORITY ?? 'http://keycloak.localhost:8180/realms/agenda';

export const oidcConfig: AuthProviderProps = {
  authority,
  client_id: 'web',
  redirect_uri: `${window.location.origin}/`,
  post_logout_redirect_uri: `${window.location.origin}/`,
  scope: 'openid profile email',
  userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
  automaticSilentRenew: true,
  monitorSession: false,
  onSigninCallback: (user: User | undefined) => {
    window.history.replaceState({}, document.title, safeReturnTo(user?.state));
    window.dispatchEvent(new PopStateEvent('popstate'));
  },
};
