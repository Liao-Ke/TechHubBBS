import { describe, it, expect } from 'vitest'
import router from '../index'

describe('Router configuration', () => {
  it('has correct number of top-level routes', () => {
    expect(router.getRoutes().length).toBeGreaterThanOrEqual(12)
  })

  it('home route exists with correct path', () => {
    const route = router.getRoutes().find((r) => r.name === 'home')
    expect(route).toBeDefined()
    expect(route!.path).toBe('/')
    expect(route!.meta.layout).toBe('default')
  })

  it('category route exists with param', () => {
    const route = router.getRoutes().find((r) => r.name === 'category')
    expect(route).toBeDefined()
    expect(route!.path).toBe('/categories/:categoryId')
  })

  it('post-detail route exists with param', () => {
    const route = router.getRoutes().find((r) => r.name === 'post-detail')
    expect(route).toBeDefined()
    expect(route!.path).toBe('/posts/:id')
  })

  it('post-create route requires auth', () => {
    const route = router.getRoutes().find((r) => r.name === 'post-create')
    expect(route).toBeDefined()
    expect(route!.path).toBe('/posts/new')
    expect(route!.meta.requiresAuth).toBe(true)
  })

  it('post-edit route requires auth', () => {
    const route = router.getRoutes().find((r) => r.name === 'post-edit')
    expect(route).toBeDefined()
    expect(route!.path).toBe('/posts/:id/edit')
    expect(route!.meta.requiresAuth).toBe(true)
  })

  it('user-profile route exists', () => {
    const route = router.getRoutes().find((r) => r.name === 'user-profile')
    expect(route).toBeDefined()
    expect(route!.path).toBe('/users/:id')
  })

  it('settings route requires auth', () => {
    const route = router.getRoutes().find((r) => r.name === 'settings')
    expect(route).toBeDefined()
    expect(route!.path).toBe('/settings')
    expect(route!.meta.requiresAuth).toBe(true)
  })

  it('notifications route requires auth', () => {
    const route = router.getRoutes().find((r) => r.name === 'notifications')
    expect(route).toBeDefined()
    expect(route!.path).toBe('/notifications')
    expect(route!.meta.requiresAuth).toBe(true)
  })

  it('drafts route requires auth', () => {
    const route = router.getRoutes().find((r) => r.name === 'drafts')
    expect(route).toBeDefined()
    expect(route!.path).toBe('/drafts')
    expect(route!.meta.requiresAuth).toBe(true)
  })

  it('login route exists', () => {
    const route = router.getRoutes().find((r) => r.name === 'login')
    expect(route).toBeDefined()
    expect(route!.path).toBe('/login')
    expect(route!.meta.layout).toBe('auth')
  })

  it('register route exists', () => {
    const route = router.getRoutes().find((r) => r.name === 'register')
    expect(route).toBeDefined()
    expect(route!.path).toBe('/register')
    expect(route!.meta.layout).toBe('auth')
  })

  describe('admin routes', () => {
    it('admin parent route requires auth and admin/moderator roles', () => {
      const route = router.getRoutes().find((r) => r.path === '/admin' && !r.name)
      expect(route).toBeDefined()
      expect(route!.meta.requiresAuth).toBe(true)
      expect(route!.meta.roles).toEqual(['ADMIN', 'MODERATOR'])
    })

    it('admin-dashboard is a child of /admin', () => {
      const route = router.getRoutes().find((r) => r.name === 'admin-dashboard')
      expect(route).toBeDefined()
      expect(route!.path).toBe('/admin')
    })

    it('admin-users requires ADMIN role', () => {
      const route = router.getRoutes().find((r) => r.name === 'admin-users')
      expect(route).toBeDefined()
      expect(route!.path).toBe('/admin/users')
      expect(route!.meta.roles).toEqual(['ADMIN'])
    })

    it('admin-posts is a child route', () => {
      const route = router.getRoutes().find((r) => r.name === 'admin-posts')
      expect(route).toBeDefined()
      expect(route!.path).toBe('/admin/posts')
    })

    it('admin-categories requires ADMIN role', () => {
      const route = router.getRoutes().find((r) => r.name === 'admin-categories')
      expect(route).toBeDefined()
      expect(route!.path).toBe('/admin/categories')
      expect(route!.meta.roles).toEqual(['ADMIN'])
    })

    it('admin-notices is a child route', () => {
      const route = router.getRoutes().find((r) => r.name === 'admin-notices')
      expect(route).toBeDefined()
      expect(route!.path).toBe('/admin/notices')
    })

    it('admin-divine requires ADMIN role', () => {
      const route = router.getRoutes().find((r) => r.name === 'admin-divine')
      expect(route).toBeDefined()
      expect(route!.path).toBe('/admin/divine')
      expect(route!.meta.roles).toEqual(['ADMIN'])
    })
  })

  describe('error pages', () => {
    it('has /403 forbidden route', () => {
      const route = router.getRoutes().find((r) => r.name === 'forbidden')
      expect(route).toBeDefined()
      expect(route!.path).toBe('/403')
    })

    it('has 404 catch-all route', () => {
      const route = router.getRoutes().find((r) => r.name === 'not-found')
      expect(route).toBeDefined()
      expect(route!.path).toBe('/:pathMatch(.*)*')
    })

    it('catch-all route is the last route (lowest priority)', () => {
      const routes = router.getRoutes()
      const lastRoute = routes[routes.length - 1]
      expect(lastRoute!.name).toBe('not-found')
    })
  })

  it('all routes use lazy loading (function, not resolved component)', () => {
    const routes = router.getRoutes()
    for (const route of routes) {
      if (!route.components?.default) continue // skip parent layouts with children
      expect(typeof route.components.default).toBe('function')
    }
  })

  it('scrollBehavior returns top: 0', () => {
    const result = router.options.scrollBehavior?.(
      { path: '/', hash: '', params: {}, query: {}, fullPath: '/', name: 'home', matched: [], meta: {}, redirectedFrom: undefined },
      { path: '/', params: {}, query: {}, fullPath: '/', hash: '', name: 'home', matched: [], meta: {}, redirectedFrom: undefined },
      null,
    )
    expect(result).toEqual({ top: 0 })
  })
})
