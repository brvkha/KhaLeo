import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { RichCardContent } from '../../features/study-session/RichCardContent'

describe('rich card layout', () => {
  it('renders back-face content in required order and hides null metadata', () => {
    render(
      <RichCardContent
        revealed
        card={{
          cardId: 'c1',
          deckId: 'd1',
          term: 'Term',
          answer: 'Answer',
          imageUrl: 'https://images.unsplash.com/photo-1',
          partOfSpeech: null,
          phonetic: null,
          examples: ['Example one'],
          frontText: 'Term',
          backText: 'Answer',
          state: 'NEW',
          nextReviewDate: null,
          sourceTier: 'NEW',
        }}
      />,
    )

    const container = screen.getByTestId('rich-card-back-content')
    const text = container.textContent ?? ''

    expect(text.indexOf('Term')).toBeLessThan(text.indexOf('Answer'))
    expect(text.indexOf('Answer')).toBeLessThan(text.indexOf('Example one'))
    expect(screen.queryByTestId('rich-card-metadata')).not.toBeInTheDocument()
  })
})
