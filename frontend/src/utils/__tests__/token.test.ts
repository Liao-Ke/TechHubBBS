import { describe, it, expect, beforeEach, vi } from 'vitest'
import { getToken, setToken, removeToken } from '../token'

describe('token', () => {
  beforeEach(() => {
    const store: Record<string, string> = {}
    vi.stubGlobal('localStorage', {
      getItem: vi.fn((key: string) => store[key] ?? null),
      setItem: vi.fn((key: string, value: string) => { store[key] = value }),
      removeItem: vi.fn((key: string) => { delete store[key] }),
      clear: vi.fn(() => { Object.keys(store).forEach(k => delete store[k]) }),
    })
  })

  it('getToken returns null when no token is set', () => {
    expect(getToken()).toBeNull()
  })

  it('setToken stores the token in localStorage', () => {
    setToken('test-token-123')
    expect(getToken()).toBe('test-token-123')
  })

  it('getToken returns the stored token', () => {
    setToken('test-token-456')
    expect(getToken()).toBe('test-token-456')
  })

  it('removeToken clears the token from localStorage', () => {
    setToken('test-token-789')
    removeToken()
    expect(getToken()).toBeNull()
  })

  it('removeToken does not throw when no token exists', () => {
    expect(() => removeToken()).not.toThrow()
  })
})
