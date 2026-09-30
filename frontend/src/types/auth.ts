export type UserType = 'SUPER_ADMIN' | 'NORMAL_USER'

export type Permission = 'CREATE' | 'READ' | 'UPDATE' | 'DELETE'

export interface AuthUser {
  userId: number
  name: string
  username: string
  email: string
  userType: UserType
  permissions: Permission[]
}

export interface LoginResponse extends AuthUser {}

export interface CurrentUserResponse {
  id: number
  name: string
  username: string
  email: string
  userType: UserType
  permissions: Permission[]
  active: boolean
}
