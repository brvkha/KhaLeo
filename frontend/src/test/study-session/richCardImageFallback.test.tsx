import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { StudyCardImage } from '../../components/StudyCardImage'

describe('study card image fallback', () => {
  it('shows fallback when image cannot be loaded', () => {
    render(<StudyCardImage imageUrl="https://images.unsplash.com/photo-1" alt="card" />)

    const image = screen.getByTestId('study-image')
    fireEvent.error(image)

    expect(screen.getByTestId('study-image-fallback')).toBeInTheDocument()
  })
})
