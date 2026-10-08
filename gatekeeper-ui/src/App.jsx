import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Organizations from './pages/Organizations'
import Routes_ from './pages/Routes'
import Analytics from './pages/Analytics'

function Protected({ children }) {
  const { user } = useAuth()
  return user ? children : <Navigate to="/login" replace />
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={
        <Protected>
          <Layout><Dashboard /></Layout>
        </Protected>
      } />
      <Route path="/organizations" element={
        <Protected>
          <Layout><Organizations /></Layout>
        </Protected>
      } />
      <Route path="/organizations/:orgId/routes" element={
        <Protected>
          <Layout><Routes_ /></Layout>
        </Protected>
      } />
      <Route path="/analytics" element={
        <Protected>
          <Layout><Analytics /></Layout>
        </Protected>
      } />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}
