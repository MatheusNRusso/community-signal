const isLocal =
  typeof window !== 'undefined' && window.location.hostname === 'localhost';

export const environment = {
  production: !isLocal,
  apiUrl: isLocal
    ? 'http://localhost:8085'
    : 'https://community-signal-9h8r.onrender.com'
};
