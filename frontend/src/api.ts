
export const TOKEN_KEY = 'gocomezbo.session';

export class ApiError extends Error {
  status: number;
  code?: string;
  fields?: Record<string, string>;

  constructor(status: number, message: string, code?: string, fields?: Record<string, string>) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = fields;
  }
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE';

export async function api<T>(path: string, options: { method?: Method; body?: unknown } = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  const session = readSession();
  if (session) headers.Authorization = `Bearer ${session.token}`;
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';

  let res: Response;
  try {
    res = await fetch(path, {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'No pudimos conectar con el servidor. Revisa que el backend esté encendido.');
  }

  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const data = text ? safeJson(text) : null;

  if (!res.ok) {
    if (res.status === 401 && session) {
      window.dispatchEvent(new Event('gocomezbo:session-expired'));
    }
    const message = (data && (data.detail || data.message)) || `Error ${res.status}`;
    throw new ApiError(res.status, message, data?.code, data?.fields);
  }
  return data as T;
}

function safeJson(text: string) {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

export interface Session {
  token: string;
  expiresAt: string;
  fullName: string;
  role: 'CUSTOMER' | 'ADMIN';
}

export function readSession(): Session | null {
  try {
    const raw = sessionStorage.getItem(TOKEN_KEY);
    if (!raw) return null;
    const s = JSON.parse(raw) as Session;
    if (new Date(s.expiresAt).getTime() <= Date.now()) {
      sessionStorage.removeItem(TOKEN_KEY);
      return null;
    }
    return s;
  } catch {
    return null;
  }
}

export function writeSession(s: Session | null) {
  try {
    if (s) sessionStorage.setItem(TOKEN_KEY, JSON.stringify(s));
    else sessionStorage.removeItem(TOKEN_KEY);
  } catch {
  }
}
