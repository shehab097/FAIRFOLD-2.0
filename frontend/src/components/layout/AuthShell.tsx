import { Outlet } from 'react-router-dom';
import { BarChart3, EyeOff, ShieldCheck, UserCheck, type LucideIcon } from 'lucide-react';
import { Icon } from '../ui/Icon';
import { Logo } from '../ui/Logo';
import { ThemeToggle } from '../ui/ThemeToggle';

const PILLARS: { icon: LucideIcon; label: string }[] = [
  { icon: EyeOff, label: 'Anonymised' },
  { icon: BarChart3, label: 'Explained' },
  { icon: UserCheck, label: 'Human-led' },
];

export function AuthShell() {
  return (
    <div className="flex min-h-screen bg-bg">
      {/* Brand panel */}
      <aside className="hidden w-[46%] flex-col justify-between border-r border-border bg-surface p-10 lg:flex xl:p-14">
        <div className="self-start">
          <Logo />
        </div>

        <div className="mx-auto w-full max-w-md">
          <h2 className="text-[32px] font-bold leading-tight tracking-tight">
            Hire on evidence,
            <br />
            <span className="text-primary-text">not assumptions.</span>
          </h2>

          <div className="mt-8 rounded-2xl bg-bg p-5 shadow-sm ring-1 ring-border">
            <div className="flex items-start justify-between gap-4">
              <div>
                <div className="text-xs text-text-secondary">Screening result</div>
                <div className="mt-0.5 text-sm font-semibold text-text">Senior Backend Engineer</div>
              </div>
              <span className="rounded-full bg-success-subtle px-2.5 py-1 text-sm font-bold text-success">86</span>
            </div>
            <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-raised">
              <div className="h-full w-[86%] rounded-full bg-primary" />
            </div>
            <p className="mt-4 rounded-lg bg-primary-subtle p-3 text-sm text-text">
              “Built a real-time pipeline handling 40k events/sec with Kafka.”
            </p>
            <div className="mt-3 flex items-center gap-1.5 text-xs text-text-secondary">
              <Icon icon={ShieldCheck} size={14} />
              Candidate identity hidden
            </div>
          </div>

          <div className="mt-8 flex flex-wrap gap-2">
            {PILLARS.map((p) => (
              <span
                key={p.label}
                className="inline-flex items-center gap-1.5 rounded-full bg-primary-subtle px-3 py-1.5 text-xs font-medium text-primary-text"
              >
                <Icon icon={p.icon} size={14} />
                {p.label}
              </span>
            ))}
          </div>
        </div>

        <div className="text-xs text-text-subtle">© FairFold</div>
      </aside>

      {/* Form panel */}
      <main className="relative flex flex-1 flex-col">
        <div className="flex items-center justify-between p-4 sm:p-6">
          <div className="lg:invisible">
            <Logo />
          </div>
          <ThemeToggle />
        </div>
        <div className="flex flex-1 items-center justify-center px-6 pb-16">
          <div className="w-full max-w-md">
            <Outlet />
          </div>
        </div>
      </main>
    </div>
  );
}
