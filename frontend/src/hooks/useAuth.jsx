import { createContext, useContext, useState, useEffect } from 'react';
import { authApi, tenantApi } from '../api';
import keycloak, { KEYCLOAK_ENABLED, KEYCLOAK_REDIRECT_URI } from '../lib/keycloak';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [tenant, setTenant] = useState(null);
  const [tenants, setTenants] = useState([]);
  const [loading, setLoading] = useState(true);

  // Persist the authenticated user + tenant returned by the backend.
  const applySession = (userData, tenantData) => {
    localStorage.setItem('user', JSON.stringify(userData));
    if (tenantData) {
      localStorage.setItem('tenant', JSON.stringify(tenantData));
      localStorage.setItem('tenantId', tenantData.id);
      setTenant(tenantData);
    } else if (userData.tenantId) {
      localStorage.setItem('tenantId', userData.tenantId);
    }
    setUser(userData);
    if (userData.role === 'ADMIN') {
      loadTenants();
    }
  };

  // Exchange the current Keycloak/IDEM token for an application session.
  const exchangeKeycloakToken = async () => {
    console.log("exchangeKeycloakToken START");
    const response = await authApi.keycloakLogin(keycloak.token);
    console.log("KEYCLOAK LOGIN RESPONSE =", response);
    const { data } = response;

    const { token, user, tenant } = data.data;
    localStorage.setItem('token', token);
    console.log("TOKEN SAVED =", localStorage.getItem("token"));
    applySession(user, tenant);
    return user;
  };

  useEffect(() => {
    if (KEYCLOAK_ENABLED) {
      // IDEM SSO mode — detect an existing IDEM session without forcing a login.
      keycloak
        .init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false })
        .then(async (authenticated) => {
          console.log("AUTHENTICATED =", authenticated);
          console.log("KEYCLOAK AUTH =", keycloak.authenticated);
          console.log("KEYCLOAK TOKEN =", keycloak.token);
          if (authenticated) {
            try {
              console.log("CALLING exchangeKeycloakToken()");
              await exchangeKeycloakToken();
              // Keep the app token fresh while the tab is open.
              keycloak.onTokenExpired = () => {
                keycloak.updateToken(30).catch(() => keycloak.login());
              };
            } catch (err) {
              console.error('IDEM token exchange failed:', err);
            }
          }
          else {
            console.log("NOT AUTHENTICATED");
          }
        })
        .catch((err) => console.error('Keycloak init failed:', err))
        .finally(() => setLoading(false));
      return;
    }

    // Classic mode — restore a previously stored session.
    const stored = localStorage.getItem('user');
    const storedTenant = localStorage.getItem('tenant');
    if (stored) {
      try { setUser(JSON.parse(stored)); } catch (err) { console.error(err); }
    }
    if (storedTenant) {
      try { setTenant(JSON.parse(storedTenant)); } catch (err) { console.error(err); }
    }
    setLoading(false);
  }, []);

  // Load available tenants for admin users
  const loadTenants = async () => {
    try {
      const { data } = await tenantApi.list();
      if (data.success) {
        setTenants(data.data || []);
      }
    } catch (err) {
      console.error('Failed to load tenants:', err);
    }
  };

  // Redirect to the IDEM/Keycloak login page.
  const loginWithKeycloak = async () => {
    try {
      // keycloak-js requires init() before login(). If SSO auto-init did not run
      // (feature flag off), initialise on demand before redirecting.
      if (!keycloak.didInitialize) {
        await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
      }
      keycloak.login({ redirectUri: KEYCLOAK_REDIRECT_URI });
    } catch (err) {
      console.error('IDEM login failed to start:', err);
      throw err;
    }
  };

  const login = async (email, password) => {
    // Safety guard: ensure we always send plain strings to the backend.
    const safeEmail = typeof email === 'object'
      ? (email?.email ?? '')
      : String(email ?? '');
    const safePassword = typeof password === 'object'
      ? (password?.password ?? '')
      : String(password ?? '');

    const { data } = await authApi.login({ email: safeEmail, password: safePassword });
    const { token, user: userData, tenant: tenantData } = data.data;

    localStorage.setItem('token', token);
    applySession(userData, tenantData);

    return userData;
  };

  const switchTenant = async (tenantId) => {
    try {
      const { data } = await tenantApi.get(tenantId);
      if (data.success) {
        const tenantData = data.data;
        localStorage.setItem('tenant', JSON.stringify(tenantData));
        localStorage.setItem('tenantId', tenantData.id);
        setTenant(tenantData);
        // Reload the page to refresh data for new tenant
        window.location.reload();
      }
    } catch (err) {
      console.error('Failed to switch tenant:', err);
      throw err;
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('tenant');
    localStorage.removeItem('tenantId');
    setUser(null);
    setTenant(null);
    setTenants([]);
    if (KEYCLOAK_ENABLED && keycloak.authenticated) {
      keycloak.logout({ redirectUri: window.location.origin + '/login' });
    }
  };

  return (
    <AuthContext.Provider value={{
      user,
      tenant,
      tenants,
      login,
      loginWithKeycloak,
      keycloakEnabled: KEYCLOAK_ENABLED,
      logout,
      switchTenant,
      loadTenants,
      loading
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
