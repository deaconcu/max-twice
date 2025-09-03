import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import * as parserVue from 'vue-eslint-parser'
import configPrettier from 'eslint-config-prettier'
import pluginPrettier from 'eslint-plugin-prettier'

export default [
  {
    name: 'app/files-to-lint',
    files: ['**/*.{js,mjs,jsx,vue}'],
  },

  {
    name: 'app/files-to-ignore',
    ignores: [
      '**/dist/**',
      '**/dist-ssr/**',
      '**/coverage/**',
      '**/node_modules/**',
      '**/*.local',
      '**/npm-debug.log*',
      '**/yarn-debug.log*',
      '**/yarn-error.log*',
      '**/package-lock.json',
      '**/yarn.lock',
      '**/.env',
      '**/.env.*',
      '!**/.env.example',
      '**/*.tmp',
      '**/*.temp',
      '**/logs/**',
      '**/*.log',
      '**/.vscode/**',
      '**/.idea/**',
      '**/*.suo',
      '**/*.ntvs*',
      '**/*.njsproj',
      '**/*.sln',
      '**/*.sw?',
      '**/.DS_Store',
      '**/Thumbs.db',
      '**/.vite/**',
      '**/vite.config.js.timestamp-*',
    ],
  },

  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],

  {
    name: 'app/vue-rules',
    languageOptions: {
      parser: parserVue,
      ecmaVersion: 'latest',
      sourceType: 'module',
      parserOptions: {
        parser: '@babel/eslint-parser',
        requireConfigFile: false,
        babelOptions: {
          babelrc: false,
          configFile: false,
        },
      },
    },
    rules: {
      // ===== JavaScript 严格规则 =====

      // 代码质量
      'no-console': 'warn', // 警告console.log，但不完全禁止（开发时可能需要）
      'no-debugger': 'error', // 禁止debugger
      'no-alert': 'error', // 禁止alert
      'no-unused-vars': 'error', // 禁止未使用的变量
      'no-undef': 'error', // 禁止未定义的变量
      'prefer-const': 'error', // 优先使用const
      'no-var': 'error', // 禁止使用var

      // 代码风格
      'prefer-template': 'error', // 强制使用模板字符串
      'template-curly-spacing': 'error', // 模板字符串花括号间距
      'object-shorthand': 'error', // 对象简写
      'prefer-arrow-callback': 'error', // 优先使用箭头函数
      'arrow-spacing': 'error', // 箭头函数间距

      // 比较和条件
      eqeqeq: 'error', // 强制使用 === 和 !==
      'no-implicit-coercion': 'error', // 禁止隐式类型转换
      'no-unneeded-ternary': 'error', // 禁止不必要的三元运算符

      // 函数
      'func-style': ['error', 'expression'], // 强制使用函数表达式
      'prefer-rest-params': 'error', // 使用剩余参数而不是arguments
      'prefer-spread': 'error', // 使用扩展运算符

      // 数组和对象
      'no-array-constructor': 'error', // 禁止使用Array构造函数
      'no-new-object': 'error', // 禁止使用Object构造函数
      'prefer-destructuring': 'error', // 优先使用解构

      // ===== Vue 严格规则 =====

      // Vue 基础规则
      'vue/html-indent': ['error', 2], // HTML缩进2个空格
      'vue/max-attributes-per-line': ['error', { singleline: 3, multiline: 1 }], // 属性换行
      'vue/singleline-html-element-content-newline': 'off', // 允许单行内容
      'vue/multiline-html-element-content-newline': 'error', // 多行内容必须换行
      'vue/html-closing-bracket-newline': [
        'error',
        {
          singleline: 'never',
          multiline: 'always',
        },
      ],

      // Vue 命名规范
      'vue/component-name-in-template-casing': ['error', 'PascalCase'], // 组件名使用PascalCase
      'vue/component-definition-name-casing': ['error', 'PascalCase'], // 组件定义名PascalCase
      'vue/prop-name-casing': ['error', 'camelCase'], // prop名使用camelCase
      'vue/attribute-hyphenation': ['error', 'always'], // 属性使用kebab-case
      'vue/v-on-event-hyphenation': ['error', 'always'], // 事件名使用kebab-case

      // Vue Composition API
      'vue/no-setup-props-destructure': 'error', // 禁止解构props
      'vue/no-ref-as-operand': 'error', // ref作为操作数警告
      'vue/prefer-import-from-vue': 'error', // 优先从vue导入
      'vue/prefer-separate-static-class': 'error', // 分离静态class

      // Vue 最佳实践
      'vue/require-default-prop': 'error', // 要求默认值
      'vue/require-prop-types': 'error', // 要求prop类型
      'vue/no-unused-properties': [
        'error',
        {
          groups: ['props', 'data', 'computed', 'methods', 'setup'],
        },
      ], // 检查未使用的属性
      'vue/no-unused-refs': 'error', // 检查未使用的refs
      'vue/no-template-shadow': 'error', // 禁止模板变量遮蔽
      'vue/no-useless-v-bind': 'error', // 禁止无用的v-bind
      'vue/no-useless-mustaches': 'error', // 禁止无用的mustache
      'vue/no-static-inline-styles': 'error', // 禁止内联样式

      // Vue 性能
      'vue/no-v-html': 'warn', // 警告v-html使用
      'vue/require-v-for-key': 'error', // v-for必须有key
      'vue/no-use-v-if-with-v-for': 'error', // 禁止v-if和v-for同时使用

      // Vue 可访问性
      // 'vue/no-autofocus': 'error', // 禁止autofocus - 该规则可能不存在
      'vue/require-explicit-emits': 'error', // 要求明确声明emits

      // ===== 严格的导入规则 =====
      'sort-imports': [
        'error',
        {
          ignoreCase: false,
          ignoreDeclarationSort: true,
          ignoreMemberSort: false,
          memberSyntaxSortOrder: ['none', 'all', 'multiple', 'single'],
          allowSeparatedGroups: false,
        },
      ],
    },
  },

  // Prettier 集成
  configPrettier,
  {
    name: 'app/prettier',
    plugins: {
      prettier: pluginPrettier,
    },
    rules: {
      'prettier/prettier': 'error',
    },
  },
]
