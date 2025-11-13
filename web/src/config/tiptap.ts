/**
 * TipTap 富文本编辑器配置
 * 配置所有编辑器扩展和语法高亮
 */

import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import { Link } from '@tiptap/extension-link'
import { Image } from '@tiptap/extension-image'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import { Underline } from '@tiptap/extension-underline'
import { Subscript } from '@tiptap/extension-subscript'
import { Superscript } from '@tiptap/extension-superscript'
import { Highlight } from '@tiptap/extension-highlight'
import { TextStyle } from '@tiptap/extension-text-style'
import { Color } from '@tiptap/extension-color'
import { TextAlign } from '@tiptap/extension-text-align'
import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableHeader } from '@tiptap/extension-table-header'
import { TableCell } from '@tiptap/extension-table-cell'
import { createLowlight } from 'lowlight'

// 导入常用语言的语法高亮
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import cpp from 'highlight.js/lib/languages/cpp'
import csharp from 'highlight.js/lib/languages/csharp'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'
import sql from 'highlight.js/lib/languages/sql'
import json from 'highlight.js/lib/languages/json'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import markdown from 'highlight.js/lib/languages/markdown'
import bash from 'highlight.js/lib/languages/bash'

// 创建 lowlight 实例
const lowlight = createLowlight()

// 注册语言
lowlight.register('javascript', javascript)
lowlight.register('typescript', typescript)
lowlight.register('python', python)
lowlight.register('java', java)
lowlight.register('cpp', cpp)
lowlight.register('csharp', csharp)
lowlight.register('go', go)
lowlight.register('rust', rust)
lowlight.register('sql', sql)
lowlight.register('json', json)
lowlight.register('xml', xml)
lowlight.register('html', xml) // HTML 使用 XML 高亮
lowlight.register('css', css)
lowlight.register('markdown', markdown)
lowlight.register('bash', bash)
lowlight.register('sh', bash) // shell 使用 bash 高亮

/**
 * 获取 TipTap 编辑器扩展配置
 */
export function getTipTapExtensions(placeholder = '请在这里输入内容...') {
  return [
    // 基础功能包（段落、标题、粗体、斜体、删除线、代码、引用等）
    StarterKit.configure({
      codeBlock: false, // 禁用默认代码块，使用 CodeBlockLowlight
    }),

    // 占位符
    Placeholder.configure({
      placeholder,
    }),

    // 链接
    Link.configure({
      openOnClick: false, // 编辑时不打开链接
      HTMLAttributes: {
        class: 'tiptap-link',
      },
    }),

    // 图片
    Image.configure({
      HTMLAttributes: {
        class: 'tiptap-image',
      },
    }),

    // 代码块（带语法高亮）
    CodeBlockLowlight.configure({
      lowlight,
      HTMLAttributes: {
        class: 'tiptap-code-block',
      },
    }),

    // 下划线
    Underline,

    // 下标
    Subscript,

    // 上标
    Superscript,

    // 高亮（多色）
    Highlight.configure({
      multicolor: true,
    }),

    // 文本样式和颜色
    TextStyle,
    Color,

    // 文本对齐
    TextAlign.configure({
      types: ['heading', 'paragraph'],
    }),

    // 表格
    Table.configure({
      resizable: true,
    }),
    TableRow,
    TableHeader,
    TableCell,
  ]
}

/**
 * 支持的编程语言列表
 */
export const SUPPORTED_LANGUAGES = [
  { value: 'javascript', label: 'JavaScript' },
  { value: 'typescript', label: 'TypeScript' },
  { value: 'python', label: 'Python' },
  { value: 'java', label: 'Java' },
  { value: 'cpp', label: 'C++' },
  { value: 'csharp', label: 'C#' },
  { value: 'go', label: 'Go' },
  { value: 'rust', label: 'Rust' },
  { value: 'sql', label: 'SQL' },
  { value: 'json', label: 'JSON' },
  { value: 'xml', label: 'XML' },
  { value: 'html', label: 'HTML' },
  { value: 'css', label: 'CSS' },
  { value: 'markdown', label: 'Markdown' },
  { value: 'bash', label: 'Bash' },
  { value: 'sh', label: 'Shell' },
] as const

/**
 * TipTap 编辑器选项
 */
export interface TipTapOptions {
  placeholder?: string
  editable?: boolean
  autofocus?: boolean | 'start' | 'end' | number
}

/**
 * 默认编辑器选项
 */
export const DEFAULT_TIPTAP_OPTIONS: TipTapOptions = {
  placeholder: '请在这里输入内容...',
  editable: true,
  autofocus: false,
}
