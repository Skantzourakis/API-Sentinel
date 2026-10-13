import { Navigate, Outlet, createBrowserRouter, useLocation } from 'react-router'
import { AppShell } from '@/components/app-shell'
import { ButtonLink } from '@/components/button-link'
import { EmptyState, Page } from '@/components/page'
import { useAuth } from '@/lib/auth'
import { AuthPage } from '@/pages/auth-page'
import { ProfilePage } from '@/pages/profile-page'
import { ProjectPage } from '@/pages/project-page'
import { ProjectsPage } from '@/pages/projects-page'
import { ScanPage } from '@/pages/scan-page'

function RequireAuth() {
  const { session } = useAuth()
  const location = useLocation()
  if (!session) return <Navigate to="/login" replace state={{ from: location }} />
  return <Outlet />
}

function NotFound() {
  return (
    <Page>
      <EmptyState title="Page not found" action={<ButtonLink to="/projects">Go to projects</ButtonLink>}>
        The address may be mistyped, or the page belongs to another account.
      </EmptyState>
    </Page>
  )
}

export const router = createBrowserRouter([
  { path: '/login', element: <AuthPage mode="login" /> },
  { path: '/register', element: <AuthPage mode="register" /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppShell />,
        children: [
          { index: true, element: <Navigate to="/projects" replace /> },
          { path: '/projects', element: <ProjectsPage /> },
          { path: '/projects/:projectId', element: <ProjectPage /> },
          { path: '/scans/:scanId', element: <ScanPage /> },
          { path: '/profile', element: <ProfilePage /> },
          { path: '*', element: <NotFound /> },
        ],
      },
    ],
  },
])
