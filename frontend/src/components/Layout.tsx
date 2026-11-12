import { useEffect, useRef, type ReactNode } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { trackPageView } from '../cookieConsent';
import { CookieBanner, openCookieBanner } from './CookieBanner';
import { Logo } from './Logo';

export function Layout({ children }: { children: ReactNode }) {
  const { session, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const mainRef = useRef<HTMLElement>(null);
  const firstRender = useRef(true);

  useEffect(() => {
    const h1 = mainRef.current?.querySelector('h1');
    document.title = h1?.textContent ? `${h1.textContent} · Banco Gocomezbo` : 'Banco Gocomezbo';
    if (firstRender.current) {
      firstRender.current = false;
    } else {
      mainRef.current?.focus();
    }
    trackPageView(location.pathname);
  }, [location.pathname]);

  const onLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <>
      <a className="skip-link" href="#main">
        Saltar al contenido
      </a>
      <CookieBanner />
      <header className="site-header">
        <div className="container header-inner">
          <Link to="/" className="brand">
            <Logo />
            <span>Banco Gocomezbo</span>
          </Link>
          <nav aria-label="Principal">
            <ul className="nav-list">
              {session ? (
                <>
                  <li><NavLink to="/cuentas">Mis cuentas</NavLink></li>
                  <li><NavLink to="/transferir">Transferir</NavLink></li>
                  <li><NavLink to="/disputas">Disputas</NavLink></li>
                  <li><NavLink to="/privacidad">Privacidad</NavLink></li>
                  {session.role === 'ADMIN' && <li><NavLink to="/admin">Administración</NavLink></li>}
                  <li>
                    <button type="button" className="link-button" onClick={onLogout}>
                      Cerrar sesión
                    </button>
                  </li>
                </>
              ) : (
                <>
                  <li><NavLink to="/resenas">Reseñas</NavLink></li>
                  <li><NavLink to="/ingresar">Ingresar</NavLink></li>
                  <li><NavLink to="/registro" className="btn btn-primary btn-small">Abrir cuenta</NavLink></li>
                </>
              )}
            </ul>
          </nav>
        </div>
      </header>

      <main id="main" ref={mainRef} tabIndex={-1} className="container">
        {children}
      </main>

      <footer className="site-footer">
        <div className="container footer-grid">
          <div>
            <p className="footer-title">Banco Gocomezbo S.A.S. (ficticio)</p>
            <p>Proyecto académico de práctica. No es una entidad financiera ni maneja dinero real.</p>
            <p>
              <Link to="/empresa">Datos de la empresa</Link>
            </p>
          </div>
          <nav aria-label="Legal">
            <ul className="footer-links">
              <li><Link to="/legal/terminos">Términos de uso</Link></li>
              <li><Link to="/legal/privacidad">Política de privacidad</Link></li>
              <li><Link to="/legal/cookies">Aviso de cookies</Link></li>
              <li>
                <button type="button" className="link-button" onClick={openCookieBanner}>
                  Preferencias de cookies
                </button>
              </li>
              <li><Link to="/legal/devoluciones">Devoluciones y disputas</Link></li>
            </ul>
          </nav>
          <nav aria-label="Transparencia">
            <ul className="footer-links">
              <li><Link to="/tarifas">Tarifas</Link></li>
              <li><Link to="/integraciones">Integraciones y terceros</Link></li>
              <li><Link to="/creditos">Créditos y licencias</Link></li>
              <li><Link to="/resenas">Reseñas de clientes</Link></li>
              <li>
                <Link to="/legal/ia" className="ai-badge">
                  Sin IA en decisiones sobre ti
                </Link>
              </li>
            </ul>
          </nav>
        </div>
      </footer>
    </>
  );
}
