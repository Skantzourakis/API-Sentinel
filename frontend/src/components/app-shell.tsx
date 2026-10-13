import { LogOut } from 'lucide-react'
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

function SignOutButton() {
  const { logout } = useAuth()
  return (
    <button
      type="button"
      onClick={logout}
      aria-label="Sign out"
      title="Sign out"
      className="grid size-8 shrink-0 place-items-center rounded-md text-sidebar-muted transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 focus-visible:ring-sidebar-ring focus-visible:outline-none"
    >
      <LogOut className="size-4" />
    </button>
  )
}

function Avatar({ email, className }: { email: string | undefined; className?: string }) {
  return (
    <span
      aria-hidden
      className={cn('grid size-7 shrink-0 place-items-center rounded-full bg-sidebar-accent text-xs font-semibold uppercase', className)}
    >
      {email?.charAt(0) ?? '?'}
    </span>
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
          <div className="flex items-center gap-3 md:hidden">
            <Link to="/projects" className="text-sm text-sidebar-foreground/85 hover:text-sidebar-accent-foreground">
              Projects
            </Link>
            <Link to="/profile" aria-label="Profile" className="rounded-full focus-visible:ring-2 focus-visible:ring-sidebar-ring focus-visible:outline-none">
              <Avatar email={session?.email} />
            </Link>
            <SignOutButton />
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

        <div className="hidden items-center gap-1 border-t border-sidebar-border px-2 py-3 md:flex">
          <NavLink
            to="/profile"
            title="Your profile"
            className={({ isActive }) =>
              cn(
                'flex min-w-0 flex-1 items-center gap-3 rounded-md px-2 py-1.5 transition-colors duration-150 focus-visible:ring-2 focus-visible:ring-sidebar-ring focus-visible:outline-none',
                isActive ? 'bg-sidebar-accent text-sidebar-accent-foreground' : 'text-sidebar-foreground/80 hover:bg-sidebar-accent/60 hover:text-sidebar-accent-foreground',
              )
            }
          >
            <Avatar email={session?.email} />
            <span className="truncate text-sm">{session?.email}</span>
          </NavLink>
          <SignOutButton />
        </div>
      </aside>

      <main className="min-w-0 flex-1">
        <Outlet />
      </main>
    </div>
  )
}
