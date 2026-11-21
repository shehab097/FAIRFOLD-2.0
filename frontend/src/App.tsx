import { Navigate, Route, Routes } from 'react-router-dom';
import { PublicShell } from './components/layout/PublicShell';
import { AuthShell } from './components/layout/AuthShell';
import { PortalShell } from './components/layout/PortalShell';
import { GuestOnly, HomeRedirect, RequireAuth, RequireRole } from './app/guards';
import { NotFoundPage } from './pages/NotFoundPage';
import { WelcomePage } from './pages/WelcomePage';
import { LoginPage } from './features/auth/pages/LoginPage';
import { RegisterPage } from './features/auth/pages/RegisterPage';
import { ForgotPasswordPage } from './features/auth/pages/ForgotPasswordPage';
import { ResetPasswordPage } from './features/auth/pages/ResetPasswordPage';
import { JobBoardPage } from './features/jobs/pages/JobBoardPage';
import { JobDetailPage } from './features/jobs/pages/JobDetailPage';
import { CompanyProfilePage } from './features/companies/pages/CompanyProfilePage';
import { CompanyPage } from './features/employer/pages/CompanyPage';
import { JobsPage } from './features/employer/pages/JobsPage';
import { JobEditorPage } from './features/employer/pages/JobEditorPage';
import { JobWorkspacePage } from './features/employer/pages/JobWorkspacePage';
import { DashboardPage } from './features/candidate/pages/DashboardPage';
import { ProfilePage } from './features/candidate/pages/ProfilePage';
import { ResumePage } from './features/candidate/pages/ResumePage';
import { ApplicationsPage } from './features/candidate/pages/ApplicationsPage';
import { ApplicationDetailPage } from './features/candidate/pages/ApplicationDetailPage';
import { NotificationsPage } from './features/notifications/pages/NotificationsPage';
import { AccountPage } from './features/account/pages/AccountPage';
import { ChangePasswordPage } from './features/account/pages/ChangePasswordPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />

      <Route element={<PublicShell />}>
        <Route path="/jobs" element={<JobBoardPage />} />
        <Route path="/jobs/:jobId" element={<JobDetailPage />} />
        <Route path="/companies/:companyId" element={<CompanyProfilePage />} />
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
              <DashboardPage />
            </RequireRole>
          }
        />
        <Route
          path="/candidate/profile"
          element={
            <RequireRole roles={['CANDIDATE']}>
              <ProfilePage />
            </RequireRole>
          }
        />
        <Route
          path="/candidate/resume"
          element={
            <RequireRole roles={['CANDIDATE']}>
              <ResumePage />
            </RequireRole>
          }
        />
        <Route
          path="/candidate/applications"
          element={
            <RequireRole roles={['CANDIDATE']}>
              <ApplicationsPage />
            </RequireRole>
          }
        />
        <Route
          path="/candidate/applications/:applicationId"
          element={
            <RequireRole roles={['CANDIDATE']}>
              <ApplicationDetailPage />
            </RequireRole>
          }
        />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/account" element={<AccountPage />} />
        <Route path="/account/password" element={<ChangePasswordPage />} />
        <Route
          path="/employer"
          element={
            <RequireRole roles={['EMPLOYER_HR', 'EMPLOYER_MANAGER']}>
              <Navigate to="/employer/jobs" replace />
            </RequireRole>
          }
        />
        <Route
          path="/employer/jobs"
          element={
            <RequireRole roles={['EMPLOYER_HR', 'EMPLOYER_MANAGER']}>
              <JobsPage />
            </RequireRole>
          }
        />
        <Route
          path="/employer/jobs/new"
          element={
            <RequireRole roles={['EMPLOYER_HR']}>
              <JobEditorPage />
            </RequireRole>
          }
        />
        <Route
          path="/employer/jobs/:jobId/edit"
          element={
            <RequireRole roles={['EMPLOYER_HR']}>
              <JobEditorPage />
            </RequireRole>
          }
        />
        <Route
          path="/employer/jobs/:jobId"
          element={
            <RequireRole roles={['EMPLOYER_HR', 'EMPLOYER_MANAGER']}>
              <JobWorkspacePage />
            </RequireRole>
          }
        />
        <Route
          path="/employer/company"
          element={
            <RequireRole roles={['EMPLOYER_HR']}>
              <CompanyPage />
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
