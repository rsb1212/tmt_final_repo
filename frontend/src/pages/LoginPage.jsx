import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import './LoginPage.css';
import BatLogo from '../data/bajaj.png';

/**
 * LoginPage — IDEM SSO only.
 *
 * The local email/password form has been removed. Any visit to /login (or the
 * root URL while unauthenticated) is redirected straight to the IDEM / RH-SSO
 * OpenID Connect authorize endpoint via the backend:
 *     GET /api/v1/auth/idem/login  →  302  →  Keycloak authorize URL
 * The backend redirects back here on completion with either
 *     ?sso=success&token=<jwt>&user=<base64-json>   or
 *     ?sso=error&reason=<code>
 *
 * IMPORTANT — avoid the classic "SSO loop":
 *   1. The redirect-to-IDEM effect must fire AT MOST ONCE per tab. Each call to
 *      /idem/login creates a NEW HttpSession on the backend with a fresh
 *      state + PKCE verifier; if the SPA fires it repeatedly (e.g. because
 *      React 18 StrictMode double-invokes effects, the AuthProvider re-renders,
 *      or the user has two tabs open), the "winning" callback loses its state
 *      and the browser lands on /login?sso=error&reason=invalid_session.
 *   2. We therefore (a) use a module-level ref that survives StrictMode's
 *      double-invoke, (b) set a sessionStorage sentinel so a hard reload in the
 *      same tab won't re-kick the flow within a short window, and (c) DO NOT
 *      list `ssoLogin` / `navigate` in the effect deps — those are recreated
 *      on every AuthProvider render and would otherwise re-fire the redirect.
 */

// Module-level guard — survives React StrictMode's intentional double effect
// invocation in dev. In prod builds StrictMode does not double-invoke effects,
// but this is cheap defence in depth.
let redirectedThisMount = false;

export default function LoginPage() {
  const { ssoLogin } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const didRun = useRef(false);

  useEffect(() => {
    if (didRun.current) return;   // guard against re-render
    didRun.current = true;

    const params = new URLSearchParams(window.location.search);
    const sso = params.get('sso');

    // 1) Successful SSO callback — finish login and enter the app.
    if (sso === 'success') {
      try {
        const token   = params.get('token');
        const userB64 = params.get('user');
        const json = decodeURIComponent(
          atob(userB64.replace(/-/g, '+').replace(/_/g, '/'))
            .split('')
            .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
            .join('')
        );
        const userData = JSON.parse(json);
        ssoLogin(token, userData);
        sessionStorage.removeItem('tmt.idem.redirecting');
        window.history.replaceState({}, document.title, '/login');
        navigate('/');
      } catch (err) {
        console.error('SSO callback parse failed', err);
        setError('Single sign-on failed. Please try again.');
        window.history.replaceState({}, document.title, '/login');
      }
      return;
    }

    // 2) SSO returned an error — show a friendly message, do NOT auto-loop.
    if (sso === 'error') {
      const reason = params.get('reason') || 'unknown';
      const friendly = {
        user_not_registered:
          'You are not registered in the application. Please contact your administrator.',
        user_disabled:
          'Your account is disabled. Please contact your administrator.',
        no_email_claim:
          'Your IDEM profile did not provide an email address. Please contact your administrator.',
      }[reason] || `Single sign-on failed (${reason}). Please try again.`;
      setError(friendly);
      sessionStorage.removeItem('tmt.idem.redirecting');
      window.history.replaceState({}, document.title, '/login');
      return;
    }

    // 3) Fresh visit — kick straight into the IDEM OIDC flow, but only once.
    if (redirectedThisMount) return;
    // 3a) If the user just clicked Logout, DO NOT auto-redirect. The Keycloak
    // SSO cookie on the IdP is still alive and would silently re-authenticate
    // them, making logout appear broken. Show the Sign-in button instead.
    if (sessionStorage.getItem('tmt.loggedOut') === '1') {
      setError('You have been signed out. Click below to sign in again.');
      return;
    }
    // Also skip if we already fired the redirect from this tab within the last
    // 30s (protects against a hard reload racing the Keycloak round-trip).
    const stamp = Number(sessionStorage.getItem('tmt.idem.redirecting') || 0);
    if (stamp && Date.now() - stamp < 30_000) {
      // Something is already in flight — just show the "redirecting" placeholder
      // and let the browser navigate. Do NOT trigger another /idem/login.
      return;
    }
    redirectedThisMount = true;
    sessionStorage.setItem('tmt.idem.redirecting', String(Date.now()));

    const base = import.meta.env.VITE_API_URL || '/api/v1';
    window.location.replace(`${base}/auth/idem/login`);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);   // <-- run once per mount; deps deliberately empty

  return (
    <div className="login-page">
      <div className="login-bg">
        <div className="grid-overlay" />
      </div>

      <div className="login-card" style={{ textAlign: 'center' }}>
        <div className="login-brand">
          <div className="login-icon">
            <img src={BatLogo} alt="Bajaj Life" width={36} height={36} />
          </div>
          <h1 className="login-title">Test Genii</h1>
          <p className="login-sub">Intelligent Test Knowledge &amp; Management Platform</p>
        </div>

        {error ? (
          <>
            <div className="alert alert-error" style={{ margin: '16px 0' }}>{error}</div>
            <button
              className="login-btn"
              onClick={() => {
                sessionStorage.removeItem('tmt.idem.redirecting');
                sessionStorage.removeItem('tmt.loggedOut');
                redirectedThisMount = false;
                const base = import.meta.env.VITE_API_URL || '/api/v1';
                window.location.replace(`${base}/auth/idem/login`);
              }}
            >
              Try IDEM Sign-in Again
            </button>
          </>
        ) : (
          <p style={{ marginTop: 24, color: 'var(--text3)' }}>
            Redirecting to IDEM sign-in…
          </p>
        )}

        <div className="login-footer">
          © 2026 Bajaj Life Insurance. All Rights Reserved.
        </div>
      </div>
    </div>
  );
}
