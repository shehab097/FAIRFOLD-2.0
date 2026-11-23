export type Role =
  | 'CANDIDATE'
  | 'EMPLOYER_HR'
  | 'EMPLOYER_MANAGER'
  | 'INTERVIEWER'
  | 'ADMIN';

export interface UserDto {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  companyId: string | null;
  avatarUrl: string | null;
  mfaEnabled: boolean;
}

export interface AuthResponse {
  accessToken: string;
  user: UserDto;
}

export interface MfaChallenge {
  mfaRequired: true;
  mfaToken: string;
}

export type LoginResult = AuthResponse | MfaChallenge;
