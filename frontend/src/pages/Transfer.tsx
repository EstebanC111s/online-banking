import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { api, ApiError } from '../api';
import { Alert, ErrorAlert } from '../components/Alert';
import { Field } from '../components/Field';
import { money } from '../format';
import type { Account, Quote, TransferResult } from '../types';
import { useAsync } from '../useAsync';

export default function Transfer() {
  const accounts = useAsync(() => api<Account[]>('/api/accounts'), []);
  const open = accounts.data?.filter((a) => a.status === 'OPEN') ?? [];

  const [fromId, setFromId] = useState<string>('');
  const [to, setTo] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<unknown>(null);
  const [quote, setQuote] = useState<Quote | null>(null);
  const [result, setResult] = useState<TransferResult | null>(null);
  const [busy, setBusy] = useState(false);
  const summaryRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    if (!fromId && open.length > 0) setFromId(String(open[0].id));
  }, [open, fromId]);

  useEffect(() => {
    if (quote || result) summaryRef.current?.focus();
  }, [quote, result]);

  const secondsLeft = useCountdown(quote?.expiresAt);
  const expired = quote !== null && secondsLeft <= 0;

  const requestQuote = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setErrors({});
    try {
      const q = await api<Quote>('/api/transfers/quotes', {
        method: 'POST',
        body: { fromAccountId: Number(fromId), toAccountNumber: to.trim(), amount, description },
      });
      setQuote(q);
    } catch (err) {
      if (err instanceof ApiError && err.fields) setErrors(err.fields);
      setError(err);
    }
  };

  const confirm = async () => {
    if (!quote) return;
    setBusy(true);
    setError(null);
    try {
      const r = await api<TransferResult>(`/api/transfers/quotes/${quote.quoteId}/confirm`, { method: 'POST' });
      setResult(r);
      setQuote(null);
      accounts.reload();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  const cancel = async () => {
    if (quote) await api(`/api/transfers/quotes/${quote.quoteId}/cancel`, { method: 'POST' }).catch(() => {});
    setQuote(null);
  };

  const restart = () => {
    setResult(null);
    setTo('');
    setAmount('');
    setDescription('');
  };

  if (accounts.data && open.length === 0) {
    return (
      <>
        <h1>Transferir</h1>
        <p>Necesitas una cuenta abierta. <Link to="/cuentas">Abre una aquí</Link>.</p>
      </>
    );
  }

  return (
    <>
      <h1>Transferir</h1>
      <ErrorAlert error={accounts.error} />

      {result && (
        <section className="card" aria-labelledby="done-title">
          <h2 id="done-title" ref={summaryRef} tabIndex={-1}>Transferencia realizada</h2>
          <Alert kind="success">
            Referencia {result.reference}. Enviaste {money(result.amount)} y pagaste {money(result.fee)} de comisión.
            Tu nuevo saldo es {money(result.newBalance)}.
          </Alert>
          <button type="button" className="btn btn-primary" onClick={restart}>Hacer otra transferencia</button>
        </section>
      )}

      {!result && !quote && (
        <form className="card narrow-form" onSubmit={requestQuote} noValidate>
          <h2>Paso 1 de 2: datos</h2>
          <div className="field">
            <label htmlFor="from">Desde</label>
            <select id="from" value={fromId} onChange={(e) => setFromId(e.target.value)}>
              {open.map((a) => (
                <option key={a.id} value={a.id}>{a.typeLabel} {a.number} ({money(a.balance)})</option>
              ))}
            </select>
          </div>
          <Field id="to" label="Número de cuenta destino" inputMode="numeric" maxLength={10} required value={to}
            onChange={(e) => setTo(e.target.value.replace(/\D/g, ''))} error={errors.toAccountNumber} hint="10 dígitos." />
          <Field id="amount" label="Monto (COP)" type="number" inputMode="numeric" min={1000} step={1000} required value={amount}
            onChange={(e) => setAmount(e.target.value)} error={errors.amount} />
          <Field id="description" label="Descripción (opcional)" maxLength={140} value={description} onChange={(e) => setDescription(e.target.value)} />
          <ErrorAlert error={error} />
          <button type="submit" className="btn btn-primary">Ver costo total</button>
          <p className="hint">Todavía no se mueve dinero.</p>
        </form>
      )}

      {quote && (
        <section className="card narrow-form" aria-labelledby="quote-title">
          <h2 id="quote-title" ref={summaryRef} tabIndex={-1}>Paso 2 de 2: revisa y confirma</h2>
          <dl className="details">
            <dt>Para</dt><dd>{quote.recipientName} · cuenta {quote.toAccountNumber}</dd>
            <dt>Desde</dt><dd>cuenta {quote.fromAccountNumber}</dd>
            <dt>Monto</dt><dd>{money(quote.amount)}</dd>
            <dt>Comisión</dt><dd>{quote.fee === 0 ? 'Gratis' : money(quote.fee)}</dd>
            <dt>Total a debitar</dt><dd><strong>{money(quote.total)}</strong></dd>
            <dt>Descripción</dt><dd>{quote.description}</dd>
          </dl>
          <p aria-live={secondsLeft <= 30 ? 'polite' : 'off'}>
            {expired ? 'Esta cotización venció.' : `Esta cotización vence en ${secondsLeft} segundos.`}
          </p>
          <ErrorAlert error={error} />
          <div className="button-row">
            {expired ? (
              <button type="button" className="btn btn-primary" onClick={() => setQuote(null)}>Volver a cotizar</button>
            ) : (
              <button type="button" className="btn btn-primary" onClick={confirm} disabled={busy}>
                {busy ? 'Enviando…' : `Confirmar y enviar ${money(quote.total)}`}
              </button>
            )}
            <button type="button" className="btn btn-secondary" onClick={cancel}>Cancelar</button>
          </div>
        </section>
      )}
    </>
  );
}

function useCountdown(iso?: string) {
  const calc = () => (iso ? Math.max(0, Math.round((new Date(iso).getTime() - Date.now()) / 1000)) : 0);
  const [left, setLeft] = useState(calc);
  useEffect(() => {
    setLeft(calc());
    if (!iso) return;
    const id = window.setInterval(() => setLeft(calc()), 1000);
    return () => window.clearInterval(id);
  }, [iso]);
  return left;
}
