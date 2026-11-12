import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { RequireAuth } from './components/RequireAuth';
import AccountDetail from './pages/AccountDetail';
import Accounts from './pages/Accounts';
import Admin from './pages/Admin';
import Company from './pages/Company';
import Credits from './pages/Credits';
import DevOutbox from './pages/DevOutbox';
import Disputes from './pages/Disputes';
import Fees from './pages/Fees';
import Home from './pages/Home';
import Integrations from './pages/Integrations';
import LegalPage from './pages/LegalPage';
import Login from './pages/Login';
import NotFound from './pages/NotFound';
import Privacy from './pages/Privacy';
import Register from './pages/Register';
import Reviews from './pages/Reviews';
import { GuardianApprove, Unsubscribe } from './pages/TokenAction';
import Transfer from './pages/Transfer';

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/ingresar" element={<Login />} />
        <Route path="/registro" element={<Register />} />
        <Route path="/resenas" element={<Reviews />} />
        <Route path="/tarifas" element={<Fees />} />
        <Route path="/empresa" element={<Company />} />
        <Route path="/creditos" element={<Credits />} />
        <Route path="/integraciones" element={<Integrations />} />
        <Route path="/legal/:slug" element={<LegalPage />} />
        <Route path="/baja" element={<Unsubscribe />} />
        <Route path="/tutor" element={<GuardianApprove />} />
        <Route path="/dev/correos" element={<DevOutbox />} />

        <Route path="/cuentas" element={<RequireAuth><Accounts /></RequireAuth>} />
        <Route path="/cuentas/:id" element={<RequireAuth><AccountDetail /></RequireAuth>} />
        <Route path="/transferir" element={<RequireAuth><Transfer /></RequireAuth>} />
        <Route path="/disputas" element={<RequireAuth><Disputes /></RequireAuth>} />
        <Route path="/privacidad" element={<RequireAuth><Privacy /></RequireAuth>} />
        <Route path="/admin" element={<RequireAuth admin><Admin /></RequireAuth>} />

        <Route path="/terminos" element={<Navigate to="/legal/terminos" replace />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Layout>
  );
}
