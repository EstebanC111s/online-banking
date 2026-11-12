import { api } from '../api';
import { ErrorAlert } from '../components/Alert';
import { money } from '../format';
import type { FeeItem } from '../types';
import { useAsync } from '../useAsync';

export function FeesTable() {
  const fees = useAsync(() => api<FeeItem[]>('/api/public/fees'), []);
  if (fees.error) return <ErrorAlert error={fees.error} />;
  if (!fees.data) return <p aria-live="polite">Cargando tarifas…</p>;
  return (
    <table>
      <caption className="sr-only">Tarifas de Banco Gocomezbo</caption>
      <thead>
        <tr>
          <th scope="col">Concepto</th>
          <th scope="col" className="num">Valor</th>
          <th scope="col">Nota</th>
        </tr>
      </thead>
      <tbody>
        {fees.data.map((f) => (
          <tr key={f.concept}>
            <th scope="row">{f.concept}</th>
            <td className="num">{money(f.amount)}</td>
            <td>{f.note}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default function Fees() {
  return (
    <>
      <h1>Tarifas</h1>
      <p>Sin costos ocultos: lo que no está en esta tabla no se cobra. Cada transferencia te muestra la comisión y el total antes de confirmarla.</p>
      <FeesTable />
    </>
  );
}
