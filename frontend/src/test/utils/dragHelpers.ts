import { fireEvent } from '@testing-library/react'

export interface Position {
  x: number
  y: number
}

export interface DragOptions {
  start?: Position
  end?: Position
}

/**
 * 模拟拖拽操作
 */
export async function dragAndDrop(
  element: HTMLElement,
  target: HTMLElement,
  position: Position | { start: Position; end: Position }
) {
  const startPos = 'start' in position ? position.start : position
  const endPos = 'start' in position ? position.end : position

  const startEventInit = {
    clientX: startPos.x,
    clientY: startPos.y,
    bubbles: true,
    cancelable: true,
  }

  const moveEventInit = {
    clientX: (startPos.x + endPos.x) / 2,
    clientY: (startPos.y + endPos.y) / 2,
    bubbles: true,
    cancelable: true,
  }

  const endEventInit = {
    clientX: endPos.x,
    clientY: endPos.y,
    bubbles: true,
    cancelable: true,
  }

  fireEvent.dragStart(element, startEventInit)
  fireEvent.dragOver(target, moveEventInit)
  fireEvent.drop(target, endEventInit)
  fireEvent.dragEnd(element, endEventInit)
}

/**
 * 模拟拖拽开始
 */
export async function dragStart(element: HTMLElement, position?: Position) {
  const eventInit = {
    clientX: position?.x || 0,
    clientY: position?.y || 0,
    bubbles: true,
    cancelable: true,
  }

  fireEvent.dragStart(element, eventInit)
}

/**
 * 模拟放置
 */
export async function drop(target: HTMLElement, position: Position) {
  const eventInit = {
    clientX: position.x,
    clientY: position.y,
    bubbles: true,
    cancelable: true,
  }

  fireEvent.drop(target, eventInit)
}

/**
 * 模拟鼠标滚轮缩放
 */
export async function wheel(element: HTMLElement, options: { deltaY: number }) {
  fireEvent.wheel(element, {
    deltaY: options.deltaY,
    bubbles: true,
    cancelable: true,
  })
}

/**
 * 获取元素位置
 */
export function getElementPosition(element: HTMLElement): Position {
  const rect = element.getBoundingClientRect()
  return {
    x: rect.x + rect.width / 2,
    y: rect.y + rect.height / 2,
  }
}

/**
 * 模拟框选
 */
export async function selectBox(elements: HTMLElement[]) {
  const firstRect = elements[0].getBoundingClientRect()
  const lastRect = elements[elements.length - 1].getBoundingClientRect()

  const start = {
    x: firstRect.left - 10,
    y: firstRect.top - 10,
  }
  const end = {
    x: lastRect.right + 10,
    y: lastRect.bottom + 10,
  }

  fireEvent.mouseDown(document, { clientX: start.x, clientY: start.y })
  fireEvent.mouseMove(document, { clientX: end.x, clientY: end.y })
  fireEvent.mouseUp(document, { clientX: end.x, clientY: end.y })
}

/**
 * 模拟多选节点
 */
export async function selectMultipleNodes(nodeIds: string[]) {
  for (let i = 0; i < nodeIds.length; i++) {
    const node = document.querySelector(`[data-testid="${nodeIds[i]}"]`) as HTMLElement
    if (node) {
      node.click({
        ctrlKey: i > 0,
        bubbles: true,
      })
    }
  }
}

/**
 * 模拟连接两个 Handle
 */
export async function connectHandles(source: HTMLElement, target: HTMLElement) {
  fireEvent.mouseDown(source, { bubbles: true })
  fireEvent.mouseEnter(target, { bubbles: true })
  fireEvent.mouseUp(target, { bubbles: true })
}
