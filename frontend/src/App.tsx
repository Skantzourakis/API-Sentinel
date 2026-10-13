import { Button } from '@/components/ui/button'

// Temporary landing page; replaced by the router and real pages in the next steps.
export default function App() {
  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-4">
      <h1 className="text-2xl font-semibold">API Sentinel</h1>
      <p className="text-muted-foreground">Frontend setup is working.</p>
      <Button>Get started</Button>
    </main>
  )
}
