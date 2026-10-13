import { LoaderCircle } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { EmptyState, ErrorState, Page, PageHeader } from '@/components/page'
import { RiskLabel, SeverityBar } from '@/components/severity'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatDateTime, timeAgo } from '@/lib/format'
import { useCreateProject, useProjects } from '@/lib/queries'
import { totalOf } from '@/lib/severity'
import type { Project } from '@/lib/types'

function NewProjectForm({ onCancel }: { onCancel: () => void }) {
  const navigate = useNavigate()
  const createProject = useCreateProject()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    createProject.mutate(
      { name: name.trim(), description: description.trim() },
      {
        onSuccess: (project) => {
          toast.success(`Created ${project.name}`)
          navigate(`/projects/${project.id}`)
        },
      },
    )
  }

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-4 border-y border-foreground/15 py-6">
      <div className="flex flex-col gap-1">
        <h2 className="font-semibold">New project</h2>
        <p className="text-sm text-muted-foreground">One project per API. Every upload of its specification becomes a scan.</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
        <div className="flex flex-col gap-2">
          <Label htmlFor="project-name">Name</Label>
          <Input
            id="project-name"
            autoFocus
            required
            maxLength={120}
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Payments API"
            className="h-9 bg-card"
          />
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="project-description">
            Description <span className="font-normal text-muted-foreground">(optional)</span>
          </Label>
          <Input
            id="project-description"
            maxLength={1000}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Public API used by the mobile app"
            className="h-9 bg-card"
          />
        </div>
      </div>
      {createProject.isError && <p className="text-sm text-destructive">{createProject.error.message}</p>}
      <div className="flex gap-2">
        <Button type="submit" disabled={!name.trim() || createProject.isPending}>
          {createProject.isPending && <LoaderCircle className="animate-spin" />}
          Create project
        </Button>
        <Button type="button" variant="ghost" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}

function ProjectRow({ project, maxFindings }: { project: Project; maxFindings: number }) {
  const scan = project.latestScan
  const completed = scan?.status === 'COMPLETED'
  const total = completed ? totalOf(scan.findingsBySeverity) : 0

  return (
    <TableRow className="group relative">
      <TableCell className="py-3.5 align-top">
        <Link to={`/projects/${project.id}`} className="font-medium group-hover:underline after:absolute after:inset-0 focus-visible:underline focus-visible:outline-none">
          {project.name}
        </Link>
        {project.description && <p className="max-w-[46ch] truncate text-muted-foreground">{project.description}</p>}
      </TableCell>
      <TableCell className="w-[30%] min-w-48 py-3.5">
        {!scan ? (
          <span className="text-muted-foreground">No scans yet</span>
        ) : completed ? (
          <div className="flex items-center gap-3">
            <SeverityBar counts={scan.findingsBySeverity} scale={maxFindings ? total / maxFindings : 0} className="flex-1" />
            <span className="tabular w-6 text-right text-muted-foreground">{total}</span>
          </div>
        ) : (
          <span className="text-destructive">Last upload failed</span>
        )}
      </TableCell>
      <TableCell className="py-3.5">
        {completed ? (
          <span className="flex items-baseline gap-2.5">
            <span className="tabular w-7 text-right font-medium">{scan.riskScore}</span>
            <RiskLabel counts={scan.findingsBySeverity} className="text-sm font-normal" />
          </span>
        ) : (
          <span className="text-muted-foreground">—</span>
        )}
      </TableCell>
      <TableCell className="tabular hidden py-3.5 text-muted-foreground md:table-cell">{project.scanCount}</TableCell>
      <TableCell className="hidden py-3.5 text-muted-foreground md:table-cell" title={scan ? formatDateTime(scan.createdAt) : undefined}>
        {scan ? timeAgo(scan.createdAt) : '—'}
      </TableCell>
    </TableRow>
  )
}

export function ProjectsPage() {
  const projects = useProjects()
  const [params, setParams] = useSearchParams()
  const creating = params.get('new') === '1'
  const setCreating = (open: boolean) => setParams(open ? { new: '1' } : {})

  const hasProjects = projects.isSuccess && projects.data.length > 0
  // Bars share one scale so their lengths compare across projects.
  const maxFindings = hasProjects
    ? Math.max(
        0,
        ...projects.data.map((p) => (p.latestScan?.status === 'COMPLETED' ? totalOf(p.latestScan.findingsBySeverity) : 0)),
      )
    : 0

  return (
    <Page>
      <PageHeader
        title="Projects"
        description="Each project tracks one API. Upload new versions of its OpenAPI specification to see how its security changes."
        actions={
          hasProjects && !creating && <Button onClick={() => setCreating(true)}>New project</Button>
        }
      />

      {creating && <NewProjectForm onCancel={() => setCreating(false)} />}

      {projects.isPending && (
        <div className="flex flex-col gap-5 pt-2">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-5 w-full" />
          ))}
        </div>
      )}

      {projects.isError && <ErrorState message={projects.error.message} onRetry={() => projects.refetch()} />}

      {projects.isSuccess && projects.data.length === 0 && !creating && (
        <EmptyState title="Create your first project" action={<Button onClick={() => setCreating(true)}>New project</Button>}>
          A project groups every scan of one API, so you can compare versions and see whether its risk goes down.
        </EmptyState>
      )}

      {hasProjects && (
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              <TableHead>Project</TableHead>
              <TableHead>Latest findings</TableHead>
              <TableHead>Risk</TableHead>
              <TableHead className="hidden md:table-cell">Scans</TableHead>
              <TableHead className="hidden md:table-cell">Last scan</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {projects.data.map((project) => (
              <ProjectRow key={project.id} project={project} maxFindings={maxFindings} />
            ))}
          </TableBody>
        </Table>
      )}
    </Page>
  )
}
