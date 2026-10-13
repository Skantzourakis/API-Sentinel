import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, api } from '@/lib/api'
import type { Severity } from '@/lib/severity'
import type { Finding, Page, Project, Scan, ScanComparison } from '@/lib/types'

export const FINDINGS_PAGE_SIZE = 20

export function useProjects() {
  return useQuery({ queryKey: ['projects'], queryFn: () => api<Project[]>('/projects') })
}

export function useProject(projectId: number | undefined) {
  return useQuery({
    queryKey: ['projects', projectId],
    queryFn: () => api<Project>(`/projects/${projectId}`),
    enabled: projectId !== undefined,
  })
}

export function useScans(projectId: number) {
  return useQuery({
    queryKey: ['projects', projectId, 'scans'],
    queryFn: () => api<Scan[]>(`/projects/${projectId}/scans`),
  })
}

export function useScan(scanId: number) {
  return useQuery({ queryKey: ['scans', scanId], queryFn: () => api<Scan>(`/scans/${scanId}`) })
}

export function useFindings(scanId: number, severity: Severity | null, page: number, enabled = true) {
  const params = new URLSearchParams({ page: String(page), size: String(FINDINGS_PAGE_SIZE) })
  if (severity) params.set('severity', severity)
  return useQuery({
    queryKey: ['scans', scanId, 'findings', severity, page],
    queryFn: () => api<Page<Finding>>(`/scans/${scanId}/findings?${params}`),
    placeholderData: keepPreviousData,
    enabled,
  })
}

// The first scan of a project has nothing to compare with; the API answers 400, shown as "no comparison".
export function useComparison(scanId: number, enabled = true) {
  return useQuery({
    queryKey: ['scans', scanId, 'comparison'],
    queryFn: async () => {
      try {
        return await api<ScanComparison>(`/scans/${scanId}/comparison`)
      } catch (error) {
        if (error instanceof ApiError && error.status === 400) return null
        throw error
      }
    },
    enabled,
  })
}

export function useCreateProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { name: string; description: string }) =>
      api<Project>('/projects', { method: 'POST', body: JSON.stringify(input) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['projects'] }),
  })
}

export function useDeleteProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (projectId: number) => api<void>(`/projects/${projectId}`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['projects'] }),
  })
}

export function useUploadScan(projectId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => {
      const body = new FormData()
      body.append('file', file)
      return api<Scan>(`/projects/${projectId}/scans/upload`, { method: 'POST', body })
    },
    // A failed parse is still recorded as a FAILED scan, so refresh the history either way.
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'scans'] }),
  })
}
