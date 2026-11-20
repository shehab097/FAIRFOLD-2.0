import { Component, type ReactNode } from 'react';

interface State {
  error: Error | null;
}

export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  render() {
    if (this.state.error) {
      return (
        <div className="flex min-h-screen items-center justify-center bg-bg p-6 text-center">
          <div>
            <div className="text-[32px] font-bold text-text">Something went wrong</div>
            <p className="mt-2 text-text-secondary">{this.state.error.message}</p>
            <button
              className="mt-6 text-primary-text hover:underline"
              onClick={() => window.location.reload()}
            >
              Reload
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
