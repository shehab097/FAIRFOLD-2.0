import { useEffect, useRef } from 'react';
import { sendAttemptEvents, type ProctorEventInput } from '../../../api/assessments';

/**
 * Records tab-hidden and window-blur signals only when the candidate consented.
 * Durations under one second are dropped (merged into noise), events are sent
 * every 10 seconds and again when the attempt is submitted or the page unloads.
 * Nothing here can affect the score or the gate.
 */
export function useProctorEvents(attemptId: string | null, enabled: boolean) {
  const queueRef = useRef<ProctorEventInput[]>([]);
  const startsRef = useRef<Record<'hidden' | 'blur', number | null>>({
    hidden: null,
    blur: null,
  });
  const flushRef = useRef<() => Promise<void>>(async () => {});

  useEffect(() => {
    if (!attemptId || !enabled) return;

    const start = (type: 'hidden' | 'blur') => {
      if (startsRef.current[type] === null) {
        startsRef.current[type] = Date.now();
      }
    };

    const push = (type: 'hidden' | 'blur') => {
      const started = startsRef.current[type];
      if (started === null) return;
      startsRef.current[type] = null;
      const seconds = Math.round((Date.now() - started) / 1000);
      if (seconds < 1) return; // events under one second are merged away
      queueRef.current.push({
        type,
        occurredAt: new Date(started).toISOString(),
        durationSeconds: seconds,
      });
    };

    const flush = async () => {
      const events = queueRef.current;
      if (events.length === 0) return;
      queueRef.current = [];
      try {
        await sendAttemptEvents(attemptId, events);
      } catch {
        // Focus signals are advisory; a failed send must never block the attempt.
      }
    };
    flushRef.current = flush;

    const onVisibility = () => {
      if (document.hidden) start('hidden');
      else push('hidden');
    };
    const onBlur = () => start('blur');
    const onFocus = () => push('blur');

    document.addEventListener('visibilitychange', onVisibility);
    window.addEventListener('blur', onBlur);
    window.addEventListener('focus', onFocus);
    const interval = window.setInterval(() => void flush(), 10_000);

    return () => {
      push('hidden');
      push('blur');
      void flush();
      flushRef.current = async () => {};
      window.clearInterval(interval);
      document.removeEventListener('visibilitychange', onVisibility);
      window.removeEventListener('blur', onBlur);
      window.removeEventListener('focus', onFocus);
    };
  }, [attemptId, enabled]);

  return { flush: () => flushRef.current() };
}
