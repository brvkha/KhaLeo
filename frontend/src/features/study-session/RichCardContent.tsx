import { useEffect, useMemo, useRef } from 'react'
import { StudyCardImage } from '../../components/StudyCardImage'
import type { StudySessionCardDto } from '../../services/studySessionApi'

type RichCardContentProps = {
  card: StudySessionCardDto
  revealed: boolean
}

export function RichCardContent({ card, revealed }: RichCardContentProps) {
  const examplesRef = useRef<HTMLDivElement | null>(null)

  const term = useMemo(() => card.term ?? card.frontText, [card.frontText, card.term])
  const answer = useMemo(() => card.answer ?? card.backText, [card.answer, card.backText])
  const examples = useMemo(() => card.examples ?? [], [card.examples])

  useEffect(() => {
    if (examplesRef.current) {
      examplesRef.current.scrollTop = 0
    }
  }, [revealed, card.cardId])

  if (!revealed) {
    return (
      <div className="space-y-4">
        <div className="min-h-48 flex items-center justify-center text-center">
          <p className="text-3xl font-semibold text-slate-900">{term}</p>
        </div>
        {card.imageUrl ? <StudyCardImage imageUrl={card.imageUrl} alt={term} /> : null}
      </div>
    )
  }

  return (
    <div className="space-y-3" data-testid="rich-card-back-content">
      <p className="text-2xl font-semibold text-slate-900">{term}</p>
      {card.imageUrl ? <StudyCardImage imageUrl={card.imageUrl} alt={term} /> : null}
      <p className="text-xl text-slate-700">{answer}</p>
      {card.phonetic || card.partOfSpeech ? (
        <div className="text-sm text-slate-600" data-testid="rich-card-metadata">
          {card.phonetic ? <p>Phonetic: {card.phonetic}</p> : null}
          {card.partOfSpeech ? <p>Part of speech: {card.partOfSpeech}</p> : null}
        </div>
      ) : null}
      <div className="max-h-36 overflow-y-auto rounded border border-slate-200 p-2" data-testid="rich-card-examples-scroll" ref={examplesRef}>
        {examples.length === 0 ? (
          <p className="text-sm text-slate-400">No examples</p>
        ) : (
          <ul className="list-disc space-y-1 pl-5 text-sm text-slate-700">
            {examples.map((example, index) => (
              <li key={`${card.cardId}-ex-${index}`}>{example}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
