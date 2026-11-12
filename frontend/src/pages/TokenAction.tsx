import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { Alert, ErrorAlert } from '../components/Alert';

function TokenAction({ title, endpoint, auto, buttonLabel, intro }: {
  title: string;
  endpoint: string;
  auto: boolean;
  buttonLabel: string;
  intro: string;
}) {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<unknown>(null);
  const ran = useRef(false);

  const run = async () => {
    setError(null);
    try {
      const res = await api<{ message: string }>(endpoint, { method: 'POST', body: { token } });
      setMessage(res.message);
    } catch (e) {
      setError(e);
    }
  };

  useEffect(() => {
    if (auto && token && !ran.current) {
      ran.current = true;
      run();
    }
  }, [auto, token]);

  return (
    <div className="narrow">
      <h1>{title}</h1>
      {!token && <Alert kind="error">El enlace está incompleto.</Alert>}
      {message ? (
        <Alert kind="success">{message}</Alert>
      ) : (
        <>
          {!auto && <p>{intro}</p>}
          <ErrorAlert error={error} />
          {!auto && token && (
            <button type="button" className="btn btn-primary" onClick={run}>{buttonLabel}</button>
          )}
        </>
      )}
    </div>
  );
}

export function Unsubscribe() {
  return <TokenAction title="Darte de baja" endpoint="/api/public/unsubscribe" auto buttonLabel="" intro="" />;
}

export function GuardianApprove() {
  return (
    <TokenAction
      title="Autorizar cuenta de un menor"
      endpoint="/api/public/guardian/approve"
      auto={false}
      buttonLabel="Autorizo la cuenta"
      intro="Un menor de edad te indicó como su padre, madre o tutor para abrir una cuenta en Banco Gocomezbo. Si estás de acuerdo, confírmalo."
    />
  );
}
