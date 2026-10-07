import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url:
    import.meta.env.VITE_KEYCLOAK_URL ||
    // OLD (NP): 'https://secure-sso-rhsso-np.apps.ocplife-np.bajajlife.com/auth',
    'https://sso-rhsso-prod.apps.ocplife.bajajlife.com/auth',
  realm:
    import.meta.env.VITE_KEYCLOAK_REALM ||
    'internal',
  clientId:
    import.meta.env.VITE_KEYCLOAK_CLIENT_ID ||
    'TMT',
});

export const KEYCLOAK_ENABLED =
  String(import.meta.env.VITE_USE_KEYCLOAK).toLowerCase() === 'true';

export const KEYCLOAK_REDIRECT_URI =
  import.meta.env.VITE_KEYCLOAK_REDIRECT_URI ||
  'http://10.3.41.102/';

export default keycloak;