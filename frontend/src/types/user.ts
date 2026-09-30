import type { Permission, UserType } from '@/types/auth'

export interface User {
  id: number
  name: string
  username: string
  email: string
  userType: UserType
  permissions: Permission[]
  active: boolean
}

export interface PageResponse<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
