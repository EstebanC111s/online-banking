import { api } from '../api';
import { ErrorAlert } from '../components/Alert';
import { useAsync } from '../useAsync';

interface CompanyInfo {
  legalName: string;
  taxId: string;
  address: string;
  email: string;
  supportHours: string;
  supervisor: string;
  disclaimer: string;
}

export default function Company() {
  const info = useAsync(() => api<CompanyInfo>('/api/public/company'), []);
  return (
    <>
      <h1>Datos de la empresa</h1>
      <ErrorAlert error={info.error} />
      {info.data && (
        <>
          <dl className="details">
            <dt>Razón social</dt><dd>{info.data.legalName}</dd>
            <dt>Identificación tributaria</dt><dd>{info.data.taxId}</dd>
            <dt>Domicilio</dt><dd>{info.data.address}</dd>
            <dt>Contacto</dt><dd><a href={`mailto:${info.data.email}`}>{info.data.email}</a></dd>
            <dt>Horario de atención</dt><dd>{info.data.supportHours}</dd>
            <dt>Ente de vigilancia</dt><dd>{info.data.supervisor}</dd>
          </dl>
          <p className="disclaimer">{info.data.disclaimer}</p>
        </>
      )}
    </>
  );
}
