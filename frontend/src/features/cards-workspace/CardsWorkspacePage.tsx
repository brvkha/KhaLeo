import { useEffect, useState } from 'react'
import {
  createPrivateCard,
  deletePrivateCard,
  listPrivateDecks,
  searchPrivateDeckCards,
} from '../../services/privateWorkspaceApi'

type DeckOption = {
  id: string
  name: string
}

type CardItem = {
  id: string
  deckId: string
  frontText: string
  backText: string
}

export function CardsWorkspacePage() {
  const [decks, setDecks] = useState<DeckOption[]>([])
  const [selectedDeckId, setSelectedDeckId] = useState('')
  const [cards, setCards] = useState<CardItem[]>([])
  const [search, setSearch] = useState('')
  const [front, setFront] = useState('')
  const [back, setBack] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    void listPrivateDecks('')
      .then((items) => {
        const mapped = items.map((deck) => ({ id: deck.id, name: deck.name }))
        setDecks(mapped)
        if (mapped.length > 0) {
          setSelectedDeckId(mapped[0].id)
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load private decks'))
  }, [])

  useEffect(() => {
    if (!selectedDeckId) {
      setCards([])
      return
    }
    void searchPrivateDeckCards(selectedDeckId, search)
      .then((items) => setCards(items))
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to search cards'))
  }, [selectedDeckId, search])

  return (
    <section>
      <h1 className="text-2xl font-semibold">Cards Workspace</h1>
      <p className="mt-2 text-sm text-slate-600">CRUD and search are scoped to your private decks only.</p>

      <div className="mt-4 rounded border border-slate-200 bg-white p-4">
        <label className="text-sm" htmlFor="private-deck-select">Private deck</label>
        <select
          id="private-deck-select"
          className="mt-1 block rounded border border-slate-300 px-3 py-2"
          value={selectedDeckId}
          onChange={(event) => setSelectedDeckId(event.target.value)}
        >
          {decks.map((deck) => (
            <option key={deck.id} value={deck.id}>
              {deck.name}
            </option>
          ))}
        </select>
      </div>

      <form
        className="mt-3 grid gap-2 rounded border border-slate-200 bg-white p-4 md:grid-cols-2"
        onSubmit={(event) => {
          event.preventDefault()
          if (!selectedDeckId || !front.trim() || !back.trim()) {
            return
          }
          void createPrivateCard({
            deckId: selectedDeckId,
            frontText: front.trim(),
            backText: back.trim(),
          })
            .then(() => {
              setFront('')
              setBack('')
              return searchPrivateDeckCards(selectedDeckId, search)
            })
            .then((items) => setCards(items))
            .catch((err) => setError(err instanceof Error ? err.message : 'Failed to create card'))
        }}
      >
        <input
          aria-label="Private card front"
          className="rounded border border-slate-300 px-3 py-2"
          placeholder="Front"
          value={front}
          onChange={(event) => setFront(event.target.value)}
        />
        <input
          aria-label="Private card back"
          className="rounded border border-slate-300 px-3 py-2"
          placeholder="Back"
          value={back}
          onChange={(event) => setBack(event.target.value)}
        />
        <button className="rounded bg-emerald-700 px-3 py-2 text-white md:col-span-2" type="submit">
          Create private card
        </button>
      </form>

      <div className="mt-3 rounded border border-slate-200 bg-white p-4">
        <label className="text-sm" htmlFor="private-card-search">Card search</label>
        <input
          id="private-card-search"
          className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
          placeholder="Search cards in selected private deck"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>

      {error ? <p className="mt-3 text-sm text-rose-600">{error}</p> : null}

      <ul className="mt-4 grid gap-3 md:grid-cols-2">
        {cards.map((card) => (
          <li className="rounded border border-slate-200 bg-white p-4" key={card.id}>
            <p className="font-medium">{card.frontText}</p>
            <p className="text-sm text-slate-600">{card.backText}</p>
            <button
              className="mt-2 rounded bg-rose-600 px-3 py-1 text-white"
              onClick={() => {
                void deletePrivateCard(card.id)
                  .then(() => searchPrivateDeckCards(selectedDeckId, search))
                  .then((items) => setCards(items))
                  .catch((err) => setError(err instanceof Error ? err.message : 'Failed to delete card'))
              }}
            >
              Delete
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
