import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { AuthResponse } from '../features/auth/types';

// No baseURL: every endpoint is an explicit relative path starting with /api,
// so the Vite proxy forwards it to the backend (single origin, no CORS).
export const api = axios.create({
  withCredentials: true,
});

let accessToken: string | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

let refreshPromise: Promise<AuthResponse | null> | null = null;

/**
 * Deduplicated token refresh: ensures only one /api/auth/refresh request
 * is ever in-flight across bootstrap, interceptors, and concurrent callers.
 */
export async function requestTokenRefresh(): Promise<AuthResponse | null> {
  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = axios
    .post<AuthResponse>('/api/auth/refresh', null, {
      withCredentials: true,
    })
    .then((res) => {
      setAccessToken(res.data.accessToken);
      return res.data;
    })
    .catch((err: AxiosError) => {
      if (err.response?.status === 401) {
        setAccessToken(null);
      }
      return null;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

export function notifySessionExpired(): void {
  setAccessToken(null);
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('auth:session-expired'));
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined;
    const url = original?.url ?? '';
    const isAuthEndpoint =
      url.includes('/auth/login') || url.includes('/auth/register') || url.includes('/auth/refresh');

    if (error.response?.status === 401 && original && !original._retry && !isAuthEndpoint) {
      original._retry = true;
      const session = await requestTokenRefresh();
      if (session?.accessToken) {
        original.headers.Authorization = `Bearer ${session.accessToken}`;
        return api(original);
      }
      // Refresh failed with 401; notify auth context cleanly without hard window reload
      notifySessionExpired();
    }
    return Promise.reject(error);
  },
);
