import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { Alert, ErrorAlert } from '../components/Alert';
import { date, dateTime, money } from '../format';
import type { Account, Dispute, Transaction } from '../types';
import { useAsync } from '../useAsync';

export default function AccountDetail() {
  const { id } = useParams();
  const accounts = useAsync(() => api<Account[]>('/api/accounts'), []);
  const txs = useAsync(() => api<Transaction[]>(`/api/accounts/${id}/transactions?limit=100`), [id]);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [depositAmount, setDepositAmount] = useState('100000');
  const [disputing, setDisputing] = useState<number | null>(null);

  const account = accounts.data?.find((a) => String(a.id) === id);

  const reloadAll = () => {
    accounts.reload();
    txs.reload();
  };

  const deposit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await api(`/api/accounts/${id}/deposits`, { method: 'POST', body: { amount: depositAmount } });
      setNotice(`Depósito simulado de ${money(Number(depositAmount))} realizado.`);
      reloadAll();
    } catch (err) {
      setError(err);
    }
  };

  const closeAccount = async (mode: 'retract' | 'close') => {
    setError(null);
    try {
      await api(`/api/accounts/${id}/${mode}`, { method: 'POST' });
      setNotice(mode === 'retract' ? 'Te retractaste: la cuenta quedó cerrada sin ningún costo.' : 'Cuenta cerrada sin costo.');
      reloadAll();
    } catch (err) {
      setError(err);
    }
  };

  if (accounts.data && !account) {
    return <><h1>Cuenta no encontrada</h1><Link to="/cuentas">Volver a mis cuentas</Link></>;
  }

  return (
    <>
      <p><Link to="/cuentas">← Mis cuentas</Link></p>
      <h1>{account ? `${account.typeLabel} ${account.number}` : 'Cuenta'}</h1>
      {account && (
        <p className="big-number" aria-label={`Saldo: ${money(account.balance)}`}>{money(account.balance)}</p>
      )}
      {notice && <Alert kind="success">{notice}</Alert>}
      <ErrorAlert error={error} />

      {account?.status === 'OPEN' && (
        <div className="two-col">
          <form className="card" onSubmit={deposit}>
            <h2>Depósito simulado</h2>
            <p className="hint">Solo para probar la app: no entra dinero real.</p>
            <div className="field">
              <label htmlFor="deposit">Monto (COP)</label>
              <input id="deposit" type="number" inputMode="numeric" min={1000} max={5000000} step={1000} value={depositAmount} onChange={(e) => setDepositAmount(e.target.value)} />
            </div>
            <button type="submit" className="btn btn-primary">Depositar</button>
          </form>

          <section className="card" aria-labelledby="close-title">
            <h2 id="close-title">Cerrar esta cuenta</h2>
            {account.canRetract ? (
              <>
                <p>
                  <strong>Derecho de retracto:</strong> hasta el {date(account.retractDeadline!)} puedes arrepentirte de haber
                  abierto esta cuenta, sin penalidad. El saldo debe estar en cero (muévelo gratis a otra cuenta tuya).
                </p>
                <button type="button" className="btn btn-secondary" onClick={() => closeAccount('retract')}>Me retracto</button>
              </>
            ) : (
              <>
                <p>Cerrar la cuenta no tiene costo. El saldo debe estar en cero.</p>
                <button type="button" className="btn btn-secondary" onClick={() => closeAccount('close')}>Cerrar cuenta</button>
              </>
            )}
          </section>
        </div>
      )}

      <section aria-labelledby="tx-title">
        <h2 id="tx-title">Movimientos</h2>
        <ErrorAlert error={txs.error} />
        {txs.data && txs.data.length === 0 && <p>Sin movimientos todavía.</p>}
        {txs.data && txs.data.length > 0 && (
          <table>
            <caption className="sr-only">Movimientos de la cuenta, del más reciente al más antiguo</caption>
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Tipo</th>
                <th scope="col">Detalle</th>
                <th scope="col" className="num">Monto</th>
                <th scope="col" className="num">Saldo</th>
                <th scope="col"><span className="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {txs.data.map((t) => (
                <tr key={t.id}>
                  <td>{dateTime(t.createdAt)}</td>
                  <td>{t.typeLabel}</td>
                  <td>
                    {t.description}
                    {t.counterpartyAccount && <span className="hint"> · cuenta ••••{t.counterpartyAccount.slice(-4)}</span>}
                    {t.reference && <span className="hint"> · {t.reference}</span>}
                  </td>
                  <td className={`num ${t.amount < 0 ? 'neg' : 'pos'}`}>{money(t.amount)}</td>
                  <td className="num">{money(t.balanceAfter)}</td>
                  <td>
                    {t.disputable && account?.status === 'OPEN' && (
                      <button type="button" className="link-button" onClick={() => setDisputing(t.id)} aria-expanded={disputing === t.id}>
                        Disputar<span className="sr-only"> {t.typeLabel} de {money(t.amount)}</span>
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {disputing !== null && (
          <DisputeForm
            transaction={txs.data!.find((t) => t.id === disputing)!}
            onCancel={() => setDisputing(null)}
            onDone={(d) => {
              setDisputing(null);
              setNotice(
                d.status === 'REFUNDED'
                  ? 'Te devolvimos la comisión de inmediato.'
                  : `Disputa #${d.id} recibida. Te respondemos a más tardar el ${date(d.dueDate)}.`,
              );
              reloadAll();
            }}
          />
        )}
      </section>
    </>
  );
}

function DisputeForm({ transaction, onCancel, onDone }: { transaction: Transaction; onCancel: () => void; onDone: (d: Dispute) => void }) {
  const [reason, setReason] = useState('');
  const [error, setError] = useState<unknown>(null);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      const d = await api<Dispute>(`/api/transactions/${transaction.id}/disputes`, { method: 'POST', body: { reason } });
      onDone(d);
    } catch (err) {
      setError(err);
    }
  };

  return (
    <form className="card" onSubmit={submit} aria-labelledby="dispute-title" onKeyDown={(e) => e.key === 'Escape' && onCancel()}>
      <h3 id="dispute-title">Disputar: {transaction.typeLabel} de {money(transaction.amount)}</h3>
      <p className="hint">
        {transaction.type === 'FEE'
          ? 'Las comisiones disputadas se devuelven de inmediato, sin preguntas.'
          : 'Revisamos las transferencias en máximo 15 días hábiles. Si la rechazamos, te diremos por qué.'}{' '}
        <Link to="/legal/devoluciones">Política de devoluciones</Link>
      </p>
      <div className="field">
        <label htmlFor="dispute-reason">¿Qué pasó?</label>
        <textarea id="dispute-reason" autoFocus rows={3} minLength={10} maxLength={500} required value={reason} onChange={(e) => setReason(e.target.value)} />
      </div>
      <ErrorAlert error={error} />
      <div className="button-row">
        <button type="submit" className="btn btn-primary">Enviar disputa</button>
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancelar</button>
      </div>
    </form>
  );
}
