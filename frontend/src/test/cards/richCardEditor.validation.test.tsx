import { describe, expect, it } from 'vitest'
import { validateRichCardDraft } from '../../features/cards/richCardValidation'

describe('rich card editor validation', () => {
  it('accepts valid payload', () => {
    const errors = validateRichCardDraft({
      term: 'Abstraction',
      answer: 'Generalized representation',
      imageUrl: 'https://images.unsplash.com/photo-1',
      examples: ['Example one', 'Example two'],
    })

    expect(errors).toEqual([])
  })

  it('rejects whitespace examples and invalid image url', () => {
    const errors = validateRichCardDraft({
      term: 'Abstraction',
      answer: 'Generalized representation',
      imageUrl: 'http://example.com/image.jpg',
      examples: ['   '],
    })

    expect(errors).toContain('example-empty')
    expect(errors).toContain('image-url-invalid')
  })
})
