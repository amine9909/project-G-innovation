<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToastStore()

const identifier = ref('')
const password = ref('')
const submitted = ref(false)

const identifierError = () => submitted.value && !identifier.value.trim()
const passwordError = () => submitted.value && !password.value

async function submitLogin() {
  submitted.value = true

  if (identifierError() || passwordError()) {
    return
  }

  try {
    await authStore.login(identifier.value.trim(), password.value)
    router.push('/')
  } catch {
    toast.error(authStore.error || 'Invalid username/email or password')
  }
}

onMounted(() => {
  const sessionExpired = window.history.state?.sessionExpired === true

  if (sessionExpired) {
    toast.info('Your session expired. Please sign in again.')
    window.history.replaceState({ ...window.history.state, sessionExpired: false }, '')
  }
})
</script>

<template>
  <main class="login-page">
    <section class="login-panel" aria-labelledby="login-title">
      <div class="login-heading">
        <div class="login-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
            <path d="M9 11a4 4 0 1 0 0-8a4 4 0 0 0 0 8Z" />
            <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
        </div>
        <h1 id="login-title">Employee Management</h1>
        <p>Sign in to continue</p>
      </div>

      <form class="login-form" @submit.prevent="submitLogin">
        <div class="field-group">
          <label for="identifier">Username or Email</label>
          <div class="input-shell" :class="{ invalid: identifierError() }">
            <svg class="input-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <path d="M12 11a4 4 0 1 0 0-8a4 4 0 0 0 0 8Z" />
            </svg>
            <input
              id="identifier"
              v-model="identifier"
              type="text"
              autocomplete="username"
              :aria-invalid="identifierError()"
            />
          </div>
        </div>
        <p v-if="identifierError()" class="field-error">Username or email is required.</p>

        <div class="field-group">
          <label for="password">Password</label>
          <div class="input-shell" :class="{ invalid: passwordError() }">
            <svg class="input-icon" viewBox="0 0 24 24" aria-hidden="true">
              <rect x="4" y="11" width="16" height="10" rx="2" />
              <path d="M8 11V8a4 4 0 0 1 8 0v3" />
              <path d="M12 15v2" />
            </svg>
            <input
              id="password"
              v-model="password"
              type="password"
              autocomplete="current-password"
              :aria-invalid="passwordError()"
            />
          </div>
        </div>
        <p v-if="passwordError()" class="field-error">Password is required.</p>

        <button type="submit" :disabled="authStore.loading">
          <span>{{ authStore.loading ? 'Signing in...' : 'Sign in' }}</span>
          <svg v-if="!authStore.loading" class="button-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M5 12h14" />
            <path d="m12 5l7 7l-7 7" />
          </svg>
        </button>

        <RouterLink class="forgot-link" to="/forgot-password">Forgot password?</RouterLink>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  overflow: hidden;
  position: relative;
  padding: 32px;
  background:
    radial-gradient(circle at 4% -8%, rgba(20, 184, 166, 0.2) 0 18%, transparent 18.2%),
    radial-gradient(circle at 99% 93%, rgba(20, 184, 166, 0.18) 0 17%, transparent 17.2%),
    radial-gradient(circle at 85% 24%, rgba(20, 184, 166, 0.12) 0 2.2%, transparent 2.4%),
    radial-gradient(circle at 10% 49%, rgba(15, 118, 110, 0.1) 0 2.2%, transparent 2.4%),
    linear-gradient(135deg, #f8fcff 0%, #eef7fb 48%, #f8fbff 100%);
}

.login-page::before {
  content: '';
  position: absolute;
  inset: auto -10% 12% -10%;
  height: 280px;
  background:
    repeating-radial-gradient(
      ellipse at center,
      transparent 0 16px,
      rgba(148, 163, 184, 0.16) 17px 18px,
      transparent 19px 31px
    );
  opacity: 0.55;
  transform: rotate(-8deg);
  pointer-events: none;
}

.login-page::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(115deg, rgba(255, 255, 255, 0.65), transparent 38%),
    linear-gradient(290deg, rgba(255, 255, 255, 0.78), transparent 42%);
  pointer-events: none;
}

.login-panel {
  position: relative;
  z-index: 1;
  width: min(100%, 420px);
  padding: 36px 32px 32px;
  border: 1px solid rgba(203, 213, 225, 0.88);
  border-top: 4px solid #0ea5a4;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow:
    0 28px 70px rgba(15, 23, 42, 0.14),
    0 1px 0 rgba(255, 255, 255, 0.8) inset;
  backdrop-filter: blur(14px);
}

.login-heading {
  display: grid;
  justify-items: center;
  margin-bottom: 28px;
  text-align: center;
}

.login-icon {
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  margin-bottom: 18px;
  border-radius: 13px;
  color: #5eead4;
  background: linear-gradient(145deg, #1e3a4b, #0f172a);
  box-shadow:
    0 18px 34px rgba(15, 23, 42, 0.22),
    0 1px 0 rgba(255, 255, 255, 0.18) inset;
}

.login-icon svg {
  width: 34px;
  height: 34px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

h1 {
  margin: 0;
  color: #0f172a;
  font-size: clamp(26px, 5vw, 30px);
  font-weight: 900;
  line-height: 1.15;
  letter-spacing: 0;
}

.login-heading p {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 16px;
}

.login-form {
  display: grid;
  gap: 12px;
}

.field-group {
  display: grid;
  gap: 8px;
}

label {
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.input-shell {
  width: 100%;
  min-height: 54px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  border: 1px solid #cbd5e1;
  border-radius: 9px;
  background: #eff6ff;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.05) inset;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.input-shell:focus-within {
  border-color: #0ea5a4;
  background: #f8fbff;
  box-shadow:
    0 0 0 4px rgba(20, 184, 166, 0.14),
    0 8px 18px rgba(15, 23, 42, 0.04) inset;
}

.input-shell.invalid {
  border-color: #dc2626;
}

.input-icon {
  width: 20px;
  height: 20px;
  flex: 0 0 auto;
  color: #64748b;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

input {
  width: 100%;
  min-width: 0;
  border: 0;
  color: #111827;
  background: transparent;
  font-size: 18px;
  outline: 0;
}

input:-webkit-autofill,
input:-webkit-autofill:hover,
input:-webkit-autofill:focus {
  -webkit-text-fill-color: #111827;
  transition: background-color 9999s ease-in-out 0s;
}

input[aria-invalid='true'] {
  color: #111827;
}

.field-error {
  margin: -4px 0 2px;
  color: #b91c1c;
  font-size: 13px;
  font-weight: 700;
}

button {
  min-height: 54px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 4px;
  border: 0;
  border-radius: 9px;
  color: #ffffff;
  background: linear-gradient(135deg, #0ea5a4, #0f766e);
  box-shadow: 0 16px 28px rgba(15, 118, 110, 0.25);
  font-size: 16px;
  font-weight: 900;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

button:hover:not(:disabled) {
  transform: translateY(-1px);
  background: linear-gradient(135deg, #14b8a6, #0f766e);
  box-shadow: 0 18px 32px rgba(15, 118, 110, 0.3);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.button-icon {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.forgot-link {
  justify-self: center;
  margin-top: 12px;
  color: #0f766e;
  font-size: 14px;
  font-weight: 900;
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

@media (max-width: 560px) {
  .login-page {
    padding: 20px;
  }

  .login-panel {
    padding: 30px 22px 26px;
  }

  .login-icon {
    width: 58px;
    height: 58px;
  }

  .input-shell,
  button {
    min-height: 50px;
  }

  input {
    font-size: 16px;
  }
}
</style>
