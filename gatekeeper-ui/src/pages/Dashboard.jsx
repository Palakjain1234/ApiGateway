import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getOrganizations } from '../api/organizations'
import { getPlatformStats } from '../api/analytics'
import Spinner from '../components/Spinner'

function StatCard({ label, value, colour = 'blue' }) {
  const colours = { blue: 'bg-blue-50 text-blue-700', green: 'bg-green-50 text-green-700', purple: 'bg-purple-50 text-purple-700', orange: 'bg-orange-50 text-orange-700' }
  return (
    <div className={`rounded-xl p-5 ${colours[colour]}`}>
      <p className="text-sm font-medium opacity-70">{label}</p>
      <p className="text-3xl font-bold mt-1">{value ?? '—'}</p>
    </div>
  )
}

export default function Dashboard() {
  const [orgs, setOrgs]   = useState([])
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.allSettled([getOrganizations(), getPlatformStats()])
      .then(([orgRes, statsRes]) => {
        if (orgRes.status === 'fulfilled')   setOrgs(orgRes.value.data?.data ?? [])
        if (statsRes.status === 'fulfilled') setStats(statsRes.value.data)
      })
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Spinner />

  const active = orgs.filter(o => o.status === 'ACTIVE').length

  return (
    <div className="p-8">
      <h2 className="text-2xl font-bold text-gray-900 mb-6">Dashboard</h2>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard label="Total Organizations" value={orgs.length}          colour="blue"   />
        <StatCard label="Active Orgs"          value={active}              colour="green"  />
        <StatCard label="Total Requests"       value={stats?.totalRequests} colour="purple" />
        <StatCard label="Rate Limited"         value={stats?.rateLimitedCount} colour="orange" />
      </div>

      {/* Recent orgs */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="px-6 py-4 border-b flex justify-between items-center">
          <h3 className="font-semibold text-gray-800">Organizations</h3>
          <Link to="/organizations" className="text-sm text-blue-600 hover:underline">View all →</Link>
        </div>
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 uppercase text-xs">
            <tr>
              {['Name','Slug','Status'].map(h => (
                <th key={h} className="px-6 py-3 text-left font-medium">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {orgs.slice(0, 5).map(org => (
              <tr key={org.id} className="hover:bg-gray-50">
                <td className="px-6 py-3 font-medium">{org.name}</td>
                <td className="px-6 py-3 text-gray-500 font-mono text-xs">{org.slug}</td>
                <td className="px-6 py-3">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium
                    ${org.status === 'ACTIVE' ? 'bg-green-100 text-green-800' :
                      org.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                      'bg-red-100 text-red-800'}`}>
                    {org.status}
                  </span>
                </td>
              </tr>
            ))}
            {orgs.length === 0 && (
              <tr><td colSpan={3} className="px-6 py-8 text-center text-gray-400">No organizations yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
