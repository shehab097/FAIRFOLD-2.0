export interface FieldError {
  field: string;
  message: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  errors?: FieldError[];
  retryAvailableAt?: string | null;
}

export function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === 'object' &&
    value !== null &&
    'status' in value &&
    'message' in value
  );
}

/** Pulls the ApiError out of either a raw response body or an Axios error. */
export function extractApiError(error: unknown): ApiError | null {
  if (isApiError(error)) return error;
  const wrapped = error as { response?: { data?: unknown } } | undefined;
  if (wrapped?.response?.data && isApiError(wrapped.response.data)) {
    return wrapped.response.data;
  }
  return null;
}

export function getApiErrorMessage(error: unknown): string {
  const apiError = extractApiError(error);
  if (apiError) return apiError.message;
  if (error instanceof Error) return error.message;
  return 'Something went wrong.';
}

export function getApiErrorCode(error: unknown): string | null {
  return extractApiError(error)?.code ?? null;
}

export function getFieldErrors(error: unknown): Record<string, string> {
  const apiError = extractApiError(error);
  if (!apiError?.errors) return {};
  const result: Record<string, string> = {};
  for (const fe of apiError.errors) {
    result[fe.field] = fe.message;
  }
  return result;
}
