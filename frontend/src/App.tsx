import { Route, Routes } from 'react-router-dom';
import { PublicShell } from './components/layout/PublicShell';
import { AuthShell } from './components/layout/AuthShell';
import { PortalShell } from './components/layout/PortalShell';
import { GuestOnly, RequireAuth, RequireRole } from './app/guards';
import { HomePage } from './pages/HomePage';
import { NotFoundPage } from './pages/NotFoundPage';
import { WelcomePage } from './pages/WelcomePage';
import { LoginPage } from './features/auth/pages/LoginPage';
import { RegisterPage } from './features/auth/pages/RegisterPage';
import { ForgotPasswordPage } from './features/auth/pages/ForgotPasswordPage';
import { ResetPasswordPage } from './features/auth/pages/ResetPasswordPage';

export default function App() {
  return (
    <Routes>
      <Route element={<PublicShell />}>
        <Route path="/" element={<HomePage />} />
      </Route>

      <Route
        element={
          <GuestOnly>
            <AuthShell />
          </GuestOnly>
        }
      >
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
      </Route>

      <Route
        element={
          <RequireAuth>
            <PortalShell />
          </RequireAuth>
        }
      >
        <Route
          path="/candidate"
          element={
            <RequireRole roles={['CANDIDATE']}>
              <WelcomePage />
            </RequireRole>
          }
        />
        <Route
          path="/employer"
          element={
            <RequireRole roles={['EMPLOYER_HR', 'EMPLOYER_MANAGER']}>
              <WelcomePage />
            </RequireRole>
          }
        />
        <Route
          path="/employer/interviews"
          element={
            <RequireRole roles={['EMPLOYER_HR', 'EMPLOYER_MANAGER', 'INTERVIEWER']}>
              <WelcomePage />
            </RequireRole>
          }
        />
        <Route
          path="/admin"
          element={
            <RequireRole roles={['ADMIN']}>
              <WelcomePage />
            </RequireRole>
          }
        />
      </Route>

      <Route path="/403" element={<NotFoundPage code="403" />} />
      <Route path="*" element={<NotFoundPage code="404" />} />
    </Routes>
  );
}
