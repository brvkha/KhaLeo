import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listPublicDecks, importPublicDeck, type PublicDeckSummaryDto } from '../../services/publicDiscoveryApi'
import { useAuthStore } from '../../store/authStore'

export function DecksDiscoveryPage() {
  const currentUser = useAuthStore((state) => state.currentUser)
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [decks, setDecks] = useState<PublicDeckSummaryDto[]>([])
  const [error, setError] = useState('')

  const refresh = async (searchQuery = query) => {
    setError('')
    try {
      const items = await listPublicDecks(searchQuery)
      setDecks(items)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load public decks')
    }
  }

  useEffect(() => {
    void refresh('')
  }, [])

  return (
    <section>
      <h1 className="text-2xl font-semibold">Public Deck Discovery</h1>
      <p className="mt-2 text-sm text-slate-600">Browse only public decks. Import requires sign-in.</p>

      <div className="mt-4 rounded border border-slate-200 bg-white p-4">
        <label className="text-sm" htmlFor="public-deck-search">Search public decks</label>
        <div className="mt-2 flex gap-2">
          <input
            id="public-deck-search"
            className="w-full rounded border border-slate-300 px-3 py-2"
            placeholder="Search public decks"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <button className="rounded bg-slate-900 px-3 py-2 text-white" onClick={() => void refresh(query)}>
            Search
          </button>
        </div>
      </div>

      {error ? <p className="mt-3 text-sm text-rose-600">{error}</p> : null}

      <ul className="mt-4 grid gap-3 md:grid-cols-2">
        {decks.map((deck) => (
          <li className="rounded border border-slate-200 bg-white p-4" key={deck.id}>
            <h2 className="font-semibold">{deck.name}</h2>
            <p className="text-sm text-slate-600">{deck.description ?? ''}</p>
            <p className="mt-1 text-xs text-slate-500">Owner: {deck.ownerName}</p>
            <button
              className="mt-3 rounded bg-emerald-700 px-3 py-1 text-white"
              onClick={() => {
                if (!currentUser) {
                  setError('Please sign in to import decks')
                  navigate('/login')
                  return
                }
                void importPublicDeck(deck.id).catch((err) => {
                  setError(err instanceof Error ? err.message : 'Failed to import deck')
                })
              }}
            >
              Import to Study
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
