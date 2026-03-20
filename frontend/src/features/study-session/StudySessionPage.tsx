import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getNextSessionCards, rateSessionCard, type StudySessionCardDto } from '../../services/studySessionApi'

type UiRating = 'Again' | 'Hard' | 'Good' | 'Easy'

const uiRatings: UiRating[] = ['Again', 'Hard', 'Good', 'Easy']

function toApiRating(rating: UiRating): 'AGAIN' | 'HARD' | 'GOOD' | 'EASY' {
  return rating.toUpperCase() as 'AGAIN' | 'HARD' | 'GOOD' | 'EASY'
}

export function StudySessionPage() {
  const { deckId } = useParams<{ deckId: string }>()
  const navigate = useNavigate()

  const [cards, setCards] = useState<StudySessionCardDto[]>([])
  const [loading, setLoading] = useState(false)
  const [rating, setRating] = useState(false)
  const [revealed, setRevealed] = useState(false)
  const [error, setError] = useState('')
  const [shownAt, setShownAt] = useState<number>(Date.now())

  const current = useMemo(() => cards[0], [cards])

  const refresh = async () => {
    if (!deckId) {
      setError('Missing deck id for study session')
      return
    }
    setLoading(true)
    setError('')
    try {
      const nextCards = await getNextSessionCards(deckId)
      setCards(nextCards)
      setRevealed(false)
      setShownAt(Date.now())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load session cards')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [deckId])

  const onRate = async (value: UiRating) => {
    if (!current || rating) {
      return
    }
    setRating(true)
    setError('')
    try {
      const elapsed = Math.max(0, Date.now() - shownAt)
      await rateSessionCard(current.cardId, toApiRating(value), elapsed)
      setCards((prev) => prev.slice(1))
      setRevealed(false)
      setShownAt(Date.now())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit rating')
    } finally {
      setRating(false)
    }
  }

  return (
    <section>
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">Study Session</h1>
        <button className="rounded border border-slate-300 px-3 py-2" onClick={() => navigate('/study')}>
          Back to study workspace
        </button>
      </div>

      <p className="mt-2 text-sm text-slate-600">Deck: {deckId ?? 'unknown'}</p>
      <p className="mt-1 text-sm text-slate-600">Cards remaining: {cards.length}</p>

      {error ? <p className="mt-3 text-sm text-rose-600">{error}</p> : null}
      {loading ? <p className="mt-3 text-sm text-slate-500">Loading session...</p> : null}

      {!loading && !current ? (
        <p className="mt-4 rounded border border-emerald-200 bg-emerald-50 p-4">
          Session complete. No due cards right now.
        </p>
      ) : null}

      {current ? (
        <article className="mt-4 rounded border border-slate-200 bg-white p-5">
          <p className="text-xs uppercase tracking-wide text-slate-500">Front side</p>
          <p className="mt-1 text-lg font-medium">{current.frontText}</p>

          {revealed ? (
            <>
              <p className="mt-4 text-xs uppercase tracking-wide text-slate-500">Back side</p>
              <p className="mt-1 text-slate-700">{current.backText}</p>
            </>
          ) : null}

          <div className="mt-4 flex flex-wrap gap-2">
            <button
              className="rounded bg-slate-900 px-3 py-2 text-white"
              onClick={() => setRevealed(true)}
              disabled={revealed}
            >
              {revealed ? 'Answer revealed' : 'Reveal answer'}
            </button>
            {uiRatings.map((value) => (
              <button
                className="rounded border border-slate-300 px-3 py-2 disabled:opacity-50"
                key={value}
                onClick={() => void onRate(value)}
                disabled={!revealed || rating}
              >
                {value}
              </button>
            ))}
          </div>
        </article>
      ) : null}
    </section>
  )
}