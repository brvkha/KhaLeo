import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getNextSessionCards, rateSessionCard, type RateCardResponseDto, type StudySessionCardDto } from '../../services/studySessionApi'

type UiRating = 'Again' | 'Hard' | 'Good' | 'Easy'

const uiRatings: UiRating[] = ['Again', 'Hard', 'Good', 'Easy']

function toApiRating(rating: UiRating): 'AGAIN' | 'HARD' | 'GOOD' | 'EASY' {
  return rating.toUpperCase() as 'AGAIN' | 'HARD' | 'GOOD' | 'EASY'
}

function formatNextReview(nextReviewAt: string): string {
  const nextTime = new Date(nextReviewAt)
  const now = new Date()
  const diffMs = nextTime.getTime() - now.getTime()
  
  if (diffMs < 60000) {
    return '~1 min'
  } else if (diffMs < 3600000) {
    const minutes = Math.round(diffMs / 60000)
    return `~${minutes} min`
  } else if (diffMs < 86400000) {
    const hours = Math.round(diffMs / 3600000)
    return `~${hours}h`
  } else {
    const days = Math.round(diffMs / 86400000)
    return `~${days}d`
  }
}

function predictNextReviewTime(sourceTier: string, rating: UiRating): string {
  const isLearning = sourceTier.includes('LEARNING')
  const isReview = sourceTier.includes('REVIEW')
  const isNew = sourceTier === 'NEW'

  if (rating === 'Again') {
    return '1 min'
  } else if (rating === 'Hard') {
    if (isNew) return '5 min'
    if (isLearning) return '10 min'
    if (isReview) return '2-5 d'
    return '5 min'
  } else if (rating === 'Good') {
    if (isNew) return '1 d'
    if (isLearning) return '1 d'
    if (isReview) return '5-10 d'
    return '1 d'
  } else if (rating === 'Easy') {
    if (isNew) return '4 d'
    if (isLearning) return '3 d'
    if (isReview) return '10-30 d'
    return '4 d'
  }
  return ''
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
  const [lastRating, setLastRating] = useState<RateCardResponseDto | null>(null)

  const current = useMemo(() => cards[0], [cards])

  const refresh = useCallback(async () => {
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
  }, [deckId])

  useEffect(() => {
    void refresh()
  }, [refresh])

  useEffect(() => {
    if (loading || cards.length > 0) {
      return
    }

    const timer = window.setInterval(() => {
      void refresh()
    }, 15_000)

    return () => window.clearInterval(timer)
  }, [cards.length, loading, refresh])

  const onRate = async (value: UiRating) => {
    if (!current || rating) {
      return
    }
    setRating(true)
    setError('')
    try {
      const elapsed = Math.max(0, Date.now() - shownAt)
      const result = await rateSessionCard(current.cardId, toApiRating(value), elapsed)
      setLastRating(result)
      setCards((prev) => {
        const next = prev.slice(1)
        if (next.length === 0) {
          void refresh()
        }
        return next
      })
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

          <div className="mt-4 flex flex-wrap gap-3">
            <button
              className="rounded bg-slate-900 px-3 py-2 text-white"
              onClick={() => setRevealed(true)}
              disabled={revealed}
            >
              {revealed ? 'Answer revealed' : 'Reveal answer'}
            </button>
            <div className="flex flex-wrap gap-3">
              {uiRatings.map((value) => (
                <div key={value} className="flex flex-col items-center">
                  <button
                    className="rounded border border-slate-300 px-3 py-2 disabled:opacity-50 min-w-16 text-center"
                    onClick={() => void onRate(value)}
                    disabled={!revealed || rating}
                  >
                    {value}
                  </button>
                  {revealed && (
                    <p className="text-xs text-slate-500 font-medium mt-1">
                      {predictNextReviewTime(current?.sourceTier ?? 'NEW', value)}
                    </p>
                  )}
                </div>
              ))}
            </div>
          </div>

          {lastRating ? (
            <div className="mt-4 rounded border border-blue-200 bg-blue-50 p-3">
              <p className="text-sm font-medium text-blue-900">
                Rated: <span className="capitalize">{lastRating.state}</span>
              </p>
              <p className="text-sm text-blue-700 mt-1">
                Next review: {formatNextReview(lastRating.nextReviewAt)}
              </p>
            </div>
          ) : null}
        </article>
      ) : null}
    </section>
  )
}