import { describe, expect, it } from 'vitest'
import { validateCardContract, validateDeckContract } from './dto'

describe('contract drift checks', () => {
  it('accepts valid deck contract and rejects missing names', () => {
    expect(
      validateDeckContract({
        decks: [{ id: 'd1', name: 'Deck', description: 'Description' }],
      }),
    ).toBe(true)

    expect(
      validateDeckContract({
        decks: [{ id: 'd2', name: '', description: 'Broken' }],
      }),
    ).toBe(false)
  })

  it('accepts valid card contract and rejects missing fields', () => {
    expect(
      validateCardContract({
        cards: [
          {
            id: 'c1',
            deckId: 'd1',
            term: 'Term',
            answer: 'Answer',
            examples: ['core'],
            version: 1,
          },
        ],
      }),
    ).toBe(true)

    expect(
      validateCardContract({
        cards: [
          {
            id: 'c2',
            deckId: '',
            term: 'Term',
            answer: 'Answer',
            examples: [],
            version: 0,
          },
        ],
      }),
    ).toBe(false)
  })
})
