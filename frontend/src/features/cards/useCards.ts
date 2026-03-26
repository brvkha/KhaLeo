import { useMemo } from 'react'
import { useLearningStore } from '../../store/learningStore'

export function useCards(deckFilter: string) {
  const cards = useLearningStore((state) => state.cards)
  const createCard = useLearningStore((state) => state.createCard)
  const updateCard = useLearningStore((state) => state.updateCard)
  const deleteCard = useLearningStore((state) => state.deleteCard)

  const filtered = useMemo(() => {
    if (!deckFilter) {
      return cards
    }
    return cards.filter((card) => card.deckId === deckFilter)
  }, [cards, deckFilter])

  const createRichCard = (payload: {
    deckId: string
    term: string
    answer: string
    imageUrl?: string | null
    partOfSpeech?: string | null
    phonetic?: string | null
    examples?: string[]
  }) => {
    createCard(payload.deckId, payload.term, payload.answer, payload.examples ?? [])
  }

  const updateRichCard = (payload: {
    cardId: string
    term: string
    answer: string
    version: number
    imageUrl?: string | null
    partOfSpeech?: string | null
    phonetic?: string | null
    examples?: string[]
  }) => {
    updateCard(payload.cardId, payload.term, payload.answer, payload.examples ?? [])
  }

  return { cards: filtered, createCard, updateCard, deleteCard, createRichCard, updateRichCard }
}
