<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { apiFetch } from '@/services/api'
import companyLogo from '@/assets/company-logo.png'
import compactLogo from '@/assets/image G.png'

interface ActivityNotification {
  id: number
  actorName: string
  actorUsername: string
  action: string
  targetEmployeeId: number | null
  targetEmployeeName: string
  targetEmployeeEmail: string | null
  createdAt: string
  read: boolean
}

const inactivityDurationMs = 180000
const warningDurationSeconds = 30
const activityEvents = ['mousedown', 'keydown', 'touchstart', 'scroll'] as const

const router = useRouter()
const authStore = useAuthStore()
const sidebarPinned = ref(true)
const sidebarHovered = ref(false)
const sidebarCollapsed = computed(() => !(sidebarPinned.value || sidebarHovered.value))
const showSessionWarning = ref(false)
const sessionCountdown = ref(warningDurationSeconds)
const showProfileMenu = ref(false)
const showNotifications = ref(false)
const notifications = ref<ActivityNotification[]>([])
const unreadNotificationCount = ref(0)
const notificationsLoading = ref(false)
const notificationsError = ref('')
const profileMenu = ref<HTMLElement | null>(null)
const notificationMenu = ref<HTMLElement | null>(null)
const userInitial = computed(() => {
  const name = authStore.user?.name?.trim()

  if (!name) {
    return '?'
  }

  return name[0]?.toUpperCase() ?? '?'
})
const canViewNotifications = computed(() => authStore.isSuperAdmin)

let inactivityTimeout: ReturnType<typeof window.setTimeout> | null = null
let countdownInterval: ReturnType<typeof window.setInterval> | null = null
let notificationInterval: ReturnType<typeof window.setInterval> | null = null
let lastActivityHandledAt = 0

async function logout() {
  closeProfileMenu()
  closeNotifications()
  stopNotificationPolling()
  stopSessionTracking()
  await authStore.logout()
  router.push('/login')
}

async function logoutFromSessionWarning() {
  await logout()
}

async function stayConnected() {
  const refreshed = await authStore.refreshSession()

  if (!refreshed) {
    stopSessionTracking()
    router.push({
      path: '/login',
      state: { sessionExpired: true },
    })
    return
  }

  closeSessionWarning()
  resetInactivityTimer()
}

function openSidebarOnHover() {
  if (!sidebarPinned.value) {
    sidebarHovered.value = true
  }
}

function closeSidebarOnLeave() {
  sidebarHovered.value = false
}

function toggleSidebarPinned() {
  if (sidebarPinned.value) {
    sidebarPinned.value = false
    sidebarHovered.value = false
    return
  }

  sidebarPinned.value = true
  sidebarHovered.value = false
}

function pinSidebarFromInteraction() {
  if (!sidebarPinned.value && sidebarHovered.value) {
    sidebarPinned.value = true
    sidebarHovered.value = false
  }
}

function handleUserActivity() {
  if (showSessionWarning.value) {
    return
  }

  const now = Date.now()
  if (now - lastActivityHandledAt < 1000) {
    return
  }

  lastActivityHandledAt = now
  resetInactivityTimer()
}

function resetInactivityTimer() {
  clearInactivityTimer()
  inactivityTimeout = window.setTimeout(openSessionWarning, inactivityDurationMs)
}

function openSessionWarning() {
  clearInactivityTimer()
  showSessionWarning.value = true
  sessionCountdown.value = warningDurationSeconds
  startSessionCountdown()
}

function closeSessionWarning() {
  showSessionWarning.value = false
  sessionCountdown.value = warningDurationSeconds
  clearCountdownInterval()
}

function startSessionCountdown() {
  clearCountdownInterval()
  countdownInterval = window.setInterval(() => {
    sessionCountdown.value -= 1

    if (sessionCountdown.value <= 0) {
      logoutFromSessionWarning()
    }
  }, 1000)
}

function clearInactivityTimer() {
  if (inactivityTimeout) {
    window.clearTimeout(inactivityTimeout)
    inactivityTimeout = null
  }
}

function clearCountdownInterval() {
  if (countdownInterval) {
    window.clearInterval(countdownInterval)
    countdownInterval = null
  }
}

function handleSessionExpired() {
  stopNotificationPolling()
  stopSessionTracking()
  authStore.clearLocalSession()
  router.push({
    path: '/login',
    state: { sessionExpired: true },
  })
}

function toggleProfileMenu() {
  closeNotifications()
  showProfileMenu.value = !showProfileMenu.value
}

function closeProfileMenu() {
  showProfileMenu.value = false
}

async function toggleNotifications() {
  closeProfileMenu()
  showNotifications.value = !showNotifications.value

  if (showNotifications.value) {
    await loadNotifications()
  }
}

function closeNotifications() {
  showNotifications.value = false
}

function handleDocumentClick(event: MouseEvent) {
  const target = event.target as Node

  if (showProfileMenu.value && profileMenu.value && !profileMenu.value.contains(target)) {
    closeProfileMenu()
  }

  if (showNotifications.value && notificationMenu.value && !notificationMenu.value.contains(target)) {
    closeNotifications()
  }
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeProfileMenu()
    closeNotifications()
  }
}

async function loadNotificationCount() {
  if (!canViewNotifications.value) {
    unreadNotificationCount.value = 0
    return
  }

  try {
    const response = await apiFetch('/api/notifications/unread-count')

    if (!response.ok) {
      return
    }

    const data = (await response.json()) as { count: number }
    unreadNotificationCount.value = data.count
  } catch {
    // Notification polling is intentionally quiet so it never interrupts dashboard use.
  }
}

async function loadNotifications() {
  if (!canViewNotifications.value) {
    notifications.value = []
    return
  }

  notificationsLoading.value = true
  notificationsError.value = ''

  try {
    const response = await apiFetch('/api/notifications/latest')

    if (!response.ok) {
      notificationsError.value = 'Unable to load notifications.'
      return
    }

    notifications.value = (await response.json()) as ActivityNotification[]
    await loadNotificationCount()
  } catch {
    notificationsError.value = 'Unable to load notifications.'
  } finally {
    notificationsLoading.value = false
  }
}

async function markNotificationAsRead(notification: ActivityNotification) {
  if (notification.read) {
    return
  }

  try {
    const response = await apiFetch(`/api/notifications/${notification.id}/read`, {
      method: 'PUT',
    })

    if (!response.ok) {
      notificationsError.value = 'Unable to update notification.'
      return
    }

    const updatedNotification = (await response.json()) as ActivityNotification
    notifications.value = notifications.value.map((item) =>
      item.id === updatedNotification.id ? updatedNotification : item,
    )
    await loadNotificationCount()
  } catch {
    notificationsError.value = 'Unable to update notification.'
  }
}

async function markAllNotificationsAsRead() {
  try {
    const response = await apiFetch('/api/notifications/read-all', {
      method: 'PUT',
    })

    if (!response.ok) {
      notificationsError.value = 'Unable to update notifications.'
      return
    }

    notifications.value = notifications.value.map((notification) => ({
      ...notification,
      read: true,
    }))
    unreadNotificationCount.value = 0
  } catch {
    notificationsError.value = 'Unable to update notifications.'
  }
}

function startNotificationPolling() {
  if (!canViewNotifications.value || notificationInterval) {
    return
  }

  loadNotificationCount()
  notificationInterval = window.setInterval(loadNotificationCount, 30000)
}

function stopNotificationPolling() {
  if (notificationInterval) {
    window.clearInterval(notificationInterval)
    notificationInterval = null
  }
}

function formatNotificationAction(action: string) {
  return (
    {
      EMPLOYEE_CREATED: 'created employee',
      EMPLOYEE_UPDATED: 'updated employee',
      EMPLOYEE_DELETED: 'deleted employee',
      PHOTO_UPLOADED: 'uploaded a photo for',
      PHOTO_REPLACED: 'replaced the photo for',
      PHOTO_DELETED: 'deleted the photo for',
      CV_UPLOADED: 'uploaded a CV for',
      CV_REPLACED: 'replaced the CV for',
      CV_DELETED: 'deleted the CV for',
      EMPLOYEE_IMPORT: 'imported employees',
    }[action] ?? action.toLowerCase().replaceAll('_', ' ')
  )
}

function formatNotificationTime(value: string) {
  const timestamp = new Date(value).getTime()

  if (Number.isNaN(timestamp)) {
    return ''
  }

  const seconds = Math.max(0, Math.floor((Date.now() - timestamp) / 1000))

  if (seconds < 60) {
    return 'Just now'
  }

  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) {
    return `${minutes}m ago`
  }

  const hours = Math.floor(minutes / 60)
  if (hours < 24) {
    return `${hours}h ago`
  }

  const days = Math.floor(hours / 24)
  if (days < 7) {
    return `${days}d ago`
  }

  return new Date(value).toLocaleDateString()
}

function startSessionTracking() {
  activityEvents.forEach((eventName) => {
    window.addEventListener(eventName, handleUserActivity, { passive: true })
  })
  resetInactivityTimer()
}

function stopSessionTracking() {
  activityEvents.forEach((eventName) => {
    window.removeEventListener(eventName, handleUserActivity)
  })
  closeSessionWarning()
  clearInactivityTimer()
}

onMounted(() => {
  startSessionTracking()
  startNotificationPolling()
  window.addEventListener('auth:session-expired', handleSessionExpired)
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleDocumentKeydown)
})

onUnmounted(() => {
  stopSessionTracking()
  stopNotificationPolling()
  window.removeEventListener('auth:session-expired', handleSessionExpired)
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleDocumentKeydown)
})

watch(canViewNotifications, (canView) => {
  if (canView) {
    startNotificationPolling()
    return
  }

  stopNotificationPolling()
  closeNotifications()
  notifications.value = []
  unreadNotificationCount.value = 0
})
</script>

<template>
  <div class="app-layout" :class="{ 'sidebar-is-collapsed': sidebarCollapsed }">
    <aside
      class="sidebar"
      :class="{ collapsed: sidebarCollapsed }"
      aria-label="Main navigation"
      @mouseenter="openSidebarOnHover"
      @mouseleave="closeSidebarOnLeave"
      @click="pinSidebarFromInteraction"
    >
      <div class="sidebar-header">
        <button
          type="button"
          class="logo-button"
          :aria-label="sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'"
          @click.stop="toggleSidebarPinned"
        >
          <img
            v-if="sidebarCollapsed"
            :src="compactLogo"
            alt="G-innovation"
            class="sidebar-logo compact"
          />
          <img v-else :src="companyLogo" alt="G-innovation" class="sidebar-logo" />
        </button>
      </div>

      <button
        type="button"
        class="sidebar-toggle"
        :aria-expanded="!sidebarCollapsed"
        :aria-label="sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'"
        @click.stop="toggleSidebarPinned"
      >
        {{ sidebarCollapsed ? '>' : '<' }}
      </button>

      <nav class="sidebar-nav" :class="{ collapsed: sidebarCollapsed }">
        <RouterLink
          to="/users"
          class="sidebar-nav-link"
          :title="sidebarCollapsed ? 'Users' : undefined"
          aria-label="Users"
        >
          <svg class="nav-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
            <path d="M9 11a4 4 0 1 0 0-8a4 4 0 0 0 0 8Z" />
            <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
          <span v-if="!sidebarCollapsed">Users</span>
        </RouterLink>
        <RouterLink
          to="/employees"
          class="sidebar-nav-link"
          :title="sidebarCollapsed ? 'Employees' : undefined"
          aria-label="Employees"
        >
          <svg class="nav-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M10 6h4" />
            <path d="M10 3h4a2 2 0 0 1 2 2v2H8V5a2 2 0 0 1 2-2Z" />
            <path d="M4 7h16v13H4V7Z" />
            <path d="M8 12h8" />
            <path d="M8 16h5" />
          </svg>
          <span v-if="!sidebarCollapsed">Employees</span>
        </RouterLink>
      </nav>

      <div class="sidebar-spacer"></div>

      <div class="sidebar-footer">
        <button
          type="button"
          class="sidebar-logout-button"
          :title="sidebarCollapsed ? 'Logout' : undefined"
          aria-label="Logout"
          @click.stop="logout"
        >
          <svg class="sidebar-logout-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M10 17l5-5l-5-5" />
            <path d="M15 12H3" />
            <path d="M21 3v18" />
          </svg>
          <span v-if="!sidebarCollapsed">Logout</span>
        </button>
      </div>
    </aside>

    <div class="dashboard-main">
      <header class="topbar">
        <div class="brand" aria-label="Employee Management">
          <span class="brand-title">Employee Management</span>
        </div>

        <div class="topbar-actions">
          <div v-if="canViewNotifications" ref="notificationMenu" class="notification-menu-wrapper">
            <button
              type="button"
              class="notification-button"
              aria-label="Notifications"
              aria-haspopup="menu"
              :aria-expanded="showNotifications"
              @click.stop="toggleNotifications"
            >
              <svg class="notification-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 7h18s-3 0-3-7" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
              <span v-if="unreadNotificationCount > 0" class="notification-badge">
                {{ unreadNotificationCount > 99 ? '99+' : unreadNotificationCount }}
              </span>
            </button>

            <div v-if="showNotifications" class="notification-dropdown" role="menu">
              <div class="notification-dropdown-header">
                <div>
                  <strong>Notifications</strong>
                  <span>{{ unreadNotificationCount }} unread</span>
                </div>
                <button
                  type="button"
                  class="mark-all-button"
                  :disabled="unreadNotificationCount === 0"
                  @click="markAllNotificationsAsRead"
                >
                  Mark all read
                </button>
              </div>

              <p v-if="notificationsError" class="notification-error">{{ notificationsError }}</p>
              <p v-else-if="notificationsLoading" class="notification-empty">Loading notifications...</p>
              <p v-else-if="notifications.length === 0" class="notification-empty">No notifications yet.</p>

              <div v-else class="notification-list">
                <button
                  v-for="notification in notifications"
                  :key="notification.id"
                  type="button"
                  class="notification-item"
                  :class="{ unread: !notification.read }"
                  role="menuitem"
                  @click="markNotificationAsRead(notification)"
                >
                  <span class="notification-dot" aria-hidden="true"></span>
                  <span class="notification-copy">
                    <span>
                      <strong>{{ notification.actorName }}</strong>
                      {{ formatNotificationAction(notification.action) }}
                    </span>
                    <span class="notification-target">{{ notification.targetEmployeeName }}</span>
                    <span class="notification-time">{{ formatNotificationTime(notification.createdAt) }}</span>
                  </span>
                </button>
              </div>
            </div>
          </div>

          <div ref="profileMenu" class="profile-menu-wrapper">
          <button
            type="button"
            class="profile-chip"
            aria-haspopup="menu"
            :aria-expanded="showProfileMenu"
            @click.stop="toggleProfileMenu"
          >
            <span class="profile-avatar">{{ userInitial }}</span>
            <span class="profile-summary">
              <span class="profile-name">{{ authStore.user?.name }}</span>
              <span class="profile-role">{{ authStore.user?.userType }}</span>
            </span>
            <svg class="profile-chevron" viewBox="0 0 24 24" aria-hidden="true">
              <path d="m6 9l6 6l6-6" />
            </svg>
          </button>

          <div v-if="showProfileMenu" class="profile-dropdown" role="menu">
            <div class="profile-dropdown-header">
              <span class="profile-avatar large">{{ userInitial }}</span>
              <div class="profile-dropdown-copy">
                <strong>{{ authStore.user?.name }}</strong>
                <span>{{ authStore.user?.email }}</span>
                <span class="role-badge">{{ authStore.user?.userType }}</span>
              </div>
            </div>

            <div class="profile-dropdown-divider"></div>

            <button type="button" class="profile-menu-action" role="menuitem" @click="logout">
              <svg class="profile-action-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M10 17l5-5l-5-5" />
                <path d="M15 12H3" />
                <path d="M21 3v18" />
              </svg>
              Logout
            </button>
          </div>
        </div>
        </div>
      </header>

      <main class="content">
        <RouterView />
      </main>
    </div>

    <div v-if="showSessionWarning" class="session-modal-overlay">
      <section class="session-modal" aria-labelledby="session-warning-title" role="dialog" aria-modal="true">
        <h2 id="session-warning-title">Session expiring soon</h2>
        <p>Your session is about to expire due to inactivity.</p>
        <p class="session-countdown">{{ sessionCountdown }} seconds remaining</p>
        <div class="session-modal-actions">
          <button type="button" class="secondary-button" @click="logoutFromSessionWarning">
            Logout
          </button>
          <button type="button" @click="stayConnected">Stay connected</button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.app-layout {
  --sidebar-width: 240px;
  --sidebar-padding-x: 16px;

  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  background: #f8fafc;
  color: #111827;
  overflow-x: hidden;
  transition: grid-template-columns 0.25s ease;
}

.app-layout.sidebar-is-collapsed {
  --sidebar-width: 80px;
  --sidebar-padding-x: 12px;
}

.sidebar {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  width: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  border-right: 1px solid #0f172a;
  background: #111827;
  padding: 20px var(--sidebar-padding-x);
  transition:
    width 0.25s ease,
    padding 0.25s ease;
}

.sidebar.collapsed {
  padding: 18px var(--sidebar-padding-x);
}

.sidebar-header {
  min-height: 52px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 22px;
}

.sidebar-logo {
  width: 164px;
  height: auto;
  display: block;
  object-fit: contain;
}

.sidebar-logo.compact {
  width: 44px;
  height: 44px;
  object-fit: contain;
}

.sidebar-toggle {
  position: absolute;
  top: 18px;
  right: -15px;
  z-index: 2;
  width: 30px;
  min-height: 30px;
  padding: 0;
  border: 1px solid #dbe3ea;
  border-radius: 999px;
  color: #0f766e;
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.12);
  line-height: 1;
}

.sidebar-nav {
  min-height: 0;
  display: grid;
  flex: 0 1 auto;
  gap: 8px;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-nav.collapsed {
  justify-items: center;
}

.sidebar-nav-link {
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 6px;
  color: #cbd5e1;
  font-weight: 700;
  text-decoration: none;
}

.sidebar-nav.collapsed .sidebar-nav-link {
  width: 44px;
  min-height: 44px;
  justify-content: center;
  gap: 0;
  padding: 0;
  border-radius: 10px;
}

.sidebar-nav-link:hover {
  color: #ffffff;
  background: rgba(20, 184, 166, 0.14);
}

.sidebar-nav-link.router-link-active {
  color: #ffffff;
  background: rgba(15, 118, 110, 0.34);
}

.nav-icon {
  width: 21px;
  height: 21px;
  flex: 0 0 auto;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.sidebar-spacer {
  min-height: 16px;
  flex: 1 1 auto;
}

.sidebar-footer {
  display: flex;
  flex: 0 0 auto;
  justify-content: stretch;
  margin-top: 24px;
}

.sidebar.collapsed .sidebar-footer {
  justify-content: center;
}

.sidebar-logout-button {
  width: 100%;
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 12px;
  border: 0;
  border-radius: 9px;
  color: #ffffff;
  background: linear-gradient(135deg, #0ea5a4, #0f766e);
  box-shadow: 0 10px 24px rgba(15, 118, 110, 0.24);
  font-size: 14px;
  font-weight: 800;
  white-space: nowrap;
}

.sidebar-logout-button:hover {
  background: linear-gradient(135deg, #14b8a6, #0f766e);
  box-shadow: 0 12px 28px rgba(15, 118, 110, 0.3);
}

.sidebar.collapsed .sidebar-logout-button {
  width: 44px;
  min-height: 44px;
  gap: 0;
  padding: 0;
  border-radius: 10px;
}

.sidebar-logout-icon {
  width: 20px;
  height: 20px;
  flex: 0 0 auto;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.dashboard-main {
  grid-column: 2;
  min-width: 0;
  min-height: 100vh;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.topbar {
  min-height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 10px 24px;
  border-bottom: 1px solid #0f172a;
  background: #111827;
  transition: padding 0.25s ease;
}

.brand {
  min-width: 0;
  display: flex;
  align-items: center;
}

.brand-title {
  color: #f8fafc;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.15;
  white-space: nowrap;
}

.topbar-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.notification-menu-wrapper {
  position: relative;
  flex: 0 0 auto;
}

.notification-button {
  position: relative;
  width: 42px;
  min-height: 42px;
  display: inline-grid;
  place-items: center;
  padding: 0;
  border-radius: 10px;
  color: #e2e8f0;
  background: transparent;
}

.notification-button:hover,
.notification-button[aria-expanded='true'] {
  color: #ffffff;
  background: rgba(148, 163, 184, 0.12);
}

.notification-icon {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.notification-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 18px;
  height: 18px;
  display: inline-grid;
  place-items: center;
  padding: 0 5px;
  border-radius: 999px;
  color: #ffffff;
  background: #ef4444;
  font-size: 10px;
  font-weight: 900;
  line-height: 1;
}

.notification-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 20;
  width: min(380px, calc(100vw - 32px));
  padding: 10px;
  border: 1px solid #dbe3ea;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.22);
}

.notification-dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 8px 10px;
}

.notification-dropdown-header div {
  display: grid;
  gap: 3px;
}

.notification-dropdown-header strong {
  color: #0f172a;
  font-size: 14px;
}

.notification-dropdown-header span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.mark-all-button {
  min-height: 32px;
  padding: 0 9px;
  border-radius: 7px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 12px;
  font-weight: 800;
}

.mark-all-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.notification-list {
  display: grid;
  gap: 6px;
  max-height: 360px;
  overflow-y: auto;
}

.notification-item {
  width: 100%;
  min-height: auto;
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
  padding: 10px;
  border-radius: 9px;
  color: #334155;
  background: #ffffff;
  text-align: left;
}

.notification-item:hover {
  background: #f8fafc;
}

.notification-item.unread {
  background: #ecfeff;
}

.notification-dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 999px;
  background: transparent;
}

.notification-item.unread .notification-dot {
  background: #0f766e;
}

.notification-copy {
  min-width: 0;
  display: grid;
  gap: 3px;
  color: #334155;
  font-size: 13px;
  line-height: 1.3;
}

.notification-copy strong {
  color: #0f172a;
}

.notification-target {
  overflow: hidden;
  color: #0f172a;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.notification-empty,
.notification-error {
  margin: 0;
  padding: 16px 10px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.notification-error {
  color: #b91c1c;
}

.profile-menu-wrapper {
  position: relative;
  flex: 0 0 auto;
}

.profile-chip {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border-radius: 10px;
  color: #f8fafc;
  background: transparent;
  text-align: left;
}

.profile-chip:hover,
.profile-chip[aria-expanded='true'] {
  background: rgba(148, 163, 184, 0.12);
}

.profile-avatar {
  display: inline-grid;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 999px;
  color: #dbeafe;
  background: #334155;
  font-size: 13px;
  font-weight: 900;
}

.profile-avatar.large {
  width: 42px;
  height: 42px;
  color: #ffffff;
  background: linear-gradient(135deg, #0ea5a4, #0f766e);
  font-size: 15px;
}

.profile-summary {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.profile-name {
  max-width: 180px;
  overflow: hidden;
  color: #f8fafc;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-role {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.1;
}

.profile-chevron {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  opacity: 0.82;
}

.profile-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 20;
  width: min(320px, calc(100vw - 32px));
  padding: 10px;
  border: 1px solid #dbe3ea;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.22);
}

.profile-dropdown-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px;
}

.profile-dropdown-copy {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.profile-dropdown-copy strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-dropdown-copy span {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-badge {
  width: fit-content;
  padding: 3px 8px;
  border-radius: 999px;
  color: #0f766e !important;
  background: #ccfbf1;
  font-size: 11px !important;
  font-weight: 800;
}

.profile-dropdown-divider {
  height: 1px;
  margin: 6px 0;
  background: #e2e8f0;
}

.profile-menu-action {
  width: 100%;
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 9px;
  padding: 0 10px;
  border-radius: 8px;
  color: #334155;
  background: #ffffff;
  font-weight: 800;
}

.profile-menu-action:hover {
  color: #0f172a;
  background: #f1f5f9;
}

.profile-action-icon {
  width: 17px;
  height: 17px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

button {
  min-height: 38px;
  padding: 0 14px;
  border: 0;
  border-radius: 6px;
  color: #ffffff;
  background: #0f766e;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.secondary-button {
  color: #334155;
  background: #e2e8f0;
}

.logo-button {
  min-height: auto;
  display: block;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  line-height: 0;
  cursor: pointer;
}

.content {
  min-width: 0;
  padding: 28px;
}

.session-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.55);
}

.session-modal {
  width: min(100%, 440px);
  padding: 24px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}

.session-modal h2 {
  margin: 0;
  color: #111827;
  font-size: 22px;
}

.session-modal p {
  margin: 12px 0 0;
  color: #475569;
}

.session-countdown {
  font-weight: 800;
}

.session-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
}

@media (max-width: 760px) {
  .app-layout {
    --sidebar-width: 220px;
    --sidebar-padding-x: 14px;
  }

  .app-layout.sidebar-is-collapsed {
    --sidebar-width: 72px;
    --sidebar-padding-x: 10px;
  }

  .topbar {
    min-height: auto;
    align-items: flex-start;
    flex-direction: column;
    padding: 16px;
    gap: 14px;
  }

  .brand {
    width: 100%;
  }

  .brand-title {
    font-size: 18px;
    white-space: normal;
  }

  .profile-menu-wrapper {
    width: 100%;
  }

  .topbar-actions {
    width: 100%;
    justify-content: space-between;
  }

  .profile-chip {
    width: 100%;
    justify-content: flex-start;
  }

  .profile-name {
    max-width: min(220px, 52vw);
  }

  .profile-dropdown {
    left: 0;
    right: auto;
    width: min(100%, 320px);
  }

  .notification-dropdown {
    left: 0;
    right: auto;
  }

  .sidebar {
    height: 100vh;
    height: 100dvh;
    padding: 14px var(--sidebar-padding-x);
  }

  .sidebar.collapsed {
    padding: 14px var(--sidebar-padding-x);
  }

  .sidebar-header {
    min-height: 44px;
    margin-bottom: 12px;
  }

  .sidebar-logo {
    width: 145px;
  }

  .sidebar-logo.compact {
    width: 40px;
    height: 40px;
  }

  .sidebar-toggle {
    position: absolute;
    top: 18px;
    right: -15px;
    width: 30px;
    min-height: 30px;
    border-radius: 999px;
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.12);
  }

  .sidebar-nav {
    grid-template-columns: 1fr;
  }

  .sidebar-nav-link {
    justify-content: flex-start;
    text-align: left;
  }

  .sidebar-nav.collapsed .sidebar-nav-link {
    justify-content: center;
  }

  .content {
    padding: 18px;
  }

  .session-modal-actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }
}
</style>
