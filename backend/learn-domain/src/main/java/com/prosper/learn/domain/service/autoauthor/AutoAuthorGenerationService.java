package com.prosper.learn.domain.service.autoauthor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.business.PostService;
import com.prosper.learn.domain.service.data.CourseDataService;
import com.prosper.learn.domain.service.data.NodeDataService;
import com.prosper.learn.dto.request.CreatePostRequest;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.NodeDO;
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
        handleResponse(nodeId, aiUserId, response);
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
                1. 如果能用一篇文章说清楚这个目录的内容，就生成一篇文章:
                   文章前用"[A]"开标注这是一篇文章，需要使用容易让人理解的方式，返回的内容使用Markdown风格的HTML，不要带样式，不要有和内容无关的语言，请根据实际情况调整长度，
                   文章要非常的侧重清楚明白，容易理解，
                   文章内容要有条理，有逻辑，内容要丰富，尽量多的覆盖这个目录下的知识点，内容要准确，不要有错误，
                   数学相关内容请使用LaTeX语法，图表相关内容请使用Mermaid语法，
                   示例：[A]文章内容
                2. 如果这个目录下的内容比较多，不能用一篇文章说清楚，就生成一个子目录列表:
                   目录前用"[C]"来标注这是一个目录，目录列表使用[{"目录名1": "目录描述"}, {"目录名2": "目录描述"}, ...]的格式，
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
     */
    private void handleResponse(long nodeId, long aiUserId, String responseBody) {
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
            postService.createPost(aiUserId, req, Enums.PostState.approved);

            // 模型承诺只返回 JSON
            //JsonNode data = objectMapper.readTree(content);
            //String decision = data.path("decision").asText();
            /*
            if ("ARTICLE".equalsIgnoreCase(decision)) {
                //String md = data.path("articleMd").asText("");
                CreatePostRequest req = new CreatePostRequest();
                req.setNodeId(nodeId);
                req.setType(Enums.PostType.article.value());
                req.setContent(content.substring(3));
                postService.createPost(aiUserId, req, Enums.PostState.approved);
                return;
            if ("DIRECTORY".equalsIgnoreCase(decision)) {
                ArrayNode children = (ArrayNode) data.path("children");
                ArrayNode titles = objectMapper.createArrayNode();
                if (children != null) {
                    int max = systemProperties.getAutoAuthor().getMaxChildrenPerNode();
                    for (int i = 0; i < children.size() && i < max; i++) {
                        String title = children.get(i).path("title").asText("Untitled");
                        titles.add(title);
                    }
                }
                CreatePostRequest req = new CreatePostRequest();
                req.setNodeId(nodeId);
                req.setType(Enums.PostType.contents.value());
                req.setContent(titles.toString()); // PostService 内部会据此批量创建子节点并写 contents
                postService.createPost(aiUserId, req, Enums.PostState.approved);
                return;
            }
            throw new RuntimeException("unknown decision: " + decision);
             */
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
