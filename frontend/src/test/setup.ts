import '@testing-library/jest-dom'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

// 清理每个测试后的 DOM
afterEach(() => {
  cleanup()
})

// Mock MSW server
const server = setupServer(
  http.get('/api/workflows', () => {
    return HttpResponse.json({
      workflows: [],
      total: 0,
    })
  }),
  
  http.post('/api/workflows', () => {
    return HttpResponse.json({
      id: 'wf-1',
      name: 'Test Workflow',
      nodes: [],
      edges: [],
    })
  }),
  
  http.get('/api/workflows/:id', () => {
    return HttpResponse.json({
      id: 'wf-1',
      name: 'Test Workflow',
      nodes: [],
      edges: [],
    })
  }),
  
  http.post('/api/workflows/:id/execute', () => {
    return HttpResponse.json({
      executionId: 'exec-1',
      status: 'RUNNING',
    })
  }),
  
  http.get('/api/executions/:id', () => {
    return HttpResponse.json({
      id: 'exec-1',
      status: 'SUCCESS',
      results: {},
    })
  }),
)

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'bypass' })
})

afterEach(() => {
  server.resetHandlers()
})

afterAll(() => {
  server.close()
})

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => {},
  }),
})

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
