import '@fontsource-variable/inter';
import './styles/index.css';

import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { AuthProvider } from './app/auth/AuthContext';
import { ThemeProvider } from './app/theme/ThemeContext';
import { ErrorBoundary } from './components/ErrorBoundary';
import { ToastProvider } from './components/ui/Toast';

const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ThemeProvider>
          <AuthProvider>
            <ToastProvider>
              <ErrorBoundary>
                <App />
              </ErrorBoundary>
            </ToastProvider>
          </AuthProvider>
        </ThemeProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
);
