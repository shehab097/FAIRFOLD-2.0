import { useAuth } from '../app/auth/AuthContext';
import { Button } from '../components/ui/Button';

export function WelcomePage() {
  const { user, signOut } = useAuth();

  return (
    <div>
      <h1 className="text-[28px] font-bold leading-9">Welcome, {user?.firstName}</h1>
      <p className="text-text-secondary">
        Signed in as {user?.email} ({user?.role}).
      </p>
      <Button variant="secondary" onClick={() => signOut()} className="mt-6">
        Sign out
      </Button>
    </div>
  );
}
