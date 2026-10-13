export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'

export type SeverityCounts = Record<Severity, number>

// Most severe first: the order used in bars, legends, summaries and filters.
export const SEVERITIES: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

export const SEVERITY_LABEL: Record<Severity, string> = {
  CRITICAL: 'Critical',
  HIGH: 'High',
  MEDIUM: 'Medium',
  LOW: 'Low',
}

// Full class names so Tailwind can find them when scanning the source.
export const SEVERITY_FILL: Record<Severity, string> = {
  CRITICAL: 'bg-critical',
  HIGH: 'bg-high',
  MEDIUM: 'bg-medium',
  LOW: 'bg-low',
}

export const SEVERITY_TEXT: Record<Severity, string> = {
  CRITICAL: 'text-critical-strong',
  HIGH: 'text-high-strong',
  MEDIUM: 'text-medium-strong',
  LOW: 'text-low-strong',
}

export const SEVERITY_BADGE: Record<Severity, string> = {
  CRITICAL: 'bg-critical-soft text-critical-strong',
  HIGH: 'bg-high-soft text-high-strong',
  MEDIUM: 'bg-medium-soft text-medium-strong',
  LOW: 'bg-low-soft text-low-strong',
}

export function totalOf(counts: SeverityCounts): number {
  return SEVERITIES.reduce((sum, severity) => sum + counts[severity], 0)
}

// A scan is as risky as its worst finding; the numeric score is capped and can hide that.
export function highestSeverity(counts: SeverityCounts): Severity | null {
  return SEVERITIES.find((severity) => counts[severity] > 0) ?? null
}

export function describeCounts(counts: SeverityCounts): string {
  const parts = SEVERITIES.filter((s) => counts[s] > 0).map((s) => `${counts[s]} ${SEVERITY_LABEL[s].toLowerCase()}`)
  return parts.length > 0 ? parts.join(', ') : 'No findings'
}
