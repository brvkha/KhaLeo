import { RICH_CARD_MAX_EXAMPLE_LENGTH, RICH_CARD_MAX_EXAMPLES } from './richCardConfig'

type Props = {
  examples: string[]
  onChange: (next: string[]) => void
}

export function RichExamplesInput({ examples, onChange }: Props) {
  const update = (index: number, value: string) => {
    const next = [...examples]
    next[index] = value
    onChange(next)
  }

  const add = () => {
    if (examples.length >= RICH_CARD_MAX_EXAMPLES) {
      return
    }
    onChange([...examples, ''])
  }

  const remove = (index: number) => {
    onChange(examples.filter((_, i) => i !== index))
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <label className="text-sm font-medium">Examples ({examples.length}/{RICH_CARD_MAX_EXAMPLES})</label>
        <button
          type="button"
          className="rounded border border-slate-300 px-2 py-1 text-xs disabled:opacity-50"
          disabled={examples.length >= RICH_CARD_MAX_EXAMPLES}
          onClick={add}
        >
          Add example
        </button>
      </div>
      {examples.map((example, index) => (
        <div key={index} className="flex items-start gap-2">
          <textarea
            className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
            rows={2}
            value={example}
            maxLength={RICH_CARD_MAX_EXAMPLE_LENGTH}
            onChange={(event) => update(index, event.target.value)}
            placeholder={`Example ${index + 1}`}
          />
          <button
            type="button"
            className="rounded border border-rose-300 px-2 py-1 text-xs text-rose-700"
            onClick={() => remove(index)}
          >
            Remove
          </button>
        </div>
      ))}
    </div>
  )
}
