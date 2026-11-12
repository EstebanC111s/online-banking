import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Alert, ErrorAlert } from '../components/Alert';
import { Field } from '../components/Field';
import { useAuth } from '../auth';

export default function Login() {
  const { login, expiredNotice, clearExpiredNotice } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/cuentas';
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const s = await login(email, password);
      clearExpiredNotice();
      navigate(s.role === 'ADMIN' && from === '/cuentas' ? '/admin' : from, { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="narrow">
      <h1>Ingresar</h1>
      {expiredNotice && <Alert kind="warning">Tu sesión se cerró por seguridad. Vuelve a ingresar.</Alert>}
      <form onSubmit={submit} noValidate>
        <Field id="email" label="Correo" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        <Field id="password" label="Contraseña" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        <ErrorAlert error={error} />
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
      <p>¿No tienes cuenta? <Link to="/registro">Ábrela aquí</Link>.</p>
    </div>
  );
}
