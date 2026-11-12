import { api } from '../api';
import { ErrorAlert } from '../components/Alert';
import { dateTime } from '../format';
import { useAsync } from '../useAsync';

interface Message {
  id: number;
  recipient: string;
  subject: string;
  body: string;
  kind: 'TRANSACTIONAL' | 'MARKETING';
  createdAt: string;
}

export default function DevOutbox() {
  const messages = useAsync(() => api<Message[]>('/api/dev/outbox'), []);
  return (
    <>
      <h1>Correos simulados (desarrollo)</h1>
      <p className="hint">Esta app no envía correos reales. Aquí ves lo que se habría enviado. Desactívalo en producción (app.dev-outbox=false).</p>
      <button type="button" className="btn btn-secondary btn-small" onClick={messages.reload}>Actualizar</button>
      <ErrorAlert error={messages.error} />
      <ul className="review-list">
        {messages.data?.map((m) => (
          <li key={m.id} className="card">
            <p className="hint">{dateTime(m.createdAt)} · para {m.recipient} · {m.kind === 'MARKETING' ? 'Publicidad' : 'Operativo'}</p>
            <h2 className="h3">{m.subject}</h2>
            <p className="pre">{linkify(m.body)}</p>
          </li>
        ))}
      </ul>
    </>
  );
}

function linkify(text: string) {
  return text.split(/(https?:\/\/\S+)/g).map((part, i) =>
    /^https?:\/\//.test(part) ? <a key={i} href={part}>{part}</a> : <span key={i}>{part}</span>,
  );
}
