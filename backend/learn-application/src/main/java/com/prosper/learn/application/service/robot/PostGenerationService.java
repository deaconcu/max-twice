package com.prosper.learn.application.service.robot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.application.dto.request.CreateDeckRequest;
import com.prosper.learn.application.dto.request.CreatePostRequest;
import com.prosper.learn.application.service.MemoryCardDeckService;
import com.prosper.learn.application.service.PostService;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.infrastructure.ai.AIService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

/**
 * Robot 生成服务
 *
 * 段落说明：
 * 1) 对接本地 opencode：先创建 session，再发送 ChatInput 获取模型输出
 * 2) 解析输出（只接受 JSON）得到决策：ARTICLE 或 DIRECTORY
 * 3) 按决策调用 PostService.createPost 入库：
 *    - ARTICLE：content=markdown
 *    - DIRECTORY：content=JSON 数组的子节点标题，交由 PostService 内部批量建子节点并写 contents
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostGenerationService {

    // ========= 依赖与配置 =========

    private final SystemProperties systemProperties;
    private final NodeDataService nodeDataService;
    private final CourseDataService courseDataService;
    private final PostService postService;
    private final PostDataService postDataService;
    private final UserDataService userDataService;
    private final MemoryCardDeckService memoryCardDeckService;
    private final PostQueueService postQueueService;
    private final ObjectMapper objectMapper;

    // AI 服务
    private final AIService aiService;

    // ========= 对外入口 =========

    /**
     * 为指定节点生成内容（外层执行器负责幂等与重试）
     * @param nodeId 节点ID
     * @param contentType 内容类型 (auto/index/article)
     * @param recursive 是否递归生成子节点
     */
    public void generateForNode(long nodeId, String contentType, boolean recursive) {
        long aiUserId = systemProperties.getRobot().getAiUserId();
        NodeDO nodeDO = nodeDataService.validateAndGet(nodeId);
        CourseDO courseDO = courseDataService.validateAndGet(nodeDO.getCourseId());

        String systemPrompt = buildSystemPrompt(contentType);
        String prompt = switch (contentType) {
            case "index" -> buildPromptForIndex(nodeDO, courseDO);
            case "article" -> buildPromptForArticle(nodeDO, courseDO);
            default -> buildPromptForAuto(nodeDO, courseDO);
        };

        // 根据配置选择 AI provider
        String response = aiService.generateContent(prompt, systemPrompt);
        log.info("AI response length: {}", response != null ? response.length() : 0);

        Long postId = handleResponse(nodeId, aiUserId, response);

        if (postId != null && recursive) {
            // 检查创建的帖子类型，如果是目录类型则将目录节点放入AI生成队列
            PostDO createdPost = postDataService.getById(postId);
            if (createdPost != null && createdPost.getType() == Enums.PostType.index.value()) {
                // post.content 存储的是节点ID列表，以逗号分隔
                if (createdPost.getContent() != null && !createdPost.getContent().isEmpty()) {
                    String[] nodeIds = createdPost.getContent().split(",");
                    for (String nodeIdStr : nodeIds) {
                        try {
                            long childNodeId = Long.parseLong(nodeIdStr.trim());

                            // 检查该节点是否已经有AI创建的post
                            boolean hasAiPost = postDataService.existPost(childNodeId, aiUserId);
                            if (!hasAiPost) {
                                // 只有没有AI post的节点才加入队列，继承父节点的contentType和recursive
                                postQueueService.enqueue(childNodeId, contentType, true, false);
                                log.info("Added child node {} to AI generation queue from contents post {}", childNodeId, postId);
                            } else {
                                log.info("Child node {} already has AI post, skipping queue", childNodeId);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Invalid node ID format in post content: {}", nodeIdStr);
                        }
                    }
                }
            }

            // 为创建的帖子生成记忆卡片组
            try {
                //createMemoryCardsForPost(postId, aiUserId, response);
                log.info("Successfully created memory cards for post: {}", postId);
            } catch (Exception e) {
                log.warn("Failed to create memory cards for post: {}, error: {}", postId, e.getMessage());
                // 不抛出异常，避免影响主要的帖子创建功能
            }
        }
    }

    // ========= 构建系统提示词 =========

    /**
     * 构建系统提示词（通用规范和要求）
     */
    private String buildSystemPrompt(String contentType) {
        return """
                你是 MaxTwice 内容生成专家。MaxTwice = 最多看两遍就懂。

                【核心定位】帮助理解的补充材料，不是教科书：
                - 目标：让读者看懂，而不是系统教学
                - 面向人群：完全不懂这个话题，但是想要快速看懂的读者
                - 风格：像朋友讲解，通俗易懂，多用比喻
                - 深度：讲清核心要点即可，不追求面面俱到
                - 禁止：学术八股、术语堆砌、一上来就公式
                - 总结：像给村姑解释微积分，能让她听懂是一种能力

                【重要】每次请求都是独立任务，不要复用之前结果。

                ## 输出格式规范
                1. 文章类型：必须以"[A]"开头，后跟纯HTML格式内容
                   - 使用HTML标签：<h1>, <h2>, <h3>, <p>, <ul>, <ol>, <li>, <table>, <pre> 等
                   - 不要使用 Markdown 语法（禁止使用 #, ##, *, -, `, ```, 等）
                   - 不要添加 style 属性或 CSS 样式
                   - 数学公式使用 LaTeX：<span>$公式$</span> 或 <div>$$公式$$</div>
                   - Mermaid 图表用 <pre class="mermaid">...</pre> 包裹
                2. 目录类型：以"[C]"开头，后跟合法的JSON数组
                3. JSON格式：必须使用双引号，格式为 [{"name": "...", "description": "..."}, ...]
                4. 不要在输出前后添加任何额外文字、代码块标记或换行

                ## 文章写作规范
                1. 结构：先讲为什么重要 → 用类比讲原理 → 必要时才给公式
                2. 多用生活例子和比喻，先建立直觉
                3. 术语必须先解释，避免用未知解释未知
                4. 内容准确、逻辑清晰、篇幅适中
                5. 数学公式使用 LaTeX 语法
                6. 图表使用 Mermaid 语法，严格遵守以下规则：
                   - 保持简单，不要复杂
                   - 使用 graph TB 或 graph LR
                   - 节点ID：简单字母数字（A, B1, C2）
                   - 节点文本：A["文本"]，不用特殊字符（引号、括号、下标等）
                   - subgraph 名称：纯英文数字，无空格中文
                   - 正确示例：
                     ```
                     graph TB
                       subgraph group1
                         A["起点"] --> B["中间点"]
                       end
                       subgraph group2
                         C["终点"]
                       end
                     ```
                   - 禁止：subgraph 理论预测 (说明)、A["文本(注释)"]、B["数组[i]"]
                7. 中文和数字、英文之间添加空格

                ## 目录生成规范
                1. 名称具体（禁用"绪论""基础"），简单直接，不要用感性化的描述，比如“牛顿-莱布尼茨公式：微积分的终极桥梁”，要直接用“牛顿-莱布尼茨公式”
                2. 描述必须包含课程上下文，写清楚这是什么课程的知识点，哪个角度讲、讲什么、不讲什么，要非常详细具体，让人脱离目录树也能理解节点范围，目录是网站的核心资产，目录的描述是重中之重，必须细致严谨定义
                3. 名称不重复
                4. 至少2个子目录，否则生成文章
                5. 节点的数量由内容决定，遵循以下原则
                   - 优先细分：当内容可以拆分为两个相对独立的主题时，应该拆分
                   - 不要刻意控制数量，必须保证内容完整覆盖课程核心知识点
                6. 子目录必须在给定节点描述的知识范围之内，不得延伸到其他学科领域
                7. 生成的每一个子目录都需要判断和课程的紧密关系，如果子目录不是核心知识点，必须名称中标注"简述"或"概述"
                8. 核心知识点不要包含简述、概述等字样，必须直接命名
                9. 无章节序号
                10. 如果内容繁杂，需要在目录中添加一篇带有"简述"或"概述"的文章来系统介绍该目录下的内容，先给一个总览，后面再细分目录讲细节

                严格按规范输出。
                """;
    }

    // ========= 构造提示词 =========

    /**
     * 构造提示词：自动判断生成文章或目录
     */
    private String buildPromptForAuto(NodeDO nodeDO, CourseDO courseDO) {
        return """
                课程：%s
                节点：%s
                定义：%s

                判断该节点应该生成文章还是目录：
                1. 如果目录名称或者描述含有简述、概述等字样，则生成文章。
                2. 如果是"%s"课程的核心内容且内容复杂 → [C] 目录
                3. 如果是单一知识点 → [A] 文章
                4. 如果涉及其他学科，不属于本课程核心知识点（即使内容复杂）→ [A] 文章
                
                要求：
                如果生成目录：
             
               """.formatted(courseDO.getName(), nodeDO.getName(), nodeDO.getDescription(), courseDO.getName());
    }

    /**
     * 构造提示词：生成文章
     */
    private String buildPromptForArticle(NodeDO nodeDO, CourseDO courseDO) {
        return """
                课程名称：%s
                当前目录：%s
                目录定义：%s

                任务：为该目录生成一篇文章，以 [A] 开头。
               """.formatted(courseDO.getName(), nodeDO.getName(), nodeDO.getDescription());
    }

    /**
     * 构造提示词：生成目录
     */
    private String buildPromptForIndex(NodeDO nodeDO, CourseDO courseDO) {
        return """
                课程：%s
                节点：%s
                定义：%s

                任务：给当前节点生成子目录，以 [C] 开头。
               """.formatted(courseDO.getName(), nodeDO.getName(), nodeDO.getDescription());
    }

    // ========= 结果解析与入库 =========

    /**
     * 解析 AI 响应，调用 PostService 创建文章/目录帖
     * @return 创建的帖子ID
     */
    private Long handleResponse(long nodeId, long aiUserId, String responseBody) {
        try {
            String content = extractContent(responseBody);
            if (content == null || content.isEmpty()) {
                throw new RuntimeException("empty result content");
            }

            Enums.PostType postType;
            if (content.startsWith("[A]")) {
                postType = Enums.PostType.article;
            } else if (content.startsWith("[C]")) {
                postType = Enums.PostType.index;
            } else {
                throw new RuntimeException("unknown content prefix, must start with [A] or [C]");
            }

            CreatePostRequest req = new CreatePostRequest();
            req.setNodeId(nodeId);
            req.setType(postType.value());
            req.setContent(content.substring(3));

            UserDO userDO = userDataService.getById(aiUserId);

            // index类型需要先创建为SUBMITTED，然后approve来处理子节点创建
            if (postType == Enums.PostType.index) {
                Long postId = postService.createPost(userDO, req, Enums.ContentState.SUBMITTED);
                postService.approve(postId, userDO);
                return postId;
            } else {
                // article类型直接创建为PUBLISHED
                return postService.createPost(userDO, req, Enums.ContentState.PUBLISHED);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 从响应中提取内容（兼容 Gemini、OpenRouter 和 OpenCode 格式）
     */
    private String extractContent(String responseBody) {
        String aiService = systemProperties.getRobot().getAiService();

        if ("gemini".equalsIgnoreCase(aiService) || "openrouter".equalsIgnoreCase(aiService)) {
            // Gemini 和 OpenRouter 直接返回文本
            return responseBody;
        } else {
            // OpenCode 返回 JSON 结构：parts[2].text
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode parts = root.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(2).path("text").asText();
                } else {
                    return root.path("text").asText();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse OpenCode response", e);
            }
        }
    }

    /**
     * 为文章创建记忆卡片组
     */
    private void createMemoryCardsForPost(Long postId, Long aiUserId, String response) {
        try {
            // 解析响应内容，确定是否为文章类型
            JsonNode root = objectMapper.readTree(response);
            JsonNode parts = root.path("parts");
            String content;
            if (parts.isArray() && parts.size() > 0) {
                content = parts.get(1).path("text").asText();
            } else {
                content = root.path("text").asText();
            }

            // 只为文章类型创建记忆卡片
            if (content == null || !content.startsWith("[A]")) {
                return;
            }

            // 创建记忆卡片组请求
            CreateDeckRequest deckRequest = new CreateDeckRequest();
            deckRequest.setSourcePostId(postId);
            //deckRequest.setTitle("AI生成记忆卡片");
            deckRequest.setDescription("基于文章内容自动生成的记忆卡片组");

            // 生成示例记忆卡片
            List<CreateDeckRequest.CardInfo> cards = generateMemoryCards(content);
            deckRequest.setCards(cards);

            // 创建卡片组
            UserDO aiUser = userDataService.getById(aiUserId);
            memoryCardDeckService.createDeck(aiUser, deckRequest);

        } catch (Exception e) {
            log.error("Failed to create memory cards for post: {}", postId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 基于文章内容生成记忆卡片
     */
    private List<CreateDeckRequest.CardInfo> generateMemoryCards(String articleContent) {
        try {
            String systemPrompt = buildMemoryCardSystemPrompt();
            String prompt = buildMemoryCardPrompt(articleContent);
            String response = aiService.generateContent(prompt, systemPrompt);

            log.info("Memory card generation response length: {}", response.length());
            return parseMemoryCardResponse(response);
        } catch (Exception e) {
            log.warn("Failed to generate AI memory cards: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建记忆卡片系统提示词
     */
    private String buildMemoryCardSystemPrompt() {
        return """
                你是记忆卡片生成专家。请严格遵守以下规范：

                ## 输出格式
                1. 必须输出合法的 JSON 数组
                2. 使用双引号，格式：[{"front": "问题", "back": "答案"}, ...]
                3. 直接输出 JSON，不要添加代码块标记、说明文字或其他内容
                4. 以 '[' 开头，以 ']' 结尾

                ## 卡片内容选取标准
                1. 重要的概念、定义、公式
                2. 专有名词
                3. 比较难理解的知识点

                ## 卡片形式
                1. 问答题
                2. 选择题
                3. 填空题
                4. 判断题

                ## 质量要求
                1. 问题要具体明确
                2. 答案要准确完整且简洁
                3. 避免答案长篇大论
                4. 如果问答题答案不够简洁，使用其他形式（选择、填空、判断）

                请严格按照 JSON 格式输出，确保使用双引号。
                """;
    }

    /**
     * 构建记忆卡片生成的提示词
     */
    private String buildMemoryCardPrompt(String articleContent) {
        return String.format("""
            基于以下文章内容生成记忆卡片组：

            %s

            请直接输出 JSON 数组。
            """, articleContent);
    }

    /**
     * 解析AI生成的记忆卡片响应
     */
    private List<CreateDeckRequest.CardInfo> parseMemoryCardResponse(String responseBody) {
        try {
            // 提取内容（兼容 Gemini 和 OpenCode）
            String content = extractContent(responseBody);

            if (content == null || content.isEmpty()) {
                throw new RuntimeException("Empty AI response");
            }

            if (content.startsWith("```json")) {
                content = content.substring(7, content.length() - 3).trim();
            }
            // 尝试解析JSON数组
            log.info("Parsing memory card content length: {}", content.length());
            JsonNode cardsArray = objectMapper.readTree(content);
            if (!cardsArray.isArray()) {
                throw new RuntimeException("Response is not a JSON array");
            }

            List<CreateDeckRequest.CardInfo> cards = new ArrayList<>();
            for (JsonNode cardNode : cardsArray) {
                String front = cardNode.path("front").asText();
                String back = cardNode.path("back").asText();

                if (!front.isEmpty() && !back.isEmpty()) {
                    CreateDeckRequest.CardInfo card = new CreateDeckRequest.CardInfo();
                    card.setFront(front);
                    card.setBack(back);
                    cards.add(card);
                }
            }

            if (cards.isEmpty()) {
                throw new RuntimeException("No valid cards generated");
            }

            return cards;
        } catch (Exception e) {
            log.warn("Failed to parse AI memory card response: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 公共接口：为指定文章内容生成记忆卡片
     */
    public List<CreateDeckRequest.CardInfo> generateMemoryCardsForContent(String articleContent) {
        return generateMemoryCards(articleContent);
    }

    /**
     * 公共接口：创建记忆卡片组
     */
    public void createMemoryCardsForPost(Long postId, Long aiUserId, List<CreateDeckRequest.CardInfo> cards) {
        // 创建记忆卡片组请求
        CreateDeckRequest deckRequest = new CreateDeckRequest();
        deckRequest.setSourcePostId(postId);
        //deckRequest.setTitle("AI生成记忆卡片");
        deckRequest.setDescription("基于文章内容AI生成的记忆卡片组");
        deckRequest.setCards(cards);

        // 创建卡片组
        UserDO aiUser = userDataService.getById(aiUserId);
        memoryCardDeckService.createDeck(aiUser, deckRequest);
        log.info("Created AI memory cards for post {}", postId);
    }
}
