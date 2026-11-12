import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import { Alert, ErrorAlert } from '../components/Alert';
import { Field } from '../components/Field';
import { useAuth } from '../auth';
import { saveCookieChoice } from '../cookieConsent';
import { dateTime } from '../format';
import type { ConsentState, Me } from '../types';
import { useAsync } from '../useAsync';

export default function Privacy() {
  const me = useAsync(() => api<Me>('/api/me'), []);
  const consents = useAsync(() => api<ConsentState[]>('/api/me/consents'), []);
  const [error, setError] = useState<unknown>(null);

  const toggle = async (c: ConsentState) => {
    setError(null);
    try {
      const updated = await api<ConsentState[]>(`/api/me/consents/${c.type}`, { method: 'PUT', body: { granted: !c.granted } });
      consents.setData(updated);
      if (c.type === 'ANALYTICS') saveCookieChoice(!c.granted);
    } catch (e) {
      setError(e);
    }
  };

  const download = async () => {
    setError(null);
    try {
      const data = await api<unknown>('/api/me/export');
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'mis-datos-banco-gocomezbo.json';
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(e);
    }
  };

  return (
    <>
      <h1>Privacidad y mis datos</h1>
      <ErrorAlert error={error} />

      <section className="card" aria-labelledby="data-title">
        <h2 id="data-title">Lo que guardamos de ti</h2>
        <p>Solo esto (datos mínimos):</p>
        {me.data && (
          <dl className="details">
            <dt>Nombre</dt><dd>{me.data.fullName}</dd>
            <dt>Correo</dt><dd>{me.data.email}</dd>
            <dt>Fecha de nacimiento</dt><dd>{me.data.birthDate ?? '-'}</dd>
            <dt>Términos aceptados</dt><dd>versión {me.data.termsVersion} · {me.data.termsAcceptedAt && dateTime(me.data.termsAcceptedAt)}</dd>
            <dt>Privacidad aceptada</dt><dd>versión {me.data.privacyVersion} · {me.data.privacyAcceptedAt && dateTime(me.data.privacyAcceptedAt)}</dd>
          </dl>
        )}
        <p>Además, tus cuentas, movimientos, disputas y tu reseña si escribiste una.</p>
        <button type="button" className="btn btn-secondary" onClick={download}>Descargar todos mis datos (JSON)</button>
      </section>

      <section className="card" aria-labelledby="consent-title">
        <h2 id="consent-title">Permisos opcionales</h2>
        <p>Ninguno es necesario para usar el banco. Puedes cambiarlos cuando quieras.</p>
        <ul className="consent-list">
          {consents.data?.map((c) => (
            <li key={c.type}>
              <label className="check">
                <input type="checkbox" checked={c.granted} onChange={() => toggle(c)} />
                {c.description}
              </label>
              {c.updatedAt && <p className="hint">Último cambio: {dateTime(c.updatedAt)}</p>}
            </li>
          ))}
        </ul>
      </section>

      <DeleteAccount />
    </>
  );
}

function DeleteAccount() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [done, setDone] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await api('/api/me/delete', { method: 'POST', body: { password } });
      setDone(true);
      setTimeout(() => {
        logout();
        navigate('/');
      }, 4000);
    } catch (err) {
      setError(err);
    }
  };

  if (done) {
    return <Alert kind="success">Borramos tus datos personales. Cerraremos tu sesión en unos segundos.</Alert>;
  }

  return (
    <section className="card danger-zone" aria-labelledby="delete-title">
      <h2 id="delete-title">Borrar mis datos y cerrar mi cuenta</h2>
      <p>Qué pasa si continúas:</p>
      <ul>
        <li>Se cierran todas tus cuentas (deben tener saldo $0; si no, te decimos cuáles).</li>
        <li>Se borran tu nombre, correo, fecha de nacimiento, permisos, reseña y correos enviados.</li>
        <li>Los movimientos contables se conservan sin datos que te identifiquen, porque la ley obliga a guardar registros contables.</li>
        <li>No se puede deshacer. Te recomendamos descargar tus datos antes.</li>
      </ul>
      {!open ? (
        <button type="button" className="btn btn-danger" onClick={() => setOpen(true)} aria-expanded={false}>
          Quiero borrar mis datos
        </button>
      ) : (
        <form onSubmit={submit}>
          <Field id="delete-password" label="Confirma con tu contraseña" type="password" autoComplete="current-password" required
            value={password} onChange={(e) => setPassword(e.target.value)} autoFocus />
          <ErrorAlert error={error} />
          <div className="button-row">
            <button type="submit" className="btn btn-danger">Borrar definitivamente</button>
            <button type="button" className="btn btn-secondary" onClick={() => setOpen(false)}>Cancelar</button>
          </div>
        </form>
      )}
    </section>
  );
}
