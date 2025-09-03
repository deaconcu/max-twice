# 代码规范与质量控制

## 📋 必须遵守的规则

### 🚨 错误级别规则（必须修复）

#### JavaScript 基础规则

- **禁止使用 `debugger`** - 生产代码中不允许调试断点
- **禁止使用 `alert()`** - 使用更友好的用户提示方式
- **禁止未使用的变量** - 删除所有未使用的变量声明
- **禁止未定义的变量** - 所有变量必须先声明
- **强制使用 `===` 和 `!==`** - 禁止使用 `==` 和 `!=`
- **优先使用 `const`** - 不会重新赋值的变量必须使用 `const`
- **禁止使用 `var`** - 统一使用 `let` 和 `const`

#### 代码风格规则

- **强制使用模板字符串** - 字符串拼接必须使用 `` `${variable}` `` 而不是
  `"string" + variable`
- **对象简写** - 对象方法和属性必须使用简写形式
- **箭头函数优先** - 回调函数优先使用箭头函数
- **解构赋值优先** - 适合的场景必须使用解构赋值

#### Vue 专用规则

- **组件命名 PascalCase** - 组件名必须使用大驼峰命名

  ```vue
  <!-- ✅ 正确 -->
  <MyComponent />
  <!-- ❌ 错误 -->
  <myComponent />
  <my-component />
  ```

- **Props命名 camelCase** - 组件props必须使用小驼峰命名

  ```javascript
  // ✅ 正确
  defineProps({
    userName: String,
    isActive: Boolean,
  })
  // ❌ 错误
  defineProps({
    user_name: String,
    'is-active': Boolean,
  })
  ```

- **属性命名 kebab-case** - HTML属性必须使用短横线命名

  ```vue
  <!-- ✅ 正确 -->
  <MyComponent user-name="john" is-active />
  <!-- ❌ 错误 -->
  <MyComponent userName="john" isActive />
  ```

- **禁止解构props** - Composition API中不能解构props

  ```javascript
  // ✅ 正确
  const props = defineProps({...})
  console.log(props.userName)

  // ❌ 错误
  const { userName } = defineProps({...})
  console.log(userName)
  ```

- **必须声明emits** - 所有emit事件必须在defineEmits中声明

  ```javascript
  // ✅ 正确
  const emit = defineEmits(['update:modelValue', 'submit'])
  emit('submit', data)

  // ❌ 错误
  const emit = defineEmits()
  emit('randomEvent', data) // 未声明的事件
  ```

- **必须为props提供类型和默认值**
  ```javascript
  // ✅ 正确
  defineProps({
    title: {
      type: String,
      required: true,
    },
    count: {
      type: Number,
      default: 0,
    },
  })
  ```

### ⚠️ 警告级别规则（强烈建议修复）

- **console.log 使用** - 开发时允许，但生产环境应该移除
- **v-html 使用** - 需要确保内容安全，防止XSS攻击

### 📐 代码格式规则

#### Prettier 自动格式化规则

- **无分号** - 语句结尾不使用分号
- **单引号** - 字符串使用单引号 `'string'`
- **2空格缩进** - 使用2个空格进行缩进
- **行宽100字符** - 每行最多100个字符
- **对象尾逗号** - ES5语法中使用尾逗号
- **Vue组件缩进** - `<script>` 和 `<style>` 标签内容缩进

## 🛠️ 可用命令

### 代码检查命令

```bash
# 检查并自动修复ESLint问题
npm run lint

# 只检查ESLint问题，不自动修复
npm run lint:check

# 格式化所有文件
npm run format

# 检查代码格式，不自动修复
npm run format:check

# TypeScript类型检查
npm run typecheck

# 提交前完整检查（自动运行lint和format）
npm run prepare
```

### 开发工作流程

#### 1. 开发时

```bash
# 启动开发服务器
npm run dev

# 实时检查代码（建议在另一个终端运行）
npm run lint:check
```

#### 2. 提交前

```bash
# 完整检查和修复
npm run prepare

# 或分步执行
npm run lint
npm run format
npm run typecheck
```

#### 3. 构建前

```bash
# 确保代码质量
npm run lint:check
npm run format:check
npm run typecheck

# 构建项目
npm run build
```

## 🎯 VS Code 集成设置

### 必需插件

- **ESLint** - 实时错误提示
- **Prettier** - 代码格式化
- **Vetur** 或 **Volar** - Vue支持

### 推荐设置 (settings.json)

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "eslint.validate": ["javascript", "vue"],
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "[vue]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  }
}
```

## 🚫 禁止事项

### 绝对不允许

- ❌ 提交包含 `debugger` 的代码
- ❌ 提交包含 `alert()` 的代码
- ❌ 忽略ESLint错误提示（除非有充分理由）
- ❌ 直接修改配置文件绕过规则
- ❌ 提交格式不正确的代码

### 生产环境禁止

- ❌ `console.log` 语句
- ❌ 测试用的硬编码数据
- ❌ 未使用的导入和变量

## 📝 常见错误修复示例

### 字符串拼接 → 模板字符串

```javascript
// ❌ 错误
console.log('user id: ' + userId.value)

// ✅ 正确
console.log(`user id: ${userId.value}`)
```

### var → const/let

```javascript
// ❌ 错误
var userName = 'john'
var userList = []

// ✅ 正确
const userName = 'john'
let userList = []
```

### == → ===

```javascript
// ❌ 错误
if (status == 'active') {
}

// ✅ 正确
if (status === 'active') {
}
```

### 对象简写

```javascript
// ❌ 错误
const user = {
  name: name,
  getId: function () {
    return this.id
  },
}

// ✅ 正确
const user = {
  name,
  getId() {
    return this.id
  },
}
```

## 🔄 团队协作规范

### 代码提交流程

1. **开发完成** → 运行 `npm run lint`
2. **修复所有错误** → 运行 `npm run format`
3. **最终检查** → 运行 `npm run typecheck`
4. **提交代码** → git会自动运行 `npm run prepare`

### Code Review 要点

- 检查是否遵循命名规范
- 确认没有未使用的变量和导入
- 验证Vue组件结构符合规范
- 确保没有hardcode的调试代码

---

**📌 重要提醒：这些规则旨在提升代码质量和团队协作效率，请严格遵守！**
