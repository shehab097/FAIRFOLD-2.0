import { describe, expect, it } from 'vitest';
import { canAccessRole, homePathForRole } from './auth';

describe('homePathForRole', () => {
  it('maps every role to its home route', () => {
    expect(homePathForRole('CANDIDATE')).toBe('/candidate');
    expect(homePathForRole('EMPLOYER_HR')).toBe('/employer');
    expect(homePathForRole('EMPLOYER_MANAGER')).toBe('/employer');
    expect(homePathForRole('INTERVIEWER')).toBe('/employer/interviews');
    expect(homePathForRole('ADMIN')).toBe('/admin');
  });
});

describe('canAccessRole', () => {
  it('allows a role present in the allowed list', () => {
    expect(canAccessRole('EMPLOYER_HR', ['EMPLOYER_HR', 'EMPLOYER_MANAGER'])).toBe(true);
    expect(canAccessRole('CANDIDATE', ['CANDIDATE'])).toBe(true);
  });

  it('denies a role not in the allowed list', () => {
    expect(canAccessRole('CANDIDATE', ['EMPLOYER_HR'])).toBe(false);
  });

  it('denies a missing role', () => {
    expect(canAccessRole(undefined, ['CANDIDATE'])).toBe(false);
  });
});
