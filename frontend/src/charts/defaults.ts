import { Chart, BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend } from 'chart.js';

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

Chart.defaults.color = '#B0A8B9';
Chart.defaults.borderColor = '#4B4453';
Chart.defaults.font.family = "'Lato', sans-serif";

export const CHART_COLORS = {
  primary: '#845EC2',
  accent: '#FF8066',
  muted: '#B0A8B9',
  danger: '#C34A36',
  grid: '#4B4453',
};
