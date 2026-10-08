import { useEffect, useState } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { getPlatformStats } from '../api/analytics'
import Spinner from '../components/Spinner'

const COLOURS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444']

export default function Analytics() {
  const [stats, setStats]   = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]   = useState('')

  useEffect(() => {
    getPlatformStats()
      .then(r => setStats(r.data))
      .catch(() => setError('Analytics service may not be running'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Spinner />

  if (error) return (
    <div className="p-8">
      <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-6 text-yellow-800">
        <h3 className="font-semibold mb-1">Analytics Unavailable</h3>
        <p className="text-sm">{error}</p>
        <p className="text-xs mt-2 text-yellow-600">Start analytics-service on port 8082 to see stats.</p>
      </div>
    </div>
  )

  const pieData = [
    { name: 'Success',    value: (stats?.totalRequests ?? 0) - (stats?.rateLimitedCount ?? 0) - (stats?.idempotencyReplays ?? 0) },
    { name: 'Rate Ltd',   value: stats?.rateLimitedCount ?? 0 },
    { name: 'Replays',    value: stats?.idempotencyReplays ?? 0 },
  ].filter(d => d.value > 0)

  const barData = stats?.slowestRoutes
    ? Object.entries(stats.slowestRoutes).map(([route, avg]) => ({ route, avg: Math.round(avg) }))
    : []

  return (
    <div className="p-8">
      <h2 className="text-2xl font-bold text-gray-900 mb-6">Analytics</h2>

      {/* Summary cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {[
          { label: 'Total Requests',    value: stats?.totalRequests   ?? 0, colour: 'bg-blue-50 text-blue-700'   },
          { label: 'Rate Limited',      value: stats?.rateLimitedCount ?? 0, colour: 'bg-orange-50 text-orange-700' },
          { label: 'Idempotency Replays', value: stats?.idempotencyReplays ?? 0, colour: 'bg-purple-50 text-purple-700' },
          { label: 'Unique Routes',     value: barData.length,        colour: 'bg-green-50 text-green-700'  },
        ].map(c => (
          <div key={c.label} className={`rounded-xl p-5 ${c.colour}`}>
            <p className="text-sm font-medium opacity-70">{c.label}</p>
            <p className="text-3xl font-bold mt-1">{c.value}</p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Request breakdown pie */}
        {pieData.length > 0 && (
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h3 className="font-semibold text-gray-800 mb-4">Request Breakdown</h3>
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie data={pieData} dataKey="value" nameKey="name" outerRadius={80} label={({ name, percent }) =>
                  `${name} ${(percent * 100).toFixed(0)}%`}>
                  {pieData.map((_, i) => <Cell key={i} fill={COLOURS[i % COLOURS.length]} />)}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )}

        {/* Slowest routes bar */}
        {barData.length > 0 && (
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h3 className="font-semibold text-gray-800 mb-4">Avg Response Time by Route (ms)</h3>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={barData} layout="vertical" margin={{ left: 20 }}>
                <XAxis type="number" tick={{ fontSize: 11 }} />
                <YAxis type="category" dataKey="route" tick={{ fontSize: 11 }} width={80} />
                <Tooltip />
                <Bar dataKey="avg" fill="#3b82f6" radius={[0,4,4,0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}

        {pieData.length === 0 && barData.length === 0 && (
          <div className="col-span-2 bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-400">
            <p className="text-lg font-medium">No data yet</p>
            <p className="text-sm mt-1">Send some requests through the Smart Gateway to see analytics here.</p>
          </div>
        )}
      </div>
    </div>
  )
}
