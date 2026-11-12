import { useState, type ReactNode } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import type { Me } from '../types';
import { useAsync } from '../useAsync';
import { ErrorAlert } from './Alert';

export function RequireAuth({ children, admin = false }: { children: ReactNode; admin?: boolean }) {
  const { session } = useAuth();
  const location = useLocation();
  if (!session) {
    return <Navigate to="/ingresar" replace state={{ from: location.pathname }} />;
  }
  if (admin && session.role !== 'ADMIN') {
    return <Navigate to="/cuentas" replace />;
  }
  return <LegalGate>{children}</LegalGate>;
}

function LegalGate({ children }: { children: ReactNode }) {
  const me = useAsync(() => api<Me>('/api/me'), []);
  const [accepted, setAccepted] = useState(false);
  const [error, setError] = useState<unknown>(null);

  if (me.loading) return <p aria-live="polite">Cargando…</p>;
  if (me.error) return <ErrorAlert error={me.error} />;
  if (!me.data?.needsLegalAcceptance || accepted) return <>{children}</>;

  const accept = async () => {
    try {
      await api('/api/me/legal-acceptance', {
        method: 'POST',
        body: { termsVersion: me.data!.currentTermsVersion, privacyVersion: me.data!.currentPrivacyVersion },
      });
      setAccepted(true);
    } catch (e) {
      setError(e);
    }
  };

  return (
    <section className="card">
      <h1>Actualizamos nuestros documentos</h1>
      <p>
        Antes de seguir, revisa los <Link to="/legal/terminos">términos de uso</Link> y la{' '}
        <Link to="/legal/privacidad">política de privacidad</Link> vigentes.
      </p>
      <ErrorAlert error={error} />
      <button type="button" className="btn btn-primary" onClick={accept}>
        Los leí y los acepto
      </button>
    </section>
  );
}
