import { Link } from 'react-router-dom';
import { useAuth } from '../app/auth/AuthContext';
import { homePathForRole } from '../lib/auth';

export function NotFoundPage({ code }: { code: '403' | '404' }) {
  const { user } = useAuth();
  const target = user ? homePathForRole(user.role) : '/';
  const text =
    code === '403' ? 'You do not have access to this page.' : 'This page does not exist.';

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-bg px-6 text-center">
      <div className="text-[32px] font-bold leading-10 text-text">{code}</div>
      <p className="mt-2 text-text-secondary">{text}</p>
      <Link to={target} className="mt-6 text-primary-text hover:underline">
        Go to {user ? 'your home' : 'the homepage'}
      </Link>
    </div>
  );
}
