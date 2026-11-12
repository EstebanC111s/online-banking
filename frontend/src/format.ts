const cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 2 });

export const money = (n: number) => cop.format(n);

export const dateTime = (iso: string) =>
  new Date(iso).toLocaleString('es-CO', { timeZone: 'America/Bogota', dateStyle: 'medium', timeStyle: 'short' });

export const date = (iso: string) =>
  new Date(iso.length === 10 ? iso + 'T12:00:00' : iso).toLocaleDateString('es-CO', {
    timeZone: 'America/Bogota',
    dateStyle: 'long',
  });

export const maskAccount = (n: string) => `•••• ${n.slice(-4)}`;
