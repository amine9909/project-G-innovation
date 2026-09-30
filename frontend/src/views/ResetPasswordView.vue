<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { apiFetch } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

interface ApiErrorResponse {
  message?: string
}

interface PasswordResetMessageResponse {
  message: string
}

const route = useRoute()
const authStore = useAuthStore()
const toast = useToastStore()

const token = computed(() => {
  const value = route.query.token
  return typeof value === 'string' ? value : ''
})

const newPassword = ref('')
const confirmPassword = ref('')
const submitted = ref(false)
const loading = ref(false)

function newPasswordError() {
  if (!submitted.value) {
    return ''
  }

  if (!newPassword.value) {
    return 'New password is required.'
  }

  if (newPassword.value.length < 6) {
    return 'Password must be at least 6 characters.'
  }

  return ''
}

function confirmPasswordError() {
  if (!submitted.value) {
    return ''
  }

  if (!confirmPassword.value) {
    return 'Please confirm your password.'
  }

  if (newPassword.value !== confirmPassword.value) {
    return 'Passwords do not match.'
  }

  return ''
}

async function parseErrorMessage(response: Response) {
  try {
    const data = (await response.json()) as ApiErrorResponse
    return data.message
  } catch {
    return ''
  }
}

async function submitResetPassword() {
  submitted.value = true

  if (!token.value) {
    toast.error('The password reset link is invalid or has expired.')
    return
  }

  if (newPasswordError() || confirmPasswordError()) {
    return
  }

  loading.value = true

  try {
    const response = await apiFetch('/api/auth/reset-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        token: token.value,
        newPassword: newPassword.value,
        confirmPassword: confirmPassword.value,
      }),
    })

    if (!response.ok) {
      toast.error(
        (await parseErrorMessage(response)) ||
          'Unable to reset password. The link may be invalid or expired.',
      )
      return
    }

    const data = (await response.json()) as PasswordResetMessageResponse
    toast.success(data.message)
    newPassword.value = ''
    confirmPassword.value = ''
    submitted.value = false
    authStore.clearLocalSession()
  } catch {
    toast.error('Unable to reset password. The link may be invalid or expired.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="reset-password-title">
      <div class="auth-heading">
        <h1 id="reset-password-title">Choose New Password</h1>
        <p>Enter and confirm your new password.</p>
      </div>

      <form class="auth-form" @submit.prevent="submitResetPassword">
        <p v-if="!token" class="form-error">The password reset link is invalid or has expired.</p>

        <label for="new-password">New password</label>
        <input
          id="new-password"
          v-model="newPassword"
          type="password"
          autocomplete="new-password"
          :aria-invalid="Boolean(newPasswordError())"
        />
        <p v-if="newPasswordError()" class="field-error">{{ newPasswordError() }}</p>

        <label for="confirm-password">Confirm password</label>
        <input
          id="confirm-password"
          v-model="confirmPassword"
          type="password"
          autocomplete="new-password"
          :aria-invalid="Boolean(confirmPasswordError())"
        />
        <p v-if="confirmPasswordError()" class="field-error">{{ confirmPasswordError() }}</p>

        <button type="submit" :disabled="loading || !token">
          {{ loading ? 'Resetting...' : 'Reset password' }}
        </button>
      </form>

      <RouterLink class="secondary-link" to="/login">Back to login</RouterLink>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background:
    linear-gradient(135deg, rgba(31, 41, 55, 0.08), rgba(15, 118, 110, 0.1)),
    #f8fafc;
}

.auth-panel {
  width: min(100%, 440px);
  padding: 32px;
  border: 1px solid #dbe3ea;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
}

.auth-heading {
  margin-bottom: 28px;
}

h1 {
  margin: 0;
  color: #111827;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.auth-heading p {
  margin: 8px 0 0;
  color: #5b6675;
  font-size: 15px;
}

.auth-form {
  display: grid;
  gap: 10px;
}

label {
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
}

input {
  width: 100%;
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  color: #111827;
  background: #ffffff;
  font-size: 15px;
}

input:focus {
  outline: 3px solid rgba(20, 184, 166, 0.18);
  border-color: #0f766e;
}

input[aria-invalid='true'] {
  border-color: #dc2626;
}

.field-error,
.form-error {
  margin: -4px 0 6px;
  color: #b91c1c;
  font-size: 13px;
}

.form-error {
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 14px;
}

.form-error {
  border: 1px solid #fecaca;
  background: #fef2f2;
}

button {
  min-height: 44px;
  margin-top: 8px;
  border: 0;
  border-radius: 6px;
  color: #ffffff;
  background: #0f766e;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.secondary-link {
  justify-self: center;
  margin-top: 18px;
  color: #0f766e;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
}

.secondary-link:hover {
  text-decoration: underline;
}
</style>
