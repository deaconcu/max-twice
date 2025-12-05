# 验证规则配置系统设计文档

## 1. 背景

### 1.1 问题
- 当前验证规则硬编码在 `application.yml` 中
- 前端验证规则需要手动与后端保持一致，容易出现不同步
- 前端无法获取后端的验证配置，导致重复定义

### 1.2 目标
- 前端通过 API 获取验证规则，保证前后端一致
- 配置集中在 `application.yml` 统一管理
- 支持环境变量覆盖，便于不同环境使用不同配置
- 实现简单，维护成本低

### 1.3 方案选型

经过评估，选择**方案 C - 配置文件方案**：

| 方案 | 优点 | 缺点 | 适用场景 |
|-----|------|------|---------|
| A. 配置中心 | 功能完善、热更新 | 复杂度高、过度设计 | 大型微服务 |
| B. 数据库表 | 灵活、可审计 | 需要新表、管理成本 | 需要动态调整 |
| **C. 配置文件** ✅ | **简单、易维护** | **需重启生效** | **验证规则稳定** |

**选择理由**：
- ✅ 验证规则属于产品设计决策，改动频率低（几个月一次）
- ✅ 不需要运营人员动态调整
- ✅ 实现最简单，维护成本最低
- ✅ 配置在代码仓库，有版本历史，便于追溯
- ✅ 支持渐进式演进，未来需要时可升级

## 2. 技术方案

### 2.1 架构设计

```
┌─────────────────────────┐
│    application.yml      │ ← 验证规则配置（唯一来源）
│  + 环境变量覆盖支持      │
└───────────┬─────────────┘
            ↓
┌─────────────────────────┐
│   SystemProperties      │ ← Spring 配置类
└───────────┬─────────────┘
            ↓
┌─────────────────────────┐
│ ValidationConfigService │ ← 服务层（应用启动时加载）
└───────────┬─────────────┘
            ↓
┌─────────────────────────┐
│   内存缓存（不变）       │ ← 启动后不变，无需刷新
└───────────┬─────────────┘
            ↓
┌─────────────────────────┐
│  API 接口               │ → 提供给前端
│  GET /api/v1/config/    │
│      validation         │
└─────────────────────────┘
            ↓
┌─────────────────────────┐
│  前端 (localStorage)    │ ← 按版本号缓存
└─────────────────────────┘
```

### 2.2 配置文件设计

#### application.yml 配置结构

```yaml
# learn-api/src/main/resources/application.yml
system:
  validation:
    # 卡片相关
    card-front-min-length: ${CARD_FRONT_MIN:5}      # 支持环境变量覆盖
    card-front-max-length: ${CARD_FRONT_MAX:500}
    card-back-min-length: ${CARD_BACK_MIN:1}
    card-back-max-length: ${CARD_BACK_MAX:500}
    deck-title-min-length: 2
    deck-title-max-length: 30
    deck-description-max-length: 200

    # 评论相关
    comment-content-min-length: 1
    comment-content-max-length: 1000

    # 用户相关
    username-min-length: 2
    username-max-length: 20
    password-min-length: 6
    password-max-length: 50
    biography-max-length: 200
    email-max-length: 100

    # 课程相关
    course-name-min-length: 2
    course-name-max-length: 50
    course-description-min-length: 10
    course-description-max-length: 500

    # 帖子相关
    post-content-min-length: 10
    post-content-max-length: 50000

    # 职业相关
    profession-name-min-length: 2
    profession-name-max-length: 50
    profession-description-min-length: 10
    profession-description-max-length: 500

    # 消息相关
    message-content-min-length: 1
    message-content-max-length: 1000

    # 路线图相关
    roadmap-content-min-length: 1
    roadmap-content-max-length: 50000
    roadmap-description-min-length: 10
    roadmap-description-max-length: 500
```

#### 环境变量覆盖示例

```bash
# 开发环境：宽松的验证规则
export CARD_FRONT_MIN=1
export CARD_FRONT_MAX=1000

# 生产环境：严格的验证规则（使用默认值）
# 不设置环境变量，使用 application.yml 中的默认值
```

### 2.3 字段中文名称映射

在代码中维护字段的中文名称映射（已在 `ConfigurableSizeValidator` 中实现）：

```java
// ConfigurableSizeValidator.java
private String getFieldLabel(String configKey) {
    switch (configKey) {
        case "card-front": return "问题";
        case "card-back": return "答案";
        case "deck-title": return "卡片组标题";
        case "deck-description": return "卡片组描述";
        case "comment-content": return "评论内容";
        // ... 其他映射
        default: return "字段";
    }
}
```

## 3. 实现细节

### 3.1 后端实现

#### 3.1.1 DTO 类

```java
// ValidationRuleDTO.java
package com.prosper.learn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRuleDTO {
    /**
     * 最小长度
     */
    private Integer minLength;

    /**
     * 最大长度
     */
    private Integer maxLength;

    /**
     * 字段中文名称
     */
    private String label;
}
```

#### 3.1.2 Service 层

```java
// ValidationConfigService.java
package com.prosper.learn.business.service.application;

import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.dto.response.ValidationRuleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 验证规则配置服务
 * 从 application.yml 读取验证规则，提供给前端
 */
@Service
@RequiredArgsConstructor
public class ValidationConfigService {

    private final SystemProperties systemProperties;

    private Map<String, ValidationRuleDTO> cachedRules;

    /**
     * 应用启动时初始化配置
     */
    @PostConstruct
    public void init() {
        this.cachedRules = loadFromYaml();
    }

    /**
     * 从 application.yml 加载所有验证规则
     */
    private Map<String, ValidationRuleDTO> loadFromYaml() {
        Map<String, ValidationRuleDTO> rules = new HashMap<>();
        SystemProperties.Validation validation = systemProperties.getValidation();

        // 卡片相关
        rules.put("card-front", ValidationRuleDTO.builder()
                .minLength(validation.getCardFrontMinLength())
                .maxLength(validation.getCardFrontMaxLength())
                .label("问题")
                .build());

        rules.put("card-back", ValidationRuleDTO.builder()
                .minLength(validation.getCardBackMinLength())
                .maxLength(validation.getCardBackMaxLength())
                .label("答案")
                .build());

        rules.put("deck-title", ValidationRuleDTO.builder()
                .minLength(validation.getDeckTitleMinLength())
                .maxLength(validation.getDeckTitleMaxLength())
                .label("卡片组标题")
                .build());

        rules.put("deck-description", ValidationRuleDTO.builder()
                .minLength(0)
                .maxLength(validation.getDeckDescriptionMaxLength())
                .label("卡片组描述")
                .build());

        // 评论相关
        rules.put("comment-content", ValidationRuleDTO.builder()
                .minLength(validation.getCommentContentMinLength())
                .maxLength(validation.getCommentContentMaxLength())
                .label("评论内容")
                .build());

        // 用户相关
        rules.put("username", ValidationRuleDTO.builder()
                .minLength(validation.getUsernameMinLength())
                .maxLength(validation.getUsernameMaxLength())
                .label("用户名")
                .build());

        rules.put("password", ValidationRuleDTO.builder()
                .minLength(validation.getPasswordMinLength())
                .maxLength(validation.getPasswordMaxLength())
                .label("密码")
                .build());

        rules.put("biography", ValidationRuleDTO.builder()
                .minLength(0)
                .maxLength(validation.getBiographyMaxLength())
                .label("个人简介")
                .build());

        rules.put("email", ValidationRuleDTO.builder()
                .minLength(0)
                .maxLength(validation.getEmailMaxLength())
                .label("邮箱")
                .build());

        // 课程相关
        rules.put("course-name", ValidationRuleDTO.builder()
                .minLength(validation.getCourseNameMinLength())
                .maxLength(validation.getCourseNameMaxLength())
                .label("课程名称")
                .build());

        rules.put("course-description", ValidationRuleDTO.builder()
                .minLength(validation.getCourseDescriptionMinLength())
                .maxLength(validation.getCourseDescriptionMaxLength())
                .label("课程描述")
                .build());

        // 帖子相关
        rules.put("post-content", ValidationRuleDTO.builder()
                .minLength(validation.getPostContentMinLength())
                .maxLength(validation.getPostContentMaxLength())
                .label("帖子内容")
                .build());

        // 职业相关
        rules.put("profession-name", ValidationRuleDTO.builder()
                .minLength(validation.getProfessionNameMinLength())
                .maxLength(validation.getProfessionNameMaxLength())
                .label("职业名称")
                .build());

        rules.put("profession-description", ValidationRuleDTO.builder()
                .minLength(validation.getProfessionDescriptionMinLength())
                .maxLength(validation.getProfessionDescriptionMaxLength())
                .label("职业描述")
                .build());

        // 消息相关
        rules.put("message-content", ValidationRuleDTO.builder()
                .minLength(validation.getMessageContentMinLength())
                .maxLength(validation.getMessageContentMaxLength())
                .label("消息内容")
                .build());

        // 路线图相关
        rules.put("roadmap-content", ValidationRuleDTO.builder()
                .minLength(validation.getRoadmapContentMinLength())
                .maxLength(validation.getRoadmapContentMaxLength())
                .label("路线图内容")
                .build());

        rules.put("roadmap-description", ValidationRuleDTO.builder()
                .minLength(validation.getRoadmapDescriptionMinLength())
                .maxLength(validation.getRoadmapDescriptionMaxLength())
                .label("路线图描述")
                .build());

        return rules;
    }

    /**
     * 获取所有验证规则
     */
    public Map<String, ValidationRuleDTO> getAllRules() {
        return new HashMap<>(cachedRules);
    }

    /**
     * 获取指定字段的验证规则
     */
    public ValidationRuleDTO getRule(String fieldKey) {
        return cachedRules.get(fieldKey);
    }
}
```

#### 3.1.3 Controller

```java
// ConfigController.java
package com.prosper.learn.web.v1.controller;

import com.prosper.learn.web.v1.dto.ApiResponse;
import com.prosper.learn.business.service.application.ValidationConfigService;
import com.prosper.learn.dto.response.ValidationRuleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 配置接口控制器
 */
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ValidationConfigService validationConfigService;

    /**
     * 获取所有验证规则配置
     * GET /api/v1/config/validation
     *
     * 返回格式:
     * {
     *   "card-front": {
     *     "minLength": 5,
     *     "maxLength": 500,
     *     "label": "问题"
     *   },
     *   "card-back": {
     *     "minLength": 1,
     *     "maxLength": 500,
     *     "label": "答案"
     *   },
     *   ...
     * }
     */
    @GetMapping("/validation")
    public ApiResponse<Map<String, ValidationRuleDTO>> getValidationRules() {
        Map<String, ValidationRuleDTO> rules = validationConfigService.getAllRules();
        return ApiResponse.success(rules);
    }
}
```

### 3.2 前端实现

#### 3.2.1 配置加载

```typescript
// src/config/validation.ts
import { ref } from 'vue'
import { apiClient } from '@/api/client'

export interface ValidationRule {
  minLength: number
  maxLength: number
  label: string
}

export type ValidationConfig = Record<string, ValidationRule>

const validationConfig = ref<ValidationConfig | null>(null)

/**
 * 加载验证配置
 */
export async function loadValidationConfig(): Promise<ValidationConfig> {
  // 优先从 localStorage 读取缓存
  const cached = localStorage.getItem('validation-config')
  const cacheVersion = localStorage.getItem('validation-config-version')

  // TODO: 从服务器获取最新版本号，比对是否需要更新

  if (cached && cacheVersion === __APP_VERSION__) {
    validationConfig.value = JSON.parse(cached)
    return validationConfig.value
  }

  // 从服务器加载
  const response = await apiClient.get<ValidationConfig>('/v1/config/validation')
  validationConfig.value = response.data

  // 缓存到 localStorage
  localStorage.setItem('validation-config', JSON.stringify(response.data))
  localStorage.setItem('validation-config-version', __APP_VERSION__)

  return validationConfig.value
}

/**
 * 获取验证配置
 */
export function getValidationConfig(): ValidationConfig {
  if (!validationConfig.value) {
    throw new Error('Validation config not loaded. Call loadValidationConfig() first.')
  }
  return validationConfig.value
}

/**
 * 创建 Vuetify 验证规则
 */
export function createValidationRules(fieldKey: string) {
  const config = getValidationConfig()
  const rule = config[fieldKey]

  if (!rule) {
    console.warn(`Validation rule not found for field: ${fieldKey}`)
    return []
  }

  const rules: Array<(v: string) => boolean | string> = []

  // 必填验证
  if (rule.minLength > 0) {
    rules.push(v => !!v || `${rule.label}不能为空`)
  }

  // 最小长度验证
  if (rule.minLength > 0) {
    rules.push(
      v => (v && v.length >= rule.minLength) ||
      `${rule.label}长度必须至少${rule.minLength}个字符`
    )
  }

  // 最大长度验证
  rules.push(
    v => (!v || v.length <= rule.maxLength) ||
    `${rule.label}长度不能超过${rule.maxLength}个字符`
  )

  return rules
}
```

#### 3.2.2 应用入口初始化

```typescript
// src/main.ts
import { createApp } from 'vue'
import App from './App.vue'
import { loadValidationConfig } from '@/config/validation'

async function bootstrap() {
  // 加载验证配置
  await loadValidationConfig()

  // 创建应用
  const app = createApp(App)
  app.mount('#app')
}

bootstrap()
```

#### 3.2.3 组件中使用

```vue
<script setup lang="ts">
import { createValidationRules, getValidationConfig } from '@/config/validation'

const config = getValidationConfig()

// 生成验证规则
const frontRules = createValidationRules('card-front')
const backRules = createValidationRules('card-back')
</script>

<template>
  <v-textarea
    v-model="cardForm.front"
    label="问题（卡片正面）"
    placeholder="输入问题..."
    :counter="config['card-front'].maxLength"
    :rules="frontRules"
    variant="outlined"
    rounded="lg"
  />

  <v-textarea
    v-model="cardForm.back"
    label="答案（卡片背面）"
    placeholder="输入答案..."
    :counter="config['card-back'].maxLength"
    :rules="backRules"
    variant="outlined"
    rounded="lg"
  />
</template>
```

## 4. 配置更新流程

### 4.1 修改配置文件

```yaml
# learn-api/src/main/resources/application.yml
system:
  validation:
    card-front-min-length: 3  # 从 5 改为 3
    card-front-max-length: 500
```

### 4.2 重新部署应用

```bash
# 方式1: 重启应用
./mvnw spring-boot:run

# 方式2: 通过环境变量覆盖（无需修改配置文件）
export CARD_FRONT_MIN=3
./mvnw spring-boot:run
```

### 4.3 前端自动更新

- 前端检测到应用版本变化时，自动清除 localStorage 缓存
- 下次刷新页面时重新加载最新配置

### 4.4 配置生效流程

```
修改 application.yml 或设置环境变量
         ↓
    重启应用
         ↓
@PostConstruct 自动加载配置到内存
         ↓
  前端调用 API 获取最新配置
         ↓
    缓存到 localStorage
```

## 5. 配置优先级

```
环境变量 > application.yml
```

- 如果设置了环境变量，使用环境变量的值
- 如果没有设置环境变量，使用 application.yml 中的默认值
- 启动时全部加载到内存，运行期间配置不变（无需缓存刷新）

## 6. 扩展性

### 6.1 未来可以添加的功能

1. **管理后台界面**
   - 可视化配置编辑
   - 配置历史记录
   - 配置回滚

2. **配置分组**
   - 按环境分组（开发/测试/生产）
   - 按模块分组

3. **配置热更新**
   - WebSocket 推送配置变更
   - 前端自动刷新

4. **配置审计**
   - 记录谁在什么时候修改了什么配置
   - 配置变更通知

### 6.2 字段扩展

当前 JSON 结构支持灵活扩展：

```json
{
  "minLength": 5,
  "maxLength": 500,
  "label": "问题",
  "description": "卡片正面的问题描述",  // 可扩展
  "pattern": "^[\\s\\S]*$",             // 可扩展正则
  "errorMessage": "自定义错误消息"       // 可扩展
}
```

## 7. 注意事项

### 7.1 性能优势

- ✅ 启动时加载到内存，运行时零查询开销
- ✅ 前端缓存到 localStorage，减少 API 请求
- ✅ 配置不变，无需缓存失效机制
- ✅ 无数据库依赖，系统更简单

### 7.2 配置变更流程

- ⚠️ 配置修改需要重启应用生效
- ✅ 可通过环境变量覆盖，无需修改代码
- ✅ 配置在代码仓库，有完整的版本历史
- ✅ 前端通过应用版本号判断是否需要更新

### 7.3 最佳实践

1. **配置管理**
   - 配置文件提交到 Git，便于追溯和回滚
   - 敏感配置使用环境变量，不提交到代码库
   - 不同环境使用不同的环境变量文件

2. **版本管理**
   - 前端应用版本号与配置绑定
   - 版本号变化时，清除前端缓存
   - 建议使用语义化版本号（如 v1.2.3）

3. **文档维护**
   - 配置变更需要更新文档
   - 重要配置变更需要通知相关人员
   - 保持前后端团队对配置的理解一致

## 8. 实施计划

### Phase 1: 基础功能（已完成）
- [x] 设计文档
- [x] 创建 ValidationRuleDTO
- [x] 实现 ValidationConfigService
- [x] 创建 ConfigController（支持 ETag）
- [x] 创建前端 Pinia Store（支持持久化）
- [x] 应用启动时初始化配置
- [x] 路由守卫定期检查更新（3分钟节流）
- [x] CreateDeckDialog 组件集成验证规则
- [ ] 测试验证

### Phase 2: 推广使用
- [ ] 其他表单组件接入验证规则（评论、帖子等）
- [ ] 统一全局验证规则
- [ ] 清理硬编码的验证逻辑

### Phase 3: 优化升级（按需）
- [ ] 根据实际需求评估是否需要升级到数据库方案
- [ ] 如需动态配置，实施数据库方案
- [ ] 如需配置中心，接入 Nacos/Apollo

## 9. 测试计划

### 9.1 单元测试
- ValidationConfigService 从 application.yml 加载逻辑
- 所有字段的验证规则正确性
- 环境变量覆盖功能

### 9.2 集成测试
- ConfigController API 接口返回数据正确性
- 前端调用 API 获取配置成功
- localStorage 缓存机制

### 9.3 E2E 测试
- 前端表单验证与后端验证一致性
- 错误消息显示正确（中文名称+长度限制）
- 提交表单时后端验证生效

### 9.4 测试用例

| 测试场景 | 预期结果 |
|---------|---------|
| 前端输入4个字符的问题 | 前端提示"问题长度必须至少5个字符" |
| 前端输入501个字符的问题 | 前端提示"问题长度不能超过500个字符" |
| 绕过前端验证提交4个字符 | 后端返回"问题长度必须在5-500字符之间，当前长度：4" |
| 修改配置为3个字符，重启应用 | 3个字符的问题可以正常提交 |
| 设置环境变量 CARD_FRONT_MIN=1 | 1个字符的问题可以正常提交 |
| 前端首次加载 | 调用 API 获取配置并缓存到 localStorage |
| 前端再次加载（版本号未变） | 直接从 localStorage 读取，不调用 API |
| 应用版本号更新 | 清除 localStorage，重新获取配置 |

---

**文档版本**: v2.0（方案 C - 配置文件方案）
**创建日期**: 2025-11-25
**最后更新**: 2025-11-25
**方案选型**: 配置文件方案（支持环境变量覆盖）
