import { Link } from 'react-router-dom';
import { api } from '../api';
import { ErrorAlert } from '../components/Alert';
import { date, money } from '../format';
import type { Dispute } from '../types';
import { useAsync } from '../useAsync';

export default function Disputes() {
  const disputes = useAsync(() => api<Dispute[]>('/api/disputes'), []);
  return (
    <>
      <h1>Mis disputas</h1>
      <p>
        Para disputar un cargo, abre la cuenta y usa “Disputar” en el movimiento. Reglas completas en la{' '}
        <Link to="/legal/devoluciones">política de devoluciones</Link>.
      </p>
      <ErrorAlert error={disputes.error} />
      {disputes.data?.length === 0 && <p>No tienes disputas.</p>}
      <ul className="review-list">
        {disputes.data?.map((d) => (
          <li key={d.id} className="card">
            <h2 className="h3">Disputa #{d.id}: {d.transactionType} de {money(d.transactionAmount)}</h2>
            <p>
              <span className={`badge ${d.status === 'REFUNDED' ? 'badge-ok' : d.status === 'REJECTED' ? 'badge-bad' : 'badge-info'}`}>
                {d.statusLabel}
              </span>
            </p>
            <p><strong>Tu motivo:</strong> {d.reason}</p>
            {d.status === 'OPEN' && <p>Te respondemos a más tardar el <strong>{date(d.dueDate)}</strong>.</p>}
            {d.resolutionNote && <p><strong>Respuesta:</strong> {d.resolutionNote}</p>}
          </li>
        ))}
      </ul>
    </>
  );
}
