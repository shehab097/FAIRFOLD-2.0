import { Outlet } from 'react-router-dom';
import { Logo } from '../ui/Logo';

const PILLARS = [
  { code: '01', title: 'Anonymised screening', desc: 'Identities stay hidden during initial evaluation so work and skills speak first.' },
  { code: '02', title: 'Explained scores', desc: 'Every score is backed by direct resume evidence and clear, auditable criteria.' },
  { code: '03', title: 'People decide', desc: 'Scores are advisory. Hiring teams review the evidence and make every decision.' },
];

export function AuthShell() {
  return (
    <div className="flex min-h-screen bg-bg">
      <aside className="hidden w-1/2 flex-col justify-between border-r border-border bg-primary-subtle p-10 md:flex">
        <div>
          <Logo />
          <p className="mt-4 max-w-md text-sm text-text-secondary">
            Structured, transparent hiring built for candidates and engineering teams.
          </p>
        </div>
        <div className="space-y-3">
          {PILLARS.map((p) => (
            <div key={p.code} className="border border-border border-l-4 border-l-primary bg-surface p-4">
              <div className="flex items-baseline gap-2">
                <span className="text-xs font-bold uppercase tracking-[0.06em] text-primary-text">{p.code}</span>
                <h2 className="text-base font-semibold text-text">{p.title}</h2>
              </div>
              <p className="mt-1 text-xs text-text-secondary">{p.desc}</p>
            </div>
          ))}
        </div>
        <div className="text-xs text-text-subtle">FairFold Hiring Platform</div>
      </aside>
      <main className="flex w-full flex-col justify-center px-6 py-10 md:w-1/2">
        <div className="mx-auto w-full max-w-md">
          <div className="mb-6 md:hidden">
            <Logo />
          </div>
          <div className="border border-border border-t-2 border-t-primary bg-surface p-6 sm:p-8">
            <Outlet />
          </div>
        </div>
      </main>
    </div>
  );
}
