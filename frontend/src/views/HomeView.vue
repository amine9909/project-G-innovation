<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <main class="home-page">
    <section class="home-panel">
      <div>
        <p class="eyebrow">Authenticated session</p>
        <h1>Welcome, {{ authStore.user?.name }}</h1>
      </div>

      <dl>
        <div>
          <dt>User type</dt>
          <dd>{{ authStore.user?.userType }}</dd>
        </div>
        <div>
          <dt>Permissions</dt>
          <dd>
            <span v-if="authStore.user?.permissions.length">
              {{ authStore.user.permissions.join(', ') }}
            </span>
            <span v-else>None</span>
          </dd>
        </div>
      </dl>

      <button type="button" @click="logout">Logout</button>
    </section>
  </main>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background: #f8fafc;
}

.home-panel {
  width: min(100%, 720px);
  padding: 32px;
  border: 1px solid #dbe3ea;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
}

.eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #111827;
  font-size: 30px;
  line-height: 1.2;
}

dl {
  display: grid;
  gap: 18px;
  margin: 28px 0;
}

dt {
  color: #5b6675;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}

dd {
  margin: 6px 0 0;
  color: #111827;
  font-size: 16px;
}

button {
  min-height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: 6px;
  color: #ffffff;
  background: #1f2937;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}
</style>
