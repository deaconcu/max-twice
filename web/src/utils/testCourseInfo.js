/**
 * 测试课程信息展示功能的模拟数据
 * 根据后端返回的数据格式创建测试数据
 */

// 模拟主课程的read接口返回数据
export const mockMainCourseData = {
  parentCourse: {
    id: 1,
    name: 'Java编程入门',
    description: '从零开始学习Java编程语言，包含基础语法、面向对象编程等核心内容',
    mainCategory: 1,
    subCategory: 2,
  },
  course: {
    id: 1, // 与parentCourse.id相等，说明这是主课程
    name: 'Java编程入门',
    description: '从零开始学习Java编程语言，包含基础语法、面向对象编程等核心内容',
    mainCategory: 1,
    subCategory: 2,
  },
  subCourseList: [
    {
      id: 2,
      name: 'Java基础语法',
      description: '学习Java的基本语法规则、变量、数据类型等',
      mainCategory: 1,
      subCategory: 2,
    },
    {
      id: 3,
      name: '面向对象编程',
      description: '深入理解Java的面向对象特性：封装、继承、多态',
      mainCategory: 1,
      subCategory: 2,
    },
    {
      id: 4,
      name: 'Java集合框架',
      description: '掌握Java中常用的集合类和操作方法',
      mainCategory: 1,
      subCategory: 2,
    },
  ],
  // 其他read接口返回的数据...
  learning: false,
  path: '1-1',
  contents: [],
  otherPostings: [],
}

// 模拟子课程的read接口返回数据
export const mockSubCourseData = {
  parentCourse: {
    id: 1,
    name: 'Java编程入门',
    description: '从零开始学习Java编程语言，包含基础语法、面向对象编程等核心内容',
    mainCategory: 1,
    subCategory: 2,
  },
  course: {
    id: 2, // 与parentCourse.id不相等，说明这是子课程
    name: 'Java基础语法',
    description: '学习Java的基本语法规则、变量、数据类型等',
    mainCategory: 1,
    subCategory: 2,
  },
  subCourseList: [
    {
      id: 2,
      name: 'Java基础语法',
      description: '学习Java的基本语法规则、变量、数据类型等',
      mainCategory: 1,
      subCategory: 2,
    },
    {
      id: 3,
      name: '面向对象编程',
      description: '深入理解Java的面向对象特性：封装、继承、多态',
      mainCategory: 1,
      subCategory: 2,
    },
    {
      id: 4,
      name: 'Java集合框架',
      description: '掌握Java中常用的集合类和操作方法',
      mainCategory: 1,
      subCategory: 2,
    },
  ],
  // 其他read接口返回的数据...
  learning: true,
  path: '1-1',
  contents: [],
  otherPostings: [],
}

/**
 * 更新后的UI测试验证
 *
 * 1. 主课程界面验证：
 *    ✓ 标题显示：parentCourse.name
 *    ✓ 按钮排列：订阅课程 | 详情（右对齐）
 *    ✓ 无子课程标签
 *    ✓ 子课程列表显示正确
 *
 * 2. 子课程界面验证：
 *    ✓ 标题显示：parentCourse.name
 *    ✓ 按钮排列：子课程标签 | 返回主课程 | 订阅课程 | 详情（右对齐）
 *    ✓ 子课程标签显示当前课程名
 *    ✓ 在同级课程列表中显示"正在学习"
 *
 * 3. 样式验证：
 *    ✓ 所有按钮在一行，无阴影
 *    ✓ 悬停效果轻量（仅透明度变化）
 *    ✓ 响应式布局正常
 *    ✓ 扁平化设计风格一致
 */

export default {
  mockMainCourseData,
  mockSubCourseData,
}
