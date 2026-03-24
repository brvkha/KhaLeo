import { useEffect, useState } from 'react'
import { banAdminUser, listAdminUsers, type AdminUserModerationItemDto, unbanAdminUser } from '../../../services/adminApi'
import { useAuthStore } from '../../../store/authStore'
import { useNotificationStore } from '../../../store/notificationStore'

export function AdminUsersPage() {
  const currentUser = useAuthStore((state) => state.currentUser)
  const pushSuccess = useNotificationStore((state) => state.pushSuccess)
  const pushError = useNotificationStore((state) => state.pushError)
  const [users, setUsers] = useState<AdminUserModerationItemDto[]>([])
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [totalPages, setTotalPages] = useState(0)
  const [sortBy, setSortBy] = useState<'createdAt' | 'email' | 'role' | 'bannedAt'>('createdAt')
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc')
  const [loading, setLoading] = useState(false)

  const loadUsers = async (search = query, targetPage = page) => {
    setLoading(true)
    try {
      const response = await listAdminUsers({ query: search, page: targetPage, size, sortBy, sortDir })
      setUsers(response.content)
      setTotalPages(response.totalPages)
    } catch (err) {
      console.error('admin_users_load_failed', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadUsers(query, page)
    }, 250)
    return () => clearTimeout(timer)
  }, [query, page, size, sortBy, sortDir])

  const handleBan = async (candidate: AdminUserModerationItemDto, action: 'ban' | 'unban') => {
    if (currentUser?.id === candidate.id) {
      pushError('Không thể tự ban chính mình.')
      return
    }

    const verb = action === 'ban' ? 'Ban' : 'Unban'
    if (!window.confirm(`${verb} user ${candidate.email}?`)) {
      return
    }

    try {
      if (action === 'ban') {
        await banAdminUser(candidate.id)
      } else {
        await unbanAdminUser(candidate.id)
      }
      pushSuccess(`Đã ${action === 'ban' ? 'ban' : 'unban'} user ${candidate.email}.`)
      await loadUsers(query, page)
    } catch (err) {
      console.error('admin_user_ban_failed', err)
    }
  }

  return (
    <section>
      <h1 className="text-2xl font-semibold">Admin User Moderation</h1>
      <div className="mt-4 rounded border border-slate-200 bg-white p-4">
        <div className="grid gap-2 md:grid-cols-4">
          <input
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            placeholder="Tìm theo email"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value)
              setPage(0)
            }}
          />
          <select
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            value={sortBy}
            onChange={(event) => {
              setSortBy(event.target.value as 'createdAt' | 'email' | 'role' | 'bannedAt')
              setPage(0)
            }}
          >
            <option value="createdAt">Sort: Created At</option>
            <option value="email">Sort: Email</option>
            <option value="role">Sort: Role</option>
            <option value="bannedAt">Sort: Banned At</option>
          </select>
          <select
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            value={sortDir}
            onChange={(event) => {
              setSortDir(event.target.value as 'asc' | 'desc')
              setPage(0)
            }}
          >
            <option value="desc">Direction: DESC</option>
            <option value="asc">Direction: ASC</option>
          </select>
          <select
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            value={size}
            onChange={(event) => {
              setSize(Number(event.target.value))
              setPage(0)
            }}
          >
            <option value={10}>Page size: 10</option>
            <option value={20}>Page size: 20</option>
            <option value={50}>Page size: 50</option>
          </select>
        </div>
      </div>

      {loading ? <p className="mt-3 text-sm text-slate-500">Loading users...</p> : null}

      <div className="mt-4 overflow-hidden rounded border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-700">
            <tr>
              <th className="p-3">Email</th>
              <th className="p-3">Role</th>
              <th className="p-3">Status</th>
              <th className="p-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user, index) => (
              <tr key={user.id} className={index % 2 === 0 ? 'bg-white' : 'bg-slate-50'}>
                <td className="p-3">{user.email}</td>
                <td className="p-3">{user.role}</td>
                <td className="p-3">{user.banned ? 'BANNED' : 'ACTIVE'}</td>
                <td className="p-3 text-right">
                  <button
                    className={`rounded px-3 py-1 text-xs text-white disabled:cursor-not-allowed disabled:bg-slate-400 ${
                      user.banned ? 'bg-emerald-700' : 'bg-rose-700'
                    }`}
                    type="button"
                    onClick={() => {
                      void handleBan(user, user.banned ? 'unban' : 'ban')
                    }}
                    disabled={currentUser?.id === user.id}
                  >
                    {currentUser?.id === user.id ? 'Cannot Self-Moderate' : user.banned ? 'Unban User' : 'Ban User'}
                  </button>
                </td>
              </tr>
            ))}
            {!users.length ? (
              <tr>
                <td className="p-3 text-slate-500" colSpan={4}>
                  No users found.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>

      <div className="mt-3 flex items-center justify-between text-sm">
        <p className="text-slate-600">Page {page + 1} / {Math.max(totalPages, 1)}</p>
        <div className="flex gap-2">
          <button
            type="button"
            className="rounded border border-slate-300 px-3 py-1 disabled:cursor-not-allowed disabled:opacity-50"
            disabled={page <= 0}
            onClick={() => setPage((prev) => Math.max(0, prev - 1))}
          >
            Previous
          </button>
          <button
            type="button"
            className="rounded border border-slate-300 px-3 py-1 disabled:cursor-not-allowed disabled:opacity-50"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((prev) => prev + 1)}
          >
            Next
          </button>
        </div>
      </div>
    </section>
  )
}
