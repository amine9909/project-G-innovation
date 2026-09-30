<script setup lang="ts">
import { computed } from 'vue'

type PageItem = number | 'ellipsis-left' | 'ellipsis-right'

const props = withDefaults(
  defineProps<{
    currentPage: number
    pageSize: number
    totalPages: number
    totalElements: number
    itemLabel: string
    pageSizeOptions?: number[]
    disabled?: boolean
  }>(),
  {
    pageSizeOptions: () => [10, 20, 50],
    disabled: false,
  },
)

const emit = defineEmits<{
  'page-change': [page: number]
  'page-size-change': [size: number]
}>()

const displayPage = computed(() => props.currentPage + 1)

const rangeStart = computed(() => {
  if (props.totalElements === 0) {
    return 0
  }

  return props.currentPage * props.pageSize + 1
})

const rangeEnd = computed(() => {
  if (props.totalElements === 0) {
    return 0
  }

  return Math.min((props.currentPage + 1) * props.pageSize, props.totalElements)
})

const pageItems = computed<PageItem[]>(() => {
  const total = props.totalPages
  const current = displayPage.value

  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index + 1)
  }

  const items: PageItem[] = [1]
  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)

  if (start > 2) {
    items.push('ellipsis-left')
  }

  for (let page = start; page <= end; page += 1) {
    items.push(page)
  }

  if (end < total - 1) {
    items.push('ellipsis-right')
  }

  items.push(total)
  return items
})

function changePage(pageNumber: number) {
  const nextPage = pageNumber - 1

  if (props.disabled || nextPage === props.currentPage) {
    return
  }

  emit('page-change', nextPage)
}

function changePageSize(event: Event) {
  const nextSize = Number((event.target as HTMLSelectElement).value)

  if (props.disabled || nextSize === props.pageSize) {
    return
  }

  emit('page-size-change', nextSize)
}
</script>

<template>
  <nav class="pagination" aria-label="Table pagination">
    <label class="page-size-control">
      <span>Rows per page</span>
      <select :value="pageSize" :disabled="disabled" @change="changePageSize">
        <option v-for="option in pageSizeOptions" :key="option" :value="option">
          {{ option }}
        </option>
      </select>
    </label>

    <div class="pagination-controls">
      <button
        type="button"
        class="pagination-text-button"
        :disabled="disabled || currentPage === 0"
        @click="changePage(displayPage - 1)"
      >
        Previous
      </button>

      <template v-for="item in pageItems" :key="item">
        <span v-if="typeof item === 'string'" class="pagination-ellipsis">...</span>
        <button
          v-else
          type="button"
          class="pagination-page-button"
          :class="{ active: item === displayPage }"
          :aria-current="item === displayPage ? 'page' : undefined"
          :disabled="disabled"
          @click="changePage(item)"
        >
          {{ item }}
        </button>
      </template>

      <button
        type="button"
        class="pagination-text-button"
        :disabled="disabled || currentPage + 1 >= totalPages"
        @click="changePage(displayPage + 1)"
      >
        Next
      </button>
    </div>

    <p class="range-summary">
      Showing {{ rangeStart }}-{{ rangeEnd }} of {{ totalElements }} {{ itemLabel }}
    </p>
  </nav>
</template>

<style scoped>
.pagination {
  display: grid;
  grid-template-columns: minmax(170px, 1fr) auto minmax(170px, 1fr);
  align-items: center;
  gap: 16px;
  margin-top: 18px;
  color: #475569;
  font-size: 13px;
}

.page-size-control {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.page-size-control select {
  min-height: 34px;
  padding: 0 30px 0 10px;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
  color: #0f172a;
  background: #f8fafc;
  font: inherit;
  cursor: pointer;
}

.page-size-control select:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.pagination-controls {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.pagination-text-button,
.pagination-page-button {
  min-height: 32px;
  border: 0;
  color: #0f172a;
  background: transparent;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.pagination-text-button {
  padding: 0 10px;
}

.pagination-page-button {
  min-width: 32px;
  padding: 0 8px;
  border-radius: 7px;
}

.pagination-page-button.active {
  border: 1px solid #dbe3ea;
  color: #0f172a;
  background: #f8fafc;
}

.pagination-text-button:disabled,
.pagination-page-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.pagination-ellipsis {
  min-width: 26px;
  color: #64748b;
  text-align: center;
  font-weight: 700;
}

.range-summary {
  margin: 0;
  justify-self: end;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 900px) {
  .pagination {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .page-size-control,
  .range-summary {
    justify-self: center;
  }
}

@media (max-width: 520px) {
  .pagination-controls {
    flex-wrap: wrap;
  }
}
</style>
