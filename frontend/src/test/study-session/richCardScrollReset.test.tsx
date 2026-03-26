import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { RichCardContent } from '../../features/study-session/RichCardContent'

const card = {
  cardId: 'c1',
  deckId: 'd1',
  term: 'Term',
  answer: 'Answer',
  imageUrl: null,
  partOfSpeech: null,
  phonetic: null,
  examples: Array.from({ length: 30 }, (_, i) => `Example ${i + 1}`),
  frontText: 'Term',
  backText: 'Answer',
  state: 'NEW' as const,
  nextReviewDate: null,
  sourceTier: 'NEW',
}

describe('rich card scroll reset', () => {
  it('resets examples scroll position when flip state changes', () => {
    const { rerender } = render(<RichCardContent card={card} revealed />)

    const examples = screen.getByTestId('rich-card-examples-scroll')
    examples.scrollTop = 100
    expect(examples.scrollTop).toBe(100)

    rerender(<RichCardContent card={card} revealed={false} />)
    rerender(<RichCardContent card={card} revealed />)

    expect(screen.getByTestId('rich-card-examples-scroll').scrollTop).toBe(0)
  })
})
