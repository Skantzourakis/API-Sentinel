import type { Severity, SeverityCounts } from '@/lib/severity'

// Mirrors the backend DTOs.

export interface Project {
  id: number
  name: string
  description: string | null
  createdAt: string
  updatedAt: string
  scanCount: number
  latestScan: Scan | null
}

export type ScanStatus = 'PENDING' | 'COMPLETED' | 'FAILED'

export interface Scan {
  id: number
  projectId: number
  status: ScanStatus
  riskScore: number
  endpointCount: number
  openApiFileName: string | null
  createdAt: string
  completedAt: string | null
  findingsBySeverity: SeverityCounts
}

export interface Finding {
  id: number
  scanId: number
  ruleId: string
  method: string
  path: string
  severity: Severity
  issueType: string
  title: string
  description: string
  recommendation: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ScanComparison {
  scanId: number
  baseScanId: number
  baseRiskScore: number
  riskScore: number
  riskScoreChange: number
  newFindings: Finding[]
  resolvedFindings: Finding[]
  unchangedFindings: number
}

export interface User {
  id: number
  email: string
  createdAt: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}
