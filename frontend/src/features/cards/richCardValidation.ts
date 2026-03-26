import { RICH_CARD_MAX_EXAMPLE_LENGTH, RICH_CARD_MAX_EXAMPLES, RICH_CARD_MAX_PAYLOAD_BYTES } from './richCardConfig'

export type RichCardDraft = {
  term: string
  answer: string
  imageUrl: string
  examples: string[]
}

export function estimateRichCardPayloadBytes(draft: RichCardDraft): number {
  const encoder = new TextEncoder()
  return encoder.encode(JSON.stringify(draft)).length
}

export function validateRichCardDraft(draft: RichCardDraft): string[] {
  const errors: string[] = []
  if (!draft.term.trim()) {
    errors.push('term-required')
  }
  if (!draft.answer.trim()) {
    errors.push('answer-required')
  }

  if (draft.examples.length > RICH_CARD_MAX_EXAMPLES) {
    errors.push('examples-limit-exceeded')
  }

  for (const item of draft.examples) {
    if (!item.trim()) {
      errors.push('example-empty')
      break
    }
    if (item.length > RICH_CARD_MAX_EXAMPLE_LENGTH) {
      errors.push('example-too-long')
      break
    }
  }

  if (draft.imageUrl.trim()) {
    try {
      const url = new URL(draft.imageUrl)
      if (url.protocol !== 'https:') {
        errors.push('image-url-invalid')
      }
    } catch {
      errors.push('image-url-invalid')
    }
  }

  if (estimateRichCardPayloadBytes(draft) > RICH_CARD_MAX_PAYLOAD_BYTES) {
    errors.push('payload-too-large')
  }

  return errors
}
