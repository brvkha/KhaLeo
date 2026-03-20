import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { createPrivateDeck, deletePrivateDeck, listPrivateDecks } from '../../services/privateWorkspaceApi'

export function StudyWorkspacePage() {
  const [query, setQuery] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [decks, setDecks] = useState<Array<{ id: string; name: string; description: string | null; isPublic?: boolean }>>([])

  const visibleDecks = useMemo(
    () => decks.filter((deck) => deck.isPublic !== true),
    [decks],
  )

  const refresh = async (searchQuery = query) => {
    setLoading(true)
    setError('')
    try {
      const nextDecks = await listPrivateDecks(searchQuery)
      setDecks(nextDecks)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load private decks')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh('')
  }, [])

  return (
    <section>
      <h1 className="text-2xl font-semibold">Study Workspace</h1>
      <p className="mt-2 text-sm text-slate-600">Only your private decks are shown in this workspace.</p>

      <div className="mt-4 rounded border border-slate-200 bg-white p-4">
        <label className="text-sm" htmlFor="private-deck-search">Private deck search</label>
        <div className="mt-2 flex gap-2">
          <input
            id="private-deck-search"
            className="w-full rounded border border-slate-300 px-3 py-2"
            placeholder="Search private decks"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <button className="rounded bg-slate-900 px-3 py-2 text-white" onClick={() => void refresh(query)}>
            Search
          </button>
        </div>
      </div>

      <form
        className="mt-4 grid gap-2 rounded border border-slate-200 bg-white p-4 md:grid-cols-3"
        onSubmit={(event) => {
          event.preventDefault()
          if (!name.trim()) {
            return
          }
          void createPrivateDeck({ name: name.trim(), description: description.trim() })
            .then(() => {
              setName('')
              setDescription('')
              return refresh(query)
            })
            .catch((err) => setError(err instanceof Error ? err.message : 'Failed to create private deck'))
        }}
      >
        <input
          aria-label="Private deck name"
          className="rounded border border-slate-300 px-3 py-2"
          placeholder="Deck name"
          value={name}
          onChange={(event) => setName(event.target.value)}
        />
        <input
          aria-label="Private deck description"
          className="rounded border border-slate-300 px-3 py-2"
          placeholder="Description"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
        />
        <button className="rounded bg-emerald-700 px-3 py-2 text-white" type="submit">
          Create private deck
        </button>
      </form>

      {error ? <p className="mt-3 text-sm text-rose-600">{error}</p> : null}
      {loading ? <p className="mt-3 text-sm text-slate-500">Loading...</p> : null}

      <ul className="mt-4 grid gap-3 md:grid-cols-2">
        {visibleDecks.map((deck) => (
          <li className="rounded border border-slate-200 bg-white p-4" key={deck.id}>
            <h2 className="font-semibold">{deck.name}</h2>
            <p className="text-sm text-slate-600">{deck.description ?? ''}</p>
            <div className="mt-2 flex gap-2">
              <Link
                className="rounded border border-slate-300 px-3 py-1"
                to={`/study/session/${deck.id}`}
              >
                Start session
              </Link>
              <button
                className="rounded bg-rose-600 px-3 py-1 text-white"
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
    </section>
  )
}
