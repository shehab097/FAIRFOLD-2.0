import { describe, expect, it } from 'vitest';
import { getApiErrorMessage, getFieldErrors } from './errors';

describe('getApiErrorMessage', () => {
  it('reads the message from a raw ApiError', () => {
    expect(
      getApiErrorMessage({
        timestamp: 'x',
        status: 409,
        error: 'Conflict',
        code: 'EMAIL_TAKEN',
        message: 'An account with this email already exists.',
      }),
    ).toBe('An account with this email already exists.');
  });

  it('reads the message from an Axios-wrapped error', () => {
    const error = {
      response: {
        data: {
          timestamp: 'x',
          status: 423,
          error: 'Locked',
          code: 'ACCOUNT_LOCKED',
          message: 'Account locked. Try again in 15 minutes.',
        },
      },
    };
    expect(getApiErrorMessage(error)).toBe('Account locked. Try again in 15 minutes.');
  });

  it('falls back to the Error message and then a default', () => {
    expect(getApiErrorMessage(new Error('boom'))).toBe('boom');
    expect(getApiErrorMessage('nope')).toBe('Something went wrong.');
  });
});

describe('getFieldErrors', () => {
  it('maps validation field errors to a record', () => {
    const error = {
      response: {
        data: {
          timestamp: 'x',
          status: 400,
          error: 'Bad Request',
          code: 'VALIDATION',
          message: 'Validation failed',
          errors: [{ field: 'password', message: 'Password must be at least 10 characters' }],
        },
      },
    };
    expect(getFieldErrors(error)).toEqual({
      password: 'Password must be at least 10 characters',
    });
  });

  it('returns an empty record when there are no field errors', () => {
    expect(
      getFieldErrors({
        timestamp: 'x',
        status: 409,
        error: 'Conflict',
        code: 'EMAIL_TAKEN',
        message: 'Taken',
      }),
    ).toEqual({});
  });
});
