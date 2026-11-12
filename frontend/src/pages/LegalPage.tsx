import { useParams } from 'react-router-dom';
import { api } from '../api';
import { ErrorAlert } from '../components/Alert';
import { date } from '../format';
import type { LegalDoc } from '../types';
import { useAsync } from '../useAsync';

export default function LegalPage() {
  const { slug = 'terminos' } = useParams();
  const doc = useAsync(() => api<LegalDoc>(`/api/public/legal/${slug}`), [slug]);

  if (doc.error) return <><h1>Documento no encontrado</h1><ErrorAlert error={doc.error} /></>;
  if (!doc.data) return <p aria-live="polite">Cargando…</p>;

  return (
    <article className="legal">
      <h1>{doc.data.title}</h1>
      <p className="hint">Versión vigente desde el {date(doc.data.version)}</p>
      <p className="disclaimer">Plantilla de un proyecto académico. No constituye asesoría legal.</p>
      {doc.data.sections.map((s) => (
        <section key={s.heading}>
          <h2>{s.heading}</h2>
          <p>{s.body}</p>
        </section>
      ))}
    </article>
  );
}
