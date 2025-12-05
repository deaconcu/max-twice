package com.prosper.learn.content.autoauthor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.dto.request.CreateDeckRequest;
import com.prosper.learn.shared.dto.request.CreatePostRequest;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * AutoAuthor 生成服务
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
public class AutoAuthorGenerationService {

    // ========= 依赖与配置 =========

    private final SystemProperties systemProperties;
    private final NodeDataService nodeDataService;
    private final CourseDataService courseDataService;
    private final PostService postService;
    private final PostDataService postDataService;
    private final UserDataService userDataService;
    private final MemoryCardDeckService memoryCardDeckService;
    private final AutoAuthorQueueService autoAuthorQueueService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String SESSION_KEY = "autoauthor:session:id";
    private static final long SESSION_EXPIRE_HOURS = 24;

    // ========= 对外入口 =========

    /**
     * 重置会话：删除 Redis 中的会话键
     */
    public void resetSession() {
        redisTemplate.delete(SESSION_KEY);
        log.info("session reset, deleted key: {}", SESSION_KEY);
    }

    /**
     * 为指定节点生成内容（外层执行器负责幂等与重试）
     */
    public void generateForNode(long nodeId) {
        long aiUserId = systemProperties.getAutoAuthor().getAiUserId();
        NodeDO nodeDO = nodeDataService.validateAndGet(nodeId);
        CourseDO courseDO = courseDataService.validateAndGet(nodeDO.getCourseId());

        String sessionId = getOrCreateSession();
        String response = sendMessageWithRetry(sessionId, buildPrompt(nodeDO, courseDO));
        log.info("sessionId={}, response={}", sessionId, response);
        Long postId = handleResponse(nodeId, aiUserId, response);

        if (postId != null) {
            // 检查创建的帖子类型，如果是目录类型则将目录节点放入AI生成队列
            PostDO createdPost = postDataService.getById(postId);
            if (createdPost != null && createdPost.getType() == Enums.PostType.contents.value()) {
                // post.content 存储的是节点ID列表，以逗号分隔
                if (createdPost.getContent() != null && !createdPost.getContent().isEmpty()) {
                    String[] nodeIds = createdPost.getContent().split(",");
                    for (String nodeIdStr : nodeIds) {
                        try {
                            long childNodeId = Long.parseLong(nodeIdStr.trim());

                            // 检查该节点是否已经有AI创建的post
                            boolean hasAiPost = postDataService.existPost(childNodeId, aiUserId);
                            if (!hasAiPost) {
                                // 只有没有AI post的节点才加入队列
                                autoAuthorQueueService.enqueue(childNodeId);
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
                createMemoryCardsForPost(postId, aiUserId, response);
                log.info("Successfully created memory cards for post: {}", postId);
            } catch (Exception e) {
                log.warn("Failed to create memory cards for post: {}, error: {}", postId, e.getMessage());
                // 不抛出异常，避免影响主要的帖子创建功能
            }
        }
    }

    // ========= 与 opencode 通信 =========

    /**
     * 获取或创建 sessionId
     */
    private String getOrCreateSession() {
        String sessionId = redisTemplate.opsForValue().get(SESSION_KEY);
        if (sessionId != null && !sessionId.isEmpty()) {
            log.debug("reusing existing session: {}", sessionId);
            return sessionId;
        }

        sessionId = createSession();
        redisTemplate.opsForValue().set(SESSION_KEY, sessionId, SESSION_EXPIRE_HOURS, TimeUnit.HOURS);
        log.info("created new session: {}", sessionId);
        return sessionId;
    }

    /**
     * 发送消息（不重连，有 sessionId 就继续使用）
     */
    private String sendMessageWithRetry(String sessionId, String prompt) {
        try {
            return sendMessage(sessionId, prompt);
        } catch (Exception e) {
            log.warn("send message failed with session {}: {}", sessionId, e.getMessage());
            // 检查 Redis 中是否还有 sessionId，有的话就抛出异常不重连
            String existingSessionId = redisTemplate.opsForValue().get(SESSION_KEY);
            if (existingSessionId != null && !existingSessionId.isEmpty()) {
                log.info("session exists in redis, not reconnecting");
                throw e;
            }

            // Redis 中没有 sessionId 时才重新创建
            log.info("no session in redis, creating new session");
            String newSessionId = getOrCreateSession();
            return sendMessage(newSessionId, prompt);
        }
    }

    /**
     * 创建 opencode 会话（POST /session）
     */
    private String createSession() {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("title", "auto-author");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(systemProperties.getAutoAuthor().getOpencodeBaseUrl() + "/session"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) throw new RuntimeException("create session failed: " + resp.statusCode());
            JsonNode json = objectMapper.readTree(resp.body());
            String id = json.path("id").asText();
            if (id == null || id.isEmpty()) throw new RuntimeException("empty session id");
            return id;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送 ChatInput（POST /session/:id/message）并返回原始响应
     */
    private String sendMessage(String sessionId, String prompt) {
        try {
            ObjectNode model = objectMapper.createObjectNode();
            model.put("providerID", systemProperties.getAutoAuthor().getProviderId());
            model.put("modelID", systemProperties.getAutoAuthor().getModelId());

            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("type", "text");
            part.put("text", prompt);
            parts.add(part);

            ObjectNode root = objectMapper.createObjectNode();
            root.set("model", model);
            root.set("parts", parts);

            log.info("send to opencode: {}", root);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(systemProperties.getAutoAuthor().getOpencodeBaseUrl() + "/session/" + sessionId + "/message"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) throw new RuntimeException("send message failed: " + resp.statusCode());
            return resp.body();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构造提示词，要求返回严格 JSON（decision/articleMd/children[]）
     */
    private String buildPrompt(NodeDO nodeDO, CourseDO courseDO) {
        return """
                请忽略之前所有对话内容，独立回答
                我在写一本教材，教材名称是："%s", 我当前在 "%s" 这个目录下，这个目录是这么定义的：%s
                请你判断并给出回答：
                1. 如果能用一篇文章说清楚这个目录的内容，就生成一篇文章，尽量做到一篇文章只讲一个知识点
                   文章前用"[A]"开标注这是一篇文章，需要使用容易让人理解的方式，返回的内容使用HTML格式，不要带样式，不要有和内容无关的语言，请根据实际情况调整长度；
                   文章一般采用如下结构：
                      1. 先介绍为什么要学习这个知识点，这个知识点有什么用处，能解决什么问题，是从一个什么原因才引出的这个知识点
                      2. 使用通俗的语言从最初的思路开始讲解这个知识点，因为读者是来学习的，不要用一个未知的东西去解释另一个未知的东西，尽可能使用类比，图表来让读者能从一个已熟悉的角度去理解新的知识点
                      3. 然后再介绍相关的公式，定理，规范，让读者在理解的基础上去学习形式化的内容
                   切记不要一上来放一堆公式，初学者不知道这些公式是干嘛的，这种书不是给读者自己看的，而是必须有个老师讲解，我们要做能让人自己看懂的书
                   文章要非常的侧重清楚明白，容易理解，
                   文章内容要有条理，有逻辑，内容要丰富，尽量多的覆盖这个目录下的知识点，内容要准确，不要有错误，
                   数学相关内容请使用LaTeX语法，
                   图表相关内容请使用Mermaid语法
                       1. 图表不要太复杂，尽量简单易懂，图表要占满行宽，节点要使用与之适配的图形，比如不要在没有判断的地方使用菱形节点
                       2. 节点名称要整体用双引号包含, 比如：节点 B1["1. xxx"]，不要在双引号中使用双引号，比如: subgraph "空间 B = {"α₁, α₂"}, 原色 {"红, 绿, 蓝"})"，P1["\"系数位置 A[i, i"]"]，都是错误的写法
                       3. 灵活使用图表生成方向 graph TB/LR，保证生成的图表宽小于高，不要使用direction xx，没有作用
                   中文和数字，英文之间添加空格
                   不要添加任何js代码，不要在[A]后添加换行
                   示例：[A]文章内容
                2. 如果这个目录下的内容比较多，不能用一篇文章说清楚，就生成一个子目录列表:
                   目录前用"[C]"来标注这是一个目录，目录列表使用[{"目录名1": "目录描述"}, {"目录名2": "目录描述"}, ...]的JSON格式，请输出合法的JSON数组
                   每一个对象中只有一个键值对，不要在一个对象中放多个键值对
                   目录名是子节点名称，目录描述是对这个子节点的介绍。
                   1. 目录名不允许重复，
                   2. 目录描述是对这个目录的完整定义，非常重要, 要精确描述这个目录要讲述的知识的范围，要让用户容易理解，不容易有歧义，而且要概括完整，不要遗漏
                   3. 前后不要附加任何文字，目录中不要添加章节序号，不要添加无关的内容，比如欢迎语，只有目录名称和目录描述
                   目录示例：[C][{"绪论": "介绍这门课程"}, {"基础知识": "讲解这门课程的基础知识, ..."}, {"进阶篇": "讲解这门课程的进阶知识, ..."}]
               """.formatted(courseDO.getName(), nodeDO.getName(), nodeDO.getDescription());
    }

    // ========= 结果解析与入库 =========

    /**
     * 解析 opencode 响应，调用 PostService 创建文章/目录帖
     * @return 创建的帖子ID
     */
    private Long handleResponse(long nodeId, long aiUserId, String responseBody) {
        try {
            // opencode Message 结构：parts[0].text 存放模型输出
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode parts = root.path("parts");
            String content;
            if (parts.isArray() && parts.size() > 0) content = parts.get(1).path("text").asText();
            else content = root.path("text").asText();
            if (content == null || content.isEmpty()) throw new RuntimeException("empty result content");

            Enums.PostType postType;
            if (content.startsWith("[A]")) {
                postType = Enums.PostType.article;
            } else if (content.startsWith("[C]")) {
                postType = Enums.PostType.contents;
            } else {
                throw new RuntimeException("unknown content prefix, must start with [A] or [C]");
            }

            CreatePostRequest req = new CreatePostRequest();
            req.setNodeId(nodeId);
            req.setType(postType.value());
            req.setContent(content.substring(3));
            return postService.createPost(userDataService.getById(aiUserId), req, Enums.ContentState.PUBLISHED);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            memoryCardDeckService.createDeck(aiUserId, deckRequest);

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
            String sessionId = getOrCreateSession();
            String prompt = buildMemoryCardPrompt(articleContent);
            String response = sendMessage(sessionId, prompt);

            log.info("response:" + response);
            return parseMemoryCardResponse(response);
        } catch (Exception e) {
            log.warn("Failed to generate AI memory cards, falling back to default cards: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建记忆卡片生成的提示词
     */
    private String buildMemoryCardPrompt(String articleContent) {
        return String.format("""
            请忽略之前所有对话内容，独立回答
            基于以下文章内容，生成记忆卡片组，每张卡片包含问题(front)和答案(back)。

            要求：
            1. 卡片的内容选取参照以下标准：
                1). 需要记忆的内容
                    * 重要的概念、定义、公式
                    * 专有名词
                2). 比较难理解的地方
            2. 卡片的形式
                1）问答
                2）选择题
                3）填空题
                4）判断题
            3. 问题要具体明确
            4. 答案要准确完整，而且要尽量简洁，答案长篇大论的卡片没有意义，卡片是需要记忆的
            5. 如果问答题做不到答案简洁，可以使用别的卡片形式（选择，填空，判断）
            6. 严格按照JSON格式返回，格式如下：
            [{"front": "问题", "back": "答案"}, ...]

            文章内容：
            %s
            
            示例Output:
            [
              {"front": "问题1", "back": "答案1"},
              {"front": "问题2", "back": "答案2"},
              {"front": "问题3", "back": "答案3"}
            ]
            你的回应以'['开头，以']'结尾，中间是合法的JSON对象
            """, articleContent);
    }

    /**
     * 解析AI生成的记忆卡片响应
     */
    private List<CreateDeckRequest.CardInfo> parseMemoryCardResponse(String responseBody) {
        try {
            // 解析opencode响应格式
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode parts = root.path("parts");
            String content;
            if (parts.isArray() && parts.size() > 0) {
                content = parts.get(1).path("text").asText();
            } else {
                content = root.path("text").asText();
            }

            if (content == null || content.isEmpty()) {
                throw new RuntimeException("Empty AI response");
            }

            if (content.startsWith("```json")) {
                content = content.substring(7, content.length() - 3).trim();
            }
            // 尝试解析JSON数组
            log.info("content:" + content);
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
        memoryCardDeckService.createDeck(aiUserId, deckRequest);
        log.info("Created AI memory cards for post {}", postId);
    }
}
