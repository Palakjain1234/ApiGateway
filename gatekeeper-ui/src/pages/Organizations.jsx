import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getOrganizations, createOrganization,
  activateOrg, suspendOrg, deleteOrg, createTenantAdmin
} from '../api/organizations'
import Badge from '../components/Badge'
import Modal from '../components/Modal'
import Spinner from '../components/Spinner'

export default function Organizations() {
  const [orgs, setOrgs]         = useState([])
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [showAdmin, setShowAdmin]   = useState(null)  // org object
  const [form, setForm]   = useState({ name: '', slug: '' })
  const [adminForm, setAdminForm] = useState({ username: '', password: '' })

  const load = () => {
    setLoading(true)
    getOrganizations()
      .then(r => setOrgs(r.data?.data ?? []))
      .catch(() => setError('Failed to load organizations'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const handleCreate = async e => {
    e.preventDefault()
    try {
      await createOrganization(form)
      setShowCreate(false)
      setForm({ name: '', slug: '' })
      load()
    } catch (err) {
      setError(err.response?.data?.error ?? 'Create failed')
    }
  }

  const handleCreateAdmin = async e => {
    e.preventDefault()
    try {
      await createTenantAdmin(showAdmin.id, adminForm)
      setShowAdmin(null)
      setAdminForm({ username: '', password: '' })
      alert('Tenant admin created successfully')
    } catch (err) {
      setError(err.response?.data?.error ?? 'Failed to create admin')
    }
  }

  const action = async (fn, id) => {
    try { await fn(id); load() }
    catch (err) { setError(err.response?.data?.error ?? 'Action failed') }
  }

  if (loading) return <Spinner />

  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900">Organizations</h2>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium"
        >
          + New Organization
        </button>
      </div>

      {error && <p className="mb-4 text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2 text-sm">{error}</p>}

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 uppercase text-xs">
            <tr>
              {['Name','Slug','Status','Created','Actions'].map(h => (
                <th key={h} className="px-6 py-3 text-left font-medium">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {orgs.map(org => (
              <tr key={org.id} className="hover:bg-gray-50">
                <td className="px-6 py-3 font-medium">
                  <Link to={`/organizations/${org.id}/routes`} className="hover:text-blue-600">
                    {org.name}
                  </Link>
                </td>
                <td className="px-6 py-3 text-gray-500 font-mono text-xs">{org.slug}</td>
                <td className="px-6 py-3"><Badge value={org.status} /></td>
                <td className="px-6 py-3 text-gray-500 text-xs">
                  {new Date(org.createdAt).toLocaleDateString()}
                </td>
                <td className="px-6 py-3">
                  <div className="flex gap-2 flex-wrap">
                    {org.status !== 'ACTIVE' && org.status !== 'DELETED' && (
                      <button onClick={() => action(activateOrg, org.id)}
                        className="text-xs text-green-700 hover:underline">Activate</button>
                    )}
                    {org.status === 'ACTIVE' && (
                      <button onClick={() => action(suspendOrg, org.id)}
                        className="text-xs text-orange-600 hover:underline">Suspend</button>
                    )}
                    {org.status !== 'DELETED' && (
                      <>
                        <button onClick={() => setShowAdmin(org)}
                          className="text-xs text-blue-600 hover:underline">Add Admin</button>
                        <button onClick={() => { if(confirm('Delete this org?')) action(deleteOrg, org.id) }}
                          className="text-xs text-red-600 hover:underline">Delete</button>
                      </>
                    )}
                    <Link to={`/organizations/${org.id}/routes`}
                      className="text-xs text-gray-600 hover:underline">Routes →</Link>
                  </div>
                </td>
              </tr>
            ))}
            {orgs.length === 0 && (
              <tr><td colSpan={5} className="px-6 py-8 text-center text-gray-400">No organizations yet</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Create org modal */}
      {showCreate && (
        <Modal title="New Organization" onClose={() => setShowCreate(false)}>
          <form onSubmit={handleCreate} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Name</label>
              <input value={form.name} onChange={e => setForm(f => ({...f, name: e.target.value}))}
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="FoodFast" required />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Slug</label>
              <input value={form.slug} onChange={e => setForm(f => ({...f, slug: e.target.value.toLowerCase()}))}
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono"
                placeholder="foodfast" pattern="[a-z0-9-]+" required />
              <p className="text-xs text-gray-400 mt-1">Used in route paths: /foodfast/orders/**</p>
            </div>
            <div className="flex gap-3 pt-2">
              <button type="submit" className="flex-1 bg-blue-600 text-white py-2 rounded-lg text-sm font-medium">
                Create
              </button>
              <button type="button" onClick={() => setShowCreate(false)}
                className="flex-1 border py-2 rounded-lg text-sm">Cancel</button>
            </div>
          </form>
        </Modal>
      )}

      {/* Create admin modal */}
      {showAdmin && (
        <Modal title={`Add Admin — ${showAdmin.name}`} onClose={() => setShowAdmin(null)}>
          <form onSubmit={handleCreateAdmin} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Username</label>
              <input value={adminForm.username}
                onChange={e => setAdminForm(f => ({...f, username: e.target.value}))}
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="foodfast-admin" required />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Password</label>
              <input type="password" value={adminForm.password}
                onChange={e => setAdminForm(f => ({...f, password: e.target.value}))}
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                minLength={8} required />
            </div>
            <div className="flex gap-3 pt-2">
              <button type="submit" className="flex-1 bg-blue-600 text-white py-2 rounded-lg text-sm font-medium">
                Create Admin
              </button>
              <button type="button" onClick={() => setShowAdmin(null)}
                className="flex-1 border py-2 rounded-lg text-sm">Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
