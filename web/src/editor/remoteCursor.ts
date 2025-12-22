import { Extension } from '@tiptap/core'
import { Plugin, PluginKey, Transaction } from 'prosemirror-state'
import { Decoration, DecorationSet } from 'prosemirror-view'

export interface RemoteCursor {
  userId: number
  name: string
  color: string
  from: number
  to: number
}

const remoteCursorKey = new PluginKey('remoteCursor')

export const RemoteCursorExtension = Extension.create({
  name: 'remoteCursor',
  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: remoteCursorKey,
        state: {
          init: () => ({ cursors: [] as RemoteCursor[] }),
          apply: (tr: Transaction, value) => {
            const meta = tr.getMeta(remoteCursorKey)
            if (meta && meta.cursors) {
              return { cursors: meta.cursors as RemoteCursor[] }
            }
            return value
          }
        },
        props: {
          decorations(state) {
            const data = remoteCursorKey.getState(state) as { cursors: RemoteCursor[] }
            if (!data || data.cursors.length === 0) return null
            const decorations: Decoration[] = []
            data.cursors.forEach((cursor) => {
              const from = Math.min(cursor.from, state.doc.content.size)
              const to = Math.min(cursor.to, state.doc.content.size)
              if (from === to) {
                const caret = document.createElement('span')
                caret.className = 'remote-caret'
                caret.style.borderLeftColor = cursor.color
                const label = document.createElement('span')
                label.className = 'remote-caret-label'
                label.textContent = cursor.name
                label.style.background = cursor.color
                caret.appendChild(label)
                decorations.push(Decoration.widget(from, caret, { key: `cursor-${cursor.userId}` }))
              } else {
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

export const updateRemoteCursors = (editor: any, cursors: RemoteCursor[]) => {
  const tr = editor.view.state.tr.setMeta(remoteCursorKey, { cursors })
  editor.view.dispatch(tr)
}
