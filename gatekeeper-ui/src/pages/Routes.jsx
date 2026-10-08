import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  getRoutes, createRoute, updateRoute,
  activateRoute, deactivateRoute, deleteRoute, getRouteHistory
} from '../api/routes'
import { getOrganization } from '../api/organizations'
import Badge from '../components/Badge'
import Modal from '../components/Modal'
import Spinner from '../components/Spinner'

const emptyRoute = {
  routeId: '', name: '', pathPattern: '', targetBaseUrl: '',
  targetPathPrefix: '', allowedMethods: ['GET'],
  requestsPerMinute: '', responseTimeoutMs: 30000,
  idempotencyEnabled: false, headerRules: []
}

export default function Routes() {
  const { orgId } = useParams()
  const [org, setOrg]       = useState(null)
  const [routes, setRoutes] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError]   = useState('')
  const [showForm, setShowForm]       = useState(false)
  const [editRoute, setEditRoute]     = useState(null)
  const [historyRoute, setHistoryRoute] = useState(null)
  const [history, setHistory]         = useState([])
  const [form, setForm] = useState(emptyRoute)

  const load = () => {
    setLoading(true)
    Promise.all([getOrganization(orgId), getRoutes(orgId)])
      .then(([orgRes, routesRes]) => {
        setOrg(orgRes.data?.data)
        setRoutes(routesRes.data?.data ?? [])
      })
      .catch(() => setError('Failed to load'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [orgId])

  const openEdit = r => {
    setEditRoute(r)
    setForm({
      routeId: r.routeId, name: r.name, pathPattern: r.pathPattern,
      targetBaseUrl: r.targetBaseUrl, targetPathPrefix: r.targetPathPrefix ?? '',
      allowedMethods: r.allowedMethods, requestsPerMinute: r.requestsPerMinute ?? '',
      responseTimeoutMs: r.responseTimeoutMs, idempotencyEnabled: r.idempotencyEnabled,
      headerRules: r.headerRules ?? []
    })
    setShowForm(true)
  }

  const openCreate = () => { setEditRoute(null); setForm(emptyRoute); setShowForm(true) }

  const handleSubmit = async e => {
    e.preventDefault()
    const payload = {
      ...form,
      requestsPerMinute: form.requestsPerMinute === '' ? null : Number(form.requestsPerMinute)
    }
    try {
      if (editRoute) await updateRoute(orgId, editRoute.routeId, payload)
      else           await createRoute(orgId, payload)
      setShowForm(false)
      load()
    } catch (err) {
      setError(err.response?.data?.error ?? 'Save failed')
    }
  }

  const action = async (fn, routeId) => {
    try { await fn(orgId, routeId); load() }
    catch (err) { setError(err.response?.data?.error ?? 'Action failed') }
  }

  const openHistory = async r => {
    const res = await getRouteHistory(orgId, r.routeId)
    setHistory(res.data?.data ?? [])
    setHistoryRoute(r)
  }

  const toggleMethod = m => {
    setForm(f => ({
      ...f,
      allowedMethods: f.allowedMethods.includes(m)
        ? f.allowedMethods.filter(x => x !== m)
        : [...f.allowedMethods, m]
    }))
  }

  if (loading) return <Spinner />

  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-2">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Routes</h2>
          {org && <p className="text-gray-500 text-sm mt-0.5">{org.name} — <span className="font-mono">{org.slug}</span></p>}
        </div>
        <button onClick={openCreate}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium">
          + New Route
        </button>
      </div>

      {error && <p className="mb-4 text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2 text-sm">{error}</p>}

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden mt-4">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 uppercase text-xs">
            <tr>
              {['Route ID','Path Pattern','Target','Methods','Rate Limit','Status','Actions'].map(h => (
                <th key={h} className="px-4 py-3 text-left font-medium">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {routes.map(r => (
              <tr key={r.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-mono font-medium text-blue-700">{r.routeId}</td>
                <td className="px-4 py-3 font-mono text-xs text-gray-600">{r.pathPattern}</td>
                <td className="px-4 py-3 text-xs text-gray-500 truncate max-w-[160px]">{r.targetBaseUrl}</td>
                <td className="px-4 py-3">
                  <div className="flex gap-1 flex-wrap">
                    {r.allowedMethods.map(m => (
                      <span key={m} className="bg-gray-100 text-gray-700 px-1.5 py-0.5 rounded text-xs font-mono">{m}</span>
                    ))}
                  </div>
                </td>
                <td className="px-4 py-3 text-xs">{r.requestsPerMinute ? `${r.requestsPerMinute}/min` : '∞'}</td>
                <td className="px-4 py-3"><Badge value={r.active} /></td>
                <td className="px-4 py-3">
                  <div className="flex gap-2 text-xs">
                    <button onClick={() => openEdit(r)} className="text-blue-600 hover:underline">Edit</button>
                    {r.active
                      ? <button onClick={() => action(deactivateRoute, r.routeId)} className="text-orange-600 hover:underline">Deactivate</button>
                      : <button onClick={() => action(activateRoute, r.routeId)} className="text-green-600 hover:underline">Activate</button>
                    }
                    <button onClick={() => openHistory(r)} className="text-gray-500 hover:underline">History</button>
                    <button onClick={() => { if(confirm('Delete route?')) action(deleteRoute, r.routeId) }}
                      className="text-red-600 hover:underline">Delete</button>
                  </div>
                </td>
              </tr>
            ))}
            {routes.length === 0 && (
              <tr><td colSpan={7} className="px-6 py-8 text-center text-gray-400">No routes configured yet</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Create/Edit modal */}
      {showForm && (
        <Modal title={editRoute ? `Edit: ${editRoute.routeId}` : 'New Route'} onClose={() => setShowForm(false)}>
          <form onSubmit={handleSubmit} className="space-y-3 max-h-[70vh] overflow-y-auto pr-1">
            {!editRoute && (
              <div>
                <label className="block text-xs font-medium mb-1">Route ID</label>
                <input value={form.routeId} onChange={e => setForm(f => ({...f, routeId: e.target.value.toLowerCase()}))}
                  className="w-full border rounded px-3 py-1.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="orders" pattern="[a-z0-9-]+" required />
              </div>
            )}
            <div>
              <label className="block text-xs font-medium mb-1">Name</label>
              <input value={form.name} onChange={e => setForm(f => ({...f, name: e.target.value}))}
                className="w-full border rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="FoodFast Order Service" required />
            </div>
            <div>
              <label className="block text-xs font-medium mb-1">Path Pattern</label>
              <input value={form.pathPattern} onChange={e => setForm(f => ({...f, pathPattern: e.target.value}))}
                className="w-full border rounded px-3 py-1.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="/foodfast/orders/**" required />
            </div>
            <div>
              <label className="block text-xs font-medium mb-1">Target Base URL</label>
              <input value={form.targetBaseUrl} onChange={e => setForm(f => ({...f, targetBaseUrl: e.target.value}))}
                className="w-full border rounded px-3 py-1.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="http://order-service:8092" required />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium mb-1">Rate Limit (req/min)</label>
                <input type="number" value={form.requestsPerMinute}
                  onChange={e => setForm(f => ({...f, requestsPerMinute: e.target.value}))}
                  className="w-full border rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Unlimited" min="1" />
              </div>
              <div>
                <label className="block text-xs font-medium mb-1">Timeout (ms)</label>
                <input type="number" value={form.responseTimeoutMs}
                  onChange={e => setForm(f => ({...f, responseTimeoutMs: Number(e.target.value)}))}
                  className="w-full border rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required min="100" />
              </div>
            </div>
            <div>
              <label className="block text-xs font-medium mb-1">Allowed Methods</label>
              <div className="flex gap-2 flex-wrap">
                {['GET','POST','PUT','DELETE','PATCH'].map(m => (
                  <label key={m} className="flex items-center gap-1 text-xs cursor-pointer">
                    <input type="checkbox" checked={form.allowedMethods.includes(m)}
                      onChange={() => toggleMethod(m)} className="rounded" />
                    <span className="font-mono">{m}</span>
                  </label>
                ))}
              </div>
            </div>
            <div>
              <label className="flex items-center gap-2 text-xs font-medium cursor-pointer">
                <input type="checkbox" checked={form.idempotencyEnabled}
                  onChange={e => setForm(f => ({...f, idempotencyEnabled: e.target.checked}))} />
                Enable Idempotency-Key enforcement
              </label>
            </div>
            <div className="flex gap-3 pt-2">
              <button type="submit" className="flex-1 bg-blue-600 text-white py-2 rounded-lg text-sm font-medium">
                {editRoute ? 'Update Route' : 'Create Route'}
              </button>
              <button type="button" onClick={() => setShowForm(false)}
                className="flex-1 border py-2 rounded-lg text-sm">Cancel</button>
            </div>
          </form>
        </Modal>
      )}

      {/* History modal */}
      {historyRoute && (
        <Modal title={`Version History — ${historyRoute.routeId}`} onClose={() => setHistoryRoute(null)}>
          <div className="space-y-2 max-h-80 overflow-y-auto">
            {history.map(v => (
              <div key={v.id} className="border rounded-lg p-3 text-xs">
                <div className="flex justify-between mb-1">
                  <span className="font-bold">v{v.versionNumber} — {v.changeType}</span>
                  <span className="text-gray-400">{new Date(v.createdAt).toLocaleString()}</span>
                </div>
                <p className="text-gray-500">by {v.createdBy}</p>
              </div>
            ))}
            {history.length === 0 && <p className="text-gray-400 text-center py-4">No history yet</p>}
          </div>
        </Modal>
      )}
    </div>
  )
}
