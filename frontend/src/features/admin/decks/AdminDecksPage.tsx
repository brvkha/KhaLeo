import { useEffect, useState } from 'react'
import { banAdminDeck, listAdminDecks, type AdminDeckModerationItemDto, unbanAdminDeck } from '../../../services/adminApi'
import { useNotificationStore } from '../../../store/notificationStore'

export function AdminDecksPage() {
  const pushSuccess = useNotificationStore((state) => state.pushSuccess)
  const [decks, setDecks] = useState<AdminDeckModerationItemDto[]>([])
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [totalPages, setTotalPages] = useState(0)
  const [sortBy, setSortBy] = useState<'createdAt' | 'name' | 'isPublic' | 'bannedAt'>('createdAt')
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc')
  const [loading, setLoading] = useState(false)

  const loadDecks = async (search = query, targetPage = page) => {
    setLoading(true)
    try {
      const response = await listAdminDecks({ query: search, page: targetPage, size, sortBy, sortDir })
      setDecks(response.content)
      setTotalPages(response.totalPages)
    } catch (err) {
      console.error('admin_decks_load_failed', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadDecks(query, page)
    }, 250)
    return () => clearTimeout(timer)
  }, [query, page, size, sortBy, sortDir])

  const handleDeckModeration = async (candidate: AdminDeckModerationItemDto, action: 'ban' | 'unban') => {
    const verb = action === 'ban' ? 'Ban' : 'Unban'
    if (!window.confirm(`${verb} deck ${candidate.name}?`)) {
      return
    }

    try {
      if (action === 'ban') {
        await banAdminDeck(candidate.id)
      } else {
        await unbanAdminDeck(candidate.id)
      }
      pushSuccess(`Đã ${action === 'ban' ? 'ban' : 'unban'} deck ${candidate.name}.`)
      await loadDecks(query, page)
    } catch (err) {
      console.error('admin_deck_ban_failed', err)
    }
  }

  return (
    <section>
      <h1 className="text-2xl font-semibold">Admin Deck Moderation</h1>
      <div className="mt-4 rounded border border-slate-200 bg-white p-4">
        <div className="grid gap-2 md:grid-cols-4">
          <input
            className="rounded border border-slate-300 px-3 py-2 text-sm"
            placeholder="Tìm theo tên deck hoặc owner"
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
              setSortBy(event.target.value as 'createdAt' | 'name' | 'isPublic' | 'bannedAt')
              setPage(0)
            }}
          >
            <option value="createdAt">Sort: Created At</option>
            <option value="name">Sort: Name</option>
            <option value="isPublic">Sort: Visibility</option>
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

      {loading ? <p className="mt-3 text-sm text-slate-500">Loading decks...</p> : null}

      <div className="mt-4 overflow-hidden rounded border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-700">
            <tr>
              <th className="p-3">Deck</th>
              <th className="p-3">Owner</th>
              <th className="p-3">Cards</th>
              <th className="p-3">Status</th>
              <th className="p-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody>
            {decks.map((deck, index) => (
              <tr key={deck.id} className={index % 2 === 0 ? 'bg-white' : 'bg-slate-50'}>
                <td className="p-3">{deck.name}</td>
                <td className="p-3">{deck.ownerEmail}</td>
                <td className="p-3">{deck.cardCount}</td>
                <td className="p-3">{deck.banned ? 'BANNED' : deck.isPublic ? 'PUBLIC' : 'PRIVATE'}</td>
                <td className="p-3 text-right">
                  <button
                    className={`rounded px-3 py-1 text-xs text-white disabled:cursor-not-allowed disabled:bg-slate-400 ${
                      deck.banned ? 'bg-emerald-700' : 'bg-rose-700'
                    }`}
                    type="button"
                    onClick={() => {
                      void handleDeckModeration(deck, deck.banned ? 'unban' : 'ban')
                    }}
                  >
                    {deck.banned ? 'Unban Deck' : 'Ban Deck'}
                  </button>
                </td>
              </tr>
            ))}
            {!decks.length ? (
              <tr>
                <td className="p-3 text-slate-500" colSpan={5}>
                  No decks found.
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
