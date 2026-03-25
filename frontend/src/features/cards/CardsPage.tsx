import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { useDecks } from '../decks/useDecks'
import { useCards } from './useCards'
import { CardSearch } from '../search/CardSearch'
import { MediaUpload } from '../media/MediaUpload'

export function CardsPage() {
  const { decks } = useDecks()
  const [deckFilter, setDeckFilter] = useState('')
  const [front, setFront] = useState('')
  const [back, setBack] = useState('')
  const [tags, setTags] = useState('')
  const [search, setSearch] = useState('')
  const { cards, createCard, deleteCard } = useCards(deckFilter)
  const [latestMedia, setLatestMedia] = useState('')

  const defaultDeckId = useMemo(() => deckFilter || decks[0]?.id || '', [deckFilter, decks])

  const onCreate = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!defaultDeckId || !front.trim() || !back.trim()) {
      return
    }
    createCard(
      defaultDeckId,
      front.trim(),
      back.trim(),
      tags
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean),
    )
    setFront('')
    setBack('')
    setTags('')
  }

  return (
    <section>
      <h1 className="text-2xl font-semibold">Card Management</h1>
      <div className="mt-4 rounded border border-slate-200 bg-white p-4">
        <label className="text-sm">Deck filter</label>
        <select
          className="mt-1 block rounded border border-slate-300 px-3 py-2"
          value={deckFilter}
          onChange={(event) => setDeckFilter(event.target.value)}
        >
          <option value="">All decks</option>
          {decks.map((deck) => (
            <option key={deck.id} value={deck.id}>
              {deck.name}
            </option>
          ))}
        </select>
      </div>
      <form className="mt-3 grid gap-3 rounded border border-slate-200 bg-white p-4 md:grid-cols-2" onSubmit={onCreate}>
        <input
          aria-label="Card front"
          className="rounded border border-slate-300 px-3 py-2"
          placeholder="Front (one concept)"
          value={front}
          onChange={(event) => setFront(event.target.value)}
        />
        <textarea
          aria-label="Card back"
          className="rounded border border-slate-300 px-3 py-2"
          placeholder="Back (explanation, examples, markdown, multiline)"
          rows={4}
          value={back}
          onChange={(event) => setBack(event.target.value)}
        />
        <input
          aria-label="Card tags"
          className="rounded border border-slate-300 px-3 py-2 md:col-span-2"
          placeholder="tags,comma,separated"
          value={tags}
          onChange={(event) => setTags(event.target.value)}
        />
        <button className="rounded bg-emerald-700 px-3 py-2 text-white md:col-span-2" type="submit">
          Create card
        </button>
      </form>
      <div className="mt-3">
        <MediaUpload onUploadSuccess={setLatestMedia} />
        {latestMedia ? <p className="mt-2 text-sm text-emerald-700">Uploaded: {latestMedia}</p> : null}
      </div>
      <div className="mt-3 rounded border border-slate-200 bg-white p-4">
        <label className="text-sm" htmlFor="card-search">Advanced search</label>
        <input
          className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
          id="card-search"
          placeholder="Search front/back/tags"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <CardSearch cards={cards} query={search} />
      </div>
      <ul className="mt-4 grid gap-3 md:grid-cols-2">
        {cards.map((card) => (
          <li className="rounded border border-slate-200 bg-white p-4" key={card.id}>
            <p className="font-medium">{card.front}</p>
            <p className="max-h-32 overflow-y-auto whitespace-pre-wrap text-sm text-slate-600">{card.back}</p>
            {card.mediaUrl ? <p className="text-xs text-indigo-700">media: {card.mediaUrl}</p> : null}
            <button
              className="mt-2 rounded bg-rose-600 px-3 py-1 text-white"
              onClick={() => deleteCard(card.id)}
            >
              Delete
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
