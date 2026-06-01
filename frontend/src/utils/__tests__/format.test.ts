import { describe, it, expect } from 'vitest'
import { formatDate, formatRelativeTime, formatNumber } from '../format'

describe('formatDate', () => {
  it('formats a date string correctly', () => {
    const result = formatDate('2026-05-27T14:30:00')
    expect(result).toBe('2026年05月27日 14:30')
  })

  it('pads single-digit month and day', () => {
    const result = formatDate('2026-01-05T08:05:00')
    expect(result).toBe('2026年01月05日 08:05')
  })
})

describe('formatRelativeTime', () => {
  it('returns "刚刚" for times within 60 seconds', () => {
    const now = new Date().toISOString()
    expect(formatRelativeTime(now)).toBe('刚刚')
  })

  it('returns "X分钟前" for times within 60 minutes', () => {
    const past = new Date(Date.now() - 5 * 60 * 1000).toISOString()
    expect(formatRelativeTime(past)).toBe('5分钟前')
  })

  it('returns "X小时前" for times within 24 hours', () => {
    const past = new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString()
    expect(formatRelativeTime(past)).toBe('3小时前')
  })

  it('returns "X天前" for times within 30 days', () => {
    const past = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString()
    expect(formatRelativeTime(past)).toBe('7天前')
  })

  it('returns "X个月前" for times within 12 months', () => {
    const past = new Date(Date.now() - 60 * 24 * 60 * 60 * 1000).toISOString()
    expect(formatRelativeTime(past)).toBe('2个月前')
  })

  it('returns "X年前" for times over 12 months ago', () => {
    const past = new Date(Date.now() - 400 * 24 * 60 * 60 * 1000).toISOString()
    expect(formatRelativeTime(past)).toBe('1年前')
  })
})

describe('formatNumber', () => {
  it('returns the number as string when less than 1000', () => {
    expect(formatNumber(0)).toBe('0')
    expect(formatNumber(999)).toBe('999')
  })

  it('formats thousands with "k" suffix', () => {
    expect(formatNumber(1000)).toBe('1.0k')
    expect(formatNumber(1234)).toBe('1.2k')
    expect(formatNumber(9999)).toBe('10.0k')
  })

  it('formats ten-thousands with "万" suffix', () => {
    expect(formatNumber(10000)).toBe('1.0万')
    expect(formatNumber(123456)).toBe('12.3万')
  })

  it('formats millions with "M" suffix', () => {
    expect(formatNumber(1000000)).toBe('1.0M')
    expect(formatNumber(12345678)).toBe('12.3M')
  })
})
