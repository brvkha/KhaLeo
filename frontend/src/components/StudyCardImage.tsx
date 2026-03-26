import { useState } from 'react'

type StudyCardImageProps = {
  imageUrl: string
  alt: string
}

export function StudyCardImage({ imageUrl, alt }: StudyCardImageProps) {
  const [failed, setFailed] = useState(false)

  if (!imageUrl || failed) {
    return (
      <div
        className="flex h-48 w-full items-center justify-center rounded border border-slate-300 bg-slate-100 text-slate-500"
        data-testid="study-image-fallback"
      >
        <span aria-hidden="true" className="text-xl">!</span>
      </div>
    )
  }

  return (
    <img
      src={imageUrl}
      alt={alt}
      className="h-auto max-h-64 w-full rounded object-contain"
      onError={() => setFailed(true)}
      data-testid="study-image"
    />
  )
}
