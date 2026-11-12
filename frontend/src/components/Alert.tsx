import type { ReactNode } from 'react';
import { ApiError } from '../api';

type Kind = 'error' | 'success' | 'info' | 'warning';

export function Alert({ kind = 'info', children }: { kind?: Kind; children: ReactNode }) {
  return (
    <div className={`alert alert-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}

export function ErrorAlert({ error }: { error: unknown }) {
  if (!error) return null;
  const message = error instanceof ApiError ? error.message : 'Ocurrió un error inesperado.';
  return <Alert kind="error">{message}</Alert>;
}
