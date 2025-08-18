# 课程信息展示功能实现

## 功能概述

根据后端read接口返回的新数据格式，实现了课程信息区的正确展示：

```javascript
// 后端返回数据格式
Map<String, Object> data = {
    "parentCourse": parentCourse,     // CourseDTOV2 类型
    "course": course,                 // CourseDTOV2 类型  
    "subCourseList": subCourseList   // List<CourseDTOV2> 类型
}
```

## 实现逻辑

### 1. 标题显示
- **标题来源**：从 `parentCourse.name` 中获取
- **备用方案**：如果没有 `parentCourse`，则显示 `course.name`

### 2. 课程类型判断
- **主课程**：`course.id === parentCourse.id`
- **子课程**：`course.id !== parentCourse.id`

### 3. 显示差异

#### 主课程 (isMainCourse = true)
- 标题：显示 `parentCourse.name`
- 子课程标签：不显示
- 子课程列表标题：显示 "子课程列表"
- 申请子课程按钮：显示
- 返回主课程按钮：不显示
- 订阅对象：当前课程

#### 子课程 (isMainCourse = false)
- 标题：显示 `parentCourse.name`
- 子课程标签：显示 "子课程: {course.name}"
- 子课程列表标题：显示 "同级课程"  
- 申请子课程按钮：不显示
- 返回主课程按钮：显示
- 订阅对象：父课程
- 正在学习标签：在子课程列表中，当前课程显示 "正在学习"

### 4. 子课程列表显示
- **数据来源**：从 `subCourseList` 中获取
- **正在学习标识**：在子课程中，当 `subcourse.id === course.id` 时显示 "正在学习" 标签

## 修改的文件

### `/src/views/Read.vue`

#### 新增响应式变量
```javascript
const subCourseList = ref([]);     // 子课程列表
const isMainCourse = ref(true);    // 是否为主课程
```

#### 修改数据处理逻辑
```javascript
// 处理新的数据格式
if (response.data.parentCourse) {
  parentCourseInfo.value = response.data.parentCourse;
}

if (response.data.subCourseList) {
  subCourseList.value = response.data.subCourseList;
}

// 判断是否为主课程
if (response.data.course && response.data.parentCourse) {
  isMainCourse.value = response.data.course.id === response.data.parentCourse.id;
} else {
  isMainCourse.value = true;
}
```

#### 模板更新
- 标题显示逻辑更新
- 子课程标签条件显示
- 子课程列表数据源更新
- 按钮显示条件调整
- 订阅功能优化

## 测试数据

参考 `/src/utils/testCourseInfo.js` 中的模拟数据：
- `mockMainCourseData`：主课程测试数据
- `mockSubCourseData`：子课程测试数据

## 向后兼容性

- 保留了原有的函数结构
- 注释了不再使用的函数而非删除
- 新增的变量有合理的默认值
- 兼容没有父课程信息的情况

## 使用方法

1. 确保后端read接口返回包含 `parentCourse`、`course`、`subCourseList` 的数据
2. 前端会自动根据数据结构判断课程类型并正确展示
3. 用户交互功能（订阅、跳转等）会根据课程类型选择正确的目标

## 注意事项

- 订阅功能：子课程会订阅父课程而非自身
- 跳转功能：返回主课程会跳转到父课程页面
- 数据完整性：需要确保后端返回的数据结构完整
