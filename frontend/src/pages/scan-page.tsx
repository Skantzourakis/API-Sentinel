import { ChevronDown } from 'lucide-react'
import { Fragment, useState } from 'react'
import { Link, useParams } from 'react-router'
import { ButtonLink } from '@/components/button-link'
import { Endpoint } from '@/components/endpoint'
import { EmptyState, ErrorState, Page, PageHeader, Section } from '@/components/page'
import { RiskChange } from '@/components/risk-change'
import { ScanStatusLabel } from '@/components/scan-status'
import { RiskLabel, SeverityBar, SeverityLabel, SeverityMark, SeveritySpectrum } from '@/components/severity'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatDateTime, humanize, timeAgo } from '@/lib/format'
import { FINDINGS_PAGE_SIZE, useComparison, useFindings, useProject, useScan } from '@/lib/queries'
import { SEVERITIES, SEVERITY_LABEL, totalOf, type Severity } from '@/lib/severity'
import type { Finding, Scan, ScanComparison } from '@/lib/types'
import { cn } from '@/lib/utils'

function Overview({ scan, comparison }: { scan: Scan; comparison: ScanComparison | null | undefined }) {
  const total = totalOf(scan.findingsBySeverity)

  return (
    <div className="grid gap-x-14 gap-y-8 md:grid-cols-[12rem_minmax(0,1fr)]">
      <div className="flex flex-col gap-1.5">
        <p className="text-sm text-muted-foreground">Risk score</p>
        <p className="flex items-baseline gap-2">
          <span className="tabular text-6xl leading-none font-semibold tracking-tight">{scan.riskScore}</span>
          <span className="text-muted-foreground">/ 100</span>
        </p>
        <RiskLabel counts={scan.findingsBySeverity} className="mt-1" />
        <div className="min-h-5">
          {comparison ? (
            <RiskChange change={comparison.riskScoreChange} baseScanId={comparison.baseScanId} />
          ) : comparison === null ? (
            <span className="text-sm text-muted-foreground">First scan of this project</span>
          ) : null}
        </div>
      </div>
      <div className="flex flex-col gap-3">
        <p className="text-sm text-muted-foreground">
          <span className="font-medium text-foreground">
            {total} {total === 1 ? 'finding' : 'findings'}
          </span>{' '}
          across {scan.endpointCount} {scan.endpointCount === 1 ? 'endpoint' : 'endpoints'}
        </p>
        <SeveritySpectrum counts={scan.findingsBySeverity} />
      </div>
    </div>
  )
}

function ChangeList({ title, findings, emptyText }: { title: string; findings: Finding[]; emptyText: string }) {
  return (
    <div className="flex min-w-0 flex-col gap-3">
      <h3 className="text-sm font-medium">
        {title} <span className="tabular font-normal text-muted-foreground">{findings.length}</span>
      </h3>
      {findings.length === 0 ? (
        <p className="text-sm text-muted-foreground">{emptyText}</p>
      ) : (
        <ul className="flex flex-col">
          {findings.map((finding) => (
            <li key={finding.id} className="flex min-w-0 flex-col gap-1 border-t py-2.5">
              <span className="flex min-w-0 items-center gap-2.5">
                <SeverityMark severity={finding.severity} />
                <span className="truncate text-sm">{finding.title}</span>
              </span>
              <Endpoint method={finding.method} path={finding.path} className="pl-[1.125rem] text-muted-foreground" />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/** Before and after as two spectra on one scale, then what was introduced and what was fixed. */
function Comparison({ scan, comparison }: { scan: Scan; comparison: ScanComparison }) {
  const { newFindings, resolvedFindings, unchangedFindings, baseScanId } = comparison
  const before = totalOf(scan.findingsBySeverity) - newFindings.length + resolvedFindings.length
  const after = totalOf(scan.findingsBySeverity)
  const max = Math.max(before, after, 1)
  const changed = newFindings.length + resolvedFindings.length > 0

  // The base scan's counts per severity follow from this scan's counts and the change lists.
  const beforeCounts = { ...scan.findingsBySeverity }
  newFindings.forEach((f) => (beforeCounts[f.severity] -= 1))
  resolvedFindings.forEach((f) => (beforeCounts[f.severity] += 1))

  return (
    <Section
      title={`Compared with scan #${baseScanId}`}
      description={
        changed
          ? `${newFindings.length} new, ${resolvedFindings.length} resolved, ${unchangedFindings} unchanged.`
          : 'Same findings as the previous version. Nothing was introduced or fixed.'
      }
      actions={
        <ButtonLink to={`/scans/${baseScanId}`} variant="outline" size="sm">
          Open scan #{baseScanId}
        </ButtonLink>
      }
    >
      <div className="grid grid-cols-[3.5rem_minmax(0,1fr)_2rem] items-center gap-x-3 gap-y-2.5 text-sm">
        <Link to={`/scans/${baseScanId}`} className="text-muted-foreground hover:text-foreground hover:underline">
          #{baseScanId}
        </Link>
        <SeverityBar counts={beforeCounts} scale={before / max} height="h-2.5" />
        <span className="tabular text-right text-muted-foreground">{before}</span>
        <span className="font-medium">#{scan.id}</span>
        <SeverityBar counts={scan.findingsBySeverity} scale={after / max} height="h-2.5" />
        <span className="tabular text-right font-medium">{after}</span>
      </div>
      {changed && (
        <div className="grid gap-x-12 gap-y-8 pt-3 md:grid-cols-2">
          <ChangeList title="New" findings={newFindings} emptyText="Nothing new was introduced." />
          <ChangeList title="Resolved" findings={resolvedFindings} emptyText="No findings were fixed in this version." />
        </div>
      )}
    </Section>
  )
}

function SeverityTabs({ scan, value, onChange }: { scan: Scan; value: Severity | null; onChange: (value: Severity | null) => void }) {
  const options: { value: Severity | null; label: string; count: number }[] = [
    { value: null, label: 'All', count: totalOf(scan.findingsBySeverity) },
    ...SEVERITIES.map((s) => ({ value: s, label: SEVERITY_LABEL[s], count: scan.findingsBySeverity[s] })),
  ]
  return (
    <div role="radiogroup" aria-label="Filter by severity" className="flex flex-wrap gap-x-6 border-b">
      {options.map((option) => {
        const selected = option.value === value
        return (
          <button
            key={option.label}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(option.value)}
            className={cn(
              '-mb-px flex items-center gap-2 border-b-2 py-2 text-sm transition-colors duration-150 focus-visible:text-foreground focus-visible:outline-none',
              selected ? 'border-foreground font-medium text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
            )}
          >
            {option.value && <SeverityMark severity={option.value} />}
            {option.label}
            <span className="tabular text-xs text-muted-foreground">{option.count}</span>
          </button>
        )
      })}
    </div>
  )
}

function FindingRows({ finding }: { finding: Finding }) {
  const [open, setOpen] = useState(false)
  return (
    <Fragment>
      <TableRow className={cn('group cursor-pointer', open && 'border-b-0')} onClick={() => setOpen(!open)}>
        <TableCell className="hidden w-28 py-3.5 align-top sm:table-cell">
          <SeverityLabel severity={finding.severity} />
        </TableCell>
        <TableCell className="py-3.5 align-top whitespace-normal">
          {/* On narrow screens severity and endpoint move into this cell instead of their own columns. */}
          <SeverityLabel severity={finding.severity} className="mb-1 sm:hidden" />
          <button
            type="button"
            aria-expanded={open}
            className="block text-left font-medium group-hover:underline focus-visible:underline focus-visible:outline-none"
            onClick={(e) => {
              e.stopPropagation()
              setOpen(!open)
            }}
          >
            {finding.title}
          </button>
          <p className="text-sm text-muted-foreground">{humanize(finding.issueType)}</p>
          <Endpoint method={finding.method} path={finding.path} className="mt-1.5 md:hidden" />
        </TableCell>
        <TableCell className="hidden max-w-80 py-3.5 align-top md:table-cell">
          <Endpoint method={finding.method} path={finding.path} />
        </TableCell>
        <TableCell className="w-8 py-3.5 align-top">
          <ChevronDown className={cn('size-4 text-muted-foreground transition-transform duration-200', open && 'rotate-180')} />
        </TableCell>
      </TableRow>
      {open && (
        <TableRow>
          <TableCell colSpan={4} className="pt-1 pb-6 whitespace-normal">
            {/* Indented to start under the finding title, past the severity column. */}
            <div className="grid gap-6 sm:pl-[7.75rem] md:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <p className="text-xs font-medium text-muted-foreground">What we found</p>
                <p className="max-w-[65ch] text-sm">{finding.description}</p>
              </div>
              <div className="flex flex-col gap-1.5">
                <p className="text-xs font-medium text-muted-foreground">How to fix it</p>
                <p className="max-w-[65ch] text-sm">{finding.recommendation}</p>
              </div>
            </div>
            <p className="mt-4 font-mono text-xs text-muted-foreground sm:pl-[7.75rem]">Rule {finding.ruleId}</p>
          </TableCell>
        </TableRow>
      )}
    </Fragment>
  )
}

function Findings({ scan }: { scan: Scan }) {
  const [severity, setSeverity] = useState<Severity | null>(null)
  const [page, setPage] = useState(0)
  const findings = useFindings(scan.id, severity, page)
  const total = totalOf(scan.findingsBySeverity)

  if (total === 0) {
    return (
      <Section title="Findings">
        <EmptyState title="No findings in this version">
          Every endpoint passed all eight rules. Keep scanning new versions to make sure it stays that way.
        </EmptyState>
      </Section>
    )
  }

  const data = findings.data
  const from = data && data.totalElements > 0 ? data.page * FINDINGS_PAGE_SIZE + 1 : 0
  const to = data ? data.page * FINDINGS_PAGE_SIZE + data.content.length : 0

  return (
    <Section title="Findings" description="Most severe first. Select a finding to see what was detected and how to fix it.">
      <SeverityTabs
        scan={scan}
        value={severity}
        onChange={(next) => {
          setSeverity(next)
          setPage(0)
        }}
      />
      {findings.isError && <ErrorState message={findings.error.message} onRetry={() => findings.refetch()} />}
      <Table className={cn(findings.isPlaceholderData && 'opacity-60 transition-opacity')}>
        <TableHeader className="sr-only">
          <TableRow>
            <TableHead>Severity</TableHead>
            <TableHead>Finding</TableHead>
            <TableHead>Endpoint</TableHead>
            <TableHead>Details</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {findings.isPending &&
            [0, 1, 2, 3].map((i) => (
              <TableRow key={i}>
                <TableCell colSpan={4} className="py-4">
                  <Skeleton className="h-5 w-full" />
                </TableCell>
              </TableRow>
            ))}
          {data?.content.map((finding) => <FindingRows key={finding.id} finding={finding} />)}
          {data && data.content.length === 0 && (
            <TableRow className="hover:bg-transparent">
              <TableCell colSpan={4} className="py-8 text-muted-foreground">
                No {severity ? SEVERITY_LABEL[severity].toLowerCase() : ''} findings in this scan.
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span className="tabular">
            {from}–{to} of {data.totalElements}
          </span>
          <div className="flex gap-1">
            <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </Button>
            <Button variant="ghost" size="sm" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>
              Next
            </Button>
          </div>
        </div>
      )}
    </Section>
  )
}

export function ScanPage() {
  const scanId = Number(useParams().scanId)
  const scan = useScan(scanId)
  const project = useProject(scan.data?.projectId)
  const completed = scan.data?.status === 'COMPLETED'
  const comparison = useComparison(scanId, completed)

  const crumbs = [
    { label: 'Projects', to: '/projects' },
    { label: project.data?.name ?? '…', to: scan.data ? `/projects/${scan.data.projectId}` : undefined },
    { label: `Scan #${scanId}` },
  ]

  if (scan.isError) {
    return (
      <Page>
        <PageHeader crumbs={crumbs} title="Scan not found" />
        <ErrorState message={scan.error.message} />
      </Page>
    )
  }

  if (scan.isPending) {
    return (
      <Page>
        <PageHeader crumbs={crumbs} title={<Skeleton className="h-8 w-40" />} />
        <Skeleton className="h-36 w-full" />
        <Skeleton className="h-64 w-full" />
      </Page>
    )
  }

  const data = scan.data

  return (
    <Page>
      <PageHeader
        crumbs={crumbs}
        title={`Scan #${data.id}`}
        meta={
          <>
            <ScanStatusLabel status={data.status} />
            <span aria-hidden>·</span>
            <span className="font-mono text-[0.8125rem]">{data.openApiFileName}</span>
            <span aria-hidden>·</span>
            <span title={formatDateTime(data.createdAt)}>Uploaded {timeAgo(data.createdAt)}</span>
          </>
        }
        actions={
          <ButtonLink to={`/projects/${data.projectId}`} variant="outline">
            Upload new version
          </ButtonLink>
        }
      />

      {data.status === 'FAILED' ? (
        <EmptyState
          title="This file could not be scanned"
          action={<ButtonLink to={`/projects/${data.projectId}`}>Upload a corrected file</ButtonLink>}
        >
          {data.openApiFileName} is not a valid OpenAPI 3 specification. Check that it parses in an OpenAPI editor, then
          upload it again.
        </EmptyState>
      ) : (
        <>
          <Overview scan={data} comparison={comparison.data} />
          {comparison.data && <Comparison scan={data} comparison={comparison.data} />}
          <Findings scan={data} />
        </>
      )}
    </Page>
  )
}
