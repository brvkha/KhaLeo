import { useEffect, useState, useRef, useCallback } from 'react'
import {
  createPrivateCard,
  createPrivateDeck,
  deletePrivateCard,
  deletePrivateDeck,
  listPrivateDecks,
  searchPrivateDeckCards,
  updatePrivateCard,
  updatePrivateDeck,
} from '../../services/privateWorkspaceApi'
import { AddCardModal } from '../../components/AddCardModal'

type DeckOption = {
  id: string
  name: string
  description: string | null
}

type CardItem = {
  id: string
  deckId: string
  frontText: string
  backText: string
}

const CARDS_PER_PAGE = 50

export function CardsWorkspacePage() {
  const [decks, setDecks] = useState<DeckOption[]>([])
  const [selectedDeckId, setSelectedDeckId] = useState('')
  const [cards, setCards] = useState<CardItem[]>([])
  const [search, setSearch] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [newDeckName, setNewDeckName] = useState('')
  const [newDeckDesc, setNewDeckDesc] = useState('')
  const [showCreateDeck, setShowCreateDeck] = useState(false)
  const [editingDeckId, setEditingDeckId] = useState<string | null>(null)
  const [editDeckName, setEditDeckName] = useState('')
  const [editDeckDesc, setEditDeckDesc] = useState('')
  const [editingCardId, setEditingCardId] = useState<string | null>(null)
  const [editCardFront, setEditCardFront] = useState('')
  const [editCardBack, setEditCardBack] = useState('')
  const [showAddCardModal, setShowAddCardModal] = useState(false)
  const [currentPage, setCurrentPage] = useState(0)
  const [hasMoreCards, setHasMoreCards] = useState(true)
  const [isLoadingMore, setIsLoadingMore] = useState(false)
  const cardsContainerRef = useRef<HTMLDivElement>(null)

  const loadDecks = async () => {
    try {
      const items = await listPrivateDecks('')
      setDecks(items)
      if (items.length > 0 && !selectedDeckId) {
        setSelectedDeckId(items[0].id)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load private decks')
    }
  }

  useEffect(() => {
    void loadDecks()
  }, [])

  // Search and load cards on deck/search change
  useEffect(() => {
    if (!selectedDeckId) {
      setCards([])
      setCurrentPage(0)
      return
    }

    const timer = setTimeout(() => {
      void searchPrivateDeckCards(selectedDeckId, search, 0, CARDS_PER_PAGE)
        .then((items) => {
          setCards(items)
          setCurrentPage(0)
          setHasMoreCards(items.length === CARDS_PER_PAGE)
        })
        .catch((err) => setError(err instanceof Error ? err.message : 'Failed to search cards'))
    }, 300)

    return () => clearTimeout(timer)
  }, [selectedDeckId, search])

  // Infinite scroll handler
  const handleScroll = useCallback(() => {
    if (!cardsContainerRef.current) return
    const { scrollTop, scrollHeight, clientHeight } = cardsContainerRef.current
    const isNearBottom = scrollHeight - scrollTop - clientHeight < 200

    if (isNearBottom && hasMoreCards && !isLoadingMore && selectedDeckId) {
      void loadMoreCards()
    }
  }, [hasMoreCards, isLoadingMore, selectedDeckId])

  const loadMoreCards = async () => {
    if (isLoadingMore || !hasMoreCards || !selectedDeckId) return

    setIsLoadingMore(true)
    try {
      const nextPage = currentPage + 1
      const newCards = await searchPrivateDeckCards(
        selectedDeckId,
        search,
        nextPage,
        CARDS_PER_PAGE,
      )
      setCards((prev) => [...prev, ...newCards])
      setCurrentPage(nextPage)
      setHasMoreCards(newCards.length === CARDS_PER_PAGE)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load more cards')
    } finally {
      setIsLoadingMore(false)
    }
  }

  const handleCreateDeck = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newDeckName.trim()) return
    try {
      setError('')
      await createPrivateDeck({
        name: newDeckName.trim(),
        description: newDeckDesc.trim(),
      })
      setNewDeckName('')
      setNewDeckDesc('')
      setShowCreateDeck(false)
      setSuccess('Deck created')
      await loadDecks()
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create deck')
    }
  }

  const handleDeleteDeck = async (deckId: string) => {
    if (!confirm('Delete this deck and all its cards?')) return
    try {
      setError('')
      await deletePrivateDeck(deckId)
      setSuccess('Deck deleted')
      await loadDecks()
      if (selectedDeckId === deckId) {
        setSelectedDeckId('')
        setCards([])
      }
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete deck')
    }
  }

  const handleUpdateDeck = async (deckId: string) => {
    if (!editDeckName.trim()) return
    try {
      setError('')
      await updatePrivateDeck({
        deckId,
        name: editDeckName.trim(),
        description: editDeckDesc.trim(),
      })
      setEditingDeckId(null)
      setSuccess('Deck updated')
      await loadDecks()
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update deck')
    }
  }

  const selectedDeck = decks.find((d) => d.id === selectedDeckId)

  const startEditCard = (card: CardItem) => {
    setEditingCardId(card.id)
    setEditCardFront(card.frontText)
    setEditCardBack(card.backText)
  }

  const cancelEditCard = () => {
    setEditingCardId(null)
    setEditCardFront('')
    setEditCardBack('')
  }

  const handleUpdateCard = async () => {
    if (!editingCardId || !editCardFront.trim() || !editCardBack.trim() || !selectedDeckId) {
      return
    }

    try {
      setError('')
      await updatePrivateCard({
        cardId: editingCardId,
        frontText: editCardFront.trim(),
        backText: editCardBack.trim(),
      })
      const items = await searchPrivateDeckCards(selectedDeckId, search, 0, CARDS_PER_PAGE)
      setCards(items)
      setSuccess('Card updated')
      cancelEditCard()
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update card')
    }
  }

  const handleAddCard = async (front: string, back: string) => {
    if (!selectedDeckId) return
    try {
      setError('')
      await createPrivateCard({
        deckId: selectedDeckId,
        frontText: front,
        backText: back,
      })
      const items = await searchPrivateDeckCards(selectedDeckId, search, 0, CARDS_PER_PAGE)
      setCards(items)
      setSuccess('Card added')
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create card')
    }
  }

  return (
    <section className="h-[calc(100vh-12rem)] min-h-[560px] flex flex-col overflow-hidden">
      <div className="px-6 py-4 border-b border-slate-200">
        <h1 className="text-2xl font-semibold">Cards Workspace</h1>
        <p className="mt-1 text-sm text-slate-600">Manage your private decks and cards</p>
      </div>

      {error && (
        <div className="mx-6 mt-3 rounded border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">
          {error}
        </div>
      )}
      {success && (
        <div className="mx-6 mt-3 rounded border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700">
          {success}
        </div>
      )}

      <div className="flex flex-1 gap-4 overflow-hidden">
        {/* Left Panel: Decks List */}
        <div className="w-44 flex flex-col border border-slate-200 rounded bg-white overflow-hidden">
          <div className="border-b border-slate-200 p-4">
            <h2 className="font-semibold text-slate-900">Private Decks</h2>
            <button
              onClick={() => setShowCreateDeck(true)}
              className="mt-2 w-full rounded bg-emerald-600 px-3 py-2 text-white text-sm hover:bg-emerald-700"
            >
              + Create Deck
            </button>
          </div>

          {showCreateDeck && (
            <form onSubmit={handleCreateDeck} className="border-b border-slate-200 p-4">
              <input
                className="w-full rounded border border-slate-300 px-2 py-1 text-sm mb-2"
                placeholder="Deck name"
                value={newDeckName}
                onChange={(e) => setNewDeckName(e.target.value)}
              />
              <textarea
                className="w-full rounded border border-slate-300 px-2 py-1 text-sm mb-2"
                placeholder="Description"
                rows={2}
                value={newDeckDesc}
                onChange={(e) => setNewDeckDesc(e.target.value)}
              />
              <div className="flex gap-2">
                <button type="submit" className="flex-1 rounded bg-emerald-600 px-2 py-1 text-white text-sm hover:bg-emerald-700">
                  Save
                </button>
                <button
                  type="button"
                  onClick={() => setShowCreateDeck(false)}
                  className="flex-1 rounded border border-slate-300 px-2 py-1 text-sm hover:bg-slate-100"
                >
                  Cancel
                </button>
              </div>
            </form>
          )}

          <div className="flex-1 overflow-y-auto">
            {decks.length === 0 ? (
              <div className="p-4 text-center text-sm text-slate-500">No decks yet</div>
            ) : (
              <ul className="space-y-1 p-2">
                {decks.map((deck) => (
                  <li key={deck.id}>
                    {editingDeckId === deck.id ? (
                      <form onSubmit={(e) => { e.preventDefault(); void handleUpdateDeck(deck.id) }} className="p-2 space-y-2">
                        <input
                          className="w-full rounded border border-slate-300 px-2 py-1 text-sm"
                          value={editDeckName}
                          onChange={(e) => setEditDeckName(e.target.value)}
                        />
                        <textarea
                          className="w-full rounded border border-slate-300 px-2 py-1 text-sm"
                          rows={2}
                          value={editDeckDesc}
                          onChange={(e) => setEditDeckDesc(e.target.value)}
                        />
                        <div className="flex gap-1">
                          <button type="submit" className="flex-1 rounded bg-blue-600 px-2 py-1 text-white text-xs hover:bg-blue-700">
                            Save
                          </button>
                          <button
                            type="button"
                            onClick={() => setEditingDeckId(null)}
                            className="flex-1 rounded border border-slate-300 px-2 py-1 text-xs hover:bg-slate-100"
                          >
                            Cancel
                          </button>
                        </div>
                      </form>
                    ) : (
                      <div
                        onClick={() => setSelectedDeckId(deck.id)}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault()
                            setSelectedDeckId(deck.id)
                          }
                        }}
                        className={`w-full text-left p-3 rounded transition cursor-pointer ${
                          selectedDeckId === deck.id
                            ? 'bg-blue-100 border border-blue-300'
                            : 'bg-slate-50 border border-transparent hover:bg-slate-100'
                        }`}
                      >
                        <p className="font-medium text-sm">{deck.name}</p>
                        {deck.description && (
                          <p className="text-xs text-slate-500 truncate">{deck.description}</p>
                        )}
                        <div className="mt-2 flex gap-1">
                          <button
                            onClick={(e) => {
                              e.stopPropagation()
                              setEditingDeckId(deck.id)
                              setEditDeckName(deck.name)
                              setEditDeckDesc(deck.description || '')
                            }}
                            className="text-xs px-2 py-1 rounded border border-slate-300 hover:bg-slate-200"
                          >
                            Edit
                          </button>
                          <button
                            onClick={(e) => {
                              e.stopPropagation()
                              void handleDeleteDeck(deck.id)
                            }}
                            className="text-xs px-2 py-1 rounded border border-rose-300 text-rose-600 hover:bg-rose-50"
                          >
                            Delete
                          </button>
                        </div>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>

        {/* Right Panel: Cards Table */}
        <div className="flex-1 flex flex-col border border-slate-200 rounded bg-white overflow-hidden">
          {selectedDeck ? (
            <>
              <div className="border-b border-slate-200 p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="font-semibold text-slate-900">{selectedDeck.name}</h2>
                    <p className="text-sm text-slate-600 mt-1">{cards.length} cards loaded</p>
                  </div>
                  <button
                    onClick={() => setShowAddCardModal(true)}
                    className="rounded bg-emerald-600 px-4 py-2 text-white text-sm hover:bg-emerald-700"
                  >
                    + Add Card
                  </button>
                </div>
              </div>

              <div className="border-b border-slate-200 px-4 py-3 flex items-center justify-between gap-3">
                <input
                  id="private-card-search"
                  className="flex-1 rounded border border-slate-300 px-3 py-2 text-sm"
                  placeholder="Search cards..."
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
                <span className="text-xs text-slate-500 whitespace-nowrap">
                  {cards.length} {cards.length === 1 ? 'card' : 'cards'}
                </span>
              </div>

              <div
                className="flex-1 overflow-auto"
                ref={cardsContainerRef}
                onScroll={handleScroll}
              >
                {cards.length === 0 ? (
                  <div className="p-4 text-center text-sm text-slate-500">No cards found</div>
                ) : (
                  <table className="w-full text-sm">
                    <thead className="sticky top-0 border-b border-slate-200 bg-slate-50">
                      <tr>
                        <th className="p-3 text-left font-medium text-slate-700 w-16">#</th>
                        <th className="p-3 text-left font-medium text-slate-700 w-1/3">Front</th>
                        <th className="p-3 text-left font-medium text-slate-700 w-1/3">Back</th>
                        <th className="p-3 text-right font-medium text-slate-700 w-1/4">Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {cards.map((card, idx) => (
                        <tr key={card.id} className={`border-b border-slate-200 ${idx % 2 === 0 ? 'bg-white' : 'bg-slate-50'}`}>
                          <td className="p-3 text-slate-500">{idx + 1}</td>
                          <td className="p-3 text-slate-900">
                            {editingCardId === card.id ? (
                              <input
                                className="w-full rounded border border-slate-300 px-2 py-1 text-sm"
                                value={editCardFront}
                                onChange={(e) => setEditCardFront(e.target.value)}
                              />
                            ) : (
                              <p className="whitespace-pre-wrap">{card.frontText}</p>
                            )}
                          </td>
                          <td className="p-3 text-slate-600">
                            {editingCardId === card.id ? (
                              <textarea
                                className="w-full rounded border border-slate-300 px-2 py-1 text-sm"
                                rows={3}
                                value={editCardBack}
                                onChange={(e) => setEditCardBack(e.target.value)}
                              />
                            ) : (
                              <p className="max-h-24 overflow-y-auto whitespace-pre-wrap">{card.backText}</p>
                            )}
                          </td>
                          <td className="p-3 text-right">
                            <div className="flex justify-end gap-2">
                              {editingCardId === card.id ? (
                                <>
                                  <button
                                    className="rounded bg-blue-600 px-3 py-1 text-white text-xs hover:bg-blue-700"
                                    onClick={() => {
                                      void handleUpdateCard()
                                    }}
                                  >
                                    Save
                                  </button>
                                  <button
                                    className="rounded border border-slate-300 px-3 py-1 text-xs hover:bg-slate-100"
                                    onClick={cancelEditCard}
                                  >
                                    Cancel
                                  </button>
                                </>
                              ) : (
                                <>
                                  <button
                                    className="rounded bg-amber-600 px-3 py-1 text-white text-xs hover:bg-amber-700"
                                    onClick={() => startEditCard(card)}
                                  >
                                    Update
                                  </button>
                                  <button
                                    className="rounded bg-rose-600 px-3 py-1 text-white text-xs hover:bg-rose-700"
                                    onClick={() => {
                                      void deletePrivateCard(card.id)
                                        .then(() => searchPrivateDeckCards(selectedDeckId, search, 0, CARDS_PER_PAGE))
                                        .then((items) => {
                                          setCards(items)
                                          setCurrentPage(0)
                                          setHasMoreCards(items.length === CARDS_PER_PAGE)
                                        })
                                        .catch((err) => setError(err instanceof Error ? err.message : 'Failed to delete card'))
                                    }}
                                  >
                                    Delete
                                  </button>
                                </>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
                {isLoadingMore && (
                  <div className="p-4 text-center text-sm text-slate-500">
                    Loading more cards...
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="flex items-center justify-center h-full text-slate-500">
              <p>Select a deck to view cards</p>
            </div>
          )}
        </div>
      </div>

      <AddCardModal
        isOpen={showAddCardModal}
        onClose={() => setShowAddCardModal(false)}
        onSubmit={handleAddCard}
        deckName={selectedDeck?.name || 'New Deck'}
      />
    </section>
  )
}

