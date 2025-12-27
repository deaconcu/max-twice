# 职业管理接口测试用例

> 测试实现：`ProfessionsControllerTest.java`
> API 文档：`docs/api/profession.md`

---

## Command 测试（写操作）

### 1. 创建职业 (POST /api/v1/professions)

**测试方法**: `testCreateProfession()`

**测试场景**:
- ✅ 成功创建职业（所有字段完整）
- ✅ 字段验证：name 为空
- ✅ 字段验证：description 为空
- ✅ 字段验证：mainCategory 缺失
- ✅ 参数验证：mainCategory = 0
- ✅ 权限验证：未登录创建

**验证点**:
- 职业状态为 SUBMITTED（待审核）
- creatorId 正确填充
- icon, reason 默认为空字符串

---

## Query 测试（读操作）

### 2. 获取职业详情 (GET /api/v1/professions/{id})

**测试方法**: `testGetProfessionDetail()`

**测试场景**:
- ✅ 获取已发布职业（未登录）
- ✅ 获取已发布职业（已登录）
- ✅ 职业不存在（返回404）
- ✅ 职业ID验证：ID = 0
- ✅ 职业ID验证：ID = -1

**验证点**:
- 返回完整职业信息
- 包含 name, description, skills, mainCategory, subCategory 等字段

---

### 3. 获取职业列表 (GET /api/v1/professions)

**测试方法**: `testGetProfessionList()`

**测试场景**:
- ✅ 获取所有已发布职业（无筛选条件）
- ✅ 按主分类筛选（mainCategory）
- ✅ 按主分类+子分类筛选
- ✅ 分页功能（lastId）
- ✅ 参数验证：mainCategory = 0
- ✅ 参数验证：subCategory = -1

**验证点**:
- 只返回 PUBLISHED 状态的职业
- 分类筛选准确
- 游标分页正确（ID < lastId）

---

### 4. 搜索职业 (GET /api/v1/professions/search)

**测试方法**: `testSearchProfessions()`

**测试场景**:
- ✅ 搜索成功（找到匹配职业）
- ✅ 搜索无结果（返回空数组）
- ✅ 关键词验证：空字符串
- ✅ 关键词验证：只有空格

**验证点**:
- 搜索结果包含关键词（不区分大小写）
- 只返回已发布职业

---

### 5. 获取热门职业 (GET /api/v1/professions/hot)

**测试方法**: `testGetHotProfessions()`

**测试场景**:
- ✅ 默认数量（不传 limit）
- ✅ 自定义数量（limit=3）
- ✅ limit 参数验证：limit = 0
- ✅ limit 参数验证：limit = -1
- ✅ 无热门数据时返回空列表

**验证点**:
- 返回数量不超过 limit
- 只返回已发布职业

---

## 边界测试

### 6. 边界场景

**测试场景**:
- 空数据库查询（返回空数组）
- 大量数据分页（验证性能）

---

## 软删除测试

### 7. 软删除机制

**测试场景**:
- 删除后不在查询中出现
- 删除后获取详情返回 404
- 重复删除不报错（幂等性）

**验证点**:
- 数据库记录仍存在（deleted_at 不为 NULL）
- 所有查询接口自动过滤已删除记录

---

## 参数验证汇总

### 8. 通用参数验证

| 接口 | 参数 | 无效值 | 预期错误码 |
|------|------|--------|-----------|
| 获取详情 | id | 0 | 422 |
| 获取详情 | id | -1 | 422 |
| 获取详情 | id | 99999 | 404 |
| 获取列表 | mainCategory | 0 | 422 |
| 获取列表 | mainCategory | -1 | 422 |
| 获取列表 | subCategory | 0 | 422 |
| 获取列表 | subCategory | -1 | 422 |
| 搜索 | keyword | 空字符串 | 422 |
| 搜索 | keyword | 只有空格 | 422 |
| 热门 | limit | 0 | 422 |
| 热门 | limit | -1 | 422 |
| 创建 | name | 空字符串 | 422 |
| 创建 | description | 空字符串 | 422 |
| 创建 | mainCategory | 缺失 | 422 |
| 创建 | mainCategory | 0 | 422 |

---

## 权限测试

### 9. 权限验证

| 接口 | 未登录 | 预期错误码 |
|------|--------|-----------|
| POST /professions | ❌ | 401 |
| GET /professions/{id} | ✅ 允许 | - |
| GET /professions | ✅ 允许 | - |
| GET /professions/search | ✅ 允许 | - |
| GET /professions/hot | ✅ 允许 | - |

---

## 测试覆盖率要求

- **接口覆盖率**: 100%（所有公开接口）
- **分支覆盖率**: >= 80%（核心业务逻辑）
- **异常场景覆盖**:
  - ✅ 参数验证失败
  - ✅ 资源不存在
  - ✅ 权限不足
  - ✅ 软删除过滤

---

## 运行测试

```bash
# 运行职业测试
mvn test -Dtest=ProfessionsControllerTest

# 运行所有测试
mvn test

# 生成测试报告
mvn test jacoco:report
```
