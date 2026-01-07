import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from 'prosemirror-state'

export interface LineLengthLimitOptions {
  maxLength: number
}

const lineLengthLimitKey = new PluginKey('lineLengthLimit')

/**
 * 限制每行最大字符数的扩展
 * 当一行超过最大长度时，自动在单词边界处换行
 */
export const LineLengthLimitExtension = Extension.create<LineLengthLimitOptions>({
  name: 'lineLengthLimit',

  addOptions() {
    return {
      maxLength: 150
    }
  },

  addProseMirrorPlugins() {
    const maxLength = this.options.maxLength

    return [
      new Plugin({
        key: lineLengthLimitKey,
        appendTransaction(transactions, _oldState, newState) {
          // 只在文档内容发生变化时检查
          const hasDocChanges = transactions.some(tr => tr.docChanged)
          if (!hasDocChanges) return null

          let needsChange = false
          const positions: { pos: number; splitAt: number }[] = []

          // 遍历文档中的所有文本块
          newState.doc.descendants((node, pos) => {
            // 只处理段落节点
            if (node.type.name !== 'paragraph') return true

            const text = node.textContent
            if (text.length <= maxLength) return true

            // 计算需要拆分的位置
            let currentPos = 0
            while (currentPos < text.length) {
              const remaining = text.slice(currentPos)
              if (remaining.length <= maxLength) break

              // 在最大长度附近找到一个好的断点
              let breakPoint = maxLength

              // 优先在空格处断开
              const spaceIndex = remaining.lastIndexOf(' ', maxLength)
              if (spaceIndex > maxLength * 0.5) {
                breakPoint = spaceIndex + 1
              } else {
                // 如果找不到好的空格位置，强制在 maxLength 处断开
                breakPoint = maxLength
              }

              // 记录需要插入换行的位置
              // pos + 1 是因为段落节点内部偏移
              const absolutePos = pos + 1 + currentPos + breakPoint
              positions.push({ pos: absolutePos, splitAt: breakPoint })
              needsChange = true
              currentPos += breakPoint
            }

            return true
          })

          if (!needsChange || positions.length === 0) return null

          // 创建事务来插入换行
          let tr = newState.tr

          // 从后向前处理，避免位置偏移问题
          const sortedPositions = positions.sort((a, b) => b.pos - a.pos)

          for (const { pos } of sortedPositions) {
            // 安全检查位置
            if (pos < 0 || pos > tr.doc.content.size) continue

            try {
              // 在当前位置分割段落
              const $pos = tr.doc.resolve(pos)
              
              // 检查是否在段落内
              if ($pos.parent.type.name === 'paragraph') {
                // 使用 split 来创建新段落
                tr = tr.split(pos)
              }
            } catch (e) {
              // 忽略无效的分割操作
              console.warn('Line length limit: split failed at position', pos)
            }
          }

          if (tr.docChanged) {
            return tr
          }

          return null
        }
      })
    ]
  }
})
