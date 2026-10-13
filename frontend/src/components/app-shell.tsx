import { Link, NavLink, Outlet } from 'react-router'
import { Logo } from '@/components/logo'
import { RiskMark } from '@/components/severity'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuth } from '@/lib/auth'
import { useProjects } from '@/lib/queries'
import type { Project } from '@/lib/types'
import { cn } from '@/lib/utils'

// Failed uploads have no findings to colour the marker with, so they count as "no scan yet".
function latestCounts(project: Project) {
  return project.latestScan?.status === 'COMPLETED' ? project.latestScan.findingsBySeverity : null
}

function ProjectNav() {
  const projects = useProjects()

  if (projects.isPending) {
    return (
      <div className="flex flex-col gap-3 px-3 py-1.5">
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} className="h-3.5 bg-sidebar-accent" />
        ))}
      </div>
    )
  }
  if (projects.isError || projects.data.length === 0) {
    return <p className="px-3 text-sm text-sidebar-muted">No projects yet</p>
  }

  return (
    <ul className="flex flex-col">
      {projects.data.map((project) => (
        <li key={project.id}>
          <NavLink
            to={`/projects/${project.id}`}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-sm px-3 py-[0.4375rem] text-sm transition-colors duration-150',
                isActive
                  ? 'bg-sidebar-accent text-sidebar-accent-foreground'
                  : 'text-sidebar-foreground/75 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground',
              )
            }
          >
            <RiskMark counts={latestCounts(project)} />
            <span className="truncate">{project.name}</span>
          </NavLink>
        </li>
      ))}
    </ul>
  )
}

function SignOut({ className }: { className?: string }) {
  const { logout } = useAuth()
  return (
    <button
      type="button"
      onClick={logout}
      className={cn(
        'shrink-0 rounded-sm text-sm text-sidebar-muted transition-colors hover:text-sidebar-accent-foreground focus-visible:ring-2 focus-visible:ring-sidebar-ring focus-visible:outline-none',
        className,
      )}
    >
      Sign out
    </button>
  )
}

export function AppShell() {
  const { session } = useAuth()

  return (
    <div className="flex min-h-svh flex-col md:flex-row">
      <aside className="flex shrink-0 flex-col bg-sidebar text-sidebar-foreground md:sticky md:top-0 md:h-svh md:w-60">
        <div className="flex h-14 items-center justify-between gap-3 px-5 md:h-16">
          <Link to="/projects" className="rounded-sm text-sidebar-accent-foreground focus-visible:ring-2 focus-visible:ring-sidebar-ring focus-visible:outline-none">
            <Logo />
          </Link>
          {/* On small screens the sidebar collapses into this bar. */}
          <div className="flex items-center gap-5 md:hidden">
            <Link to="/projects" className="text-sm text-sidebar-foreground/85 hover:text-sidebar-accent-foreground">
              Projects
            </Link>
            <SignOut />
          </div>
        </div>

        <nav className="hidden flex-1 flex-col gap-2 overflow-y-auto px-2 pt-4 pb-3 md:flex" aria-label="Projects">
          <div className="flex items-baseline justify-between px-3 pb-1">
            <NavLink
              to="/projects"
              end
              className={({ isActive }) =>
                cn(
                  'text-xs font-medium transition-colors',
                  isActive ? 'text-sidebar-accent-foreground' : 'text-sidebar-muted hover:text-sidebar-accent-foreground',
                )
              }
            >
              Projects
            </NavLink>
            <Link to="/projects?new=1" className="text-xs text-sidebar-muted transition-colors hover:text-sidebar-accent-foreground">
              New
            </Link>
          </div>
          <ProjectNav />
        </nav>

        <div className="hidden flex-col gap-1.5 border-t border-sidebar-border px-5 py-4 md:flex">
          <span className="truncate text-sm text-sidebar-foreground/80" title={session?.email}>
            {session?.email}
          </span>
          <SignOut className="self-start" />
        </div>
      </aside>

      <main className="min-w-0 flex-1">
        <Outlet />
      </main>
    </div>
  )
}
