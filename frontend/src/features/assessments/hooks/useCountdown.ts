import { useEffect, useState } from 'react';

/**
 * Seconds left in an attempt. `offsetMs` is the server-client clock difference
 * captured when the attempt starts (`serverTime - Date.now()`); using it means
 * a wrong machine clock cannot shorten or extend the timer.
 */
export function useCountdown(endsAt: string | null | undefined, offsetMs: number): number | null {
  const [remaining, setRemaining] = useState<number | null>(null);

  useEffect(() => {
    if (!endsAt) {
      setRemaining(null);
      return;
    }
    const end = new Date(endsAt).getTime();
    if (Number.isNaN(end)) {
      setRemaining(null);
      return;
    }
    const tick = () => {
      setRemaining(Math.max(0, Math.ceil((end - (Date.now() + offsetMs)) / 1000)));
    };
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [endsAt, offsetMs]);

  return remaining;
}

export function formatRemaining(seconds: number | null): string {
  if (seconds === null) return '—';
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${minutes}:${String(rest).padStart(2, '0')}`;
}
