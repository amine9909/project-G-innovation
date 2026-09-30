export interface Employee {
  id: number
  firstName: string
  lastName: string
  email: string
  phoneNumber: string | null
  jobTitle: string
  department: string | null
  hireDate: string
  salary: number | null
  hasPhoto: boolean
  hasCv: boolean
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
