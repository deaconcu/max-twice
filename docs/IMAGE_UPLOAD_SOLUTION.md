# 图片上传方案文档

## 目录
- [方案概述](#方案概述)
- [技术选型](#技术选型)
- [架构设计](#架构设计)
- [实施计划](#实施计划)
- [成本预估](#成本预估)
- [风险评估](#风险评估)
- [后续优化](#后续优化)

---

## 方案概述

### 现状
- 用户编辑文章时需要插入图片
- 当前方案：用户手动上传到第三方图床，粘贴链接
- 问题：操作繁琐，影响用户体验

### 目标
- 提供一键上传图片功能
- 用户在编辑器中直接上传，自动插入图片链接
- 成本可控，性能优秀
- 易于扩展和维护

### 方案选择
**使用 Cloudflare R2 + 自定义域名**

---

## 技术选型

### 为什么选择 Cloudflare R2？

#### 对比分析

| 指标 | Cloudflare R2 | AWS S3 | 阿里云 OSS | 又拍云 |
|------|--------------|---------|-----------|--------|
| **存储费用** | $0.015/GB/月 | $0.023/GB/月 | ¥0.12/GB/月 | ¥0.16/GB/月 |
| **流量费用** | **$0（免费）** | $0.09/GB | ¥0.5/GB | ¥0.29/GB |
| **免费额度** | 10GB 存储 | 5GB/12个月 | 新用户优惠 | 10GB+15GB流量 |
| **CDN 加速** | ✅ 免费全球 | ❌ 需付费 | ❌ 需付费 | ✅ 包含 |
| **API 兼容** | S3 兼容 | S3 原生 | 部分兼容 | 自有 API |
| **国内访问** | ⚠️ 需优化 | ⚠️ 较慢 | ✅ 快 | ✅ 快 |

#### 核心优势

1. **零流量费用** ⭐⭐⭐
   - 无论用户访问多少次图片，都不收费
   - 避免流量费爆炸的风险
   - 成本可预测：只有存储费

2. **全球 CDN 加速** ⭐⭐⭐
   - Cloudflare 拥有 300+ 全球边缘节点
   - 自动选择最近节点返回内容
   - 用户访问速度快

3. **成本极低** ⭐⭐⭐
   - 10GB 免费存储（约 10 万张图片）
   - 初期几乎零成本
   - 成长期月费 <$5（10 万用户规模）

4. **S3 兼容 API**
   - 使用成熟的 AWS S3 SDK
   - 大量现成的库和工具
   - 易于迁移

#### 月度成本对比（100GB 存储 + 10TB 流量）

```
Cloudflare R2:
  存储: 100GB × $0.015 = $1.50
  流量: 10TB × $0      = $0
  总计: $1.50/月

AWS S3:
  存储: 100GB × $0.023 = $2.30
  流量: 10TB × $90     = $900
  总计: $902.30/月

阿里云 OSS:
  存储: 100GB × ¥0.12  = ¥12
  流量: 10TB × ¥500    = ¥5000
  总计: ¥5012/月
```

---

## 架构设计

### 整体架构

```
┌─────────────┐
│   用户浏览器  │
└──────┬──────┘
       │
       │ 1. 上传图片
       ↓
┌─────────────────────────────┐
│   前端 (Vue 3 + Vite)        │
│   - 图片选择                  │
│   - 客户端压缩（可选）         │
│   - 调用后端 API              │
└──────┬──────────────────────┘
       │
       │ 2. POST /api/v1/upload/image
       ↓
┌─────────────────────────────┐
│   后端 (Spring Boot)         │
│   - 验证用户权限              │
│   - 验证文件类型/大小          │
│   - 图片压缩/优化（可选）      │
│   - 生成唯一文件名            │
│   - 上传到 R2                │
│   - 返回公开 URL             │
└──────┬──────────────────────┘
       │
       │ 3. S3 API (需要密钥)
       ↓
┌─────────────────────────────┐
│   Cloudflare R2 存储         │
│   存储桶: max-twice-images   │
│   ├── avatars/              │
│   ├── posts/                │
│   ├── courses/              │
│   └── roadmaps/             │
└──────┬──────────────────────┘
       │
       │ 4. 绑定自定义域名
       ↓
┌─────────────────────────────┐
│   Cloudflare CDN            │
│   域名: images.maxtwice.com │
│   - 全球边缘节点缓存          │
│   - 自动 HTTPS               │
│   - DDoS 防护                │
└──────┬──────────────────────┘
       │
       │ 5. 用户访问图片
       ↓
┌─────────────┐
│  用户浏览器   │
│  <img src=" │
│  https://   │
│  images.    │
│  maxtwice.  │
│  com/..."/> │
└─────────────┘
```

### 文件存储结构

```
存储桶: max-twice-images
├── avatars/                    # 用户头像
│   ├── user-1.jpg
│   ├── user-2.jpg
│   └── ...
├── posts/                      # 文章配图
│   ├── {uuid}-1.jpg
│   ├── {uuid}-2.jpg
│   └── ...
├── courses/                    # 课程封面
│   ├── course-1.jpg
│   ├── course-2.jpg
│   └── ...
└── roadmaps/                   # 路线图
    ├── roadmap-1.jpg
    └── ...
```

### URL 命名规范

```
头像:
https://images.maxtwice.com/avatars/user-{userId}-{timestamp}.jpg

文章配图:
https://images.maxtwice.com/posts/{uuid}.jpg

课程封面:
https://images.maxtwice.com/courses/{courseId}-{timestamp}.jpg

路线图:
https://images.maxtwice.com/roadmaps/{roadmapId}-{timestamp}.jpg
```

---

## 实施计划

### 阶段一：基础功能（1-2 天）

#### 1. Cloudflare 配置

**步骤**：
1. 注册 Cloudflare 账号（如已有则跳过）
2. 添加域名到 Cloudflare
3. 在域名注册商修改 NS 记录（指向 Cloudflare）
4. 创建 R2 存储桶：`max-twice-images`
5. 生成 API Token（Access Key + Secret Key）
6. 绑定自定义域名：`images.maxtwice.com`

**预期时间**：2-4 小时（包括 DNS 生效时间）

#### 2. 后端开发

**新增 Maven 依赖**：
```xml
<!-- AWS S3 SDK (R2 兼容) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.0</version>
</dependency>
```

**配置文件**：
```yaml
cloudflare:
  r2:
    account-id: ${R2_ACCOUNT_ID}
    access-key-id: ${R2_ACCESS_KEY_ID}
    secret-access-key: ${R2_SECRET_ACCESS_KEY}
    bucket-name: max-twice-images
    public-domain: https://images.maxtwice.com
    endpoint: https://${cloudflare.r2.account-id}.r2.cloudflarestorage.com
```

**新增代码**：
1. `R2Config.java` - R2 客户端配置
2. `ImageUploadService.java` - 图片上传服务
3. `ImageUploadController.java` - 上传接口
4. `ImageUploadDTO.java` - 响应对象

**预期时间**：4-6 小时

#### 3. 前端开发

**新增功能**：
1. 图片上传按钮（Tiptap 编辑器集成）
2. 上传进度显示
3. 错误处理
4. 自动插入图片到编辑器

**预期时间**：4-6 小时

### 阶段二：优化功能（2-3 天）

#### 1. 图片压缩
- 后端使用 Thumbnailator 压缩图片
- 强制压缩：任意边超过 2048px 等比例缩小
- 质量：85%
- 转换为 WebP 格式

#### 2. 文件验证
- 文件类型白名单：jpg, jpeg, png, webp（不允许 GIF）
- 文件大小限制：5MB
- 图片尺寸限制：上传时最大 4096x4096

#### 3. 安全增强
- 用户上传频率限制：1秒间隔，20张/分钟，100张/小时，200张/天
- 文件扫描（可选）
- 水印添加（可选）

### 阶段三：监控和运维（持续）

#### 1. 日志记录
- 上传成功/失败日志
- 文件大小统计
- 用户上传行为分析

#### 2. 监控指标
- R2 存储空间使用量
- 每日上传数量
- 失败率

#### 3. 告警机制
- 存储空间超过阈值告警
- 上传失败率异常告警

---

## 成本预估

**说明**：以下预估基于强制压缩后的实际大小（平均 150-200KB）

### 初期（0-1000 用户）

**存储预估**：
- 用户头像：1000 × 50KB = 50MB
- 文章配图：500 篇 × 3 张 × 150KB = 225MB
- 课程封面：100 门 × 100KB = 10MB
- **总计**：约 285MB

**月度费用**：
```
存储: 285MB（在 10GB 免费额度内）
流量: 无限（免费）
操作: 在免费额度内
总费用: $0/月
```

### 成长期（1000-10000 用户）

**存储预估**：
- 用户头像：10,000 × 50KB = 500MB
- 文章配图：5,000 篇 × 3 张 × 150KB = 2.25GB
- 课程封面：1,000 门 × 100KB = 100MB
- **总计**：约 2.85GB

**月度费用**：
```
存储: 2.85GB（在 10GB 免费额度内）
流量: 无限（免费）
操作: 在免费额度内
总费用: $0/月
```

### 成熟期（10000-100000 用户）

**存储预估**：
- 用户头像：100,000 × 50KB = 5GB
- 文章配图：50,000 篇 × 3 张 × 150KB = 22.5GB
- 课程封面：5,000 门 × 100KB = 500MB
- **总计**：约 28GB

**月度费用**：
```
存储:
  前 10GB: 免费
  超出 18GB: 18 × $0.015 = $0.27

流量: 无限（免费）

操作:
  上传: 约 15 万次/月（在 1000 万免费额度内）
  访问: 通过 CDN，不计入操作次数

总费用: $0.27/月
```

### 年度成本对比（基于强制压缩）

| 用户规模 | Cloudflare R2（压缩后）| AWS S3 | 阿里云 OSS |
|---------|--------------------|---------|-----------|
| 1,000   | $0/年              | ~$50/年  | ~¥300/年   |
| 10,000  | $0/年              | ~$500/年 | ~¥3000/年  |
| 100,000 | ~$3.24/年          | ~$5000/年 | ~¥30000/年 |

---

## 风险评估

### 技术风险

#### 1. Cloudflare 服务不稳定
**风险等级**：低
**原因**：
- Cloudflare 是全球最大的 CDN 之一
- SLA 保证 99.9%+ 可用性
- 极少出现大规模故障

**应对措施**：
- 监控 Cloudflare 状态页
- 准备备份方案（阿里云 OSS）
- 使用多 CDN 策略（长期）

#### 2. 国内访问速度慢
**风险等级**：中
**原因**：
- Cloudflare 在国内没有节点
- 部分地区可能被限速

**应对措施**：
- 监控国内用户访问速度
- 如果问题严重，考虑使用国内 CDN 加速
- 双 CDN 方案：国内用户 → 阿里云，国外用户 → Cloudflare

#### 3. S3 SDK 兼容性问题
**风险等级**：低
**原因**：
- R2 声称 100% S3 兼容
- 但可能存在边缘情况

**应对措施**：
- 充分测试上传、删除等功能
- 关注 Cloudflare 更新日志
- 保持 SDK 版本更新

### 成本风险

#### 1. 免费额度用尽
**风险等级**：低
**触发条件**：
- 存储超过 10GB
- A 类操作超过 1000 万次/月

**应对措施**：
- 监控存储使用量
- 设置告警阈值（8GB）
- 超出后费用仍然很低（$0.015/GB/月）

#### 2. 滥用上传（当作免费图床）⚠️
**风险等级**：高
**场景**：
- 恶意用户大量上传图片，当作免费图床使用
- 在其他网站引用我们的图片链接（盗链）
- 机器人批量上传
- 上传非法内容

**潜在影响**：
- 存储成本快速增长
- 带宽被滥用（虽然流量免费，但影响其他用户）
- 存储非法内容导致法律风险
- 服务器资源被占用

**应对措施**：

**1. 访问控制**
- ✅ **强制登录**：必须登录才能上传
- ✅ **无用户等级限制**：所有用户平等，不限制总量
- ✅ **防止滥用**：通过短期限制防止恶意行为
  - 1秒上传间隔：防止脚本自动化
  - 20张/分钟：防止短时间爆发
  - 100张/小时：防止持续滥用
  - 200张/天：防止当图床使用
  - 总量不限：鼓励长期创作

**2. 使用限制**
- ✅ **图片必须关联内容**：上传的图片必须被引用在文章/评论中
- ✅ **未使用图片清理**：上传后 24 小时未被引用，自动删除
- ✅ **删除文章时删除图片**：避免僵尸图片
- ✅ **上传频率限制**：
  - 最小间隔 1 秒（防止脚本）
  - 1 分钟最多 20 张（防止短时间爆发）
  - 1 小时最多 100 张（防止持续滥用）
  - 1 天最多 200 张（防止当图床使用）

**3. 防盗链**
- ✅ **Referer 检查**：只允许从自己域名引用
- ✅ **签名 URL**（可选）：图片 URL 包含时效性签名
- ✅ **热链接检测**：监控外部引用，超过阈值封禁

**4. 内容审核**
- ✅ **文件类型白名单**：只允许 jpg/png/webp
- ✅ **文件大小限制**：单个文件最大 5MB
- ✅ **图片尺寸限制**：上传时最大 4096x4096
- ✅ **强制压缩**：转换为 WebP 格式，85% 质量，任意边超过 2048px 等比例缩小
- ✅ **内容识别**（可选）：使用 AI 识别违规内容
- ✅ **人工审核**：可疑上传进入审核队列

**5. 监控和告警**
- ✅ **存储空间监控**：接近免费额度时告警
- ✅ **异常上传监控**：单用户单日300+张图片预警
- ✅ **用户行为分析**：识别异常用户模式
- ✅ **成本预警**：存储费用超过预算告警

**6. 数据库记录 + Redis 配额**

**MySQL 数据表**（持久化存储，用于记录和清理）
```sql
-- 记录所有上传（仅用于记录和清理，不做配额限制）
CREATE TABLE image_uploads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255),
    file_size BIGINT,
    file_url VARCHAR(512) NOT NULL,
    ref_type VARCHAR(50) COMMENT 'post/comment/avatar/course/roadmap',
    ref_id BIGINT COMMENT '关联的资源ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0:未使用 1:使用中',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL COMMENT '首次被引用时间',

    -- 索引设计（复合索引提高选择性）
    UNIQUE INDEX idx_file_url (file_url),
    INDEX idx_status_created (status, created_at),
    INDEX idx_ref (ref_type, ref_id),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_user_type (user_id, ref_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**状态说明**：
- `status = 0`（未使用）：上传后未关联到内容，24小时后自动清理
- `status = 1`（使用中）：已关联到内容，长期保留
- 移除 `status = 2`：物理删除时直接删除记录，不需要"已删除"状态

**图片类型说明**：
- `post/comment`：内容配图，上传时 status=0，保存内容时改为 status=1
- `avatar/course/roadmap`：头像/封面，上传时直接 status=1，替换时删除旧图

**索引设计理由**：
- `idx_file_url`：唯一索引，保存内容时快速查找图片
- `idx_status_created`：复合索引，定时清理未使用图片（高效过滤 status=0）
- `idx_ref`：复合索引，删除内容时查找关联图片
- `idx_user_type`：用户的特定类型图片（如查找用户当前头像）

-- 不需要用户等级字段，所有用户平等
```

**Redis 配额管理**（高性能，实时，仅用于防止滥用）
```redis
# 上传间隔检查（防止脚本）
user:upload:last:{userId}              = timestamp
TTL: 1秒

# 每分钟上传计数（防止短时间爆发）
user:upload:minute:{userId}            = 当前上传数
TTL: 1分钟（自动重置）

# 每小时上传计数（防止持续滥用）
user:upload:hour:{userId}              = 计数
TTL: 1小时

# 每日上传计数（防止当图床使用）
user:upload:daily:{userId}:{date}      = 当前上传数
TTL: 24小时（自动重置）

# 不再需要总量计数，鼓励长期创作
```

**配额配置**（统一配置，不存储在 Redis）
- 配置方式：在 Java 代码中定义常量，或从 `application.yml` 读取
- 全局统一：所有用户共享相同配额，无等级区分
- 设计理念：以防止滥用为目标，而非限制创作
```yaml
# application.yml
upload:
  quota:
    min-interval: 1       # 最小上传间隔（秒）- 防止脚本
    minute-limit: 20      # 每分钟限额 - 防止短时间爆发
    hour-limit: 100       # 每小时限额 - 防止持续滥用
    daily-limit: 200      # 每日限额 - 防止当图床使用
    total-limit: -1       # 总量不限 - 鼓励长期创作

  file:
    max-size: 5242880     # 单文件最大 5MB
    max-dimension: 4096   # 上传时最大尺寸（像素）
    allowed-types:        # 允许的文件类型
      - image/jpeg
      - image/png
      - image/webp

  compression:
    enabled: true         # 强制压缩
    max-width: 2048       # 最大宽度（任意边超过等比例缩小）
    max-height: 2048      # 最大高度（任意边超过等比例缩小）
    quality: 0.85         # 压缩质量 85%
    format: webp          # 输出格式 WebP

  cleanup:
    unused-hours: 24      # 未使用图片清理时间
```

**7. 后端实现示例（简化版，无等级限制）**
```java
@Service
public class ImageUploadService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ImageUploadRepository imageRepository;

    // 配额配置（从 application.yml 读取）
    @Value("${upload.quota.min-interval:1}")
    private int minInterval;

    @Value("${upload.quota.minute-limit:20}")
    private int minuteLimit;

    @Value("${upload.quota.hour-limit:100}")
    private int hourLimit;

    @Value("${upload.quota.daily-limit:200}")
    private int dailyLimit;

    /**
     * 检查用户配额（只防止恶意滥用，不限制正常创作）
     */
    public void checkUserQuota(Long userId) {
        String today = LocalDate.now().toString();

        // 1. 检查上传间隔（防止脚本）
        String lastKey = "user:upload:last:" + userId;
        String lastTime = redisTemplate.opsForValue().get(lastKey);
        if (lastTime != null) {
            long seconds = System.currentTimeMillis() / 1000
                         - Long.parseLong(lastTime);
            if (seconds < minInterval) {
                throw new RateLimitException("上传过于频繁，请稍后再试");
            }
        }

        // 2. 检查每分钟限额（防止短时间爆发）
        String minuteKey = "user:upload:minute:" + userId;
        Integer minuteCount = getCount(minuteKey);
        if (minuteCount >= minuteLimit) {
            throw new RateLimitException(
                String.format("上传速度过快，请1分钟后再试（限制：%d张/分钟）", minuteLimit)
            );
        }

        // 3. 检查每小时限额（防止持续滥用）
        String hourKey = "user:upload:hour:" + userId;
        Integer hourCount = getCount(hourKey);
        if (hourCount >= hourLimit) {
            throw new RateLimitException(
                String.format("上传量异常，请1小时后再试（限制：%d张/小时）", hourLimit)
            );
        }

        // 4. 检查每日限额（防止当图床使用）
        String dailyKey = "user:upload:daily:" + userId + ":" + today;
        Integer dailyCount = getCount(dailyKey);
        if (dailyCount >= dailyLimit) {
            throw new QuotaExceededException(
                String.format("今日上传量已达上限，请明天再试（限制：%d张/天）", dailyLimit)
            );
        }

        // 无总量限制，鼓励长期创作
    }

    /**
     * 记录上传（无总量累计）
     */
    public void recordUpload(Long userId, String fileUrl, long fileSize) {
        String today = LocalDate.now().toString();
        long now = System.currentTimeMillis() / 1000;

        // 1. 每分钟计数
        String minuteKey = "user:upload:minute:" + userId;
        redisTemplate.opsForValue().increment(minuteKey, 1);
        redisTemplate.expire(minuteKey, Duration.ofMinutes(1));

        // 2. 每小时计数
        String hourKey = "user:upload:hour:" + userId;
        redisTemplate.opsForValue().increment(hourKey, 1);
        redisTemplate.expire(hourKey, Duration.ofHours(1));

        // 3. 每日计数
        String dailyKey = "user:upload:daily:" + userId + ":" + today;
        redisTemplate.opsForValue().increment(dailyKey, 1);
        redisTemplate.expire(dailyKey, Duration.ofHours(24));

        // 4. 最后上传时间
        String lastKey = "user:upload:last:" + userId;
        redisTemplate.opsForValue().set(lastKey, String.valueOf(now),
                                        Duration.ofSeconds(minInterval));

        // 5. 持久化到数据库（用于统计和清理，不做配额限制）
        ImageUpload upload = new ImageUpload();
        upload.setUserId(userId);
        upload.setFileUrl(fileUrl);
        upload.setFileSize(fileSize);
        upload.setStatus(0); // 未使用
        upload.setCreatedAt(LocalDateTime.now());
        imageRepository.save(upload);
    }

    /**
     * 获取用户今日配额信息（仅展示）
     */
    public QuotaInfo getUserQuota(Long userId) {
        String today = LocalDate.now().toString();
        String dailyKey = "user:upload:daily:" + userId + ":" + today;
        int dailyUsed = getCount(dailyKey);

        return new QuotaInfo(dailyUsed, dailyLimit);
    }

    /**
     * 获取计数（处理 null）
     */
    private Integer getCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Integer.parseInt(value);
    }

    /**
     * 清理未使用的图片（定时任务）
     * 只清理内容配图（post/comment），不清理头像/封面
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    public void cleanupUnusedImages() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<ImageUpload> unused = imageRepository
            .findByStatusAndCreatedAtBefore(0, cutoff);

        log.info("清理未使用图片，共 {} 张", unused.size());

        for (ImageUpload image : unused) {
            try {
                // 从 R2 物理删除
                s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(extractKeyFromUrl(image.getFileUrl()))
                    .build());

                // 从数据库物理删除（不需要标记为已删除）
                imageRepository.delete(image);

            } catch (Exception e) {
                log.error("删除图片失败: {}", image.getFileUrl(), e);
            }
        }
    }

    /**
     * 标记图片为已使用（保存文章时调用）
     */
    public void markImagesAsUsed(List<String> imageUrls, Long refId, String refType) {
        for (String url : imageUrls) {
            ImageUpload image = imageRepository.findByFileUrl(url);
            if (image != null && image.getStatus() == 0) {
                image.setStatus(1);
                image.setRefId(refId);
                image.setRefType(refType);
                image.setUsedAt(LocalDateTime.now());
                imageRepository.save(image);
            }
        }
    }

    /**
     * 上传头像（替换时删除旧头像）
     */
    @Transactional
    public String uploadAvatar(Long userId, byte[] data) {
        // 1. 删除旧头像
        ImageUpload oldAvatar = imageRepository
            .findByUserIdAndRefTypeAndStatus(userId, "avatar", 1);
        if (oldAvatar != null) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(extractKeyFromUrl(oldAvatar.getFileUrl()))
                    .build());
                imageRepository.delete(oldAvatar);
            } catch (Exception e) {
                log.error("删除旧头像失败: {}", oldAvatar.getFileUrl(), e);
            }
        }

        // 2. 上传新头像
        String url = s3Client.upload(data, "avatars");

        // 3. 记录新头像（直接 status = 1）
        ImageUpload newAvatar = new ImageUpload();
        newAvatar.setUserId(userId);
        newAvatar.setFileUrl(url);
        newAvatar.setRefType("avatar");
        newAvatar.setRefId(userId);
        newAvatar.setStatus(1);  // 立即标记为使用中
        newAvatar.setUsedAt(LocalDateTime.now());
        imageRepository.save(newAvatar);

        return url;
    }

    /**
     * 上传课程封面（替换时删除旧封面）
     */
    @Transactional
    public String uploadCourseCover(Long courseId, byte[] data) {
        // 1. 删除旧封面
        List<ImageUpload> oldCovers = imageRepository
            .findByRefTypeAndRefId("course", courseId);
        for (ImageUpload old : oldCovers) {
            if (old.getStatus() == 1) {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(extractKeyFromUrl(old.getFileUrl()))
                        .build());
                    imageRepository.delete(old);
                } catch (Exception e) {
                    log.error("删除旧封面失败: {}", old.getFileUrl(), e);
                }
            }
        }

        // 2. 上传新封面
        String url = s3Client.upload(data, "courses");

        // 3. 记录新封面（直接 status = 1）
        ImageUpload newCover = new ImageUpload();
        newCover.setUserId(getCurrentUserId());
        newCover.setFileUrl(url);
        newCover.setRefType("course");
        newCover.setRefId(courseId);
        newCover.setStatus(1);  // 立即标记为使用中
        newCover.setUsedAt(LocalDateTime.now());
        imageRepository.save(newCover);

        return url;
    }

    private String extractKeyFromUrl(String url) {
        // https://images.maxtwice.com/posts/abc.jpg → posts/abc.jpg
        return url.substring(url.indexOf(".com/") + 5);
    }
}

/**
 * 配额信息（简化版）
 */
@Data
@AllArgsConstructor
public class QuotaInfo {
    private Integer dailyUsed;      // 今日已使用
    private Integer dailyLimit;     // 今日限额

    public Integer getDailyRemaining() {
        return dailyLimit - dailyUsed;
    }
}
```

**图片类型处理策略总结**：

| 图片类型 | ref_type | 上传时状态 | 清理策略 | 说明 |
|---------|----------|-----------|---------|------|
| **文章配图** | post | status=0 | 24小时未使用自动删除 | 上传时未关联，保存文章时标记 |
| **评论配图** | comment | status=0 | 24小时未使用自动删除 | 同文章配图 |
| **用户头像** | avatar | status=1 | 替换时删除旧头像 | 上传时立即关联到用户 |
| **课程封面** | course | status=1 | 替换时删除旧封面 | 上传时立即关联到课程 |
| **路线图** | roadmap | status=1 | 替换时删除旧图 | 上传时立即关联到路线图 |

**Repository 方法**：
```java
@Repository
public interface ImageUploadRepository extends JpaRepository<ImageUpload, Long> {
    // 定时清理未使用图片
    List<ImageUpload> findByStatusAndCreatedAtBefore(Integer status, LocalDateTime createdAt);

    // 保存文章时查找图片
    ImageUpload findByFileUrl(String fileUrl);

    // 删除内容时查找关联图片
    List<ImageUpload> findByRefTypeAndRefId(String refType, Long refId);

    // 上传头像时查找旧头像
    ImageUpload findByUserIdAndRefTypeAndStatus(Long userId, String refType, Integer status);
}
```

**配置文件（application.yml）**
```yaml
upload:
  quota:
    min-interval: 1              # 最小上传间隔（秒）
    minute-limit: 20             # 每分钟限额
    hour-limit: 100              # 每小时限额
    daily-limit: 200             # 每日限额

  file:
    max-size: 5242880            # 单文件最大 5MB
    max-dimension: 4096          # 上传时最大尺寸
    allowed-types:
      - image/jpeg
      - image/png
      - image/webp

  compression:
    enabled: true                # 强制压缩
    max-width: 2048              # 最大宽度（任意边超过等比例缩小）
    max-height: 2048             # 最大高度
    quality: 0.85                # 85% 质量
    format: webp                 # WebP 格式

  cleanup:
    unused-hours: 24             # 未使用图片清理时间
```

**为什么使用 Redis？**

| 指标 | Redis | MySQL |
|------|-------|-------|
| **性能** | 超快（内存操作） | 较慢（磁盘IO） |
| **并发** | 原子操作，无锁 | 需要加锁 |
| **TTL** | 原生支持自动过期 | 需要定时任务清理 |
| **INCR** | 天然原子操作 | 需要事务 |
| **成本** | 极低（内存计数） | 高（频繁写入） |

**Redis 优势**：
1. ✅ **高性能**：每秒 10 万+ 次操作
2. ✅ **原子性**：INCR 命令线程安全
3. ✅ **自动过期**：每日/每小时计数自动重置
4. ✅ **简单**：无需复杂的定时任务
5. ✅ **低成本**：减轻数据库压力

**8. 前端防护**
```typescript
// 限制上传频率（客户端防护）
const uploadRateLimiter = {
  lastUpload: 0,
  minInterval: 1000, // 1秒

  canUpload(): boolean {
    const now = Date.now()
    if (now - this.lastUpload < this.minInterval) {
      return false
    }
    this.lastUpload = now
    return true
  }
}

// 上传前检查
async function uploadImage(file: File) {
  // 检查文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    showToast('仅支持 JPG、PNG、WebP 格式', 'error')
    return
  }

  // 检查上传频率
  if (!uploadRateLimiter.canUpload()) {
    showToast('上传过于频繁，请稍后再试', 'warning')
    return
  }

  // 检查文件大小
  if (file.size > 5 * 1024 * 1024) {
    showToast('图片大小不能超过 5MB', 'error')
    return
  }

  // 执行上传...
}
```

**9. Cloudflare 配置防盗链**
```javascript
// Cloudflare Workers (可选)
export default {
  async fetch(request, env) {
    const referer = request.headers.get('Referer')

    // 检查 Referer
    if (!referer || !referer.includes('maxtwice.com')) {
      return new Response('Forbidden', { status: 403 })
    }

    // 正常返回图片
    return env.BUCKET.get(key)
  }
}
```

**10. 成本控制阈值**
```yaml
# 监控配置
monitoring:
  storage:
    warning-threshold: 8GB      # 80% 免费额度
    critical-threshold: 9.5GB   # 95% 免费额度

  daily-upload:
    warning-threshold: 1000     # 单日上传超过 1000 张
    critical-threshold: 5000    # 单日上传超过 5000 张

  user-upload:
    suspicious-threshold: 300   # 单用户单日超过 300 张（人工审核）
    ban-threshold: 500          # 单用户单日超过 500 张（自动封禁）
```

### 业务风险

#### 1. 依赖单一服务商
**风险等级**：中
**原因**：
- 所有图片存储在 Cloudflare
- 如果服务变更，迁移成本高

**应对措施**：
- 使用自定义域名（便于迁移）
- 定期备份图片到其他存储
- 抽象存储层，便于切换

#### 2. 数据丢失
**风险等级**：极低
**原因**：
- Cloudflare R2 有数据冗余
- 云服务商很少出现数据丢失

**应对措施**：
- 重要图片定期备份
- 保留用户上传记录
- 支持用户重新上传

---

## 后续优化

### 短期优化（1-3 个月）

#### 1. 图片处理
- **自动压缩**：后端自动压缩大图片
- **格式转换**：自动转换为 WebP 格式（节省 30% 存储）
- **缩略图生成**：列表页使用小图，详情页使用原图

#### 2. 性能优化
- **懒加载**：图片滚动到可视区域才加载
- **渐进式加载**：先显示模糊图，再加载清晰图
- **响应式图片**：根据设备分辨率加载不同尺寸

#### 3. 用户体验
- **拖拽上传**：支持拖拽图片到编辑器
- **粘贴上传**：支持 Ctrl+V 粘贴图片
- **批量上传**：一次上传多张图片

### 中期优化（3-6 个月）

#### 1. 图片管理
- **图片库**：用户可以查看自己上传的所有图片
- **图片搜索**：按标签、时间搜索图片
- **图片删除**：用户可以删除不需要的图片

#### 2. 智能处理
- **水印添加**：自动添加网站水印
- **EXIF 清理**：移除敏感地理位置信息
- **图片识别**：识别违规内容（可选）

#### 3. 监控分析
- **使用统计**：每日上传量、存储空间趋势
- **热门图片**：统计访问量最高的图片
- **成本预测**：预测未来 3 个月的存储成本

### 长期优化（6-12 个月）

#### 1. 多 CDN 策略
- **国内 CDN**：使用阿里云/腾讯云 CDN 加速国内访问
- **智能调度**：根据用户地理位置自动选择最优 CDN
- **成本优化**：根据流量成本动态调整 CDN 配置

#### 2. 高级功能
- **图片编辑**：裁剪、旋转、滤镜等
- **AI 增强**：自动优化图片质量
- **视频支持**：扩展到视频上传

#### 3. 备份策略
- **多地域备份**：关键图片备份到多个存储
- **增量备份**：定期增量备份到对象存储
- **灾难恢复**：建立完整的灾难恢复流程

---

## 决策建议

### 推荐方案：Cloudflare R2

**理由**：
1. ✅ **零流量费用**：最大优势，成本可控
2. ✅ **全球 CDN**：用户访问速度快
3. ✅ **免费额度充足**：初期零成本
4. ✅ **S3 兼容**：成熟的生态系统
5. ✅ **易于迁移**：使用自定义域名，随时可切换

**适用场景**：
- 初创项目（成本敏感）
- 全球用户（需要 CDN）
- 图片密集型应用（大量流量）

### 备选方案：阿里云 OSS + CDN

**使用条件**：
- 如果国内用户反馈 Cloudflare 访问慢
- 如果需要备案（国内业务）

**混合方案**：
- 主存储：Cloudflare R2（国外用户）
- 镜像存储：阿里云 OSS（国内用户）
- 智能 DNS：根据用户 IP 返回不同 CDN

---

## 总结

### 核心优势
- 💰 **成本极低**：初期免费，10 万用户时月费仅 $0.27
- 🚀 **性能优秀**：全球 CDN 加速，强制压缩优化加载速度
- 🔒 **安全可靠**：Cloudflare 基础设施，SLA 保证
- 🛠️ **易于实施**：S3 兼容 API，成熟的 SDK
- 📈 **可扩展性**：轻松支持百万用户级别
- 🎨 **自动优化**：强制压缩为 WebP 格式，节省 70% 存储

### 关键指标
- **初期成本**：$0/月（免费额度内）
- **成长期成本**：$0/月（10,000 用户，免费额度内）
- **成熟期成本**：$0.27/月（100,000 用户）
- **实施时间**：2-3 天
- **技术难度**：中等（有成熟 SDK）
- **维护成本**：低
- **平均图片大小**：150-200KB（压缩后）
- **用户体验**：iPhone 拍照支持，无需手动压缩

### 建议
**立即开始实施 Cloudflare R2 方案**

---

## 附录

### 参考资源
- [Cloudflare R2 官方文档](https://developers.cloudflare.com/r2/)
- [AWS S3 SDK for Java](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [Cloudflare R2 定价](https://developers.cloudflare.com/r2/pricing/)

### 相关文档
- `IMAGE_UPLOAD_GUIDE.md` - 用户使用指南
- `BACKEND_IMPLEMENTATION.md` - 后端实现文档
- `FRONTEND_IMPLEMENTATION.md` - 前端实现文档

---

**文档版本**：v1.0
**创建时间**：2025-12-16
**更新时间**：2025-12-16
**负责人**：开发团队
