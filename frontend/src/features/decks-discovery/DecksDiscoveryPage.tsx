import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listPublicDecks, importPublicDeck, type PublicDeckSummaryDto } from '../../services/publicDiscoveryApi'
import { useAuthStore } from '../../store/authStore'
import { useNotificationStore } from '../../store/notificationStore'

type ViewMode = 'card' | 'table'

export function DecksDiscoveryPage() {
  const currentUser = useAuthStore((state) => state.currentUser)
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [decks, setDecks] = useState<PublicDeckSummaryDto[]>([])
  const [error, setError] = useState('')
  const [viewMode, setViewMode] = useState<ViewMode>('table')
  const [loading, setLoading] = useState(false)
  const pushSuccess = useNotificationStore((state) => state.pushSuccess)
  const pushError = useNotificationStore((state) => state.pushError)

  const refresh = async (searchQuery = query) => {
    setError('')
    setLoading(true)
    try {
      const items = await listPublicDecks(searchQuery)
      setDecks(items)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load public decks'
      setError(message)
      console.error('public_decks_load_failed', err)
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
          <h1 className="text-2xl font-semibold">Public Deck Discovery</h1>
          <p className="mt-1 text-sm text-slate-600">Browse only public decks. Import requires sign-in.</p>
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
        <label className="text-sm" htmlFor="public-deck-search">Search public decks</label>
        <input
          id="public-deck-search"
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
          {decks.map((deck) => (
            <li className="rounded border border-slate-200 bg-white p-4" key={deck.id}>
              <h2 className="font-semibold">{deck.name}</h2>
              <p className="text-sm text-slate-600">{deck.description ?? ''}</p>
              <p className="mt-1 text-xs text-slate-500">Owner: {deck.ownerName}</p>
              <p className="mt-1 text-xs text-slate-500 font-medium">Cards: {deck.cardCount}</p>
              <button
                className="mt-3 w-full rounded bg-emerald-700 px-3 py-2 text-white hover:bg-emerald-800"
                onClick={() => {
                  if (!currentUser) {
                    const message = 'Please sign in to import decks'
                    setError(message)
                    pushError(message)
                    navigate('/login?returnTo=%2Fdecks')
                    return
                  }
                  void importPublicDeck(deck.id)
                    .then(() => {
                      pushSuccess(`Import deck ${deck.name} thành công.`)
                    })
                    .catch((err) => {
                      const message = err instanceof Error ? err.message : 'Failed to import deck'
                      setError(message)
                      console.error('public_deck_import_failed', err)
                    })
                }}
              >
                Import to Study
              </button>
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
                <th className="p-3 text-left font-semibold text-slate-700">Owner</th>
                <th className="p-3 text-center font-semibold text-slate-700">Cards</th>
                <th className="p-3 text-right font-semibold text-slate-700">Action</th>
              </tr>
            </thead>
            <tbody>
              {decks.map((deck, idx) => (
                <tr key={deck.id} className={`border-b border-slate-200 ${idx % 2 === 0 ? 'bg-white' : 'bg-slate-50'}`}>
                  <td className="p-3 font-medium text-slate-900">{deck.name}</td>
                  <td className="p-3 text-slate-600 truncate">{deck.description ?? ''}</td>
                  <td className="p-3 text-slate-600">{deck.ownerName}</td>
                  <td className="p-3 text-center text-slate-600">{deck.cardCount}</td>
                  <td className="p-3 text-right">
                    <button
                      className="rounded bg-emerald-700 px-3 py-1 text-white text-xs hover:bg-emerald-800"
                      onClick={() => {
                        if (!currentUser) {
                          const message = 'Please sign in to import decks'
                          setError(message)
                          pushError(message)
                          navigate('/login?returnTo=%2Fdecks')
                          return
                        }
                        void importPublicDeck(deck.id)
                          .then(() => {
                            pushSuccess(`Import deck ${deck.name} thành công.`)
                          })
                          .catch((err) => {
                            const message = err instanceof Error ? err.message : 'Failed to import deck'
                            setError(message)
                            console.error('public_deck_import_failed', err)
                          })
                      }}
                    >
                      Import
                    </button>
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
