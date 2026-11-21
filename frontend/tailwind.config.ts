import type { Config } from 'tailwindcss';

const zero = '0px';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    borderRadius: {
      none: zero,
      sm: '4px',
      DEFAULT: '6px',
      md: '6px',
      lg: '8px',
      xl: '8px',
      '2xl': zero,
      '3xl': zero,
      full: zero,
    },
    boxShadow: { none: 'none' },
    backgroundImage: {},
    colors: {
      transparent: 'transparent',
      current: 'currentColor',
      white: 'var(--text)',
      bg: 'var(--bg)',
      surface: 'var(--surface)',
      raised: 'var(--raised)',
      scrim: 'var(--scrim)',
      border: 'var(--border)',
      'border-strong': 'var(--border-strong)',
      text: 'var(--text)',
      'text-secondary': 'var(--text-secondary)',
      'text-subtle': 'var(--text-subtle)',
      primary: 'var(--primary)',
      'primary-hover': 'var(--primary-hover)',
      'primary-pressed': 'var(--primary-pressed)',
      'primary-text': 'var(--primary-text)',
      'primary-subtle': 'var(--primary-subtle)',
      danger: 'var(--danger)',
      'danger-subtle': 'var(--danger-subtle)',
      accent: 'var(--accent)',
      'accent-subtle': 'var(--accent-subtle)',
      highlight: 'var(--highlight)',
    },
    fontFamily: { sans: ['var(--font-sans)'] },
  },
} satisfies Config;
