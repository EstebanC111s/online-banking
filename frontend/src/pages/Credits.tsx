import { api } from '../api';
import { ErrorAlert } from '../components/Alert';
import { useAsync } from '../useAsync';

interface LicenseItem {
  asset: string;
  author: string;
  license: string;
  url?: string;
  usedFor: string;
}

export default function Credits() {
  const items = useAsync(() => api<LicenseItem[]>('/api/public/licenses'), []);
  return (
    <>
      <h1>Créditos y licencias</h1>
      <p>Cada imagen, ícono, fuente y librería que usamos, quién la hizo y con qué licencia.</p>
      <ErrorAlert error={items.error} />
      {items.data && (
        <table>
          <caption className="sr-only">Recursos y sus licencias</caption>
          <thead>
            <tr>
              <th scope="col">Recurso</th>
              <th scope="col">Autor</th>
              <th scope="col">Licencia</th>
              <th scope="col">Dónde se usa</th>
            </tr>
          </thead>
          <tbody>
            {items.data.map((i) => (
              <tr key={i.asset}>
                <th scope="row">{i.url ? <a href={i.url} rel="noopener noreferrer" target="_blank">{i.asset}<span className="sr-only"> (abre en otra pestaña)</span></a> : i.asset}</th>
                <td>{i.author}</td>
                <td>{i.license}</td>
                <td>{i.usedFor}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
