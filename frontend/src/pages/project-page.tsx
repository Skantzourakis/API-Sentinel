import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import { ButtonLink } from '@/components/button-link'
import { EmptyState, ErrorState, Page, PageHeader, Section } from '@/components/page'
import { RiskChange } from '@/components/risk-change'
import { RiskLabel, SeverityBar } from '@/components/severity'
import { FileDropArea, UploadButton } from '@/components/upload'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatDateTime, timeAgo } from '@/lib/format'
import { useDeleteProject, useProject, useScans, useUploadScan } from '@/lib/queries'
import { totalOf } from '@/lib/severity'
import type { Scan } from '@/lib/types'

function DeleteProject({ projectId, name }: { projectId: number; name: string }) {
  const [confirming, setConfirming] = useState(false)
  const deleteProject = useDeleteProject()
  const navigate = useNavigate()

  if (!confirming) {
    return (
      <Button variant="ghost" onClick={() => setConfirming(true)}>
        Delete
      </Button>
    )
  }
  return (
    <span className="flex items-center gap-2 text-sm">
      <span className="text-muted-foreground">Delete {name} and all its scans?</span>
      <Button
        variant="destructive"
        disabled={deleteProject.isPending}
        onClick={() =>
          deleteProject.mutate(projectId, {
            onSuccess: () => {
              toast.success(`Deleted ${name}`)
              navigate('/projects')
            },
          })
        }
      >
        Delete
      </Button>
      <Button variant="ghost" onClick={() => setConfirming(false)}>
        Cancel
      </Button>
    </span>
  )
}

/** The newest completed scan in one sentence, with how it moved since the one before. */
function Latest({ scans }: { scans: Scan[] }) {
  const completed = scans.filter((s) => s.status === 'COMPLETED')
  if (completed.length === 0) return null
  const [latest, previous] = completed

  return (
    <p className="flex flex-wrap items-center gap-x-3 gap-y-1 border-t border-foreground/15 pt-4">
      <span className="text-muted-foreground">Latest</span>
      <Link to={`/scans/${latest.id}`} className="font-medium hover:underline">
        Scan #{latest.id}
      </Link>
      <span className="flex items-baseline gap-1.5">
        <span className="tabular text-xl font-semibold">{latest.riskScore}</span>
        <span className="text-sm text-muted-foreground">/ 100</span>
      </span>
      <RiskLabel counts={latest.findingsBySeverity} />
      {previous && <RiskChange change={latest.riskScore - previous.riskScore} baseScanId={previous.id} />}
    </p>
  )
}

function ScanStrata({ scans }: { scans: Scan[] }) {
  // One scale for every bar, so lengths compare across scans.
  const maxFindings = Math.max(0, ...scans.filter((s) => s.status === 'COMPLETED').map((s) => totalOf(s.findingsBySeverity)))

  return (
    <Table>
      <TableHeader>
        <TableRow className="hover:bg-transparent">
          <TableHead>Scan</TableHead>
          <TableHead className="w-[45%]">Findings</TableHead>
          <TableHead>Risk</TableHead>
          <TableHead className="hidden sm:table-cell">Uploaded</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {scans.map((scan) => {
          const failed = scan.status === 'FAILED'
          const total = totalOf(scan.findingsBySeverity)
          return (
            <TableRow key={scan.id} className="group relative">
              <TableCell className="py-3.5">
                <Link to={`/scans/${scan.id}`} className="font-medium group-hover:underline after:absolute after:inset-0 focus-visible:underline focus-visible:outline-none">
                  #{scan.id}
                </Link>
                <p className="max-w-[26ch] truncate font-mono text-xs text-muted-foreground">{scan.openApiFileName}</p>
              </TableCell>
              <TableCell className="py-3.5">
                {failed ? (
                  <span className="text-destructive">Not a valid OpenAPI file</span>
                ) : (
                  <div className="flex items-center gap-3">
                    <SeverityBar counts={scan.findingsBySeverity} scale={maxFindings ? total / maxFindings : 0} height="h-2.5" className="flex-1" />
                    <span className="tabular w-6 text-right text-muted-foreground">{total}</span>
                  </div>
                )}
              </TableCell>
              <TableCell className="py-3.5">
                {failed ? (
                  <span className="text-muted-foreground">—</span>
                ) : (
                  <span className="flex items-baseline gap-2.5">
                    <span className="tabular w-7 text-right font-medium">{scan.riskScore}</span>
                    <RiskLabel counts={scan.findingsBySeverity} className="text-sm font-normal" />
                  </span>
                )}
              </TableCell>
              <TableCell className="hidden py-3.5 text-muted-foreground sm:table-cell" title={formatDateTime(scan.createdAt)}>
                {timeAgo(scan.createdAt)}
              </TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}

export function ProjectPage() {
  const projectId = Number(useParams().projectId)
  const project = useProject(projectId)
  const scans = useScans(projectId)
  const upload = useUploadScan(projectId)
  const navigate = useNavigate()

  function onFile(file: File) {
    upload.mutate(file, {
      onSuccess: (scan) => {
        toast.success(`Scanned ${file.name}`, { description: `${totalOf(scan.findingsBySeverity)} findings, risk ${scan.riskScore}` })
        navigate(`/scans/${scan.id}`)
      },
      onError: (error) => toast.error('Upload failed', { description: error.message }),
    })
  }

  if (project.isError) {
    return (
      <Page>
        <PageHeader crumbs={[{ label: 'Projects', to: '/projects' }]} title="Project not found" />
        <ErrorState message={project.error.message} />
        <ButtonLink to="/projects" variant="outline" className="self-start">
          Back to projects
        </ButtonLink>
      </Page>
    )
  }

  const hasScans = scans.isSuccess && scans.data.length > 0
  const scanCount = project.data?.scanCount ?? 0

  return (
    <FileDropArea onFile={onFile} disabled={upload.isPending}>
      <Page>
        <PageHeader
          crumbs={[{ label: 'Projects', to: '/projects' }, { label: project.data?.name ?? '…' }]}
          title={project.data?.name ?? <Skeleton className="h-8 w-64" />}
          description={project.data?.description}
          meta={
            project.data && (
              <>
                <span>
                  {scanCount} {scanCount === 1 ? 'scan' : 'scans'}
                </span>
                <span aria-hidden>·</span>
                <span title={formatDateTime(project.data.createdAt)}>Created {timeAgo(project.data.createdAt)}</span>
              </>
            )
          }
          actions={
            project.data && (
              <>
                <DeleteProject projectId={projectId} name={project.data.name} />
                {hasScans && <UploadButton onFile={onFile} uploading={upload.isPending}>Upload new version</UploadButton>}
              </>
            )
          }
        />

        {scans.isPending && <Skeleton className="h-48 w-full" />}
        {scans.isError && <ErrorState message={scans.error.message} onRetry={() => scans.refetch()} />}

        {scans.isSuccess && !hasScans && (
          <EmptyState title="Upload the first version" action={<UploadButton onFile={onFile} uploading={upload.isPending} />}>
            Choose this API&apos;s OpenAPI 3 file (.yaml, .yml or .json), or drop it anywhere on this page. It is analysed
            immediately; your API never receives a request.
          </EmptyState>
        )}

        {hasScans && (
          <>
            <Latest scans={scans.data} />
            <Section
              title="History"
              description="One row per upload, newest first. Bar length is the number of findings, so a shorter bar means fewer problems."
            >
              <ScanStrata scans={scans.data} />
              <p className="text-sm text-muted-foreground">Drop a new version of the file anywhere on this page to scan it.</p>
            </Section>
          </>
        )}
      </Page>
    </FileDropArea>
  )
}
