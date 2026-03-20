import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { StudyWorkspacePage } from '../../features/study-workspace/StudyWorkspacePage'

vi.mock('../../services/privateWorkspaceApi', () => ({
  listPrivateDecks: vi.fn(),
  createPrivateDeck: vi.fn(async () => undefined),
  deletePrivateDeck: vi.fn(async () => undefined),
}))

import { listPrivateDecks } from '../../services/privateWorkspaceApi'

describe('StudyWorkspacePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows only private decks and supports private search flow', async () => {
    vi.mocked(listPrivateDecks).mockResolvedValue([
      { id: 'd-private', name: 'Owned Private Deck', description: 'mine', isPublic: false },
      { id: 'd-public', name: 'Public Deck', description: 'not private', isPublic: true },
    ])

    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <StudyWorkspacePage />
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByText('Owned Private Deck')).toBeInTheDocument())
    expect(screen.queryByText('Public Deck')).not.toBeInTheDocument()

    await user.type(screen.getByLabelText('Private deck search'), 'owned')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    await waitFor(() => {
      expect(listPrivateDecks).toHaveBeenCalledWith('owned')
    })
  })
})