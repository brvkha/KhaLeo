import { describe, expect, it } from 'vitest'
import { validateCardContract } from './dto'

describe('rich card contract mapping', () => {
  it('accepts valid rich card payload', () => {
    expect(
      validateCardContract({
        cards: [
          {
            id: 'c1',
            deckId: 'd1',
            term: 'Abstraction',
            answer: 'Generalized representation',
            examples: ['Example one'],
            version: 1,
            imageUrl: null,
            partOfSpeech: null,
            phonetic: null,
          },
        ],
      }),
    ).toBe(true)
  })

  it('rejects missing required rich card fields', () => {
    expect(
      validateCardContract({
        cards: [
          {
            id: 'c2',
            deckId: 'd1',
            term: '',
            answer: 'Answer',
            examples: [],
            version: 0,
          },
        ] as any,
      }),
    ).toBe(false)
  })
})
