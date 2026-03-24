import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { deletePrivateDeck, listPrivateDecks, getDeckStats } from '../../services/privateWorkspaceApi'
import type { DeckStatsDto } from '../../services/privateWorkspaceApi'

type ViewMode = 'card' | 'table'

export function StudyWorkspacePage() {
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [viewMode, setViewMode] = useState<ViewMode>('table')
  const [decks, setDecks] = useState<Array<{ id: string; name: string; description: string | null; isPublic?: boolean }>>([])
  const [deckStats, setDeckStats] = useState<Record<string, DeckStatsDto>>({})

  const visibleDecks = useMemo(
    () => decks.filter((deck) => deck.isPublic !== true),
    [decks],
  )

  const refresh = async (searchQuery = '') => {
    setLoading(true)
    setError('')
    try {
      const nextDecks = await listPrivateDecks(searchQuery)
      setDecks(nextDecks)
      
      // Fetch stats for each deck from backend
      const stats: Record<string, DeckStatsDto> = {}
      for (const deck of nextDecks) {
        try {
          stats[deck.id] = await getDeckStats(deck.id)
        } catch (err) {
          console.error(`Failed to fetch stats for deck ${deck.id}:`, err)
          // Set default stats on error
          stats[deck.id] = { deckId: deck.id, learning: 0, review: 0, new_cards: 0 }
        }
      }
      setDeckStats(stats)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load private decks')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh('')
  }, [])

  // Auto-search with debounce
  useEffect(() => {
    const timer = setTimeout(() => {
      void refresh(query)
    }, 300)

    return () => clearTimeout(timer)
  }, [query])

  return (
    <section>
      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold">Study Workspace</h1>
          <p className="mt-1 text-sm text-slate-600">Only your private decks are shown</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setViewMode('card')}
            className={`px-3 py-2 rounded text-sm font-medium transition ${
              viewMode === 'card'
                ? 'bg-blue-600 text-white'
                : 'border border-slate-300 bg-white hover:bg-slate-50'
            }`}
          >
            Card View
          </button>
          <button
            onClick={() => setViewMode('table')}
            className={`px-3 py-2 rounded text-sm font-medium transition ${
              viewMode === 'table'
                ? 'bg-blue-600 text-white'
                : 'border border-slate-300 bg-white hover:bg-slate-50'
            }`}
          >
            Table View
          </button>
        </div>
      </div>

      <div className="mt-4 rounded border border-slate-200 bg-white p-4">
        <label className="text-sm" htmlFor="private-deck-search">Search decks</label>
        <input
          id="private-deck-search"
          className="mt-2 w-full rounded border border-slate-300 px-3 py-1 text-sm"
          placeholder="Type to search..."
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
      </div>

      {error ? <p className="mt-3 text-sm text-rose-600">{error}</p> : null}
      {loading ? <p className="mt-3 text-sm text-slate-500">Loading...</p> : null}

      {viewMode === 'card' ? (
        <ul className="mt-4 grid gap-3 md:grid-cols-2">
          {visibleDecks.map((deck) => (
            <li className="rounded border border-slate-200 bg-white p-4" key={deck.id}>
              <h2 className="font-semibold">{deck.name}</h2>
              <p className="text-sm text-slate-600 mt-1">{deck.description ?? ''}</p>
              
              <div className="mt-3 flex gap-4 text-sm">
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-emerald-500"></div>
                  <span className="text-slate-700">Learning: <span className="font-medium">{deckStats[deck.id]?.learning ?? 0}</span></span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-blue-500"></div>
                  <span className="text-slate-700">Review: <span className="font-medium">{deckStats[deck.id]?.review ?? 0}</span></span>
                </div>
              </div>
              <p className="text-sm text-slate-500 mt-2">New: {deckStats[deck.id]?.new_cards ?? 0}</p>
              
              <div className="mt-3 flex gap-2">
                <Link
                  className="rounded border border-slate-300 px-3 py-1 text-sm hover:bg-slate-50"
                  to={`/study/session/${deck.id}`}
                >
                  Start session
                </Link>
                <button
                  className="rounded bg-rose-600 px-3 py-1 text-white text-sm hover:bg-rose-700"
                  onClick={() => {
                    void deletePrivateDeck(deck.id)
                      .then(() => refresh(query))
                      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to delete private deck'))
                  }}
                >
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <div className="mt-4 rounded border border-slate-200 bg-white overflow-hidden">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-200 bg-slate-50">
              <tr>
                <th className="p-3 text-left font-semibold text-slate-700">Deck Name</th>
                <th className="p-3 text-left font-semibold text-slate-700">Description</th>
                <th className="p-3 text-center font-semibold text-emerald-700">Learning</th>
                <th className="p-3 text-center font-semibold text-blue-700">Review</th>
                <th className="p-3 text-center font-semibold text-slate-700">New</th>
                <th className="p-3 text-right font-semibold text-slate-700">Action</th>
              </tr>
            </thead>
            <tbody>
              {visibleDecks.map((deck, idx) => (
                <tr key={deck.id} className={`border-b border-slate-200 ${idx % 2 === 0 ? 'bg-white' : 'bg-slate-50'}`}>
                  <td className="p-3 font-medium text-slate-900">{deck.name}</td>
                  <td className="p-3 text-slate-600 truncate">{deck.description ?? ''}</td>
                  <td className="p-3 text-center">
                    <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-emerald-100 text-emerald-700 text-xs font-medium">
                      {deckStats[deck.id]?.learning ?? 0}
                    </span>
                  </td>
                  <td className="p-3 text-center">
                    <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-blue-100 text-blue-700 text-xs font-medium">
                      {deckStats[deck.id]?.review ?? 0}
                    </span>
                  </td>
                  <td className="p-3 text-center text-slate-600">{deckStats[deck.id]?.new_cards ?? 0}</td>
                  <td className="p-3 text-right">
                    <div className="flex justify-end gap-2">
                      <Link
                        className="rounded border border-slate-300 px-2 py-1 text-xs hover:bg-slate-100"
                        to={`/study/session/${deck.id}`}
                      >
                        Start
                      </Link>
                      <button
                        className="rounded bg-rose-600 px-2 py-1 text-white text-xs hover:bg-rose-700"
                        onClick={() => {
                          void deletePrivateDeck(deck.id)
                            .then(() => refresh(query))
                            .catch((err) => setError(err instanceof Error ? err.message : 'Failed to delete private deck'))
                        }}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
