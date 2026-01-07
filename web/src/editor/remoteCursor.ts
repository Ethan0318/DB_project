import { Extension } from '@tiptap/core'
import { Plugin, PluginKey, Transaction } from 'prosemirror-state'
import { Decoration, DecorationSet } from 'prosemirror-view'

export interface RemoteCursor {
  userId: number
  name: string
  color: string
  from: number
  to: number
  updatedAt?: number
}

/**
 * 操作类型定义
 */
export interface TextOperation {
  type: 'insert' | 'delete'
  position: number   // 操作发生的位置
  length: number     // 插入/删除的长度
  userId?: number    // 发起操作的用户ID
}

// 存储每个用户的光标位置
export interface CursorState {
  cursors: Map<number, RemoteCursor>
  version: number
}

const remoteCursorKey = new PluginKey<CursorState>('remoteCursor')

/**
 * 将位置限制在文档范围内
 */
const clampPosition = (pos: number, docSize: number): number => {
  return Math.max(0, Math.min(pos, docSize))
}

/**
 * 核心算法：Operational Transformation 索引转换
 * 
 * 当本地用户执行操作时，计算远程光标的新位置
 * 
 * 规则：
 * 1. 如果操作位置在光标之后 → 光标不动
 * 2. 如果操作位置在光标之前 → 光标需要调整
 * 3. 如果操作位置正好在光标位置 → 插入时光标右移，删除时保持
 * 
 * @param cursorPos 远程光标当前位置
 * @param operation 本地用户的操作
 * @returns 转换后的光标位置
 */
export const transformPosition = (
  cursorPos: number,
  operation: TextOperation
): number => {
  const { type, position, length } = operation

  if (type === 'insert') {
    // 插入操作
    if (position < cursorPos) {
      // 插入点在光标之前：光标右移
      return cursorPos + length
    } else if (position === cursorPos) {
      // 插入点恰好在光标位置：光标右移（新内容在光标左边）
      // 注意：这是一个设计决策，也可以选择不移动
      return cursorPos + length
    } else {
      // 插入点在光标之后：光标不动
      return cursorPos
    }
  } else if (type === 'delete') {
    // 删除操作
    const deleteEnd = position + length
    
    if (deleteEnd <= cursorPos) {
      // 删除区域完全在光标之前：光标左移
      return cursorPos - length
    } else if (position >= cursorPos) {
      // 删除区域完全在光标之后：光标不动
      return cursorPos
    } else {
      // 删除区域包含光标位置：光标移动到删除起始位置
      return position
    }
  }

  return cursorPos
}

/**
 * 转换远程光标位置
 * 处理选区（from 和 to 可能不同）
 */
export const transformCursor = (
  cursor: RemoteCursor,
  operation: TextOperation,
  docSize: number
): RemoteCursor => {
  const newFrom = clampPosition(transformPosition(cursor.from, operation), docSize)
  const newTo = clampPosition(transformPosition(cursor.to, operation), docSize)
  
  return {
    ...cursor,
    from: newFrom,
    to: Math.max(newFrom, newTo) // 确保 to >= from
  }
}

/**
 * 批量转换所有远程光标
 * @param cursors 所有远程光标
 * @param operation 本地操作
 * @param excludeUserId 排除的用户ID（操作发起者）
 * @param docSize 文档大小
 */
export const transformAllCursors = (
  cursors: Map<number, RemoteCursor>,
  operation: TextOperation,
  excludeUserId: number | undefined,
  docSize: number
): Map<number, RemoteCursor> => {
  const newCursors = new Map<number, RemoteCursor>()
  
  cursors.forEach((cursor, userId) => {
    if (userId === excludeUserId) {
      // 不转换操作发起者的光标（他们会自己更新）
      newCursors.set(userId, cursor)
    } else {
      newCursors.set(userId, transformCursor(cursor, operation, docSize))
    }
  })
  
  return newCursors
}

/**
 * 从 ProseMirror Transaction 中提取操作
 * 分析文档变化，生成 TextOperation 列表
 */
export const extractOperationsFromTransaction = (tr: Transaction): TextOperation[] => {
  const operations: TextOperation[] = []
  
  if (!tr.docChanged) return operations
  
  // 遍历所有 steps 来提取操作
  tr.steps.forEach((step, index) => {
    const stepMap = step.getMap()
    
    stepMap.forEach((oldStart, oldEnd, newStart, newEnd) => {
      const oldLen = oldEnd - oldStart
      const newLen = newEnd - newStart
      
      if (oldLen === 0 && newLen > 0) {
        // 纯插入
        operations.push({
          type: 'insert',
          position: oldStart,
          length: newLen
        })
      } else if (oldLen > 0 && newLen === 0) {
        // 纯删除
        operations.push({
          type: 'delete',
          position: oldStart,
          length: oldLen
        })
      } else if (oldLen > 0 && newLen > 0) {
        // 替换 = 删除 + 插入
        operations.push({
          type: 'delete',
          position: oldStart,
          length: oldLen
        })
        operations.push({
          type: 'insert',
          position: oldStart,
          length: newLen
        })
      }
    })
  })
  
  return operations
}

/**
 * 应用多个操作来转换光标位置
 */
export const applyOperationsToCursors = (
  cursors: Map<number, RemoteCursor>,
  operations: TextOperation[],
  excludeUserId: number | undefined,
  docSize: number
): Map<number, RemoteCursor> => {
  let currentCursors = cursors
  
  for (const op of operations) {
    currentCursors = transformAllCursors(currentCursors, op, excludeUserId, docSize)
  }
  
  return currentCursors
}

export const RemoteCursorExtension = Extension.create({
  name: 'remoteCursor',
  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: remoteCursorKey,
        state: {
          init: (): CursorState => ({ cursors: new Map(), version: 0 }),
          apply: (tr: Transaction, value: CursorState): CursorState => {
            const meta = tr.getMeta(remoteCursorKey)
            const docSize = tr.doc.content.size
            
            // 处理外部设置光标的操作
            if (meta?.type === 'setCursors') {
              const newCursors = new Map<number, RemoteCursor>()
              ;(meta.cursors as RemoteCursor[]).forEach(cursor => {
                newCursors.set(cursor.userId, {
                  ...cursor,
                  from: clampPosition(cursor.from, docSize),
                  to: clampPosition(cursor.to, docSize)
                })
              })
              return { cursors: newCursors, version: value.version + 1 }
            }
            
            // 更新单个光标
            if (meta?.type === 'updateCursor') {
              const cursor = meta.cursor as RemoteCursor
              const newCursors = new Map(value.cursors)
              newCursors.set(cursor.userId, {
                ...cursor,
                from: clampPosition(cursor.from, docSize),
                to: clampPosition(cursor.to, docSize)
              })
              return { cursors: newCursors, version: value.version + 1 }
            }
            
            // 删除光标
            if (meta?.type === 'removeCursor') {
              const newCursors = new Map(value.cursors)
              newCursors.delete(meta.userId as number)
              return { cursors: newCursors, version: value.version + 1 }
            }
            
            // 手动转换光标（带操作列表）
            if (meta?.type === 'transformCursors' && meta.operations) {
              const operations = meta.operations as TextOperation[]
              const excludeUserId = meta.excludeUserId as number | undefined
              const newCursors = applyOperationsToCursors(
                value.cursors,
                operations,
                excludeUserId,
                docSize
              )
              return { cursors: newCursors, version: value.version + 1 }
            }
            
            // 如果文档发生变化，自动转换光标
            if (tr.docChanged && value.cursors.size > 0) {
              const operations = extractOperationsFromTransaction(tr)
              if (operations.length > 0) {
                // 获取操作发起者（如果有的话）
                const localUserId = meta?.localUserId as number | undefined
                const newCursors = applyOperationsToCursors(
                  value.cursors,
                  operations,
                  localUserId,
                  docSize
                )
                return { cursors: newCursors, version: value.version + 1 }
              }
            }
            
            // 文档没变化，但需要确保光标在有效范围内
            if (value.cursors.size > 0) {
              let needsUpdate = false
              const newCursors = new Map<number, RemoteCursor>()
              
              value.cursors.forEach((cursor, userId) => {
                const clampedFrom = clampPosition(cursor.from, docSize)
                const clampedTo = clampPosition(cursor.to, docSize)
                if (clampedFrom !== cursor.from || clampedTo !== cursor.to) {
                  needsUpdate = true
                  newCursors.set(userId, { ...cursor, from: clampedFrom, to: clampedTo })
                } else {
                  newCursors.set(userId, cursor)
                }
              })
              
              if (needsUpdate) {
                return { cursors: newCursors, version: value.version + 1 }
              }
            }
            
            return value
          }
        },
        props: {
          decorations(state) {
            const data = remoteCursorKey.getState(state)
            if (!data || data.cursors.size === 0) return null
            
            const decorations: Decoration[] = []
            const docSize = state.doc.content.size
            
            data.cursors.forEach((cursor) => {
              const from = clampPosition(cursor.from, docSize)
              const to = clampPosition(cursor.to, docSize)
              
              if (from === to) {
                // 光标（插入点）
                const caret = document.createElement('span')
                caret.className = 'remote-caret'
                caret.style.borderLeftColor = cursor.color
                const label = document.createElement('span')
                label.className = 'remote-caret-label'
                label.textContent = cursor.name
                label.style.background = cursor.color
                caret.appendChild(label)
                decorations.push(Decoration.widget(from, caret, { 
                  key: `cursor-${cursor.userId}`,
                  side: 1
                }))
              } else {
                // 选区
                decorations.push(
                  Decoration.inline(from, to, {
                    class: 'remote-selection',
                    style: `background: ${cursor.color}33;`
                  })
                )
              }
            })
            return DecorationSet.create(state.doc, decorations)
          }
        }
      })
    ]
  }
})

// ========== 导出的 API 函数 ==========

/**
 * 设置所有远程光标
 */
export const setRemoteCursors = (editor: any, cursors: RemoteCursor[]) => {
  if (!editor?.view?.state) return
  const tr = editor.view.state.tr.setMeta(remoteCursorKey, { 
    type: 'setCursors', 
    cursors 
  })
  editor.view.dispatch(tr)
}

/**
 * 更新单个远程光标
 */
export const updateRemoteCursor = (editor: any, cursor: RemoteCursor) => {
  if (!editor?.view?.state) return
  const tr = editor.view.state.tr.setMeta(remoteCursorKey, { 
    type: 'updateCursor', 
    cursor 
  })
  editor.view.dispatch(tr)
}

/**
 * 移除远程光标
 */
export const removeRemoteCursor = (editor: any, userId: number) => {
  if (!editor?.view?.state) return
  const tr = editor.view.state.tr.setMeta(remoteCursorKey, { 
    type: 'removeCursor', 
    userId 
  })
  editor.view.dispatch(tr)
}

/**
 * 手动触发光标转换（当收到远程内容更新时使用）
 */
export const transformCursorsWithOperations = (
  editor: any,
  operations: TextOperation[],
  excludeUserId?: number
) => {
  if (!editor?.view?.state) return
  const tr = editor.view.state.tr.setMeta(remoteCursorKey, {
    type: 'transformCursors',
    operations,
    excludeUserId
  })
  editor.view.dispatch(tr)
}

/**
 * 获取当前所有远程光标
 */
export const getRemoteCursors = (editor: any): RemoteCursor[] => {
  if (!editor?.view?.state) return []
  const data = remoteCursorKey.getState(editor.view.state)
  return data ? Array.from(data.cursors.values()) : []
}

/**
 * 兼容旧接口
 */
export const updateRemoteCursors = (editor: any, cursors: RemoteCursor[]) => {
  setRemoteCursors(editor, cursors)
}

export { remoteCursorKey }
