type Props = {
  term: string
  answer: string
  imageUrl: string
  partOfSpeech: string
  phonetic: string
  examples: string[]
}

export function RichCardPreview({ term, answer, imageUrl, partOfSpeech, phonetic, examples }: Props) {
  return (
    <div className="rounded border border-slate-200 bg-white p-4">
      <h3 className="text-sm font-semibold text-slate-700">Preview</h3>
      <p className="mt-2 text-lg font-semibold">{term || 'Term preview'}</p>
      {imageUrl ? <p className="mt-1 text-xs text-slate-600">Image: {imageUrl}</p> : null}
      <p className="mt-2 whitespace-pre-wrap text-sm text-slate-700">{answer || 'Answer preview'}</p>
      {phonetic ? <p className="mt-1 text-xs text-slate-500">Phonetic: {phonetic}</p> : null}
      {partOfSpeech ? <p className="mt-1 text-xs text-slate-500">Part of speech: {partOfSpeech}</p> : null}
      {examples.length > 0 ? (
        <ul className="mt-2 list-disc space-y-1 pl-5 text-xs text-slate-600">
          {examples.filter((item) => item.trim()).map((item, index) => (
            <li key={index}>{item}</li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}
