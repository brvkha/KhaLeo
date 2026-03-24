import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAdminStats, type AdminStatsDto } from '../../../services/adminApi'

export function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminStatsDto | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    void getAdminStats()
      .then((payload) => setStats(payload))
      .catch((err) => {
        console.error('admin_stats_load_failed', err)
      })
      .finally(() => setLoading(false))
  }, [])

  return (
    <section>
      <h1 className="text-2xl font-semibold">Admin Dashboard</h1>
      {loading ? <p className="mt-3 text-sm text-slate-500">Loading stats...</p> : null}
      <div className="mt-4 grid gap-3 md:grid-cols-3">
        <article className="rounded border border-slate-200 bg-white p-4">
          <p className="text-sm text-slate-500">Total decks</p>
          <p className="text-2xl font-semibold">{stats?.totalDecks ?? 0}</p>
        </article>
        <article className="rounded border border-slate-200 bg-white p-4">
          <p className="text-sm text-slate-500">Total cards</p>
          <p className="text-2xl font-semibold">{stats?.totalCards ?? 0}</p>
        </article>
        <article className="rounded border border-slate-200 bg-white p-4">
          <p className="text-sm text-slate-500">Reviews Last 24h</p>
          <p className="text-2xl font-semibold">{stats?.reviewsLast24Hours ?? 0}</p>
        </article>
      </div>
      <div className="mt-3 rounded border border-slate-200 bg-white p-4">
        <p className="text-sm text-slate-600">Total users: {stats?.totalUsers ?? 0}</p>
      </div>
      <div className="mt-4 rounded border border-slate-200 bg-white p-4">
        <p className="font-medium">Moderation shortcuts</p>
        <div className="mt-2 flex flex-wrap gap-2 text-sm">
          <Link className="rounded bg-slate-900 px-3 py-1 text-white" to="/admin/users">
            Users
          </Link>
          <Link className="rounded bg-slate-900 px-3 py-1 text-white" to="/admin/decks">
            Decks
          </Link>
          <Link className="rounded bg-slate-900 px-3 py-1 text-white" to="/admin/cards">
            Cards
          </Link>
          <Link className="rounded bg-slate-900 px-3 py-1 text-white" to="/admin/audit">
            Audit Logs
          </Link>
        </div>
      </div>
    </section>
  )
}
