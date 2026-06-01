import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDraftStore } from '@/stores/draft'

describe('useDraftStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('initial state', () => {
    it('has null currentDraftId', () => {
      const store = useDraftStore()
      expect(store.currentDraftId).toBeNull()
    })

    it('has isDirty as false', () => {
      const store = useDraftStore()
      expect(store.isDirty).toBe(false)
    })
  })

  describe('setDirty', () => {
    it('sets isDirty to true by default', () => {
      const store = useDraftStore()
      store.setDirty()
      expect(store.isDirty).toBe(true)
    })

    it('sets isDirty to the provided boolean', () => {
      const store = useDraftStore()
      store.setDirty(true)
      expect(store.isDirty).toBe(true)

      store.setDirty(false)
      expect(store.isDirty).toBe(false)
    })
  })

  describe('setDraftId', () => {
    it('sets currentDraftId to the provided string', () => {
      const store = useDraftStore()
      store.setDraftId('draft-abc-123')
      expect(store.currentDraftId).toBe('draft-abc-123')
    })

    it('sets currentDraftId to null', () => {
      const store = useDraftStore()
      store.currentDraftId = 'existing-draft'
      store.setDraftId(null)
      expect(store.currentDraftId).toBeNull()
    })
  })

  describe('clearDraft', () => {
    it('resets currentDraftId and isDirty', () => {
      const store = useDraftStore()
      store.currentDraftId = 'draft-xyz'
      store.isDirty = true

      store.clearDraft()

      expect(store.currentDraftId).toBeNull()
      expect(store.isDirty).toBe(false)
    })
  })
})
