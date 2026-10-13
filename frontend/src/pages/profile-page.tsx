import { Fragment, type ReactNode } from 'react'
import { Link } from 'react-router'
import { ErrorState, Page, PageHeader, Section } from '@/components/page'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuth } from '@/lib/auth'
import { formatDateTime, timeAgo } from '@/lib/format'
import { useCurrentUser, useProjects } from '@/lib/queries'

function Details({ rows }: { rows: [string, ReactNode][] }) {
  return (
    <dl className="grid max-w-2xl grid-cols-[9rem_minmax(0,1fr)] border-t border-foreground/15 sm:grid-cols-[12rem_minmax(0,1fr)]">
      {rows.map(([label, value]) => (
        <Fragment key={label}>
          <dt className="border-b py-3 text-sm text-muted-foreground">{label}</dt>
          <dd className="min-w-0 border-b py-3">{value}</dd>
        </Fragment>
      ))}
    </dl>
  )
}

export function ProfilePage() {
  const { session, logout } = useAuth()
  const user = useCurrentUser()
  const projects = useProjects()

  const scanCount = projects.data?.reduce((sum, project) => sum + project.scanCount, 0)
  const email = user.data?.email ?? session?.email

  return (
    <Page>
      <PageHeader
        title={
          <span className="flex items-center gap-4">
            <span
              aria-hidden
              className="grid size-12 shrink-0 place-items-center rounded-full bg-sidebar text-lg font-semibold text-sidebar-accent-foreground uppercase"
            >
              {email?.charAt(0) ?? '?'}
            </span>
            <span className="min-w-0 truncate">{email}</span>
          </span>
        }
        description="Your account and the session in this browser."
      />

      {user.isError && <ErrorState message={user.error.message} onRetry={() => user.refetch()} />}

      <Section title="Account">
        {user.isPending ? (
          <Skeleton className="h-32 w-full max-w-2xl" />
        ) : (
          user.data && (
            <Details
              rows={[
                ['Email', <span key="email" className="break-all">{user.data.email}</span>],
                [
                  'Member since',
                  <span key="since" title={formatDateTime(user.data.createdAt)}>
                    {new Intl.DateTimeFormat('en', { dateStyle: 'long' }).format(new Date(user.data.createdAt))}{' '}
                    <span className="text-muted-foreground">({timeAgo(user.data.createdAt)})</span>
                  </span>,
                ],
                [
                  'Projects',
                  <Link key="projects" to="/projects" className="tabular hover:underline">
                    {projects.data?.length ?? '…'}
                  </Link>,
                ],
                ['Scans', <span key="scans" className="tabular">{scanCount ?? '…'}</span>],
              ]}
            />
          )
        )}
      </Section>

      <Section title="Session" description="You stay signed in until the session expires or you sign out.">
        <Details
          rows={[
            [
              'Expires',
              session?.expiresAt ? (
                <span key="expires" title={formatDateTime(new Date(session.expiresAt).toISOString())}>{timeAgo(new Date(session.expiresAt).toISOString())}</span>
              ) : (
                '—'
              ),
            ],
          ]}
        />
        <div>
          <Button variant="outline" onClick={logout}>
            Sign out
          </Button>
        </div>
      </Section>
    </Page>
  )
}
