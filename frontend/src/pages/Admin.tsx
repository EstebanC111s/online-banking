import { useState, type FormEvent } from 'react';
import { api } from '../api';
import { Alert, ErrorAlert } from '../components/Alert';
import { date, money } from '../format';
import type { Dispute } from '../types';
import { useAsync } from '../useAsync';

export default function Admin() {
  const queue = useAsync(() => api<Dispute[]>('/api/admin/disputes'), []);
  const [notes, setNotes] = useState<Record<number, string>>({});
  const [error, setError] = useState<unknown>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const resolve = async (id: number, refund: boolean) => {
    setError(null);
    try {
      await api(`/api/admin/disputes/${id}/resolve`, { method: 'POST', body: { refund, note: notes[id] ?? '' } });
      setNotice(`Disputa #${id} ${refund ? 'devuelta' : 'rechazada'}.`);
      queue.reload();
    } catch (e) {
      setError(e);
    }
  };

  return (
    <>
      <h1>Administración</h1>
      {notice && <Alert kind="success">{notice}</Alert>}
      <ErrorAlert error={error} />

      <section aria-labelledby="queue-title">
        <h2 id="queue-title">Disputas por resolver</h2>
        <ErrorAlert error={queue.error} />
        {queue.data?.length === 0 && <p>No hay disputas abiertas.</p>}
        <ul className="review-list">
          {queue.data?.map((d) => (
            <li key={d.id} className="card">
              <h3>#{d.id} · {d.transactionType} de {money(d.transactionAmount)} · vence {date(d.dueDate)}</h3>
              <p><strong>Motivo del cliente:</strong> {d.reason}</p>
              <div className="field">
                <label htmlFor={`note-${d.id}`}>Respuesta al cliente (obligatoria para rechazar)</label>
                <textarea id={`note-${d.id}`} rows={2} value={notes[d.id] ?? ''} onChange={(e) => setNotes({ ...notes, [d.id]: e.target.value })} />
              </div>
              <div className="button-row">
                <button type="button" className="btn btn-primary" onClick={() => resolve(d.id, true)}>Devolver dinero</button>
                <button type="button" className="btn btn-secondary" onClick={() => resolve(d.id, false)}>Rechazar con motivo</button>
              </div>
            </li>
          ))}
        </ul>
      </section>

      <Newsletter />
    </>
  );
}

function Newsletter() {
  const [subject, setSubject] = useState('');
  const [body, setBody] = useState('');
  const [result, setResult] = useState<{ recipients: number; skippedWithoutConsent: number } | null>(null);
  const [error, setError] = useState<unknown>(null);

  const send = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      setResult(await api('/api/admin/newsletter', { method: 'POST', body: { subject, body } }));
    } catch (err) {
      setError(err);
    }
  };

  return (
    <form className="card" onSubmit={send} aria-labelledby="news-title">
      <h2 id="news-title">Enviar boletín</h2>
      <p className="hint">Solo llega a quien aceptó publicidad, y cada correo lleva su enlace de baja.</p>
      <div className="field">
        <label htmlFor="news-subject">Asunto</label>
        <input id="news-subject" required maxLength={200} value={subject} onChange={(e) => setSubject(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="news-body">Mensaje</label>
        <textarea id="news-body" required rows={4} maxLength={3000} value={body} onChange={(e) => setBody(e.target.value)} />
      </div>
      <ErrorAlert error={error} />
      {result && <Alert kind="success">Enviado a {result.recipients}. Omitidos por no tener consentimiento: {result.skippedWithoutConsent}.</Alert>}
      <button type="submit" className="btn btn-primary">Enviar</button>
    </form>
  );
}
