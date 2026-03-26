type DeckDto = {
  id: string
  name: string
  description: string
}

type CardDto = {
  id: string
  deckId: string
  term: string
  answer: string
  imageUrl?: string | null
  partOfSpeech?: string | null
  phonetic?: string | null
  examples: string[]
  version: number
}

export type DeckContract = {
  decks: DeckDto[]
}

export type CardContract = {
  cards: CardDto[]
}

export function validateDeckContract(input: DeckContract): boolean {
  return input.decks.every((deck) => Boolean(deck.id && deck.name))
}

export function validateCardContract(input: CardContract): boolean {
  return input.cards.every((card) => Boolean(card.id && card.deckId && card.term && card.answer && Number.isFinite(card.version)))
}
