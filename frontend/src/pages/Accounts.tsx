import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { Alert, ErrorAlert } from '../components/Alert';
import { useAuth } from '../auth';
import { date, money } from '../format';
import type { Account, AccountType } from '../types';
import { useAsync } from '../useAsync';

export default function Accounts() {
  const { session } = useAuth();
  const accounts = useAsync(() => api<Account[]>('/api/accounts'), []);
  const [type, setType] = useState<AccountType>('SAVINGS');
  const [error, setError] = useState<unknown>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const open = async () => {
    setError(null);
    try {
      const acc = await api<Account>('/api/accounts', { method: 'POST', body: { type } });
      setNotice(`Abriste la ${acc.typeLabel.toLowerCase()} ${acc.number}. Tienes hasta el ${date(acc.retractDeadline!)} para retractarte sin costo.`);
      accounts.reload();
    } catch (e) {
      setError(e);
    }
  };

  const openAccounts = accounts.data?.filter((a) => a.status === 'OPEN') ?? [];
  const closed = accounts.data?.filter((a) => a.status !== 'OPEN') ?? [];

  return (
    <>
      <h1>Hola, {session?.fullName.split(' ')[0]}</h1>
      <ErrorAlert error={accounts.error} />
      {notice && <Alert kind="success">{notice}</Alert>}

      <section aria-labelledby="accounts-title">
        <h2 id="accounts-title">Mis cuentas</h2>
        {accounts.loading && <p aria-live="polite">Cargando…</p>}
        {accounts.data && openAccounts.length === 0 && <p>Aún no tienes cuentas abiertas.</p>}
        <ul className="account-grid">
          {openAccounts.map((a) => (
            <li key={a.id} className="card account-card">
              <p className="hint">{a.typeLabel}</p>
              <p className="account-number">N.º {a.number}</p>
              <p className="big-number">{money(a.balance)}</p>
              {a.canRetract && <p className="hint">Retracto sin costo hasta el {date(a.retractDeadline!)}</p>}
              <Link to={`/cuentas/${a.id}`} className="btn btn-secondary btn-small">
                Ver movimientos<span className="sr-only"> de la cuenta {a.number}</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section aria-labelledby="open-title" className="card">
        <h2 id="open-title">Abrir una cuenta nueva</h2>
        <p>Gratis. Sin cuota de manejo. Puedes retractarte en los 5 días hábiles siguientes.</p>
        <div className="field">
          <label htmlFor="account-type">Tipo de cuenta</label>
          <select id="account-type" value={type} onChange={(e) => setType(e.target.value as AccountType)}>
            <option value="SAVINGS">Cuenta de ahorros</option>
            <option value="CHECKING">Cuenta corriente</option>
          </select>
        </div>
        <ErrorAlert error={error} />
        <button type="button" className="btn btn-primary" onClick={open}>Abrir cuenta</button>
      </section>

      {closed.length > 0 && (
        <section aria-labelledby="closed-title">
          <h2 id="closed-title">Cuentas cerradas</h2>
          <ul>
            {closed.map((a) => (
              <li key={a.id}>
                {a.typeLabel} {a.number}, {a.status === 'RETRACTED' ? 'cerrada por retracto' : 'cerrada'}{' '}
                (<Link to={`/cuentas/${a.id}`}>ver historial</Link>)
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}
