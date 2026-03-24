import { useEffect, useState } from 'react'
import {
  exportAdminModerationActionsCsv,
  listAdminModerationActions,
  type AdminModerationActionDto,
} from '../../../services/adminApi'

export function AdminModerationAuditPage() {
  const [items, setItems] = useState<AdminModerationActionDto[]>([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [totalPages, setTotalPages] = useState(0)
  const [adminUserId, setAdminUserId] = useState('')
  const [adminEmail, setAdminEmail] = useState('')
  const [targetType, setTargetType] = useState<'' | 'USER' | 'DECK' | 'CARD'>('')
  const [status, setStatus] = useState<'' | 'SUCCESS' | 'FAILURE'>('')
  const [sortBy, setSortBy] = useState<'createdAt' | 'actionType' | 'targetType' | 'status'>('createdAt')
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc')

  useEffect(() => {
    setLoading(true)
    void listAdminModerationActions({
      adminUserId,
      adminEmail,
      targetType,
      status,
      page,
      size,
      sortBy,
      sortDir,
    })
      .then((response) => {
        setItems(response.content)
        setTotalPages(response.totalPages)
      })
      .catch((err) => {
        console.error('admin_moderation_actions_load_failed', err)
      })
      .finally(() => {
        setLoading(false)
      })
  }, [adminUserId, adminEmail, targetType, status, page, size, sortBy, sortDir])

  const handleExportCsv = () => {
    void exportAdminModerationActionsCsv({
      adminUserId,
      adminEmail,
      targetType,
      status,
      size: 1000,
    })
      .then((csv) => {
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = 'moderation-actions.csv'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
      })
      .catch((err) => {
        console.error('admin_moderation_actions_export_failed', err)
      })
  }

  return (
    <section>
      <h1 className="text-2xl font-semibold">Admin Moderation Audit Log</h1>

      <div className="mt-4 rounded border border-slate-200 bg-white p-4">
        <div className="grid gap-2 md:grid-cols-7">
          <input
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            placeholder="Filter: adminUserId"
            value={adminUserId}
            onChange={(event) => {
              setAdminUserId(event.target.value)
              setPage(0)
            }}
          />

          <input
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            placeholder="Filter: admin email"
            value={adminEmail}
            onChange={(event) => {
              setAdminEmail(event.target.value)
              setPage(0)
            }}
          />

          <select
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            value={targetType}
            onChange={(event) => {
              setTargetType(event.target.value as '' | 'USER' | 'DECK' | 'CARD')
              setPage(0)
            }}
          >
            <option value="">Target: All</option>
            <option value="USER">Target: USER</option>
            <option value="DECK">Target: DECK</option>
            <option value="CARD">Target: CARD</option>
          </select>

          <select
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as '' | 'SUCCESS' | 'FAILURE')
              setPage(0)
            }}
          >
            <option value="">Status: All</option>
            <option value="SUCCESS">Status: SUCCESS</option>
            <option value="FAILURE">Status: FAILURE</option>
          </select>

          <select
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            value={sortBy}
            onChange={(event) => {
              setSortBy(event.target.value as 'createdAt' | 'actionType' | 'targetType' | 'status')
              setPage(0)
            }}
          >
            <option value="createdAt">Sort: Created At</option>
            <option value="actionType">Sort: Action Type</option>
            <option value="targetType">Sort: Target Type</option>
            <option value="status">Sort: Status</option>
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

          <button
            type="button"
            className="rounded bg-emerald-700 px-3 py-2 text-sm font-medium text-white hover:bg-emerald-800"
            onClick={handleExportCsv}
          >
            Export CSV
          </button>
        </div>
      </div>

      {loading ? <p className="mt-3 text-sm text-slate-500">Loading moderation actions...</p> : null}

      <div className="mt-4 overflow-hidden rounded border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-700">
            <tr>
              <th className="p-3">When</th>
              <th className="p-3">Action</th>
              <th className="p-3">Admin</th>
              <th className="p-3">Target</th>
              <th className="p-3">Target Detail</th>
              <th className="p-3">Status</th>
              <th className="p-3">Reason</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, index) => (
              <tr key={item.id} className={index % 2 === 0 ? 'bg-white' : 'bg-slate-50'}>
                <td className="p-3">{new Date(item.createdAt).toLocaleString()}</td>
                <td className="p-3">{item.actionType}</td>
                <td className="p-3">{item.adminEmail}</td>
                <td className="p-3">{item.targetType}</td>
                <td className="p-3">
                  <p className="text-slate-800">{item.targetDisplayName || '-'}</p>
                  <p className="font-mono text-xs text-slate-500">{item.targetId}</p>
                </td>
                <td className="p-3">{item.status}</td>
                <td className="p-3">{item.reasonCode ?? '-'}</td>
              </tr>
            ))}
            {!items.length ? (
              <tr>
                <td className="p-3 text-slate-500" colSpan={7}>
                  No moderation actions found.
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
