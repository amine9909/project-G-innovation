<script setup lang="ts">
import { useToastStore } from '@/stores/toast'

const toastStore = useToastStore()
</script>

<template>
  <Teleport to="body">
    <div class="toast-viewport" aria-live="polite" aria-atomic="true">
      <TransitionGroup name="toast" tag="div" class="toast-stack">
        <article
          v-for="toast in toastStore.toasts"
          :key="toast.id"
          class="toast"
          :class="`toast-${toast.type}`"
          :role="toast.type === 'error' || toast.type === 'warning' ? 'alert' : 'status'"
        >
          <span class="toast-indicator" aria-hidden="true"></span>
          <p>{{ toast.message }}</p>
          <button
            type="button"
            class="toast-close"
            aria-label="Close notification"
            @click="toastStore.dismiss(toast.id)"
          >
            &times;
          </button>
        </article>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-viewport {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 1000;
  width: min(380px, calc(100vw - 32px));
  pointer-events: none;
}

.toast-stack {
  display: grid;
  gap: 10px;
}

.toast {
  display: grid;
  grid-template-columns: 4px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
  padding: 14px;
  border: 1px solid #dbe3ea;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.16);
  pointer-events: auto;
}

.toast p {
  margin: 0;
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
}

.toast-indicator {
  width: 4px;
  height: 100%;
  min-height: 32px;
  border-radius: 999px;
}

.toast-success .toast-indicator {
  background: #16a34a;
}

.toast-error .toast-indicator {
  background: #dc2626;
}

.toast-warning .toast-indicator {
  background: #d97706;
}

.toast-info .toast-indicator {
  background: #0f766e;
}

.toast-close {
  width: 28px;
  min-height: 28px;
  padding: 0;
  border: 0;
  border-radius: 6px;
  color: #64748b;
  background: #f1f5f9;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.toast-close:hover {
  color: #111827;
  background: #e2e8f0;
}

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

@media (max-width: 640px) {
  .toast-viewport {
    top: 12px;
    right: 12px;
    width: calc(100vw - 24px);
  }
}
</style>
