<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import TablePagination from '@/components/TablePagination.vue'
import { apiFetch } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import type { Permission } from '@/types/auth'
import type { PageResponse, User } from '@/types/user'

const authStore = useAuthStore()
const toast = useToastStore()

const users = ref<User[]>([])
const loading = ref(false)
const error = ref('')
const search = ref('')
const page = ref(0)
const size = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)

const showCreateForm = ref(false)
const createLoading = ref(false)
const showEditForm = ref(false)
const editLoading = ref(false)
const selectedUser = ref<User | null>(null)
const showDeleteConfirm = ref(false)
const deleteLoading = ref(false)
const userToDelete = ref<User | null>(null)
const showPermissionsForm = ref(false)
const permissionsLoading = ref(false)
const permissionUser = ref<User | null>(null)

const minimumTableLoadingMs = 500

const createForm = reactive({
  name: '',
  username: '',
  email: '',
  password: '',
})

const createValidationErrors = reactive({
  name: '',
  username: '',
  email: '',
  password: '',
})

const editForm = reactive({
  name: '',
  username: '',
  email: '',
  active: true,
})

const editValidationErrors = reactive({
  name: '',
  username: '',
  email: '',
  active: '',
})

const permissionForm = reactive<Record<Permission, boolean>>({
  CREATE: false,
  READ: false,
  UPDATE: false,
  DELETE: false,
})

const permissionOptions: Array<{
  value: Permission
  label: string
  description: string
}> = [
  {
    value: 'CREATE',
    label: 'Create',
    description: 'Allows creating new records.',
  },
  {
    value: 'READ',
    label: 'Read',
    description: 'Allows viewing existing records.',
  },
  {
    value: 'UPDATE',
    label: 'Update',
    description: 'Allows editing existing records.',
  },
  {
    value: 'DELETE',
    label: 'Delete',
    description: 'Allows deleting records.',
  },
]

const canCreateUser = computed(() => {
  return authStore.isSuperAdmin || Boolean(authStore.user?.permissions.includes('CREATE'))
})

const canUpdateUser = computed(() => {
  return authStore.isSuperAdmin || Boolean(authStore.user?.permissions.includes('UPDATE'))
})

const canDeleteUser = computed(() => {
  return authStore.isSuperAdmin || Boolean(authStore.user?.permissions.includes('DELETE'))
})

async function loadUsers() {
  const loadingStartedAt = Date.now()
  loading.value = true
  error.value = ''

  const query = new URLSearchParams({
    search: search.value.trim(),
    page: String(page.value),
    size: String(size.value),
  })

  try {
    const response = await apiFetch(`/api/users?${query.toString()}`)

    if (response.status === 403) {
      error.value = 'You do not have permission to view users.'
      return
    }

    if (!response.ok) {
      error.value = 'Unable to load users.'
      return
    }

    const pageResponse = (await response.json()) as PageResponse<User>
    users.value = pageResponse.content
    page.value = pageResponse.number
    size.value = pageResponse.size
    totalPages.value = pageResponse.totalPages
    totalElements.value = pageResponse.totalElements
  } catch {
    error.value = 'Unable to load users.'
  } finally {
    await waitForMinimumTableLoadingDuration(loadingStartedAt)
    loading.value = false
  }
}

async function waitForMinimumTableLoadingDuration(startedAt: number) {
  const elapsed = Date.now() - startedAt

  if (elapsed >= minimumTableLoadingMs) {
    return
  }

  await new Promise((resolve) => window.setTimeout(resolve, minimumTableLoadingMs - elapsed))
}

function submitSearch() {
  page.value = 0
  loadUsers()
}

function clearSearch() {
  search.value = ''
  page.value = 0
  loadUsers()
}

function goToPage(nextPageNumber: number) {
  if (nextPageNumber === page.value || nextPageNumber < 0 || nextPageNumber >= totalPages.value) {
    return
  }

  page.value = nextPageNumber
  loadUsers()
}

function changePageSize(nextSize: number) {
  if (nextSize === size.value) {
    return
  }

  size.value = nextSize
  page.value = 0
  loadUsers()
}

function formatUserType(userType: User['userType']) {
  if (userType === 'SUPER_ADMIN') {
    return 'Super Administrator'
  }

  return 'Normal User'
}

function getInitials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean)

  if (parts.length === 0) {
    return '?'
  }

  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}

function getAvatarTone(value: string | number) {
  const text = String(value)
  let hash = 0

  for (let index = 0; index < text.length; index += 1) {
    hash += text.charCodeAt(index)
  }

  return `avatar-tone-${hash % 6}`
}

function openCreateForm() {
  clearCreateMessages()
  showCreateForm.value = true
}

function closeCreateForm() {
  if (createLoading.value) {
    return
  }

  showCreateForm.value = false
  resetCreateForm()
  clearCreateMessages()
}

function resetCreateForm() {
  createForm.name = ''
  createForm.username = ''
  createForm.email = ''
  createForm.password = ''
  clearCreateValidationErrors()
}

function clearCreateMessages() {
  clearCreateValidationErrors()
}

function clearCreateValidationErrors() {
  createValidationErrors.name = ''
  createValidationErrors.username = ''
  createValidationErrors.email = ''
  createValidationErrors.password = ''
}

function validateCreateForm() {
  clearCreateValidationErrors()

  if (!createForm.name.trim()) {
    createValidationErrors.name = 'Name is required.'
  }

  if (!createForm.username.trim()) {
    createValidationErrors.username = 'Username is required.'
  }

  if (!createForm.email.trim()) {
    createValidationErrors.email = 'Email is required.'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(createForm.email.trim())) {
    createValidationErrors.email = 'Enter a valid email address.'
  }

  if (!createForm.password) {
    createValidationErrors.password = 'Password is required.'
  } else if (createForm.password.length < 6) {
    createValidationErrors.password = 'Password must be at least 6 characters.'
  }

  return !Object.values(createValidationErrors).some(Boolean)
}

async function createUser() {
  if (!validateCreateForm()) {
    return
  }

  createLoading.value = true

  try {
    const response = await apiFetch('/api/users', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: createForm.name.trim(),
        username: createForm.username.trim(),
        email: createForm.email.trim(),
        password: createForm.password,
      }),
    })

    if (response.status === 403) {
      toast.error('You do not have permission to create users.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to create user. The username or email may already be in use.')
      return
    }

    showCreateForm.value = false
    resetCreateForm()
    page.value = 0
    await loadUsers()
    toast.success('User created successfully.')
  } catch {
    toast.error('Unable to create user. The username or email may already be in use.')
  } finally {
    createLoading.value = false
  }
}

function openEditForm(user: User) {
  selectedUser.value = user
  editForm.name = user.name
  editForm.username = user.username
  editForm.email = user.email
  editForm.active = user.active
  clearEditMessages()
  showEditForm.value = true
}

function closeEditForm() {
  if (editLoading.value) {
    return
  }

  showEditForm.value = false
  resetEditForm()
  clearEditMessages()
}

function resetEditForm() {
  selectedUser.value = null
  editForm.name = ''
  editForm.username = ''
  editForm.email = ''
  editForm.active = true
  clearEditValidationErrors()
}

function clearEditMessages() {
  clearEditValidationErrors()
}

function clearEditValidationErrors() {
  editValidationErrors.name = ''
  editValidationErrors.username = ''
  editValidationErrors.email = ''
  editValidationErrors.active = ''
}

function validateEditForm() {
  clearEditValidationErrors()

  if (!editForm.name.trim()) {
    editValidationErrors.name = 'Name is required.'
  }

  if (!editForm.username.trim()) {
    editValidationErrors.username = 'Username is required.'
  }

  if (!editForm.email.trim()) {
    editValidationErrors.email = 'Email is required.'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editForm.email.trim())) {
    editValidationErrors.email = 'Enter a valid email address.'
  }

  if (typeof editForm.active !== 'boolean') {
    editValidationErrors.active = 'Active status must be true or false.'
  }

  return !Object.values(editValidationErrors).some(Boolean)
}

async function updateUser() {
  if (!selectedUser.value || !validateEditForm()) {
    return
  }

  editLoading.value = true

  try {
    const response = await apiFetch(`/api/users/${selectedUser.value.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: editForm.name.trim(),
        username: editForm.username.trim(),
        email: editForm.email.trim(),
        active: editForm.active,
      }),
    })

    if (response.status === 403) {
      toast.error('You do not have permission to update users.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to update user. The username or email may already be in use.')
      return
    }

    showEditForm.value = false
    resetEditForm()
    await loadUsers()
    toast.success('User updated successfully.')
  } catch {
    toast.error('Unable to update user. The username or email may already be in use.')
  } finally {
    editLoading.value = false
  }
}

function shouldShowDeleteButton(user: User) {
  return canDeleteUser.value && user.userType === 'NORMAL_USER' && user.id !== authStore.user?.userId
}

function shouldShowEditButton(user: User) {
  return canUpdateUser.value && (user.userType !== 'SUPER_ADMIN' || authStore.isSuperAdmin)
}

function shouldShowPermissionsButton(user: User) {
  return authStore.isSuperAdmin && user.userType === 'NORMAL_USER'
}

function openDeleteConfirm(user: User) {
  userToDelete.value = user
  showDeleteConfirm.value = true
}

function closeDeleteConfirm() {
  if (deleteLoading.value) {
    return
  }

  showDeleteConfirm.value = false
  userToDelete.value = null
}

async function deleteUser() {
  if (!userToDelete.value) {
    return
  }

  deleteLoading.value = true

  try {
    const response = await apiFetch(`/api/users/${userToDelete.value.id}`, {
      method: 'DELETE',
    })

    if (response.status === 403) {
      toast.error('You do not have permission to delete this user.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to delete user.')
      return
    }

    showDeleteConfirm.value = false
    userToDelete.value = null
    toast.success('User deleted successfully.')
    await loadUsers()

    if (users.value.length === 0 && page.value > 0) {
      page.value -= 1
      await loadUsers()
    }
  } catch {
    toast.error('Unable to delete user.')
  } finally {
    deleteLoading.value = false
  }
}

function openPermissionsForm(user: User) {
  permissionUser.value = user
  permissionForm.CREATE = user.permissions.includes('CREATE')
  permissionForm.READ = user.permissions.includes('READ')
  permissionForm.UPDATE = user.permissions.includes('UPDATE')
  permissionForm.DELETE = user.permissions.includes('DELETE')
  showPermissionsForm.value = true
}

function closePermissionsForm() {
  if (permissionsLoading.value) {
    return
  }

  showPermissionsForm.value = false
  permissionUser.value = null
  resetPermissionForm()
}

function resetPermissionForm() {
  permissionForm.CREATE = false
  permissionForm.READ = false
  permissionForm.UPDATE = false
  permissionForm.DELETE = false
}

async function updatePermissions() {
  if (!permissionUser.value) {
    return
  }

  permissionsLoading.value = true

  const permissions = Object.entries(permissionForm)
    .filter(([, enabled]) => enabled)
    .map(([permission]) => permission as Permission)

  try {
    const response = await apiFetch(`/api/users/${permissionUser.value.id}/permissions`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        permissions,
      }),
    })

    if (response.status === 403) {
      toast.error('Only Super Administrators can manage permissions.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to update user permissions.')
      return
    }

    showPermissionsForm.value = false
    permissionUser.value = null
    resetPermissionForm()
    await loadUsers()
    toast.success('Permissions updated successfully.')
  } catch {
    toast.error('Unable to update user permissions.')
  } finally {
    permissionsLoading.value = false
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <div class="page-title-group">
        <div class="page-heading-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
            <path d="M9 11a4 4 0 1 0 0-8a4 4 0 0 0 0 8Z" />
            <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
        </div>
        <div class="page-heading-accent" aria-hidden="true"></div>
        <div class="page-heading-copy">
          <h1>Users</h1>
          <p>Manage application users and permissions.</p>
        </div>
      </div>

      <button v-if="canCreateUser" type="button" class="create-button" @click="openCreateForm">
        <svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 5v14" />
          <path d="M5 12h14" />
        </svg>
        Create User
      </button>
    </div>

    <div class="content-card">
      <form class="toolbar" @submit.prevent="submitSearch">
        <div class="search-field">
          <svg class="search-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M11 19a8 8 0 1 0 0-16a8 8 0 0 0 0 16Z" />
            <path d="m21 21l-4.35-4.35" />
          </svg>
          <input
            v-model="search"
            type="search"
            placeholder="Search by name, username or email"
            aria-label="Search users"
          />
        </div>
        <button type="submit" class="toolbar-button search-button" :disabled="loading">
          <svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M11 19a8 8 0 1 0 0-16a8 8 0 0 0 0 16Z" />
            <path d="m21 21l-4.35-4.35" />
          </svg>
          Search
        </button>
        <button
          v-if="search.trim()"
          type="button"
          class="toolbar-button secondary-button subtle-button"
          @click="clearSearch"
        >
          <svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M18 6L6 18" />
            <path d="M6 6l12 12" />
          </svg>
          Clear
        </button>
      </form>

      <div v-if="loading" class="table-wrapper" aria-hidden="true">
        <table class="skeleton-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Username</th>
              <th>Email</th>
              <th>User type</th>
              <th>Permissions</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in 6" :key="`user-skeleton-${row}`">
              <td><span class="skeleton skeleton-text width-lg"></span></td>
              <td><span class="skeleton skeleton-text width-md"></span></td>
              <td><span class="skeleton skeleton-text width-xl"></span></td>
              <td><span class="skeleton skeleton-text width-lg"></span></td>
              <td>
                <div class="skeleton-inline">
                  <span class="skeleton skeleton-pill"></span>
                  <span class="skeleton skeleton-pill"></span>
                </div>
              </td>
              <td><span class="skeleton skeleton-pill status"></span></td>
              <td>
                <div class="skeleton-inline actions">
                  <span class="skeleton skeleton-button"></span>
                  <span class="skeleton skeleton-button"></span>
                  <span class="skeleton skeleton-button wide"></span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="error" class="error-panel">{{ error }}</div>

      <div v-else-if="users.length === 0" class="state-panel">No users found.</div>

      <template v-else>
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Username</th>
                <th>Email</th>
                <th>User type</th>
                <th>Permissions</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>
                  <div class="entity-cell">
                    <span class="initials-avatar" :class="getAvatarTone(user.id)">
                      {{ getInitials(user.name) }}
                    </span>
                    <span class="entity-name">{{ user.name }}</span>
                  </div>
                </td>
                <td>{{ user.username }}</td>
                <td>{{ user.email }}</td>
                <td>
                  <span class="type-badge" :class="{ super: user.userType === 'SUPER_ADMIN' }">
                    {{ formatUserType(user.userType) }}
                  </span>
                </td>
                <td>
                  <div v-if="user.permissions.length" class="badges">
                    <span
                      v-for="permission in user.permissions"
                      :key="`${user.id}-${permission}`"
                      class="permission-badge"
                    >
                      {{ permission }}
                    </span>
                  </div>
                  <span v-else class="neutral-badge">
                    <span class="pill-dot neutral"></span>
                    None
                  </span>
                </td>
                <td>
                  <span class="status-badge" :class="{ inactive: !user.active }">
                    {{ user.active ? 'Active' : 'Inactive' }}
                  </span>
                </td>
                <td>
                  <div class="table-actions">
                    <button
                      v-if="shouldShowEditButton(user)"
                      type="button"
                      class="table-action-button"
                      @click="openEditForm(user)"
                    >
                      <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M4 20h4L19 9l-4-4L4 16v4Z" />
                        <path d="M13 7l4 4" />
                      </svg>
                      Edit
                    </button>
                    <button
                      v-if="shouldShowDeleteButton(user)"
                      type="button"
                      class="table-action-button danger-button"
                      @click="openDeleteConfirm(user)"
                    >
                      <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M5 7h14" />
                        <path d="M10 11v6" />
                        <path d="M14 11v6" />
                        <path d="M7 7l1 14h8l1-14" />
                        <path d="M9 7V4h6v3" />
                      </svg>
                      Delete
                    </button>
                    <button
                      v-if="shouldShowPermissionsButton(user)"
                      type="button"
                      class="table-action-button"
                      @click="openPermissionsForm(user)"
                    >
                      <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M12 3l7 3v5c0 5-3 8-7 10c-4-2-7-5-7-10V6l7-3Z" />
                        <path d="M9 12l2 2l4-4" />
                      </svg>
                      Permissions
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <TablePagination
          :current-page="page"
          :page-size="size"
          :total-pages="totalPages"
          :total-elements="totalElements"
          item-label="users"
          :disabled="loading"
          @page-change="goToPage"
          @page-size-change="changePageSize"
        />
      </template>
    </div>

    <div v-if="showCreateForm" class="modal-overlay" @click.self="closeCreateForm">
      <section class="modal" aria-labelledby="create-user-title">
        <div class="modal-heading">
          <h2 id="create-user-title">Create User</h2>
          <button type="button" class="icon-button" :disabled="createLoading" @click="closeCreateForm">
            Close
          </button>
        </div>

        <form class="create-form" @submit.prevent="createUser">
          <div class="form-field">
            <label for="create-name">Name</label>
            <input
              id="create-name"
              v-model="createForm.name"
              type="text"
              autocomplete="name"
              :aria-invalid="Boolean(createValidationErrors.name)"
            />
            <p v-if="createValidationErrors.name" class="field-error">
              {{ createValidationErrors.name }}
            </p>
          </div>

          <div class="form-field">
            <label for="create-username">Username</label>
            <input
              id="create-username"
              v-model="createForm.username"
              type="text"
              autocomplete="username"
              :aria-invalid="Boolean(createValidationErrors.username)"
            />
            <p v-if="createValidationErrors.username" class="field-error">
              {{ createValidationErrors.username }}
            </p>
          </div>

          <div class="form-field">
            <label for="create-email">Email</label>
            <input
              id="create-email"
              v-model="createForm.email"
              type="email"
              autocomplete="email"
              :aria-invalid="Boolean(createValidationErrors.email)"
            />
            <p v-if="createValidationErrors.email" class="field-error">
              {{ createValidationErrors.email }}
            </p>
          </div>

          <div class="form-field">
            <label for="create-password">Password</label>
            <input
              id="create-password"
              v-model="createForm.password"
              type="password"
              autocomplete="new-password"
              :aria-invalid="Boolean(createValidationErrors.password)"
            />
            <p v-if="createValidationErrors.password" class="field-error">
              {{ createValidationErrors.password }}
            </p>
          </div>

          <div class="modal-actions">
            <button
              type="button"
              class="secondary-button"
              :disabled="createLoading"
              @click="closeCreateForm"
            >
              Cancel
            </button>
            <button type="submit" :disabled="createLoading">
              {{ createLoading ? 'Creating...' : 'Create User' }}
            </button>
          </div>
        </form>
      </section>
    </div>

    <div v-if="showEditForm" class="modal-overlay" @click.self="closeEditForm">
      <section class="modal" aria-labelledby="edit-user-title">
        <div class="modal-heading">
          <h2 id="edit-user-title">Edit User</h2>
          <button type="button" class="icon-button" :disabled="editLoading" @click="closeEditForm">
            Close
          </button>
        </div>

        <form class="edit-form" @submit.prevent="updateUser">
          <div class="form-field">
            <label for="edit-name">Name</label>
            <input
              id="edit-name"
              v-model="editForm.name"
              type="text"
              autocomplete="name"
              :aria-invalid="Boolean(editValidationErrors.name)"
            />
            <p v-if="editValidationErrors.name" class="field-error">
              {{ editValidationErrors.name }}
            </p>
          </div>

          <div class="form-field">
            <label for="edit-username">Username</label>
            <input
              id="edit-username"
              v-model="editForm.username"
              type="text"
              autocomplete="username"
              :aria-invalid="Boolean(editValidationErrors.username)"
            />
            <p v-if="editValidationErrors.username" class="field-error">
              {{ editValidationErrors.username }}
            </p>
          </div>

          <div class="form-field">
            <label for="edit-email">Email</label>
            <input
              id="edit-email"
              v-model="editForm.email"
              type="email"
              autocomplete="email"
              :aria-invalid="Boolean(editValidationErrors.email)"
            />
            <p v-if="editValidationErrors.email" class="field-error">
              {{ editValidationErrors.email }}
            </p>
          </div>

          <div class="form-field">
            <label class="checkbox-field">
              <input v-model="editForm.active" type="checkbox" />
              Active
            </label>
            <p v-if="editValidationErrors.active" class="field-error">
              {{ editValidationErrors.active }}
            </p>
          </div>

          <div class="modal-actions">
            <button
              type="button"
              class="secondary-button"
              :disabled="editLoading"
              @click="closeEditForm"
            >
              Cancel
            </button>
            <button type="submit" :disabled="editLoading">
              {{ editLoading ? 'Saving...' : 'Save Changes' }}
            </button>
          </div>
        </form>
      </section>
    </div>

    <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="closeDeleteConfirm">
      <section class="modal" aria-labelledby="delete-user-title">
        <div class="modal-heading">
          <h2 id="delete-user-title">Delete User</h2>
          <button
            type="button"
            class="icon-button"
            :disabled="deleteLoading"
            @click="closeDeleteConfirm"
          >
            Close
          </button>
        </div>

        <div class="delete-copy">
          <p>Are you sure you want to delete "{{ userToDelete?.name }}"?</p>
          <p>This action cannot be undone.</p>
        </div>

        <div class="modal-actions">
          <button
            type="button"
            class="secondary-button"
            :disabled="deleteLoading"
            @click="closeDeleteConfirm"
          >
            Cancel
          </button>
          <button type="button" class="danger-button" :disabled="deleteLoading" @click="deleteUser">
            {{ deleteLoading ? 'Deleting...' : 'Delete User' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="showPermissionsForm" class="modal-overlay" @click.self="closePermissionsForm">
      <section class="modal" aria-labelledby="permissions-user-title">
        <div class="modal-heading">
          <div>
            <h2 id="permissions-user-title">Manage Permissions</h2>
            <p class="modal-subtitle">
              Permissions for {{ permissionUser?.name }} (@{{ permissionUser?.username }})
            </p>
          </div>
          <button
            type="button"
            class="icon-button"
            :disabled="permissionsLoading"
            @click="closePermissionsForm"
          >
            Close
          </button>
        </div>

        <form class="permissions-form" @submit.prevent="updatePermissions">
          <div class="permission-options">
            <label
              v-for="option in permissionOptions"
              :key="option.value"
              class="permission-option"
            >
              <input v-model="permissionForm[option.value]" type="checkbox" />
              <span>
                <strong>{{ option.label }}</strong>
                <small>{{ option.description }}</small>
              </span>
            </label>
          </div>

          <div class="modal-actions">
            <button
              type="button"
              class="secondary-button"
              :disabled="permissionsLoading"
              @click="closePermissionsForm"
            >
              Cancel
            </button>
            <button type="submit" :disabled="permissionsLoading">
              {{ permissionsLoading ? 'Saving...' : 'Save Permissions' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: 20px;
}

.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 28px;
  overflow: hidden;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background:
    radial-gradient(circle at 94% 0%, rgba(219, 234, 254, 0.72), transparent 28%),
    radial-gradient(circle at 84% 96%, rgba(204, 251, 241, 0.46), transparent 24%),
    linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
}

.page-title-group {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 18px;
}

.page-heading-icon {
  display: grid;
  width: 64px;
  height: 64px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 18px;
  color: #0f766e;
  background: linear-gradient(135deg, rgba(204, 251, 241, 0.96), rgba(224, 242, 254, 0.92));
}

.page-heading-icon svg {
  width: 34px;
  height: 34px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.page-heading-accent {
  width: 4px;
  height: 62px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: linear-gradient(180deg, #14b8a6, #0f766e);
}

.page-heading-copy {
  min-width: 0;
}

.page-heading h1 {
  margin: 0;
  color: #111827;
  font-size: 30px;
  line-height: 1.1;
  font-weight: 900;
}

.page-heading p {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 15px;
  line-height: 1.45;
}

.page-heading .create-button {
  flex: 0 0 auto;
}

.content-card {
  padding: 24px;
  border: 1px solid #dbe3ea;
  border-radius: 8px;
  background: #ffffff;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding: 10px;
  border: 1px solid #e5edf7;
  border-radius: 12px;
  background: #f8fbff;
}

.search-field {
  position: relative;
  flex: 1 1 360px;
  min-width: 240px;
}

.search-field input {
  width: 100%;
  padding-left: 40px;
  border-color: #dbe7f3;
  border-radius: 9px;
  background: #ffffff;
  color: #0f172a;
}

.search-field input::placeholder {
  color: #64748b;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  width: 18px;
  height: 18px;
  color: #64748b;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  transform: translateY(-50%);
  pointer-events: none;
}

input {
  min-height: 40px;
  flex: 1;
  min-width: 180px;
  padding: 9px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  color: #111827;
  font-size: 14px;
}

input:focus {
  outline: 3px solid rgba(20, 184, 166, 0.18);
  border-color: #0f766e;
}

input[aria-invalid='true'] {
  border-color: #dc2626;
}

label {
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

button {
  min-height: 40px;
  padding: 0 14px;
  border: 0;
  border-radius: 6px;
  color: #ffffff;
  background: #0f766e;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.create-button {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 18px;
  border-radius: 9px;
  background: linear-gradient(135deg, #0ea5a4, #0f766e);
  box-shadow: 0 10px 24px rgba(15, 118, 110, 0.26);
  white-space: nowrap;
}

.create-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #14b8a6, #0f766e);
}

.secondary-button {
  color: #334155;
  background: #e2e8f0;
}

.toolbar-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 15px;
  border-radius: 9px;
  font-size: 14px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.search-button {
  border: 1px solid #0f766e;
  color: #ffffff;
  background: linear-gradient(135deg, #0ea5a4, #0f766e);
  box-shadow: 0 8px 18px rgba(15, 118, 110, 0.18);
}

.search-button:hover:not(:disabled) {
  border-color: #0d9488;
  background: linear-gradient(135deg, #14b8a6, #0d9488);
}

.subtle-button {
  border: 1px solid #d8e3ec;
  color: #334155;
  background: #f1f6f9;
}

.subtle-button:hover:not(:disabled) {
  border-color: #cbd5e1;
  background: #e8f0f5;
}

.button-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.state-panel,
.error-panel {
  padding: 16px 18px;
  border-radius: 8px;
  font-weight: 600;
}

.state-panel {
  color: #475569;
  background: #f8fafc;
}

.error-panel {
  color: #991b1b;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.table-wrapper {
  overflow-x: auto;
  border: 1px solid #e5edf7;
  border-radius: 10px;
  background: #ffffff;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 940px;
}

th,
td {
  padding: 12px 14px;
  border-bottom: 1px solid #edf2f7;
  text-align: left;
  vertical-align: middle;
}

th {
  color: #1e3a5f;
  background: #f8fbff;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}

tbody tr {
  transition: background-color 0.16s ease;
}

tbody tr:hover {
  background: #f8fbff;
}

tbody tr:last-child td {
  border-bottom: 0;
}

.entity-cell {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 150px;
}

.entity-name {
  color: #111827;
  font-weight: 700;
}

.initials-avatar {
  display: inline-grid;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.avatar-tone-0 {
  color: #075985;
  background: #e0f2fe;
}

.avatar-tone-1 {
  color: #166534;
  background: #dcfce7;
}

.avatar-tone-2 {
  color: #7c3aed;
  background: #ede9fe;
}

.avatar-tone-3 {
  color: #be123c;
  background: #ffe4e6;
}

.avatar-tone-4 {
  color: #c2410c;
  background: #ffedd5;
}

.avatar-tone-5 {
  color: #0f766e;
  background: #ccfbf1;
}

.badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.permission-badge,
.status-badge,
.type-badge,
.neutral-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.permission-badge {
  color: #075985;
  background: #e0f2fe;
}

.status-badge {
  color: #166534;
  background: #dcfce7;
}

.status-badge.inactive {
  color: #991b1b;
  background: #fee2e2;
}

.type-badge {
  color: #334155;
  background: #f1f5f9;
}

.type-badge.super {
  color: #6d28d9;
  background: #ede9fe;
}

.neutral-badge {
  color: #475569;
  background: #f1f5f9;
}

.pill-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #94a3b8;
}

.pill-dot.neutral {
  background: #94a3b8;
}

.muted {
  color: #64748b;
}

.table-action-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 10px;
  color: #006ee6;
  background: #eef6ff;
  font-size: 13px;
  font-weight: 800;
}

.table-action-button:hover:not(:disabled) {
  background: #dbeafe;
}

.table-action-button.danger-button {
  color: #e11d48;
  background: #fff1f2;
}

.table-action-button.danger-button:hover:not(:disabled) {
  background: #ffe4e6;
}

.action-icon {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skeleton-table td {
  vertical-align: middle;
}

.skeleton {
  display: block;
  border-radius: 6px;
  background: linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 37%, #e5e7eb 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

.skeleton-text {
  height: 14px;
}

.skeleton-pill {
  width: 58px;
  height: 24px;
  border-radius: 999px;
}

.skeleton-pill.status {
  width: 64px;
}

.skeleton-button {
  width: 52px;
  height: 32px;
}

.skeleton-button.wide {
  width: 88px;
}

.skeleton-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skeleton-inline.actions {
  min-width: 208px;
}

.width-md {
  width: 92px;
}

.width-lg {
  width: 128px;
}

.width-xl {
  width: 178px;
}

@keyframes skeleton-loading {
  0% {
    background-position: 100% 50%;
  }

  100% {
    background-position: 0 50%;
  }
}

.danger-button {
  color: #ffffff;
  background: #dc2626;
}

.danger-button:hover {
  background: #b91c1c;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
  color: #475569;
  font-size: 14px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.45);
}

.modal {
  width: min(100%, 520px);
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  padding: 24px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}

.modal-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.modal-heading h2 {
  margin: 0;
  color: #111827;
  font-size: 22px;
}

.modal-subtitle {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
}

.icon-button {
  min-height: 34px;
  color: #334155;
  background: #e2e8f0;
}

.create-form,
.edit-form,
.permissions-form {
  display: grid;
  gap: 16px;
}

.form-field {
  display: grid;
  gap: 7px;
}

.form-field input {
  width: 100%;
}

.checkbox-field {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.checkbox-field input {
  width: auto;
  min-width: 0;
  min-height: 0;
  flex: 0 0 auto;
}

.field-error {
  margin: 0;
  color: #b91c1c;
  font-size: 13px;
}

.delete-copy {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
  color: #334155;
}

.delete-copy p {
  margin: 0;
}

.permission-options {
  display: grid;
  gap: 10px;
}

.permission-option {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border: 1px solid #dbe3ea;
  border-radius: 8px;
  background: #f8fafc;
}

.permission-option input {
  width: auto;
  min-width: 0;
  min-height: 0;
  flex: 0 0 auto;
  margin-top: 3px;
}

.permission-option span {
  display: grid;
  gap: 3px;
}

.permission-option strong {
  color: #111827;
  font-size: 14px;
}

.permission-option small {
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

@media (max-width: 720px) {
  .page-heading,
  .toolbar,
  .pagination,
  .modal-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .content-card {
    padding: 16px;
  }

  .page-heading {
    padding: 20px;
  }

  .page-title-group {
    align-items: flex-start;
    gap: 14px;
  }

  .page-heading-icon {
    width: 54px;
    height: 54px;
    border-radius: 15px;
  }

  .page-heading-icon svg {
    width: 29px;
    height: 29px;
  }

  .page-heading-accent {
    height: 54px;
  }

  .page-heading h1 {
    font-size: 26px;
  }

  .page-heading .create-button {
    width: 100%;
    justify-content: center;
  }

  .search-field,
  .toolbar-button {
    width: 100%;
    min-width: 0;
  }

  .pagination-controls {
    justify-content: space-between;
  }

  .modal-overlay {
    padding: 14px;
  }

  .modal {
    padding: 18px;
  }
}
</style>
