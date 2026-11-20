import axios from 'axios';
import { api, setAccessToken } from './client';
import type { AuthResponse } from '../features/auth/types';

export interface RegisterInput {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: 'candidate' | 'employer';
  consent: boolean;
}

export interface ForgotPasswordInput {
  email: string;
}

export async function bootstrapSession(): Promise<AuthResponse | null> {
  try {
    const { data } = await axios.post<AuthResponse>('/api/auth/refresh', null, {
      baseURL: '/api',
      withCredentials: true,
    });
    setAccessToken(data.accessToken);
    return data;
  } catch {
    return null;
  }
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/api/auth/login', { email, password });
  setAccessToken(data.accessToken);
  return data;
}

export async function register(input: RegisterInput): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/api/auth/register', input);
  setAccessToken(data.accessToken);
  return data;
}

export async function logout(): Promise<void> {
  try {
    await api.post('/api/auth/logout');
  } finally {
    setAccessToken(null);
  }
}

export async function forgotPassword(email: string): Promise<void> {
  await api.post('/api/auth/forgot-password', { email });
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  await api.post('/api/auth/reset-password', { token, newPassword });
}
