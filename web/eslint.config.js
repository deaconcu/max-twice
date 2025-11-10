import js from '@eslint/js'
import vue from 'eslint-plugin-vue'
import ts from 'typescript-eslint'
import prettier from 'eslint-plugin-prettier/recommended'
import globals from 'globals'

export default ts.config(
  // ESLint 推荐规则
  js.configs.recommended,

  // TypeScript 严格规则
  ...ts.configs.strictTypeChecked,
  ...ts.configs.stylisticTypeChecked,

  // Vue 3 推荐规则
  ...vue.configs['flat/recommended'],

  // Prettier 集成
  prettier,

  {
    files: ['**/*.ts', '**/*.tsx', '**/*.vue'],
    // 语言选项
    languageOptions: {
      parserOptions: {
        parser: ts.parser,
        project: './tsconfig.app.json',
        tsconfigRootDir: import.meta.dirname,
        extraFileExtensions: ['.vue'],
        sourceType: 'module',
        ecmaVersion: 'latest',
      },
      globals: {
        ...globals.browser,
      },
    },

    // 自定义规则
    rules: {
      // Vue 规则
      'vue/multi-word-component-names': 'off', // 允许单词组件名
      'vue/no-v-html': 'warn', // v-html 警告而非错误
      'vue/require-default-prop': 'error', // 必须有默认值
      'vue/require-explicit-emits': 'error', // 必须显式声明 emits
      'vue/component-name-in-template-casing': ['error', 'PascalCase'], // 组件名 PascalCase
      'vue/custom-event-name-casing': ['error', 'camelCase'], // 事件名 camelCase
      'vue/define-macros-order': [
        'error',
        {
          // 宏顺序
          order: ['defineProps', 'defineEmits', 'defineSlots', 'defineExpose'],
        },
      ],
      'vue/no-unused-refs': 'error', // 未使用的 ref
      'vue/padding-line-between-blocks': ['error', 'always'], // 块之间空行

      // TypeScript 规则
      '@typescript-eslint/no-explicit-any': 'error', // 禁止 any
      '@typescript-eslint/explicit-function-return-type': 'off', // 允许推断返回类型
      '@typescript-eslint/no-floating-promises': 'off', // 允许不处理 Promise（如 router.push）
      '@typescript-eslint/no-unsafe-argument': 'off', // Vue 组件类型推断问题，暂时关闭
      '@typescript-eslint/no-unsafe-assignment': 'off', // Vue 组件类型推断问题，暂时关闭
      '@typescript-eslint/no-unsafe-member-access': 'off', // Vue 组件类型推断问题，暂时关闭
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          // 未使用变量
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
        },
      ],
      '@typescript-eslint/no-non-null-assertion': 'warn', // 非空断言警告

      // 通用规则
      'no-console': ['warn', { allow: ['warn', 'error'] }], // 警告 console.log
      'no-debugger': 'error', // 禁止 debugger
      'prefer-const': 'error', // 优先使用 const
      'no-var': 'error', // 禁止 var
    },
  },

  {
    // 忽略文件
    ignores: ['dist/**', 'node_modules/**', '*.config.js', '*.config.ts', '.vscode/**', '.idea/**'],
  }
)
