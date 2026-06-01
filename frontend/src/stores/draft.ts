import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDraftStore = defineStore('draft', () => {
  const currentDraftId = ref<string | null>(null)
  const isDirty = ref(false)

  function setDirty(dirty: boolean = true) {
    isDirty.value = dirty
  }

  function setDraftId(id: string | null) {
    currentDraftId.value = id
  }

  function clearDraft() {
    currentDraftId.value = null
    isDirty.value = false
  }

  return { currentDraftId, isDirty, setDirty, setDraftId, clearDraft }
})
