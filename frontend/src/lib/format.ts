const relative = new Intl.RelativeTimeFormat('en', { numeric: 'auto' })

const UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['year', 365 * 24 * 3600],
  ['month', 30 * 24 * 3600],
  ['week', 7 * 24 * 3600],
  ['day', 24 * 3600],
  ['hour', 3600],
  ['minute', 60],
]

export function timeAgo(iso: string): string {
  const seconds = (new Date(iso).getTime() - Date.now()) / 1000
  for (const [unit, size] of UNITS) {
    if (Math.abs(seconds) >= size) return relative.format(Math.round(seconds / size), unit)
  }
  return 'just now'
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso))
}

const ACRONYMS: Record<string, string> = { bola: 'BOLA', url: 'URL', https: 'HTTPS', api: 'API' }

// "POTENTIAL_BOLA" -> "Potential BOLA"
export function humanize(value: string): string {
  const words = value.toLowerCase().split('_').map((word) => ACRONYMS[word] ?? word)
  const text = words.join(' ')
  return text.charAt(0).toUpperCase() + text.slice(1)
}
