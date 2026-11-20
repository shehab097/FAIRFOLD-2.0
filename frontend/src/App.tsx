import { Route, Routes } from 'react-router-dom';
import { PublicShell } from './components/layout/PublicShell';
import { HomePage } from './pages/HomePage';

export default function App() {
  return (
    <Routes>
      <Route element={<PublicShell />}>
        <Route path="/" element={<HomePage />} />
      </Route>
    </Routes>
  );
}
