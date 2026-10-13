import { cn } from '@/lib/utils'
import {
  SEVERITIES,
  SEVERITY_FILL,
  SEVERITY_LABEL,
  SEVERITY_TEXT,
  describeCounts,
  highestSeverity,
  totalOf,
  type Severity,
  type SeverityCounts,
} from '@/lib/severity'

/** Square colour marker; squares rather than dots match the squared spectrum. */
export function SeverityMark({ severity, className }: { severity: Severity | 'NONE'; className?: string }) {
  return (
    <span
      aria-hidden
      className={cn('inline-block size-2 shrink-0 rounded-[1px]', severity === 'NONE' ? 'bg-success' : SEVERITY_FILL[severity], className)}
    />
  )
}

/** "■ High" with the text in the severity's strong tone. */
export function SeverityLabel({ severity, className }: { severity: Severity; className?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-2 text-sm font-medium', SEVERITY_TEXT[severity], className)}>
      <SeverityMark severity={severity} />
      {SEVERITY_LABEL[severity]}
    </span>
  )
}

/** "Critical risk", "Medium risk" or "No findings", from the most severe finding. */
export function RiskLabel({ counts, className }: { counts: SeverityCounts; className?: string }) {
  const highest = highestSeverity(counts)
  return (
    <span className={cn('font-medium', highest ? SEVERITY_TEXT[highest] : 'text-success', className)}>
      {highest ? `${SEVERITY_LABEL[highest]} risk` : 'No findings'}
    </span>
  )
}

/** Marker for navigation: the colour of the latest scan's worst finding, outlined when there is none yet. */
export function RiskMark({ counts, className }: { counts: SeverityCounts | null; className?: string }) {
  if (counts === null) {
    return <span aria-hidden className={cn('inline-block size-2 shrink-0 rounded-[1px] border border-current opacity-50', className)} />
  }
  return <SeverityMark severity={highestSeverity(counts) ?? 'NONE'} className={className} />
}

/**
 * The product's signature: a scan's findings as one squared spectrum, most severe first.
 * `scale` (0–1) shortens the bar relative to other scans, so lists read as strata.
 */
export function SeverityBar({
  counts,
  scale = 1,
  height = 'h-1.5',
  className,
}: {
  counts: SeverityCounts
  scale?: number
  height?: string
  className?: string
}) {
  const total = totalOf(counts)

  if (total === 0) {
    return (
      <span role="img" aria-label="No findings" className={cn('flex w-full items-center', height, className)}>
        <span className="h-full w-3 bg-success" />
      </span>
    )
  }

  return (
    <span role="img" aria-label={describeCounts(counts)} title={describeCounts(counts)} className={cn('flex w-full', className)}>
      <span className={cn('flex gap-px', height)} style={{ width: `${Math.max(scale, 0.04) * 100}%` }}>
        {SEVERITIES.filter((s) => counts[s] > 0).map((severity) => (
          <span key={severity} className={cn('min-w-1', SEVERITY_FILL[severity])} style={{ flexGrow: counts[severity] }} />
        ))}
      </span>
    </span>
  )
}

/** Large spectrum for the scan page, labelled directly under each segment instead of with a legend. */
export function SeveritySpectrum({ counts, className }: { counts: SeverityCounts; className?: string }) {
  const present = SEVERITIES.filter((s) => counts[s] > 0)

  if (present.length === 0) {
    return (
      <div className={cn('flex flex-col gap-2', className)}>
        <span className="h-4 w-full bg-success/80" />
        <span className="text-sm text-muted-foreground">No findings in any severity</span>
      </div>
    )
  }

  return (
    <div role="img" aria-label={describeCounts(counts)} className={cn('flex gap-px', className)}>
      {present.map((severity) => (
        <div key={severity} className="flex min-w-[4.5rem] flex-col gap-2" style={{ flexGrow: counts[severity], flexBasis: 0 }}>
          <span className={cn('h-4', SEVERITY_FILL[severity])} />
          <span className="flex flex-col pr-2">
            <span className="tabular text-lg leading-tight font-semibold">{counts[severity]}</span>
            <span className={cn('text-xs font-medium', SEVERITY_TEXT[severity])}>{SEVERITY_LABEL[severity]}</span>
          </span>
        </div>
      ))}
    </div>
  )
}
