import { useMemo, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { api, ApiError } from '../api';
import { Alert } from '../components/Alert';
import { Field } from '../components/Field';

function ageFrom(birth: string): number | null {
  if (!birth) return null;
  const b = new Date(birth + 'T12:00:00');
  const now = new Date();
  let age = now.getFullYear() - b.getFullYear();
  const m = now.getMonth() - b.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < b.getDate())) age--;
  return age;
}

export default function Register() {
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    birthDate: '',
    guardianEmail: '',
    acceptTerms: false,
    acceptPrivacy: false,
    marketingOptIn: false,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [globalError, setGlobalError] = useState<string | null>(null);
  const [done, setDone] = useState<string | null>(null);

  const age = useMemo(() => ageFrom(form.birthDate), [form.birthDate]);
  const isMinor = age !== null && age < 18;

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setErrors({});
    setGlobalError(null);
    try {
      const res = await api<{ status: string; message: string }>('/api/auth/register', {
        method: 'POST',
        body: { ...form, guardianEmail: isMinor ? form.guardianEmail : null },
      });
      setDone(res.status);
      setMessage(res.message);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(err.fields ?? {});
        setGlobalError(err.message);
      } else {
        setGlobalError('No se pudo crear la cuenta.');
      }
    }
  };

  if (done) {
    return (
      <div className="narrow">
        <h1>{done === 'PENDING_GUARDIAN' ? 'Falta la autorización de tu tutor' : '¡Listo!'}</h1>
        <Alert kind="success">{message}</Alert>
        {done !== 'PENDING_GUARDIAN' && <Link className="btn btn-primary" to="/ingresar">Ingresar</Link>}
      </div>
    );
  }

  return (
    <div className="narrow">
      <h1>Abrir cuenta</h1>
      <p>Solo te pedimos lo necesario. Abrir y mantener la cuenta cuesta $0.</p>
      {globalError && <Alert kind="error">{globalError}</Alert>}
      <form onSubmit={submit} noValidate>
        <Field id="fullName" label="Nombre completo" autoComplete="name" required value={form.fullName} onChange={set('fullName')} error={errors.fullName} />
        <Field id="email" label="Correo" type="email" autoComplete="email" required value={form.email} onChange={set('email')} error={errors.email}
          hint="Lo usas para ingresar y para recibir avisos de tus movimientos." />
        <Field id="password" label="Contraseña" type="password" autoComplete="new-password" required value={form.password} onChange={set('password')} error={errors.password}
          hint="Mínimo 8 caracteres, con letras y números." />
        <Field id="birthDate" label="Fecha de nacimiento" type="date" required value={form.birthDate} onChange={set('birthDate')} error={errors.birthDate}
          hint="Solo para verificar tu edad. Si eres menor de 18, tu padre, madre o tutor debe autorizar la cuenta." />

        {isMinor && (
          <Field id="guardianEmail" label="Correo de tu padre, madre o tutor" type="email" required value={form.guardianEmail} onChange={set('guardianEmail')}
            error={errors.guardianEmail} hint="Le enviaremos un enlace para que autorice tu cuenta. No podrás operar hasta entonces." />
        )}

        <fieldset>
          <legend>Documentos</legend>
          <label className="check">
            <input type="checkbox" checked={form.acceptTerms} onChange={set('acceptTerms')} aria-describedby={errors.acceptTerms ? 'terms-error' : undefined} />
            Acepto los <Link to="/legal/terminos" target="_blank">términos de uso<span className="sr-only"> (abre en otra pestaña)</span></Link>
          </label>
          {errors.acceptTerms && <p className="field-error" id="terms-error">{errors.acceptTerms}</p>}
          <label className="check">
            <input type="checkbox" checked={form.acceptPrivacy} onChange={set('acceptPrivacy')} aria-describedby={errors.acceptPrivacy ? 'privacy-error' : undefined} />
            Acepto la <Link to="/legal/privacidad" target="_blank">política de privacidad<span className="sr-only"> (abre en otra pestaña)</span></Link>
          </label>
          {errors.acceptPrivacy && <p className="field-error" id="privacy-error">{errors.acceptPrivacy}</p>}
        </fieldset>

        <fieldset>
          <legend>Opcional</legend>
          <label className="check">
            <input type="checkbox" checked={form.marketingOptIn} onChange={set('marketingOptIn')} />
            Quiero recibir correos con novedades (puedes darte de baja con un clic cuando quieras)
          </label>
        </fieldset>

        <button type="submit" className="btn btn-primary">Crear cuenta</button>
      </form>
    </div>
  );
}
