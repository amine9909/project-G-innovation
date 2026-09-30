<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import TablePagination from '@/components/TablePagination.vue'
import { apiFetch } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import type { Employee, PageResponse } from '@/types/employee'

type EmployeeExportFormat = 'excel' | 'pdf'
type EmployeeSortBy = '' | 'name' | 'hireDate' | 'salary'
type EmployeeSortDir = 'asc' | 'desc'

interface EmployeeImportResponse {
  message: string
  importedCount: number
}

interface EmployeeImportErrorResponse {
  message?: string
  errorCount?: number
  errors?: string[]
}

const authStore = useAuthStore()
const toast = useToastStore()

const employees = ref<Employee[]>([])
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
const selectedEmployee = ref<Employee | null>(null)
const detailEmployee = ref<Employee | null>(null)
const showDeleteConfirm = ref(false)
const deleteLoading = ref(false)
const employeeToDelete = ref<Employee | null>(null)
const showPhotoModal = ref(false)
const photoEmployee = ref<Employee | null>(null)
const photoLoading = ref(false)
const photoUploading = ref(false)
const photoDeleting = ref(false)
const photoPreviewUrl = ref('')
const selectedPhotoFile = ref<File | null>(null)
const showCvModal = ref(false)
const cvEmployee = ref<Employee | null>(null)
const selectedCvFile = ref<File | null>(null)
const cvUploading = ref(false)
const cvDeleting = ref(false)
const cvViewing = ref(false)
const cvDownloading = ref(false)
const showCvPreviewModal = ref(false)
const cvPreviewUrl = ref('')
const contractDownloadingId = ref<number | null>(null)
const showExportMenu = ref(false)
const importLoading = ref(false)
const exportLoading = ref(false)
const templateDownloading = ref(false)
const importErrorTitle = ref('')
const importErrorSummary = ref('')
const importErrorDetails = ref<string[]>([])
const showImportErrorDetails = ref(false)
const hasOpenedImportDetails = ref(false)
const importErrorToastId = ref<number | null>(null)
const importFileInput = ref<HTMLInputElement | null>(null)

const minimumTableLoadingMs = 500
const maxEmployeeImportSizeBytes = 5 * 1024 * 1024
const numberFormatter = new Intl.NumberFormat()

const filters = reactive<{
  department: string
  jobTitle: string
  hireDateFrom: string
  hireDateTo: string
  salaryMin: string
  salaryMax: string
  sortBy: EmployeeSortBy
  sortDir: EmployeeSortDir
}>({
  department: '',
  jobTitle: '',
  hireDateFrom: '',
  hireDateTo: '',
  salaryMin: '',
  salaryMax: '',
  sortBy: '',
  sortDir: 'asc',
})

const createForm = reactive({
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  jobTitle: '',
  department: '',
  hireDate: '',
  salary: '',
})

const createValidationErrors = reactive({
  firstName: '',
  lastName: '',
  email: '',
  jobTitle: '',
  hireDate: '',
  salary: '',
})

const editForm = reactive<{
  firstName: string
  lastName: string
  email: string
  phoneNumber: string
  jobTitle: string
  department: string
  hireDate: string
  salary: string | number | null
}>({
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  jobTitle: '',
  department: '',
  hireDate: '',
  salary: '',
})

const editValidationErrors = reactive({
  firstName: '',
  lastName: '',
  email: '',
  jobTitle: '',
  hireDate: '',
  salary: '',
})

const canCreateEmployee = computed(() => {
  return authStore.isSuperAdmin || Boolean(authStore.user?.permissions.includes('CREATE'))
})

const canExportEmployees = computed(() => {
  return authStore.isSuperAdmin || Boolean(authStore.user?.permissions.includes('READ'))
})

const canImportEmployees = computed(() => {
  return authStore.isSuperAdmin || Boolean(authStore.user?.permissions.includes('CREATE'))
})

const canUpdateEmployee = computed(() => {
  return authStore.isSuperAdmin || Boolean(authStore.user?.permissions.includes('UPDATE'))
})

const canDeleteEmployee = computed(() => {
  return authStore.isSuperAdmin || Boolean(authStore.user?.permissions.includes('DELETE'))
})

const hasActiveEmployeeFilters = computed(() => {
  return (
    Boolean(filters.department.trim()) ||
    Boolean(filters.jobTitle.trim()) ||
    Boolean(filters.hireDateFrom) ||
    Boolean(filters.hireDateTo) ||
    Boolean(filters.salaryMin) ||
    Boolean(filters.salaryMax) ||
    Boolean(filters.sortBy)
  )
})

const canManagePhoto = computed(() => {
  return authStore.isSuperAdmin || Boolean(authStore.user?.permissions.includes('UPDATE'))
})

const canManageCv = computed(() => {
  return authStore.isSuperAdmin || Boolean(authStore.user?.permissions.includes('UPDATE'))
})

async function loadEmployees() {
  const loadingStartedAt = Date.now()
  loading.value = true
  error.value = ''

  const query = new URLSearchParams({
    search: search.value.trim(),
    page: String(page.value),
    size: String(size.value),
  })
  appendEmployeeFilters(query)

  try {
    const response = await apiFetch(`/api/employees?${query.toString()}`)

    if (response.status === 403) {
      error.value = 'You do not have permission to view employees.'
      return
    }

    if (!response.ok) {
      error.value = 'Unable to load employees.'
      return
    }

    const pageResponse = (await response.json()) as PageResponse<Employee>
    employees.value = pageResponse.content
    page.value = pageResponse.number
    size.value = pageResponse.size
    totalPages.value = pageResponse.totalPages
    totalElements.value = pageResponse.totalElements
  } catch {
    error.value = 'Unable to load employees.'
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
  loadEmployees()
}

function clearSearch() {
  search.value = ''
  page.value = 0
  loadEmployees()
}

function appendEmployeeFilters(query: URLSearchParams) {
  appendQueryParam(query, 'department', filters.department.trim())
  appendQueryParam(query, 'jobTitle', filters.jobTitle.trim())
  appendQueryParam(query, 'hireDateFrom', filters.hireDateFrom)
  appendQueryParam(query, 'hireDateTo', filters.hireDateTo)
  appendQueryParam(query, 'salaryMin', filters.salaryMin)
  appendQueryParam(query, 'salaryMax', filters.salaryMax)

  if (filters.sortBy) {
    query.set('sortBy', filters.sortBy)
    query.set('sortDir', filters.sortDir)
  }
}

function appendQueryParam(query: URLSearchParams, key: string, value: string) {
  if (value) {
    query.set(key, value)
  }
}

function applyEmployeeFilters() {
  page.value = 0
  loadEmployees()
}

function clearEmployeeFilters() {
  filters.department = ''
  filters.jobTitle = ''
  filters.hireDateFrom = ''
  filters.hireDateTo = ''
  filters.salaryMin = ''
  filters.salaryMax = ''
  filters.sortBy = ''
  filters.sortDir = 'asc'
  page.value = 0
  loadEmployees()
}

function goToPage(nextPageNumber: number) {
  if (nextPageNumber === page.value || nextPageNumber < 0 || nextPageNumber >= totalPages.value) {
    return
  }

  page.value = nextPageNumber
  loadEmployees()
}

function changePageSize(nextSize: number) {
  if (nextSize === size.value) {
    return
  }

  size.value = nextSize
  page.value = 0
  loadEmployees()
}

function fullName(employee: Employee) {
  return `${employee.firstName} ${employee.lastName}`
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

function formatOptional(value: string | null) {
  return value?.trim() ? value : '—'
}

function formatSalary(salary: number | null) {
  if (salary === null) {
    return '—'
  }

  return numberFormatter.format(salary)
}

function openEmployeeDetailsDrawer(employee: Employee) {
  detailEmployee.value = employee
}

function closeEmployeeDetailsDrawer() {
  detailEmployee.value = null
}

function openEmployeeDetailsEdit(employee: Employee) {
  closeEmployeeDetailsDrawer()
  openEditForm(employee)
}

function openEmployeeDetailsPhoto(employee: Employee) {
  closeEmployeeDetailsDrawer()
  openPhotoModal(employee)
}

function openEmployeeDetailsCv(employee: Employee) {
  closeEmployeeDetailsDrawer()
  openCvModal(employee)
}

function downloadEmployeeDetailsContract(employee: Employee) {
  downloadContract(employee)
}

function triggerEmployeeImport() {
  clearImportMessages()
  importFileInput.value?.click()
}

function toggleExportMenu() {
  clearImportMessages()
  showExportMenu.value = !showExportMenu.value
}

function closeExportMenu() {
  showExportMenu.value = false
}

function handleDocumentClick() {
  if (showExportMenu.value) {
    closeExportMenu()
  }
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeEmployeeDetailsDrawer()
  }
}

function getDownloadFilename(contentDisposition: string | null) {
  if (!contentDisposition) {
    return null
  }

  const filenameStarMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (filenameStarMatch?.[1]) {
    try {
      return decodeURIComponent(filenameStarMatch[1].replace(/"/g, ''))
    } catch {
      return null
    }
  }

  const filenameMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return filenameMatch?.[1] ?? null
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

async function readEmployeeImportError(response: Response) {
  try {
    return (await response.json()) as EmployeeImportErrorResponse
  } catch {
    return null
  }
}

function clearImportMessages(dismissLinkedToast = true) {
  if (dismissLinkedToast && importErrorToastId.value !== null) {
    toast.dismiss(importErrorToastId.value)
  }

  importErrorToastId.value = null
  importErrorTitle.value = ''
  importErrorSummary.value = ''
  importErrorDetails.value = []
  showImportErrorDetails.value = false
  hasOpenedImportDetails.value = false
}

function getImportErrorRowCount(errors: string[]) {
  const rowNumbers = new Set<number>()

  errors.forEach((error) => {
    const match = error.match(/^Row\s+(\d+):/i)
    if (match?.[1]) {
      rowNumbers.add(Number(match[1]))
    }
  })

  return rowNumbers.size
}

function showEmployeeImportValidationError(errorResponse: EmployeeImportErrorResponse) {
  const errors = errorResponse.errors ?? []
  const errorCount = errorResponse.errorCount ?? errors.length
  const rowCount = getImportErrorRowCount(errors)
  const rowLabel = rowCount === 1 ? 'row' : 'rows'
  const issueLabel = errorCount === 1 ? 'issue' : 'issues'
  const summary =
    rowCount > 0
      ? `${rowCount} ${rowLabel} could not be imported. Please review the file and try again.`
      : `${errorCount} validation ${issueLabel} found. Please review the file and try again.`

  importErrorTitle.value = 'Import failed'
  importErrorSummary.value = summary
  importErrorDetails.value = errors
  showImportErrorDetails.value = false
  hasOpenedImportDetails.value = false
  importErrorToastId.value = toast.error(
    rowCount > 0
      ? `Import failed. ${rowCount} ${rowLabel} could not be imported.`
      : `Import failed. ${errorCount} validation ${issueLabel} found.`,
  )
}

function toggleImportErrorDetails() {
  showImportErrorDetails.value = !showImportErrorDetails.value

  if (showImportErrorDetails.value) {
    hasOpenedImportDetails.value = true
  }
}

watch(
  () => {
    const toastId = importErrorToastId.value

    return toastId === null || toast.toasts.some((activeToast) => activeToast.id === toastId)
  },
  (isLinkedToastActive) => {
    if (!isLinkedToastActive && !hasOpenedImportDetails.value) {
      clearImportMessages(false)
    }
  },
)

async function importEmployees(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null

  clearImportMessages()

  if (!file) {
    return
  }

  if (!file.name.toLowerCase().endsWith('.xlsx')) {
    toast.error('Import file must be an .xlsx Excel file.')
    input.value = ''
    return
  }

  if (file.size > maxEmployeeImportSizeBytes) {
    toast.error('Import file must be 5 MB or smaller.')
    input.value = ''
    return
  }

  importLoading.value = true

  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await apiFetch('/api/employees/import', {
      method: 'POST',
      body: formData,
    })

    if (response.status === 403) {
      toast.error('You do not have permission to import employees.')
      return
    }

    if (!response.ok) {
      const importError = await readEmployeeImportError(response)

      if (importError?.errors?.length) {
        showEmployeeImportValidationError(importError)
        return
      }

      toast.error(
        importError?.message?.trim() || 'Unable to import employees. Please check the file and try again.',
      )
      return
    }

    const data = (await response.json()) as EmployeeImportResponse
    toast.success(data.message || 'Employees imported successfully.')
    page.value = 0
    await loadEmployees()
  } catch {
    toast.error('Unable to import employees. Please check the file and try again.')
  } finally {
    importLoading.value = false
    input.value = ''
  }
}

async function downloadEmployeeExport(format: EmployeeExportFormat) {
  exportLoading.value = true
  clearImportMessages()
  closeExportMenu()

  const isExcel = format === 'excel'
  const fallbackFilename = isExcel ? 'employees.xlsx' : 'employees.pdf'

  try {
    const response = await apiFetch(`/api/employees/export/${format}`)

    if (response.status === 403) {
      toast.error('You do not have permission to export employees.')
      return
    }

    if (!response.ok) {
      toast.error(`Unable to export employees as ${isExcel ? 'Excel' : 'PDF'}.`)
      return
    }

    const blob = await response.blob()
    const filename = getDownloadFilename(response.headers.get('Content-Disposition')) ?? fallbackFilename
    downloadBlob(blob, filename)
    toast.success(`Employees exported as ${isExcel ? 'Excel' : 'PDF'} successfully.`)
  } catch {
    toast.error(`Unable to export employees as ${isExcel ? 'Excel' : 'PDF'}.`)
  } finally {
    exportLoading.value = false
  }
}

async function downloadImportTemplate() {
  templateDownloading.value = true
  clearImportMessages()

  try {
    const response = await apiFetch('/api/employees/import-template')

    if (response.status === 403) {
      toast.error('You do not have permission to download the employee import template.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to download the employee import template.')
      return
    }

    const blob = await response.blob()
    const filename =
      getDownloadFilename(response.headers.get('Content-Disposition')) ??
      'employee-import-template.xlsx'
    downloadBlob(blob, filename)
    toast.success('Employee import template downloaded successfully.')
  } catch {
    toast.error('Unable to download the employee import template.')
  } finally {
    templateDownloading.value = false
  }
}

function revokePhotoPreviewUrl() {
  if (photoPreviewUrl.value) {
    URL.revokeObjectURL(photoPreviewUrl.value)
    photoPreviewUrl.value = ''
  }
}

function revokeCvPreviewUrl() {
  if (cvPreviewUrl.value) {
    URL.revokeObjectURL(cvPreviewUrl.value)
    cvPreviewUrl.value = ''
  }
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
  createForm.firstName = ''
  createForm.lastName = ''
  createForm.email = ''
  createForm.phoneNumber = ''
  createForm.jobTitle = ''
  createForm.department = ''
  createForm.hireDate = ''
  createForm.salary = ''
  clearCreateValidationErrors()
}

function clearCreateMessages() {
  clearCreateValidationErrors()
}

function clearCreateValidationErrors() {
  createValidationErrors.firstName = ''
  createValidationErrors.lastName = ''
  createValidationErrors.email = ''
  createValidationErrors.jobTitle = ''
  createValidationErrors.hireDate = ''
  createValidationErrors.salary = ''
}

function validateCreateForm() {
  clearCreateValidationErrors()

  if (!createForm.firstName.trim()) {
    createValidationErrors.firstName = 'First name is required.'
  }

  if (!createForm.lastName.trim()) {
    createValidationErrors.lastName = 'Last name is required.'
  }

  if (!createForm.email.trim()) {
    createValidationErrors.email = 'Email is required.'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(createForm.email.trim())) {
    createValidationErrors.email = 'Enter a valid email address.'
  }

  if (!createForm.jobTitle.trim()) {
    createValidationErrors.jobTitle = 'Job title is required.'
  }

  if (!createForm.hireDate) {
    createValidationErrors.hireDate = 'Hire date is required.'
  }

  const hasSalary =
    createForm.salary !== '' && createForm.salary !== null && createForm.salary !== undefined

  if (hasSalary && !Number.isFinite(Number(createForm.salary))) {
    createValidationErrors.salary = 'Salary must be a valid number.'
  }

  return !Object.values(createValidationErrors).some(Boolean)
}

async function createEmployee() {
  if (!validateCreateForm()) {
    return
  }

  createLoading.value = true

  try {
    const response = await apiFetch('/api/employees', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        firstName: createForm.firstName.trim(),
        lastName: createForm.lastName.trim(),
        email: createForm.email.trim(),
        phoneNumber: createForm.phoneNumber.trim() || null,
        jobTitle: createForm.jobTitle.trim(),
        department: createForm.department.trim() || null,
        hireDate: createForm.hireDate,
        salary:
          createForm.salary === '' || createForm.salary === null || createForm.salary === undefined
            ? null
            : Number(createForm.salary),
      }),
    })

    if (response.status === 403) {
      toast.error('You do not have permission to create employees.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to create employee. The email may already be in use.')
      return
    }

    showCreateForm.value = false
    resetCreateForm()
    page.value = 0
    await loadEmployees()
    toast.success('Employee created successfully.')
  } catch {
    toast.error('Unable to create employee. The email may already be in use.')
  } finally {
    createLoading.value = false
  }
}

function openEditForm(employee: Employee) {
  selectedEmployee.value = employee
  editForm.firstName = employee.firstName
  editForm.lastName = employee.lastName
  editForm.email = employee.email
  editForm.phoneNumber = employee.phoneNumber ?? ''
  editForm.jobTitle = employee.jobTitle
  editForm.department = employee.department ?? ''
  editForm.hireDate = employee.hireDate
  editForm.salary = employee.salary === null ? '' : String(employee.salary)
  clearEditMessages()
  showEditForm.value = true
}

function closeEditForm() {
  if (editLoading.value) {
    return
  }

  showEditForm.value = false
  selectedEmployee.value = null
  resetEditForm()
  clearEditMessages()
}

function resetEditForm() {
  editForm.firstName = ''
  editForm.lastName = ''
  editForm.email = ''
  editForm.phoneNumber = ''
  editForm.jobTitle = ''
  editForm.department = ''
  editForm.hireDate = ''
  editForm.salary = ''
  clearEditValidationErrors()
}

function clearEditMessages() {
  clearEditValidationErrors()
}

function clearEditValidationErrors() {
  editValidationErrors.firstName = ''
  editValidationErrors.lastName = ''
  editValidationErrors.email = ''
  editValidationErrors.jobTitle = ''
  editValidationErrors.hireDate = ''
  editValidationErrors.salary = ''
}

function validateEditForm() {
  clearEditValidationErrors()

  if (!editForm.firstName.trim()) {
    editValidationErrors.firstName = 'First name is required.'
  }

  if (!editForm.lastName.trim()) {
    editValidationErrors.lastName = 'Last name is required.'
  }

  if (!editForm.email.trim()) {
    editValidationErrors.email = 'Email is required.'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editForm.email.trim())) {
    editValidationErrors.email = 'Enter a valid email address.'
  }

  if (!editForm.jobTitle.trim()) {
    editValidationErrors.jobTitle = 'Job title is required.'
  }

  if (!editForm.hireDate) {
    editValidationErrors.hireDate = 'Hire date is required.'
  }

  const hasSalary =
    editForm.salary !== '' && editForm.salary !== null && editForm.salary !== undefined

  if (hasSalary && !Number.isFinite(Number(editForm.salary))) {
    editValidationErrors.salary = 'Salary must be a valid number.'
  }

  return !Object.values(editValidationErrors).some(Boolean)
}

async function updateEmployee() {
  if (!selectedEmployee.value || !validateEditForm()) {
    return
  }

  editLoading.value = true

  try {
    const response = await apiFetch(`/api/employees/${selectedEmployee.value.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        firstName: editForm.firstName.trim(),
        lastName: editForm.lastName.trim(),
        email: editForm.email.trim(),
        phoneNumber: editForm.phoneNumber.trim() || null,
        jobTitle: editForm.jobTitle.trim(),
        department: editForm.department.trim() || null,
        hireDate: editForm.hireDate,
        salary:
          editForm.salary === '' || editForm.salary === null || editForm.salary === undefined
            ? null
            : Number(editForm.salary),
      }),
    })

    if (response.status === 403) {
      toast.error('You do not have permission to update employees.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to update employee. The email may already be in use.')
      return
    }

    showEditForm.value = false
    resetEditForm()
    selectedEmployee.value = null
    await loadEmployees()
    toast.success('Employee updated successfully.')
  } catch {
    toast.error('Unable to update employee. The email may already be in use.')
  } finally {
    editLoading.value = false
  }
}

function openDeleteConfirm(employee: Employee) {
  employeeToDelete.value = employee
  showDeleteConfirm.value = true
}

function closeDeleteConfirm() {
  if (deleteLoading.value) {
    return
  }

  showDeleteConfirm.value = false
  employeeToDelete.value = null
}

async function deleteEmployee() {
  if (!employeeToDelete.value) {
    return
  }

  deleteLoading.value = true

  try {
    const response = await apiFetch(`/api/employees/${employeeToDelete.value.id}`, {
      method: 'DELETE',
    })

    if (response.status === 403) {
      toast.error('You do not have permission to delete employees.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to delete employee.')
      return
    }

    showDeleteConfirm.value = false
    employeeToDelete.value = null
    toast.success('Employee deleted successfully.')
    await loadEmployees()

    if (employees.value.length === 0 && page.value > 0) {
      page.value -= 1
      await loadEmployees()
    }
  } catch {
    toast.error('Unable to delete employee.')
  } finally {
    deleteLoading.value = false
  }
}

async function openPhotoModal(employee: Employee) {
  photoEmployee.value = employee
  selectedPhotoFile.value = null
  revokePhotoPreviewUrl()
  showPhotoModal.value = true

  if (employee.hasPhoto) {
    await loadEmployeePhoto()
  }
}

function closePhotoModal() {
  if (photoUploading.value || photoDeleting.value) {
    return
  }

  showPhotoModal.value = false
  photoEmployee.value = null
  selectedPhotoFile.value = null
  revokePhotoPreviewUrl()
}

async function loadEmployeePhoto() {
  if (!photoEmployee.value) {
    return
  }

  const employeeId = photoEmployee.value.id
  photoLoading.value = true

  try {
    const response = await apiFetch(`/api/employees/${employeeId}/photo`)

    if (!showPhotoModal.value || photoEmployee.value?.id !== employeeId) {
      return
    }

    if (response.status === 403) {
      toast.error('You do not have permission to view this photo.')
      return
    }

    if (response.status === 404) {
      revokePhotoPreviewUrl()
      return
    }

    if (!response.ok) {
      toast.error('Unable to load employee photo.')
      return
    }

    const blob = await response.blob()

    if (!showPhotoModal.value || photoEmployee.value?.id !== employeeId) {
      return
    }

    revokePhotoPreviewUrl()
    photoPreviewUrl.value = URL.createObjectURL(blob)
  } catch {
    toast.error('Unable to load employee photo.')
  } finally {
    photoLoading.value = false
  }
}

function handlePhotoFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  selectedPhotoFile.value = null

  if (!file) {
    return
  }

  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  const maxPhotoSizeBytes = 5 * 1024 * 1024

  if (!allowedTypes.includes(file.type)) {
    toast.error('Photo must be a JPEG, PNG, or WebP image.')
    input.value = ''
    return
  }

  if (file.size > maxPhotoSizeBytes) {
    toast.error('Photo must be 5 MB or smaller.')
    input.value = ''
    return
  }

  selectedPhotoFile.value = file
}

function refreshPhotoEmployee() {
  const refreshed = employees.value.find((employee) => employee.id === photoEmployee.value?.id)

  if (refreshed) {
    photoEmployee.value = refreshed
  }
}

async function uploadPhoto() {
  if (!photoEmployee.value || !selectedPhotoFile.value) {
    return
  }

  photoUploading.value = true

  const formData = new FormData()
  formData.append('file', selectedPhotoFile.value)

  try {
    const response = await apiFetch(`/api/employees/${photoEmployee.value.id}/photo`, {
      method: 'PUT',
      body: formData,
    })

    if (response.status === 403) {
      toast.error('You do not have permission to update employee photos.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to upload employee photo.')
      return
    }

    selectedPhotoFile.value = null
    await loadEmployees()
    refreshPhotoEmployee()
    await loadEmployeePhoto()
    toast.success('Employee photo saved successfully.')
  } catch {
    toast.error('Unable to upload employee photo.')
  } finally {
    photoUploading.value = false
  }
}

async function deletePhoto() {
  if (!photoEmployee.value) {
    return
  }

  if (!window.confirm('Are you sure you want to delete this employee photo?')) {
    return
  }

  photoDeleting.value = true

  try {
    const response = await apiFetch(`/api/employees/${photoEmployee.value.id}/photo`, {
      method: 'DELETE',
    })

    if (response.status === 403) {
      toast.error('You do not have permission to delete employee photos.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to delete employee photo.')
      return
    }

    revokePhotoPreviewUrl()
    selectedPhotoFile.value = null
    await loadEmployees()
    refreshPhotoEmployee()
    toast.success('Employee photo deleted successfully.')
  } catch {
    toast.error('Unable to delete employee photo.')
  } finally {
    photoDeleting.value = false
  }
}

function openCvModal(employee: Employee) {
  cvEmployee.value = employee
  selectedCvFile.value = null
  showCvModal.value = true
}

function closeCvModal() {
  if (cvUploading.value || cvDeleting.value || cvViewing.value || cvDownloading.value) {
    return
  }

  closeCvPreviewModal()
  showCvModal.value = false
  cvEmployee.value = null
  selectedCvFile.value = null
}

function closeCvPreviewModal() {
  showCvPreviewModal.value = false
  revokeCvPreviewUrl()
}

function handleCvFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  selectedCvFile.value = null

  if (!file) {
    return
  }

  const maxCvSizeBytes = 10 * 1024 * 1024

  if (file.type !== 'application/pdf') {
    toast.error('CV must be a PDF file.')
    input.value = ''
    return
  }

  if (file.size > maxCvSizeBytes) {
    toast.error('CV must be 10 MB or smaller.')
    input.value = ''
    return
  }

  selectedCvFile.value = file
}

function refreshCvEmployee() {
  const refreshed = employees.value.find((employee) => employee.id === cvEmployee.value?.id)

  if (refreshed) {
    cvEmployee.value = refreshed
  }
}

async function uploadCv() {
  if (!cvEmployee.value || !selectedCvFile.value) {
    return
  }

  cvUploading.value = true

  const formData = new FormData()
  formData.append('file', selectedCvFile.value)

  try {
    const response = await apiFetch(`/api/employees/${cvEmployee.value.id}/cv`, {
      method: 'PUT',
      body: formData,
    })

    if (response.status === 403) {
      toast.error('You do not have permission to update employee CVs.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to upload employee CV.')
      return
    }

    selectedCvFile.value = null
    await loadEmployees()
    refreshCvEmployee()
    toast.success('Employee CV saved successfully.')
  } catch {
    toast.error('Unable to upload employee CV.')
  } finally {
    cvUploading.value = false
  }
}

async function viewCv() {
  if (!cvEmployee.value) {
    return
  }

  cvViewing.value = true

  try {
    const response = await apiFetch(`/api/employees/${cvEmployee.value.id}/cv`)

    if (response.status === 403) {
      toast.error('You do not have permission to view this CV.')
      return
    }

    if (response.status === 404) {
      toast.error('No CV is available for this employee.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to open employee CV.')
      return
    }

    const blob = await response.blob()
    const url = URL.createObjectURL(blob)

    revokeCvPreviewUrl()
    cvPreviewUrl.value = url
    showCvPreviewModal.value = true
  } catch {
    toast.error('Unable to open employee CV.')
  } finally {
    cvViewing.value = false
  }
}

function getCvDownloadFilename(contentDisposition: string | null) {
  if (!contentDisposition) {
    return null
  }

  const filenameStarMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (filenameStarMatch?.[1]) {
    try {
      return decodeURIComponent(filenameStarMatch[1].replace(/"/g, ''))
    } catch {
      return null
    }
  }

  const filenameMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return filenameMatch?.[1] ?? null
}

async function downloadCv() {
  if (!cvEmployee.value) {
    return
  }

  cvDownloading.value = true

  try {
    const response = await apiFetch(`/api/employees/${cvEmployee.value.id}/cv/download`)

    if (response.status === 403) {
      toast.error('You do not have permission to download this CV.')
      return
    }

    if (response.status === 404) {
      toast.error('No CV is available for this employee.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to download employee CV.')
      return
    }

    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const filename =
      getCvDownloadFilename(response.headers.get('Content-Disposition')) ??
      `employee-${cvEmployee.value.id}-cv.pdf`
    const link = document.createElement('a')

    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } catch {
    toast.error('Unable to download employee CV.')
  } finally {
    cvDownloading.value = false
  }
}

async function deleteCv() {
  if (!cvEmployee.value) {
    return
  }

  if (!window.confirm('Are you sure you want to delete this employee CV?')) {
    return
  }

  cvDeleting.value = true

  try {
    const response = await apiFetch(`/api/employees/${cvEmployee.value.id}/cv`, {
      method: 'DELETE',
    })

    if (response.status === 403) {
      toast.error('You do not have permission to delete employee CVs.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to delete employee CV.')
      return
    }

    await loadEmployees()
    refreshCvEmployee()
    selectedCvFile.value = null
    toast.success('Employee CV deleted successfully.')
  } catch {
    toast.error('Unable to delete employee CV.')
  } finally {
    cvDeleting.value = false
  }
}

function getContractDownloadFilename(contentDisposition: string | null) {
  if (!contentDisposition) {
    return null
  }

  const filenameStarMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (filenameStarMatch?.[1]) {
    try {
      return decodeURIComponent(filenameStarMatch[1].replace(/"/g, ''))
    } catch {
      return null
    }
  }

  const filenameMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return filenameMatch?.[1] ?? null
}

async function downloadContract(employee: Employee) {
  contractDownloadingId.value = employee.id

  try {
    const response = await apiFetch(`/api/employees/${employee.id}/contract`)

    if (response.status === 403) {
      toast.error('You do not have permission to download employee contracts.')
      return
    }

    if (response.status === 404) {
      toast.error('Employee contract could not be generated.')
      return
    }

    if (!response.ok) {
      toast.error('Unable to download employee contract.')
      return
    }

    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const filename =
      getContractDownloadFilename(response.headers.get('Content-Disposition')) ??
      `employee-${employee.id}-contract.pdf`
    const link = document.createElement('a')

    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    toast.success('Employee contract downloaded successfully.')
  } catch {
    toast.error('Unable to download employee contract.')
  } finally {
    contractDownloadingId.value = null
  }
}

onMounted(() => {
  loadEmployees()
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleDocumentKeydown)
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleDocumentKeydown)
  revokePhotoPreviewUrl()
  revokeCvPreviewUrl()
})
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <div class="page-title-group">
        <div class="page-heading-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M10 6h4" />
            <path d="M10 3h4a2 2 0 0 1 2 2v2H8V5a2 2 0 0 1 2-2Z" />
            <path d="M4 7h16v13H4V7Z" />
            <path d="M8 12h8" />
            <path d="M8 16h5" />
          </svg>
        </div>
        <div class="page-heading-accent" aria-hidden="true"></div>
        <div class="page-heading-copy">
          <h1>Employees</h1>
          <p>Manage employee records, photos, CVs and contracts.</p>
        </div>
      </div>

      <button
        v-if="canCreateEmployee"
        type="button"
        class="create-button"
        @click="openCreateForm"
      >
        <svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 5v14" />
          <path d="M5 12h14" />
        </svg>
        Create Employee
      </button>
    </div>

    <div v-if="importErrorSummary" class="import-details-panel">
      <div class="import-error-heading">
        <div>
          <strong>{{ importErrorTitle }}</strong>
          <p>{{ importErrorSummary }}</p>
        </div>
        <div class="import-panel-actions">
          <button
            v-if="importErrorDetails.length"
            type="button"
            class="import-details-button"
            @click="toggleImportErrorDetails"
          >
            {{ showImportErrorDetails ? 'Hide details' : 'View details' }}
          </button>
          <button
            type="button"
            class="import-panel-close"
            aria-label="Dismiss import details"
            @click="clearImportMessages()"
          >
            &times;
          </button>
        </div>
      </div>

      <ul v-if="showImportErrorDetails" class="import-error-details">
        <li v-for="detail in importErrorDetails" :key="detail">{{ detail }}</li>
      </ul>
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
            placeholder="Search by name, email, job title or department"
            aria-label="Search employees"
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

        <div class="toolbar-actions">
          <button
            v-if="canImportEmployees"
            type="button"
            class="toolbar-button secondary-button outline-action-button"
            :disabled="importLoading"
            @click="triggerEmployeeImport"
          >
            <svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 3v12" />
              <path d="m7 8l5-5l5 5" />
              <path d="M5 15v4h14v-4" />
            </svg>
            {{ importLoading ? 'Importing...' : 'Import Excel' }}
          </button>

          <div v-if="canExportEmployees" class="export-menu-wrapper" @click.stop>
            <button
              type="button"
              class="toolbar-button secondary-button outline-action-button"
              :disabled="exportLoading"
              :aria-expanded="showExportMenu"
              @click="toggleExportMenu"
            >
              <svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M7 3h7l4 4v14H7V3Z" />
                <path d="M14 3v5h4" />
                <path d="M10 13h5" />
                <path d="M10 17h5" />
              </svg>
              {{ exportLoading ? 'Exporting...' : 'Export' }}
            </button>

            <div v-if="showExportMenu" class="export-menu">
              <button
                type="button"
                class="export-menu-item"
                :disabled="exportLoading"
                @click="downloadEmployeeExport('excel')"
              >
                Export as Excel
              </button>
              <button
                type="button"
                class="export-menu-item"
                :disabled="exportLoading"
                @click="downloadEmployeeExport('pdf')"
              >
                Export as PDF
              </button>
            </div>
          </div>

          <button
            v-if="canExportEmployees"
            type="button"
            class="toolbar-button secondary-button outline-action-button"
            :disabled="templateDownloading"
            @click="downloadImportTemplate"
          >
            <svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 3v12" />
              <path d="m7 10l5 5l5-5" />
              <path d="M5 19h14" />
            </svg>
            {{ templateDownloading ? 'Downloading...' : 'Download Template' }}
          </button>
        </div>

        <input
          ref="importFileInput"
          class="visually-hidden-file"
          type="file"
          accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          @change="importEmployees"
        />
      </form>

      <form class="filter-bar" aria-label="Employee filters" @submit.prevent="applyEmployeeFilters">
        <label class="filter-field">
          <span>Department</span>
          <input
            v-model="filters.department"
            type="text"
            placeholder="Any department"
            @change="page = 0"
          />
        </label>

        <label class="filter-field">
          <span>Job Title</span>
          <input
            v-model="filters.jobTitle"
            type="text"
            placeholder="Any job title"
            @change="page = 0"
          />
        </label>

        <label class="filter-field compact">
          <span>Hire From</span>
          <input v-model="filters.hireDateFrom" type="date" @change="page = 0" />
        </label>

        <label class="filter-field compact">
          <span>Hire To</span>
          <input v-model="filters.hireDateTo" type="date" @change="page = 0" />
        </label>

        <label class="filter-field compact">
          <span>Salary Min</span>
          <input
            v-model="filters.salaryMin"
            type="number"
            min="0"
            step="0.01"
            placeholder="Min"
            @change="page = 0"
          />
        </label>

        <label class="filter-field compact">
          <span>Salary Max</span>
          <input
            v-model="filters.salaryMax"
            type="number"
            min="0"
            step="0.01"
            placeholder="Max"
            @change="page = 0"
          />
        </label>

        <label class="filter-field compact">
          <span>Sort By</span>
          <select v-model="filters.sortBy" @change="page = 0">
            <option value="">Default</option>
            <option value="name">Name</option>
            <option value="hireDate">Hire Date</option>
            <option value="salary">Salary</option>
          </select>
        </label>

        <label class="filter-field compact">
          <span>Direction</span>
          <select v-model="filters.sortDir" @change="page = 0">
            <option value="asc">Ascending</option>
            <option value="desc">Descending</option>
          </select>
        </label>

        <div class="filter-actions">
          <button type="submit" class="toolbar-button search-button" :disabled="loading">
            Apply Filters
          </button>
          <button
            type="button"
            class="toolbar-button secondary-button subtle-button"
            :disabled="loading || !hasActiveEmployeeFilters"
            @click="clearEmployeeFilters"
          >
            Clear Filters
          </button>
        </div>
      </form>

      <div v-if="loading" class="table-wrapper" aria-hidden="true">
        <table class="skeleton-table">
          <thead>
            <tr>
              <th>Employee</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Job Title</th>
              <th>Department</th>
              <th>Hire Date</th>
              <th>Salary</th>
              <th>Files</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in 6" :key="`employee-skeleton-${row}`">
              <td><span class="skeleton skeleton-text width-lg"></span></td>
              <td><span class="skeleton skeleton-text width-xl"></span></td>
              <td><span class="skeleton skeleton-text width-md"></span></td>
              <td><span class="skeleton skeleton-text width-lg"></span></td>
              <td><span class="skeleton skeleton-text width-md"></span></td>
              <td><span class="skeleton skeleton-text width-sm"></span></td>
              <td><span class="skeleton skeleton-text width-sm"></span></td>
              <td>
                <div class="skeleton-inline">
                  <span class="skeleton skeleton-pill"></span>
                  <span class="skeleton skeleton-pill small"></span>
                </div>
              </td>
              <td>
                <div class="skeleton-inline actions">
                  <span class="skeleton skeleton-button"></span>
                  <span class="skeleton skeleton-button"></span>
                  <span class="skeleton skeleton-button"></span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="error" class="error-panel">{{ error }}</div>

      <div v-else-if="employees.length === 0" class="state-panel">No employees found.</div>

      <template v-else>
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Employee</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Job Title</th>
                <th>Department</th>
                <th>Hire Date</th>
                <th>Salary</th>
                <th>Files</th>
                <th class="actions-column">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="employee in employees" :key="employee.id">
                <td>
                  <button
                    type="button"
                    class="entity-cell entity-button"
                    :aria-label="`View details for ${fullName(employee)}`"
                    @click="openEmployeeDetailsDrawer(employee)"
                  >
                    <span class="initials-avatar" :class="getAvatarTone(employee.id)">
                      {{ getInitials(fullName(employee)) }}
                    </span>
                    <span class="entity-name">{{ fullName(employee) }}</span>
                  </button>
                </td>
                <td>{{ employee.email }}</td>
                <td>{{ formatOptional(employee.phoneNumber) }}</td>
                <td>{{ employee.jobTitle }}</td>
                <td>{{ formatOptional(employee.department) }}</td>
                <td>{{ employee.hireDate }}</td>
                <td>{{ formatSalary(employee.salary) }}</td>
                <td>
                  <div v-if="employee.hasPhoto || employee.hasCv" class="badges">
                    <span v-if="employee.hasPhoto" class="file-badge">
                      <span class="pill-dot"></span>
                      Photo
                    </span>
                    <span v-if="employee.hasCv" class="file-badge">
                      <span class="pill-dot"></span>
                      CV
                    </span>
                  </div>
                  <span v-else class="neutral-badge">
                    <span class="pill-dot neutral"></span>
                    None
                  </span>
                </td>
                <td class="actions-column">
                  <div class="table-actions">
                    <button
                      v-if="canUpdateEmployee"
                      type="button"
                      class="table-action-button"
                      @click="openEditForm(employee)"
                    >
                      <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M4 20h4L19 9l-4-4L4 16v4Z" />
                        <path d="M13 7l4 4" />
                      </svg>
                      Edit
                    </button>
                    <button
                      type="button"
                      class="table-action-button"
                      @click="openPhotoModal(employee)"
                    >
                      <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M4 7h4l2-2h4l2 2h4v12H4V7Z" />
                        <path d="M12 10a3 3 0 1 1 0 6a3 3 0 0 1 0-6Z" />
                      </svg>
                      Photo
                    </button>
                    <button
                      type="button"
                      class="table-action-button"
                      @click="openCvModal(employee)"
                    >
                      <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M7 3h7l4 4v14H7V3Z" />
                        <path d="M14 3v5h4" />
                      </svg>
                      CV
                    </button>
                    <button
                      type="button"
                      class="table-action-button"
                      :disabled="contractDownloadingId === employee.id"
                      @click="downloadContract(employee)"
                    >
                      <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M10 13a5 5 0 0 0 7 0l2-2a5 5 0 0 0-7-7l-1 1" />
                        <path d="M14 11a5 5 0 0 0-7 0l-2 2a5 5 0 0 0 7 7l1-1" />
                      </svg>
                      {{ contractDownloadingId === employee.id ? 'Downloading...' : 'Contract' }}
                    </button>
                    <button
                      v-if="canDeleteEmployee"
                      type="button"
                      class="table-action-button danger-button"
                      @click="openDeleteConfirm(employee)"
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
          item-label="employees"
          :disabled="loading"
          @page-change="goToPage"
          @page-size-change="changePageSize"
        />
      </template>
    </div>

    <div
      v-if="detailEmployee"
      class="details-drawer-overlay"
      aria-label="Close employee details"
      @click.self="closeEmployeeDetailsDrawer"
    >
      <aside class="details-drawer" aria-labelledby="employee-details-title" role="dialog" aria-modal="true">
        <header class="drawer-header">
          <h2 id="employee-details-title">Employee Details</h2>
          <button
            type="button"
            class="drawer-close-button"
            aria-label="Close employee details"
            @click="closeEmployeeDetailsDrawer"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M18 6L6 18" />
              <path d="M6 6l12 12" />
            </svg>
          </button>
        </header>

        <div class="drawer-body">
          <section class="drawer-profile-card">
            <span class="drawer-avatar" :class="getAvatarTone(detailEmployee.id)">
              {{ getInitials(fullName(detailEmployee)) }}
            </span>
            <div class="drawer-profile-copy">
              <div class="drawer-name-row">
                <h3>{{ fullName(detailEmployee) }}</h3>
                <span class="drawer-status-badge">Employee</span>
              </div>
              <p class="drawer-contact-line">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M4 4h16v16H4V4Z" />
                  <path d="m4 7l8 6l8-6" />
                </svg>
                {{ detailEmployee.email }}
              </p>
              <p class="drawer-contact-line">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2a19.8 19.8 0 0 1-8.63-3.07a19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.12 4.18A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.12.9.32 1.77.59 2.61a2 2 0 0 1-.45 2.11L8 9.69a16 16 0 0 0 6 6l1.25-1.25a2 2 0 0 1 2.11-.45c.84.27 1.71.47 2.61.59A2 2 0 0 1 22 16.92Z" />
                </svg>
                {{ formatOptional(detailEmployee.phoneNumber) }}
              </p>
              <div class="drawer-profile-meta">
                <span>{{ formatOptional(detailEmployee.department) }}</span>
                <span>{{ detailEmployee.jobTitle }}</span>
              </div>
            </div>
          </section>

          <div class="drawer-action-grid">
            <button
              v-if="canUpdateEmployee"
              type="button"
              class="drawer-action-button primary"
              @click="openEmployeeDetailsEdit(detailEmployee)"
            >
              <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 20h4L19 9l-4-4L4 16v4Z" />
                <path d="M13 7l4 4" />
              </svg>
              Edit
            </button>
            <button
              type="button"
              class="drawer-action-button"
              @click="openEmployeeDetailsPhoto(detailEmployee)"
            >
              <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 7h4l2-2h4l2 2h4v12H4V7Z" />
                <path d="M12 10a3 3 0 1 1 0 6a3 3 0 0 1 0-6Z" />
              </svg>
              Photo
            </button>
            <button
              type="button"
              class="drawer-action-button"
              @click="openEmployeeDetailsCv(detailEmployee)"
            >
              <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M7 3h7l4 4v14H7V3Z" />
                <path d="M14 3v5h4" />
              </svg>
              CV
            </button>
            <button
              type="button"
              class="drawer-action-button"
              :disabled="contractDownloadingId === detailEmployee.id"
              @click="downloadEmployeeDetailsContract(detailEmployee)"
            >
              <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M10 13a5 5 0 0 0 7 0l2-2a5 5 0 0 0-7-7l-1 1" />
                <path d="M14 11a5 5 0 0 0-7 0l-2 2a5 5 0 0 0 7 7l1-1" />
              </svg>
              {{ contractDownloadingId === detailEmployee.id ? 'Downloading...' : 'Contract' }}
            </button>
          </div>

          <section class="drawer-section-card">
            <h3>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M10 6h4" />
                <path d="M10 3h4a2 2 0 0 1 2 2v2H8V5a2 2 0 0 1 2-2Z" />
                <path d="M4 7h16v13H4V7Z" />
              </svg>
              Employment Information
            </h3>
            <dl class="drawer-details-list">
              <div>
                <dt>Department</dt>
                <dd>{{ formatOptional(detailEmployee.department) }}</dd>
              </div>
              <div>
                <dt>Job Title</dt>
                <dd>{{ detailEmployee.jobTitle }}</dd>
              </div>
              <div>
                <dt>Hire Date</dt>
                <dd>{{ detailEmployee.hireDate }}</dd>
              </div>
              <div>
                <dt>Salary</dt>
                <dd>{{ formatSalary(detailEmployee.salary) }}</dd>
              </div>
            </dl>
          </section>

          <section class="drawer-section-card">
            <h3>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M7 3h7l4 4v14H7V3Z" />
                <path d="M14 3v5h4" />
              </svg>
              Documents & Status
            </h3>
            <dl class="drawer-details-list">
              <div>
                <dt>Photo Status</dt>
                <dd>
                  <span class="drawer-file-status" :class="{ available: detailEmployee.hasPhoto }">
                    <span></span>
                    {{ detailEmployee.hasPhoto ? 'Available' : 'Not uploaded' }}
                  </span>
                </dd>
              </div>
              <div>
                <dt>CV Status</dt>
                <dd>
                  <span class="drawer-file-status" :class="{ available: detailEmployee.hasCv }">
                    <span></span>
                    {{ detailEmployee.hasCv ? 'Available' : 'Not uploaded' }}
                  </span>
                </dd>
              </div>
            </dl>
          </section>
        </div>
      </aside>
    </div>

    <div v-if="showCreateForm" class="modal-overlay" @click.self="closeCreateForm">
      <section class="modal" aria-labelledby="create-employee-title">
        <div class="modal-heading">
          <h2 id="create-employee-title">Create Employee</h2>
          <button
            type="button"
            class="icon-button"
            :disabled="createLoading"
            @click="closeCreateForm"
          >
            Close
          </button>
        </div>

        <form class="create-form" @submit.prevent="createEmployee">
          <div class="form-grid">
            <div class="form-field">
              <label for="create-first-name">First Name</label>
              <input
                id="create-first-name"
                v-model="createForm.firstName"
                type="text"
                autocomplete="given-name"
                :aria-invalid="Boolean(createValidationErrors.firstName)"
              />
              <p v-if="createValidationErrors.firstName" class="field-error">
                {{ createValidationErrors.firstName }}
              </p>
            </div>

            <div class="form-field">
              <label for="create-last-name">Last Name</label>
              <input
                id="create-last-name"
                v-model="createForm.lastName"
                type="text"
                autocomplete="family-name"
                :aria-invalid="Boolean(createValidationErrors.lastName)"
              />
              <p v-if="createValidationErrors.lastName" class="field-error">
                {{ createValidationErrors.lastName }}
              </p>
            </div>
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
            <label for="create-phone-number">Phone Number</label>
            <input
              id="create-phone-number"
              v-model="createForm.phoneNumber"
              type="text"
              autocomplete="tel"
            />
          </div>

          <div class="form-grid">
            <div class="form-field">
              <label for="create-job-title">Job Title</label>
              <input
                id="create-job-title"
                v-model="createForm.jobTitle"
                type="text"
                :aria-invalid="Boolean(createValidationErrors.jobTitle)"
              />
              <p v-if="createValidationErrors.jobTitle" class="field-error">
                {{ createValidationErrors.jobTitle }}
              </p>
            </div>

            <div class="form-field">
              <label for="create-department">Department</label>
              <input id="create-department" v-model="createForm.department" type="text" />
            </div>
          </div>

          <div class="form-grid">
            <div class="form-field">
              <label for="create-hire-date">Hire Date</label>
              <input
                id="create-hire-date"
                v-model="createForm.hireDate"
                type="date"
                :aria-invalid="Boolean(createValidationErrors.hireDate)"
              />
              <p v-if="createValidationErrors.hireDate" class="field-error">
                {{ createValidationErrors.hireDate }}
              </p>
            </div>

            <div class="form-field">
              <label for="create-salary">Salary</label>
              <input
                id="create-salary"
                v-model="createForm.salary"
                type="number"
                step="0.01"
                :aria-invalid="Boolean(createValidationErrors.salary)"
              />
              <p v-if="createValidationErrors.salary" class="field-error">
                {{ createValidationErrors.salary }}
              </p>
            </div>
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
              {{ createLoading ? 'Creating...' : 'Create Employee' }}
            </button>
          </div>
        </form>
      </section>
    </div>

    <div v-if="showEditForm" class="modal-overlay" @click.self="closeEditForm">
      <section class="modal" aria-labelledby="edit-employee-title">
        <div class="modal-heading">
          <h2 id="edit-employee-title">Edit Employee</h2>
          <button
            type="button"
            class="icon-button"
            :disabled="editLoading"
            @click="closeEditForm"
          >
            Close
          </button>
        </div>

        <form class="edit-form" @submit.prevent="updateEmployee">
          <div class="form-grid">
            <div class="form-field">
              <label for="edit-first-name">First Name</label>
              <input
                id="edit-first-name"
                v-model="editForm.firstName"
                type="text"
                autocomplete="given-name"
                :aria-invalid="Boolean(editValidationErrors.firstName)"
              />
              <p v-if="editValidationErrors.firstName" class="field-error">
                {{ editValidationErrors.firstName }}
              </p>
            </div>

            <div class="form-field">
              <label for="edit-last-name">Last Name</label>
              <input
                id="edit-last-name"
                v-model="editForm.lastName"
                type="text"
                autocomplete="family-name"
                :aria-invalid="Boolean(editValidationErrors.lastName)"
              />
              <p v-if="editValidationErrors.lastName" class="field-error">
                {{ editValidationErrors.lastName }}
              </p>
            </div>
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
            <label for="edit-phone-number">Phone Number</label>
            <input
              id="edit-phone-number"
              v-model="editForm.phoneNumber"
              type="text"
              autocomplete="tel"
            />
          </div>

          <div class="form-grid">
            <div class="form-field">
              <label for="edit-job-title">Job Title</label>
              <input
                id="edit-job-title"
                v-model="editForm.jobTitle"
                type="text"
                :aria-invalid="Boolean(editValidationErrors.jobTitle)"
              />
              <p v-if="editValidationErrors.jobTitle" class="field-error">
                {{ editValidationErrors.jobTitle }}
              </p>
            </div>

            <div class="form-field">
              <label for="edit-department">Department</label>
              <input id="edit-department" v-model="editForm.department" type="text" />
            </div>
          </div>

          <div class="form-grid">
            <div class="form-field">
              <label for="edit-hire-date">Hire Date</label>
              <input
                id="edit-hire-date"
                v-model="editForm.hireDate"
                type="date"
                :aria-invalid="Boolean(editValidationErrors.hireDate)"
              />
              <p v-if="editValidationErrors.hireDate" class="field-error">
                {{ editValidationErrors.hireDate }}
              </p>
            </div>

            <div class="form-field">
              <label for="edit-salary">Salary</label>
              <input
                id="edit-salary"
                v-model="editForm.salary"
                type="number"
                step="0.01"
                :aria-invalid="Boolean(editValidationErrors.salary)"
              />
              <p v-if="editValidationErrors.salary" class="field-error">
                {{ editValidationErrors.salary }}
              </p>
            </div>
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
      <section class="modal" aria-labelledby="delete-employee-title">
        <div class="modal-heading">
          <h2 id="delete-employee-title">Delete Employee</h2>
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
          <p>Are you sure you want to delete "{{ employeeToDelete ? fullName(employeeToDelete) : '' }}"?</p>
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
          <button
            type="button"
            class="danger-button"
            :disabled="deleteLoading"
            @click="deleteEmployee"
          >
            {{ deleteLoading ? 'Deleting...' : 'Delete Employee' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="showPhotoModal" class="modal-overlay" @click.self="closePhotoModal">
      <section class="modal" aria-labelledby="employee-photo-title">
        <div class="modal-heading">
          <div>
            <h2 id="employee-photo-title">Employee Photo</h2>
            <p class="modal-subtitle">{{ photoEmployee ? fullName(photoEmployee) : '' }}</p>
          </div>
          <button
            type="button"
            class="icon-button"
            :disabled="photoUploading || photoDeleting"
            @click="closePhotoModal"
          >
            Close
          </button>
        </div>

        <div class="photo-preview-panel">
          <div v-if="photoLoading" class="state-panel">Loading photo...</div>
          <img
            v-else-if="photoPreviewUrl && photoEmployee"
            :src="photoPreviewUrl"
            :alt="`Photo of ${fullName(photoEmployee)}`"
            class="employee-photo-preview"
          />
          <div v-else class="state-panel">No photo uploaded.</div>
        </div>

        <div v-if="canManagePhoto" class="photo-management">
          <div class="form-field">
            <label for="employee-photo-file">Photo File</label>
            <input
              id="employee-photo-file"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              :disabled="photoUploading || photoDeleting"
              @change="handlePhotoFileChange"
            />
          </div>

          <div class="modal-actions">
            <button
              v-if="photoEmployee?.hasPhoto"
              type="button"
              class="danger-button"
              :disabled="photoUploading || photoDeleting"
              @click="deletePhoto"
            >
              {{ photoDeleting ? 'Deleting...' : 'Delete Photo' }}
            </button>
            <button
              type="button"
              :disabled="!selectedPhotoFile || photoUploading || photoDeleting"
              @click="uploadPhoto"
            >
              {{ photoUploading ? 'Uploading...' : photoEmployee?.hasPhoto ? 'Replace Photo' : 'Upload Photo' }}
            </button>
          </div>
        </div>

      </section>
    </div>

    <div v-if="showCvModal" class="modal-overlay" @click.self="closeCvModal">
      <section class="modal" aria-labelledby="employee-cv-title">
        <div class="modal-heading">
          <div>
            <h2 id="employee-cv-title">Employee CV</h2>
            <p class="modal-subtitle">CV for {{ cvEmployee ? fullName(cvEmployee) : '' }}</p>
          </div>
          <button
            type="button"
            class="icon-button"
            :disabled="cvUploading || cvDeleting || cvViewing || cvDownloading"
            @click="closeCvModal"
          >
            Close
          </button>
        </div>

        <div class="cv-status-panel">
          <div v-if="cvEmployee?.hasCv" class="cv-actions">
            <button type="button" :disabled="cvViewing || cvDownloading" @click="viewCv">
              {{ cvViewing ? 'Opening...' : 'View CV' }}
            </button>
            <button type="button" :disabled="cvViewing || cvDownloading" @click="downloadCv">
              {{ cvDownloading ? 'Downloading...' : 'Download CV' }}
            </button>
          </div>
          <div v-else class="state-panel">No CV uploaded.</div>
        </div>

        <div v-if="canManageCv" class="cv-management">
          <div class="form-field">
            <label for="employee-cv-file">CV File</label>
            <input
              id="employee-cv-file"
              type="file"
              accept="application/pdf"
              :disabled="cvUploading || cvDeleting || cvViewing || cvDownloading"
              @change="handleCvFileChange"
            />
          </div>

          <div class="modal-actions">
            <button
              v-if="cvEmployee?.hasCv"
              type="button"
              class="danger-button"
              :disabled="cvUploading || cvDeleting || cvViewing || cvDownloading"
              @click="deleteCv"
            >
              {{ cvDeleting ? 'Deleting...' : 'Delete CV' }}
            </button>
            <button
              type="button"
              :disabled="!selectedCvFile || cvUploading || cvDeleting || cvViewing || cvDownloading"
              @click="uploadCv"
            >
              {{ cvUploading ? 'Uploading...' : cvEmployee?.hasCv ? 'Replace CV' : 'Upload CV' }}
            </button>
          </div>
        </div>

      </section>
    </div>

    <div v-if="showCvPreviewModal" class="modal-overlay" @click.self="closeCvPreviewModal">
      <section class="modal cv-preview-modal" aria-labelledby="cv-preview-title">
        <div class="modal-heading">
          <div>
            <h2 id="cv-preview-title">Employee CV</h2>
            <p class="modal-subtitle">
              {{ cvEmployee ? fullName(cvEmployee) : 'CV preview' }}
            </p>
          </div>
          <div class="preview-actions">
            <button type="button" :disabled="cvDownloading" @click="downloadCv">
              {{ cvDownloading ? 'Downloading...' : 'Download' }}
            </button>
            <button type="button" class="icon-button" @click="closeCvPreviewModal">Close</button>
          </div>
        </div>

        <iframe
          v-if="cvPreviewUrl"
          :src="cvPreviewUrl"
          title="Employee CV"
          class="cv-preview-frame"
        ></iframe>
        <div v-else class="state-panel">CV preview is not available.</div>
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
  margin-bottom: 12px;
  padding: 10px;
  border: 1px solid #e5edf7;
  border-radius: 12px;
  background: #f8fbff;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
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

.export-menu-wrapper {
  position: relative;
}

.export-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 10;
  display: grid;
  min-width: 170px;
  padding: 6px;
  border: 1px solid #dbe3ea;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.16);
}

.export-menu-item {
  justify-content: flex-start;
  min-height: 36px;
  padding: 0 10px;
  color: #334155;
  background: #ffffff;
  text-align: left;
  font-weight: 700;
}

.export-menu-item:hover:not(:disabled) {
  background: #f1f5f9;
}

.visually-hidden-file {
  display: none;
}

.filter-bar {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
  padding: 14px;
  border: 1px solid #e5edf7;
  border-radius: 12px;
  background:
    radial-gradient(circle at 96% 0%, rgba(204, 251, 241, 0.38), transparent 24%),
    #f8fbff;
}

.filter-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.filter-field span {
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.filter-field input,
.filter-field select {
  width: 100%;
  min-width: 0;
  min-height: 40px;
  padding: 9px 11px;
  border: 1px solid #dbe7f3;
  border-radius: 9px;
  color: #0f172a;
  background: #ffffff;
  font-size: 14px;
}

.filter-field input::placeholder {
  color: #94a3b8;
}

.filter-field input:focus,
.filter-field select:focus {
  outline: 3px solid rgba(20, 184, 166, 0.18);
  border-color: #0f766e;
}

.filter-actions {
  display: flex;
  align-items: end;
  gap: 10px;
  grid-column: span 2;
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

.outline-action-button {
  border: 1px solid #dbeafe;
  color: #006ee6;
  background: #eef6ff;
}

.outline-action-button:hover:not(:disabled) {
  background: #dbeafe;
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
.error-panel,
.import-details-panel {
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

.import-details-panel {
  display: grid;
  gap: 12px;
  color: #7f1d1d;
  background: #fff7f7;
  border: 1px solid #fecaca;
}

.import-error-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.import-error-heading p {
  margin: 4px 0 0;
}

.import-panel-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.import-details-button {
  min-height: 32px;
  flex: 0 0 auto;
  padding: 0 10px;
  color: #991b1b;
  background: #fee2e2;
  font-size: 13px;
}

.import-panel-close {
  width: 32px;
  min-height: 32px;
  padding: 0;
  color: #991b1b;
  background: #fee2e2;
  font-size: 18px;
  line-height: 1;
}

.import-panel-close:hover {
  background: #fecaca;
}

.import-error-details {
  max-height: 220px;
  overflow-y: auto;
  display: grid;
  gap: 6px;
  margin: 0;
  padding: 12px 12px 12px 28px;
  border: 1px solid #fecaca;
  border-radius: 6px;
  background: #fff7f7;
  font-size: 14px;
  font-weight: 500;
}

.table-wrapper {
  overflow-x: auto;
  border: 1px solid #e5edf7;
  border-radius: 10px;
  background: #ffffff;
}

table {
  width: 100%;
  min-width: 1280px;
  border-collapse: collapse;
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

.entity-button {
  min-height: auto;
  padding: 0;
  border: 0;
  color: inherit;
  background: transparent;
  text-align: left;
}

.entity-button:hover .entity-name {
  color: #0f766e;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.entity-button:focus-visible {
  outline: 3px solid rgba(20, 184, 166, 0.2);
  outline-offset: 4px;
  border-radius: 999px;
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

.file-badge,
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

.file-badge {
  color: #075985;
  background: #e0f2fe;
}

.neutral-badge {
  color: #475569;
  background: #f1f5f9;
}

.pill-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #64748b;
}

.pill-dot.neutral {
  background: #94a3b8;
}

.muted {
  color: #64748b;
}

.actions-column {
  width: 430px;
  min-width: 430px;
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
  flex-wrap: nowrap;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.details-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 35;
  display: flex;
  justify-content: flex-end;
  background: rgba(15, 23, 42, 0.34);
  backdrop-filter: blur(3px);
}

.details-drawer {
  width: min(100%, 520px);
  height: 100vh;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  border-left: 1px solid #dbe3ea;
  background: #ffffff;
  box-shadow: -22px 0 60px rgba(15, 23, 42, 0.2);
}

.drawer-header {
  min-height: 74px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid #e5edf7;
}

.drawer-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 900;
}

.drawer-close-button {
  width: 40px;
  min-height: 40px;
  display: grid;
  place-items: center;
  padding: 0;
  color: #1e3a5f;
  background: #ffffff;
  border-radius: 999px;
}

.drawer-close-button:hover {
  background: #f1f5f9;
}

.drawer-close-button svg,
.drawer-contact-line svg,
.drawer-section-card h3 svg {
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.drawer-close-button svg {
  width: 20px;
  height: 20px;
}

.drawer-body {
  min-height: 0;
  overflow-y: auto;
  display: grid;
  align-content: start;
  gap: 16px;
  padding: 18px 20px 24px;
  background:
    radial-gradient(circle at 98% 0%, rgba(204, 251, 241, 0.28), transparent 28%),
    #fbfdff;
}

.drawer-profile-card,
.drawer-section-card {
  border: 1px solid #dbe7f3;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.drawer-profile-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px;
}

.drawer-avatar {
  display: grid;
  width: 92px;
  height: 92px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 999px;
  font-size: 24px;
  font-weight: 900;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.12);
}

.drawer-profile-copy {
  min-width: 0;
  display: grid;
  gap: 8px;
}

.drawer-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.drawer-name-row h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 900;
  line-height: 1.2;
}

.drawer-status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 3px 10px;
  border-radius: 999px;
  color: #047857;
  background: #d1fae5;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.drawer-contact-line {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0;
  color: #52637a;
  font-size: 14px;
  line-height: 1.35;
}

.drawer-contact-line svg {
  width: 17px;
  height: 17px;
  flex: 0 0 auto;
  color: #5b6f90;
}

.drawer-profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 2px;
}

.drawer-profile-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 10px;
  border-radius: 999px;
  color: #1e3a5f;
  background: #eef6ff;
  font-size: 12px;
  font-weight: 800;
}

.drawer-action-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.drawer-action-button {
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid #dbe7f3;
  border-radius: 9px;
  color: #0f172a;
  background: #ffffff;
  font-weight: 900;
  white-space: nowrap;
}

.drawer-action-button:hover:not(:disabled) {
  border-color: #bfdbfe;
  background: #eef6ff;
}

.drawer-action-button.primary {
  border-color: #0f766e;
  color: #ffffff;
  background: linear-gradient(135deg, #0ea5a4, #0f766e);
  box-shadow: 0 10px 24px rgba(15, 118, 110, 0.22);
}

.drawer-action-button.primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #14b8a6, #0f766e);
}

.drawer-section-card {
  overflow: hidden;
}

.drawer-section-card h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  padding: 15px 18px;
  border-bottom: 1px solid #e5edf7;
  color: #0f172a;
  font-size: 16px;
  font-weight: 900;
}

.drawer-section-card h3 svg {
  width: 20px;
  height: 20px;
  color: #1e3a5f;
}

.drawer-details-list {
  display: grid;
  margin: 0;
  padding: 0 18px 14px;
}

.drawer-details-list div {
  display: grid;
  grid-template-columns: minmax(120px, 0.8fr) minmax(0, 1fr);
  gap: 12px;
  padding: 13px 0;
  border-bottom: 1px solid #edf2f7;
}

.drawer-details-list div:last-child {
  border-bottom: 0;
}

.drawer-details-list dt {
  color: #64748b;
  font-weight: 700;
}

.drawer-details-list dd {
  margin: 0;
  color: #0f172a;
  font-weight: 800;
}

.drawer-file-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
}

.drawer-file-status span {
  width: 9px;
  height: 9px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: #94a3b8;
}

.drawer-file-status.available {
  color: #047857;
}

.drawer-file-status.available span {
  background: #10b981;
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
  width: 54px;
  height: 24px;
  border-radius: 999px;
}

.skeleton-pill.small {
  width: 38px;
}

.skeleton-button {
  width: 52px;
  height: 32px;
}

.skeleton-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skeleton-inline.actions {
  min-width: 172px;
}

.width-sm {
  width: 68px;
}

.width-md {
  width: 92px;
}

.width-lg {
  width: 128px;
}

.width-xl {
  width: 168px;
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
  width: min(100%, 620px);
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
.edit-form {
  display: grid;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-field {
  display: grid;
  gap: 7px;
}

.form-field input {
  width: 100%;
}

label {
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

input[aria-invalid='true'] {
  border-color: #dc2626;
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

.photo-preview-panel {
  display: grid;
  place-items: center;
  min-height: 220px;
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.employee-photo-preview {
  display: block;
  max-width: 100%;
  max-height: 420px;
  border-radius: 8px;
  object-fit: contain;
}

.photo-management {
  display: grid;
  gap: 16px;
}

.cv-status-panel {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
}

.cv-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.cv-management {
  display: grid;
  gap: 16px;
}

.cv-preview-modal {
  width: min(100%, 1040px);
  height: min(90vh, 860px);
  display: grid;
  grid-template-rows: auto 1fr;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cv-preview-frame {
  width: 100%;
  height: 100%;
  min-height: 520px;
  border: 1px solid #dbe3ea;
  border-radius: 8px;
  background: #f8fafc;
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

  .toolbar-actions {
    width: 100%;
    align-items: stretch;
    flex-direction: column;
    margin-left: 0;
  }

  .filter-bar {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    align-items: stretch;
    flex-direction: column;
    grid-column: auto;
  }

  .details-drawer-overlay {
    align-items: stretch;
  }

  .details-drawer {
    width: 100%;
    height: 100vh;
    height: 100dvh;
    border-left: 0;
  }

  .drawer-header {
    min-height: 66px;
    padding: 16px 18px;
  }

  .drawer-body {
    padding: 14px;
  }

  .drawer-profile-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .drawer-avatar {
    width: 76px;
    height: 76px;
    font-size: 20px;
  }

  .drawer-name-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .drawer-action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .drawer-details-list div {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .search-field,
  .toolbar-button,
  .export-menu-wrapper {
    width: 100%;
    min-width: 0;
  }

  .export-menu {
    position: static;
    margin-top: 8px;
  }

  .import-error-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .import-details-button {
    width: fit-content;
  }

  .import-panel-actions {
    width: fit-content;
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

  .form-grid {
    grid-template-columns: 1fr;
  }

  .cv-preview-modal {
    height: min(88vh, 760px);
  }

  .preview-actions {
    width: 100%;
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .cv-preview-frame {
    min-height: 420px;
  }
}
</style>
