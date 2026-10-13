import { LoaderCircle } from 'lucide-react'
import { useRef, useState, type DragEvent, type ReactNode } from 'react'
import { LogoMark } from '@/components/logo'
import { Button } from '@/components/ui/button'

const ACCEPT = '.yaml,.yml,.json'

export function UploadButton({
  onFile,
  uploading,
  variant = 'default',
  children = 'Upload specification',
}: {
  onFile: (file: File) => void
  uploading: boolean
  variant?: 'default' | 'outline'
  children?: ReactNode
}) {
  const input = useRef<HTMLInputElement>(null)
  return (
    <>
      <Button variant={variant} disabled={uploading} onClick={() => input.current?.click()}>
        {uploading && <LoaderCircle className="animate-spin" />}
        {uploading ? 'Scanning…' : children}
      </Button>
      <input
        ref={input}
        type="file"
        accept={ACCEPT}
        className="sr-only"
        tabIndex={-1}
        aria-hidden
        onChange={(e) => {
          const file = e.target.files?.[0]
          if (file) onFile(file)
          e.target.value = ''
        }}
      />
    </>
  )
}

/** Lets a file be dropped anywhere on the page; the overlay appears only while a file is dragged over it. */
export function FileDropArea({
  onFile,
  disabled,
  children,
}: {
  onFile: (file: File) => void
  disabled: boolean
  children: ReactNode
}) {
  const [dragging, setDragging] = useState(false)
  const hasFiles = (event: DragEvent) => event.dataTransfer.types.includes('Files')

  return (
    <div
      onDragEnter={(e) => {
        if (hasFiles(e) && !disabled) setDragging(true)
      }}
      onDragOver={(e) => {
        if (hasFiles(e)) e.preventDefault()
      }}
      onDrop={(e) => {
        e.preventDefault()
        setDragging(false)
        const file = e.dataTransfer.files[0]
        if (file && !disabled) onFile(file)
      }}
    >
      {children}
      {dragging && (
        <div
          className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-background/95"
          onDragLeave={() => setDragging(false)}
        >
          <LogoMark className="h-6 [&>span]:w-2" />
          <p className="text-xl font-semibold">Drop the file to scan it</p>
          <p className="text-muted-foreground">OpenAPI 3 in .yaml, .yml or .json, up to 2 MB</p>
        </div>
      )}
    </div>
  )
}
