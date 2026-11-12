import { api } from '../api';
import { ErrorAlert } from '../components/Alert';
import { date } from '../format';
import { useAsync } from '../useAsync';

interface Integration {
  name: string;
  purpose: string;
  dataShared: string;
  status: string;
  requiresConsent: boolean;
  lastReviewed: string;
}

export default function Integrations() {
  const items = useAsync(() => api<Integration[]>('/api/public/integrations'), []);
  const analytics = useAsync(() => api<Record<string, number>>('/api/public/analytics'), []);
  return (
    <>
      <h1>Integraciones y terceros</h1>
      <p>Todo lo que recibe datos de la app, qué recibe exactamente y si necesita tu permiso. Revisamos esta lista periódicamente.</p>
      <ErrorAlert error={items.error} />
      {items.data && (
        <table>
          <caption className="sr-only">Integraciones</caption>
          <thead>
            <tr>
              <th scope="col">Integración</th>
              <th scope="col">Para qué</th>
              <th scope="col">Qué datos recibe</th>
              <th scope="col">Estado</th>
              <th scope="col">¿Requiere tu permiso?</th>
              <th scope="col">Última revisión</th>
            </tr>
          </thead>
          <tbody>
            {items.data.map((i) => (
              <tr key={i.name}>
                <th scope="row">{i.name}</th>
                <td>{i.purpose}</td>
                <td>{i.dataShared}</td>
                <td>{i.status}</td>
                <td>{i.requiresConsent ? 'Sí' : 'No'}</td>
                <td>{date(i.lastReviewed)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>Lo que mide nuestra analítica, en crudo</h2>
      <p>Solo visitas por página, de quienes la aceptaron. Nada más.</p>
      {analytics.data && Object.keys(analytics.data).length === 0 && <p>Todavía no hay visitas registradas.</p>}
      {analytics.data && Object.keys(analytics.data).length > 0 && (
        <ul>
          {Object.entries(analytics.data).map(([path, count]) => (
            <li key={path}><code>{path}</code>: {count}</li>
          ))}
        </ul>
      )}
    </>
  );
}
