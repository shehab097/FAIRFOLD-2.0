import { Chart, BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend } from 'chart.js';

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

Chart.defaults.color = '#B0A8B9';
Chart.defaults.borderColor = '#4B4453';
Chart.defaults.font.family = "'Lato', sans-serif";

export const CHART_COLORS = {
  primary: '#10B981',
  accent: '#34D399',
  muted: '#94A3B8',
  danger: '#EF4444',
  grid: '#334155',
};
