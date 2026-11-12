import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { readCookieChoice, saveCookieChoice } from '../cookieConsent';

export function CookieBanner() {
  const [open, setOpen] = useState(() => readCookieChoice() === null);

  useEffect(() => {
    const reopen = () => setOpen(true);
    window.addEventListener('gocomezbo:open-cookie-banner', reopen);
    return () => window.removeEventListener('gocomezbo:open-cookie-banner', reopen);
  }, []);

  if (!open) return null;

  const choose = (analytics: boolean) => {
    saveCookieChoice(analytics);
    setOpen(false);
  };

  return (
    <section className="cookie-banner" aria-labelledby="cookie-title">
      <h2 id="cookie-title">Tu privacidad</h2>
      <p>
        Usamos solo almacenamiento esencial para mantener tu sesión. ¿Nos dejas contar de forma anónima qué páginas
        se visitan? No guardamos tu identidad, IP ni dispositivo. <Link to="/legal/cookies">Leer el aviso de cookies</Link>.
      </p>
      <div className="button-row">
        <button type="button" className="btn btn-secondary" onClick={() => choose(false)}>
          Rechazar analítica
        </button>
        <button type="button" className="btn btn-secondary" onClick={() => choose(true)}>
          Aceptar analítica
        </button>
      </div>
    </section>
  );
}

export function openCookieBanner() {
  window.dispatchEvent(new Event('gocomezbo:open-cookie-banner'));
}
