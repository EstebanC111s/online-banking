import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { api, ApiError } from '../api';
import { Alert, ErrorAlert } from '../components/Alert';
import { useAuth } from '../auth';
import { date } from '../format';
import type { ReviewSummary, ReviewView } from '../types';
import { useAsync } from '../useAsync';

function Stars({ rating }: { rating: number }) {
  return (
    <span className="stars" role="img" aria-label={`${rating} de 5 estrellas`}>
      <span aria-hidden="true">{'★'.repeat(rating)}{'☆'.repeat(5 - rating)}</span>
    </span>
  );
}

export default function Reviews() {
  const { session } = useAuth();
  const summary = useAsync(() => api<ReviewSummary>('/api/public/reviews'), []);

  return (
    <>
      <h1>Reseñas de clientes</h1>
      <p>
        Solo pueden opinar clientes con movimientos reales. Publicamos todas, también las malas, y el promedio se
        calcula con todas. No pagamos ni inventamos reseñas.
      </p>
      <ErrorAlert error={summary.error} />
      {summary.data && (
        <>
          <section aria-labelledby="summary-title" className="card">
            <h2 id="summary-title">Resumen</h2>
            {summary.data.count === 0 ? (
              <p>Todavía no hay reseñas. Preferimos mostrar cero a inventarlas.</p>
            ) : (
              <>
                <p className="big-number">
                  {summary.data.average.toFixed(1)} <Stars rating={Math.round(summary.data.average)} />
                </p>
                <p>{summary.data.count} reseñas en total</p>
                <ul className="distribution">
                  {[5, 4, 3, 2, 1].map((n) => (
                    <li key={n}>{n} estrellas: {summary.data!.distribution[String(n)] ?? 0}</li>
                  ))}
                </ul>
              </>
            )}
          </section>

          {session ? (
            <MyReview onSaved={summary.reload} />
          ) : (
            <p><Link to="/ingresar">Inicia sesión</Link> para dejar tu reseña.</p>
          )}

          <section aria-labelledby="list-title">
            <h2 id="list-title">Todas las reseñas</h2>
            <ul className="review-list">
              {summary.data.reviews.map((r: ReviewView) => (
                <li key={r.id} className="card">
                  <p>
                    <Stars rating={r.rating} /> <strong>{r.authorName}</strong>{' '}
                    {r.verifiedCustomer && <span className="badge badge-ok">Cliente verificado</span>}
                  </p>
                  <p>{r.comment}</p>
                  <p className="hint">{date(r.createdAt)}{r.editedAt ? ' · editada' : ''}</p>
                </li>
              ))}
            </ul>
          </section>
        </>
      )}
    </>
  );
}

function MyReview({ onSaved }: { onSaved: () => void }) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSaved(false);
    if (rating === 0) {
      setError(new ApiError(400, 'Elige una calificación de 1 a 5 estrellas.'));
      return;
    }
    try {
      await api('/api/reviews/mine', { method: 'PUT', body: { rating, comment } });
      setSaved(true);
      onSaved();
    } catch (err) {
      setError(err);
    }
  };

  return (
    <form className="card" onSubmit={submit} aria-labelledby="my-review-title">
      <h2 id="my-review-title">Tu reseña</h2>
      <fieldset>
        <legend>Calificación</legend>
        <div className="radio-row">
          {[1, 2, 3, 4, 5].map((n) => (
            <label key={n}>
              <input type="radio" name="rating" value={n} checked={rating === n} onChange={() => setRating(n)} /> {n}
            </label>
          ))}
        </div>
      </fieldset>
      <div className="field">
        <label htmlFor="review-comment">Comentario</label>
        <textarea id="review-comment" value={comment} onChange={(e) => setComment(e.target.value)} minLength={10} maxLength={600} required rows={4} />
      </div>
      <ErrorAlert error={error} />
      {saved && <Alert kind="success">Reseña publicada.</Alert>}
      <button type="submit" className="btn btn-primary">Publicar</button>
    </form>
  );
}
