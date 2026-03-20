import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { DecksDiscoveryPage } from '../../features/decks-discovery/DecksDiscoveryPage'
import { useAuthStore } from '../../store/authStore'

vi.mock('../../services/publicDiscoveryApi', () => ({
  listPublicDecks: vi.fn(),
  importPublicDeck: vi.fn(async () => undefined),
}))

import { listPublicDecks } from '../../services/publicDiscoveryApi'

describe('DecksDiscoveryPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ currentUser: null })
    vi.clearAllMocks()
  })

  it('allows guest browse and redirects login for import action', async () => {
    vi.mocked(listPublicDecks).mockResolvedValue([
      {
        id: 'd-public',
        name: 'Public Deck One',
        ownerName: 'owner@example.com',
        description: 'deck desc',
        tags: [],
        cardCount: 10,
        updatedAt: new Date().toISOString(),
      },
    ])

    const user = userEvent.setup()

    render(
      <MemoryRouter initialEntries={['/decks']}>
        <Routes>
          <Route path="/decks" element={<DecksDiscoveryPage />} />
          <Route path="/login" element={<p>Login Screen</p>} />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByText('Public Deck One')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: 'Import to Study' }))

    await waitFor(() => expect(screen.getByText('Login Screen')).toBeInTheDocument())
  })
})
