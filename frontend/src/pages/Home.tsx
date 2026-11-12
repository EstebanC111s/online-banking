import { Link } from 'react-router-dom';
import { api } from '../api';
import { ErrorAlert } from '../components/Alert';
import { useAuth } from '../auth';
import type { PromiseItem } from '../types';
import { useAsync } from '../useAsync';
import { FeesTable } from './Fees';

export default function Home() {
  const { session } = useAuth();
  const promises = useAsync(() => api<PromiseItem[]>('/api/public/promises'), []);

  return (
    <>
      <section className="hero">
        <h1>Banca en línea sin letra pequeña</h1>
        <p className="lead">
          Ves el costo de cada operación antes de confirmarla, puedes descargar o borrar tus datos cuando quieras y
          cada promesa que hacemos viene con su prueba.
        </p>
        <div className="button-row">
          {session ? (
            <Link to="/cuentas" className="btn btn-primary">Ir a mis cuentas</Link>
          ) : (
            <>
              <Link to="/registro" className="btn btn-primary">Abrir cuenta gratis</Link>
              <Link to="/ingresar" className="btn btn-secondary">Ya tengo cuenta</Link>
            </>
          )}
        </div>
        <p className="disclaimer">Proyecto académico: no uses datos reales ni dinero real.</p>
      </section>

      <section aria-labelledby="promises-title">
        <h2 id="promises-title">Nuestras promesas, con evidencia</h2>
        <p>Cada promesa se calcula en vivo con los datos del sistema. Si dejamos de cumplir una, aquí se ve.</p>
        <ErrorAlert error={promises.error} />
        {promises.loading && <p aria-live="polite">Cargando promesas…</p>}
        <ul className="promise-list">
          {promises.data?.map((p) => (
            <li key={p.claim} className="card promise">
              <p className="promise-claim">
                <span className={p.kept ? 'badge badge-ok' : 'badge badge-bad'}>
                  {p.kept ? 'Se cumple' : 'No se está cumpliendo'}
                </span>{' '}
                {p.claim}
              </p>
              <p><strong>Evidencia:</strong> {p.evidence}</p>
              <p className="hint"><strong>Cómo comprobarlo:</strong> {p.howToVerify}</p>
            </li>
          ))}
        </ul>
      </section>

      <section aria-labelledby="fees-title">
        <h2 id="fees-title">Todas nuestras tarifas</h2>
        <p>Esta tabla es la misma que usa el sistema para calcular lo que cobra. No hay otros costos.</p>
        <FeesTable />
      </section>
    </>
  );
}
