import { api, readSession } from './api';

const KEY = 'gocomezbo.cookie-choice';
const VERSION = '2026-11-11';

export interface CookieChoice {
  analytics: boolean;
  decidedAt: string;
  version: string;
}

export function readCookieChoice(): CookieChoice | null {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return null;
    const c = JSON.parse(raw) as CookieChoice;
    return c.version === VERSION ? c : null;
  } catch {
    return null;
  }
}

export function saveCookieChoice(analytics: boolean) {
  const choice: CookieChoice = { analytics, decidedAt: new Date().toISOString(), version: VERSION };
  try {
    localStorage.setItem(KEY, JSON.stringify(choice));
  } catch {
  }
  window.dispatchEvent(new Event('gocomezbo:cookie-choice'));
  if (readSession()) {
    api('/api/me/consents/ANALYTICS', { method: 'PUT', body: { granted: analytics, source: 'COOKIE_BANNER' } }).catch(() => {});
  }
}

export function trackPageView(path: string) {
  if (!readCookieChoice()?.analytics) return;
  api('/api/public/analytics/pageview', { method: 'POST', body: { path } }).catch(() => {});
}
