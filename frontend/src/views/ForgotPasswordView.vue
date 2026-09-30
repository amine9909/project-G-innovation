<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { apiFetch } from '@/services/api'
import { useToastStore } from '@/stores/toast'

interface PasswordResetMessageResponse {
  message: string
}

const email = ref('')
const submitted = ref(false)
const loading = ref(false)
const toast = useToastStore()

function emailError() {
  if (!submitted.value) {
    return ''
  }

  if (!email.value.trim()) {
    return 'Email is required.'
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    return 'Enter a valid email address.'
  }

  return ''
}

async function submitForgotPassword() {
  submitted.value = true

  if (emailError()) {
    return
  }

  loading.value = true

  try {
    const response = await apiFetch('/api/auth/forgot-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email: email.value.trim() }),
    })

    if (!response.ok) {
      toast.error(
        response.status === 400
          ? 'Enter a valid email address.'
          : 'Unable to send password reset email right now. Please try again later.',
      )
      return
    }

    const data = (await response.json()) as PasswordResetMessageResponse
    toast.success(data.message)
  } catch {
    toast.error('Unable to send password reset email right now. Please try again later.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="forgot-password-title">
      <div class="auth-heading">
        <h1 id="forgot-password-title">Reset Password</h1>
        <p>Enter your email address and we will send a reset link if an account exists.</p>
      </div>

      <form class="auth-form" @submit.prevent="submitForgotPassword">
        <label for="email">Email</label>
        <input
          id="email"
          v-model="email"
          type="email"
          autocomplete="email"
          :aria-invalid="Boolean(emailError())"
        />
        <p v-if="emailError()" class="field-error">{{ emailError() }}</p>

        <button type="submit" :disabled="loading">
          {{ loading ? 'Sending...' : 'Send reset link' }}
        </button>

        <RouterLink class="secondary-link" to="/login">Back to login</RouterLink>
      </form>
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

.field-error {
  margin: -4px 0 6px;
  color: #b91c1c;
  font-size: 13px;
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
  margin-top: 8px;
  color: #0f766e;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
}

.secondary-link:hover {
  text-decoration: underline;
}
</style>
