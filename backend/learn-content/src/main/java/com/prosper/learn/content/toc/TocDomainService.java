package com.prosper.learn.content.toc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.BusinessException;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

/**
 * 内容管理服务
 * 
 * 负责管理用户课程目录的增删改查操作，包括：
 * - 目录结构的获取和创建
 * - 目录类型的帖子可以加入到左侧目录中
 * - 帖子的置顶和取消置顶
 * 
 * 核心概念：
 * - 每个用户对每个课程都有独立的目录结构
 * - 目录内容采用引用计数机制管理，支持多用户共享相同的目录版本
 * - 目录以 JSON 格式存储，支持嵌套结构
 * 
 * @author Claude
 * @since 2024-01-20
 */
@Service
@RequiredArgsConstructor
public class TocDomainService {

    /** 课程数据访问接口 */
    private final CourseDataService courseDataService;
    
    /** JSON 对象映射器，用于目录结构的序列化和反序列化 */
    private final ObjectMapper objectMapper;
    
    /** 帖子数据访问接口 */
    private final PostDataService postDataService;
    
    /** 用户课程目录数据访问接口 */
    private final UserCourseTocDataService userCourseTocDataService;
    
    /** 课程目录数据访问接口，管理目录内容和引用计数 */
    private final CourseTocDataService courseTocDataService;
    
    /** 内容管理相关配置属性 */
    private final SystemProperties systemProperties;

    /**
     * 验证课程存在性
     * 
     * @param courseId 课程ID
     * @return 课程实体对象
     * @throws BusinessException 当课程不存在时抛出 CONTENTS_COURSE_NOT_FOUND 异常
     */
    private CourseDO validateCourseExists(long courseId) {
        CourseDO courseDO = courseDataService.getById(courseId);
        if (courseDO == null) {
            throw StatusCode.COURSE_NOT_FOUND.exception();
        }
        return courseDO;
    }

    /**
     * 验证帖子存在性并检查类型
     * 
     * 只有内容类型（contents）的帖子可以被选择到课程目录中，
     * 文章类型（article）的帖子不允许作为课程内容。
     * 
     * @param postId 帖子ID
     * @return 帖子实体对象
     * @throws BusinessException 当帖子不存在时抛出 CONTENTS_POST_NOT_FOUND 异常
     * @throws BusinessException 当帖子类型为文章时抛出 CONTENTS_INVALID_POST_TYPE 异常
     */
    private PostDO validatePostForContents(long postId) {
        PostDO postDO = postDataService.validateAndGet(postId);
        if (postDO.getType() == Enums.PostType.article.value()) {
            throw StatusCode.INVALID_POST_TYPE.exception();
        }
        return postDO;
    }

    /**
     * 验证用户目录存在性
     * 
     * 检查指定用户在指定课程下是否已经创建了目录结构。
     * 
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 用户课程目录实体对象
     * @throws BusinessException 当用户目录不存在时抛出 TOC_USER_TOC_NOT_FOUND 异常
     */
    private UserCourseTocDO validateUserTocExists(long userId, long courseId) {
        UserCourseTocDO userCourseTocDO = userCourseTocDataService.getByUserAndCourse(userId, courseId);
        if (userCourseTocDO == null) {
            throw StatusCode.TOC_USER_TOC_NOT_FOUND.exception();
        }
        return userCourseTocDO;
    }

    /**
     * 验证目录索引有效性
     *
     * 检查给定的目录索引是否在有效范围内。
     * 目录索引从1开始，不能超过目录哈希数组的长度。
     *
     * @param tocIndex 目录索引（从1开始）
     * @param tocHashArr 目录哈希数组
     * @throws BusinessException 当索引超出范围时抛出 TOC_INDEX_OUT_OF_BOUNDS 异常
     */
    private void validateTocIndex(int tocIndex, String[] tocHashArr) {
        if (tocIndex > tocHashArr.length) {
            throw StatusCode.TOC_INDEX_OUT_OF_BOUNDS.exception();
        }
    }

    /**
     * 验证并解析索引数组
     *
     * @param indexArray 索引数组字符串（逗号分隔）
     * @param maxLength 允许的最大长度
     * @param tocHashesLength 当前目录哈希数组的长度
     * @return 解析后的索引数组
     * @throws BusinessException 当索引数组格式错误、长度超限或值超出范围时抛出异常
     */
    private int[] validateAndParseIndexArray(String indexArray, int maxLength, int tocHashesLength) {
        String[] indexStrings = indexArray.split(",");

        // 验证长度
        if (indexStrings.length > maxLength) {
            throw StatusCode.INVALID_PARAMETER.exception("索引数组长度不能超过" + maxLength);
        }

        int[] indexes = new int[indexStrings.length];
        for (int i = 0; i < indexStrings.length; i++) {
            try {
                indexes[i] = Integer.parseInt(indexStrings[i]);
                // 验证索引值范围
                if (Math.abs(indexes[i]) > tocHashesLength) {
                    throw StatusCode.TOC_INDEX_OUT_OF_BOUNDS.exception();
                }
            } catch (NumberFormatException e) {
                throw StatusCode.INVALID_PARAMETER.exception("索引值必须是有效的整数");
            }
        }

        return indexes;
    }

    /**
     * 更新用户课程目录
     *
     * 根据提供的索引数组重新组织用户的课程目录结构。
     * 索引数组中的每个值对应当前目录哈希数组中的位置（从1开始），
     * 特殊值0表示使用默认目录（只包含根节点）。
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param indexArray 索引数组字符串（逗号分隔），如 "1,2,0,3"
     *                   - 0: 使用默认目录
     *                   - 正整数: 使用现有目录数组中对应位置的哈希（从1开始）
     * @throws BusinessException 当课程不存在、用户目录不存在、索引无效等情况时抛出异常
     */
    @Transactional
    public void updateUserCourseToc(long userId, long courseId, String indexArray) {
        // 1. 验证课程存在性
        CourseDO courseDO = courseDataService.validateAndGet(courseId);

        // 2. 验证用户目录存在性
        UserCourseTocDO userCourseTocDO = validateUserTocExists(userId, courseId);

        // 3. 解析当前目录哈希数组
        String toc = userCourseTocDO.getToc();
        String[] tocHashes = toc.split(",");

        // 4. 验证并解析索引数组
        int[] indexes = validateAndParseIndexArray(indexArray, 9, tocHashes.length);

        // 5. 生成默认目录结构
        ObjectNode defaultTocNode = objectMapper.createObjectNode();
        defaultTocNode.set(Long.toString(courseDO.getRootNodeId()), objectMapper.createObjectNode());
        String defaultTocStr = defaultTocNode.toString();
        String defaultTocHash = Utils.hashSHA(defaultTocStr);

        // 6. 如果默认目录不存在则创建
        if (courseTocDataService.get(defaultTocHash) == null) {
            courseTocDataService.insert(new CourseTocDO(defaultTocHash, defaultTocStr));
        }

        // 7. 构建新的目录数组
        String[] newTocArr = new String[indexes.length];
        int defaultCount = 0;
        for (int i = 0; i < indexes.length; i++) {
            if (indexes[i] != 0) {
                newTocArr[i] = tocHashes[Math.abs(indexes[i]) - 1];
            } else {
                newTocArr[i] = defaultTocHash;
                defaultCount++;
            }
        }

        // 8. 增加默认目录的引用计数
        if (defaultCount > 0) {
            courseTocDataService.incrRef(defaultTocHash, defaultCount);
        }

        // 9. 更新用户课程目录
        String newToc = String.join(",", newTocArr);
        userCourseTocDO.setToc(newToc);
        userCourseTocDataService.update(userCourseTocDO);
    }

    /**
     * 获取当前目录内容并完成更新流程的通用方法
     * 
     * 这是目录更新的核心模板方法，处理：
     * 1. 获取当前目录内容并减少旧版本引用计数
     * 2. 应用内容更新操作
     * 3. 保存新版本并增加引用计数
     * 4. 更新用户目录指向
     * 
     * @param tocHashArr 目录哈希数组
     * @param tocIndex 目录索引（从1开始）
     * @param userCourseTocDO 用户课程目录对象
     * @param contentUpdater 内容更新函数，接收当前目录内容，返回新的目录内容
     */
    private void getCurrentTocAndUpdate(String[] tocHashArr, int tocIndex, UserCourseTocDO userCourseTocDO, 
                                       java.util.function.Function<String, String> contentUpdater) {
        // 1. 获取当前目录内容并减少引用计数
        CourseTocDO courseTocDO = courseTocDataService.get(tocHashArr[tocIndex - 1]);
        courseTocDataService.incrRef(courseTocDO.getHash(), -1);
        String currentTocContent = courseTocDO.getToc();
        
        // 2. 应用内容更新操作
        String newTocContent = contentUpdater.apply(currentTocContent);
        
        // 3. 保存新版本并增加引用计数
        String hash = Utils.hashSHA(newTocContent);
        if (courseTocDataService.get(hash) == null) {
            courseTocDataService.insert(new CourseTocDO(hash, newTocContent));
        }
        courseTocDataService.incrRef(hash, 1);

        // 4. 更新用户目录指向
        tocHashArr[tocIndex - 1] = hash;
        userCourseTocDO.setToc(String.join(",", tocHashArr));
        userCourseTocDataService.update(userCourseTocDO);
    }

    /**
     * 获取用户在指定课程下的完整目录结构
     * 
     * 目录结构采用分层设计：
     * 1. 每个用户课程有一个目录哈希列表，支持多版本目录
     * 2. 每个哈希对应一个具体的目录JSON内容
     * 3. 目录内容使用引用计数管理，支持多用户共享
     * 
     * @param userId 用户ID
     * @param courseId 课程ID  
     * @param create 当用户目录不存在时是否自动创建
     * @return 目录结构的JSON数组，如果不存在且不创建则返回null
     * @throws BusinessException 当课程不存在时抛出异常
     */
    public ArrayNode getToc(long userId, long courseId, boolean create) {
        CourseDO courseDO = validateCourseExists(courseId);

        UserCourseTocDO userCourseTocDO = userCourseTocDataService.getByUserAndCourse(userId, courseId);
        String tocHashStr = "";

        // 如果用户目录不存在且不需要创建，直接返回null
        if (userCourseTocDO == null && !create) return null;

        if (userCourseTocDO == null) {
            // 创建根目录结构：包含课程根节点的目录
            ObjectNode s = objectMapper.createObjectNode();
            ObjectNode rootNodeContent = objectMapper.createObjectNode();

            // 自动生成默认目录：查找根节点下 score 最高的 contents 类型 Post
            List<PostDO> topPosts = postDataService.getListByNodeAndScore(
                    courseDO.getRootNodeId(), 1, Enums.ContentState.PUBLISHED.value());

            if (!topPosts.isEmpty()) {
                PostDO topPost = topPosts.get(0);
                // 只有 contents 类型才能作为目录
                if (topPost.getType() == Enums.PostType.index.value()) {
                    // 创建子节点结构（使用 Post 的 content 字段）
                    Arrays.stream(topPost.getContent().split(","))
                            .forEach(id -> rootNodeContent.putObject(id));
                    // 设置选中的帖子
                    rootNodeContent.put(systemProperties.getContents().getChosenField(), topPost.getId());
                }
            }

            s.put(Long.toString(courseDO.getRootNodeId()), rootNodeContent);

            String tocStr = s.toString();
            tocHashStr = Utils.hashSHA(tocStr);

            // 检查是否已存在相同内容的目录，避免重复存储
            CourseTocDO courseTocDO = courseTocDataService.get(tocHashStr);
            if (courseTocDO == null) {
                CourseTocDO newToc = new CourseTocDO();
                newToc.setHash(tocHashStr);
                newToc.setToc(tocStr);
                newToc.setRefCount(1);
                courseTocDataService.insert(newToc);
            }

            // 为用户创建目录记录，指向刚创建的目录版本
            userCourseTocDO = new UserCourseTocDO();
            userCourseTocDO.setUserId(userId);
            userCourseTocDO.setCourseId(courseId);
            userCourseTocDO.setToc(tocHashStr);

            userCourseTocDataService.insert(userCourseTocDO);
        } else {
            // 用户目录已存在，获取目录哈希列表
            tocHashStr = userCourseTocDO.getToc();
        }

        ArrayNode arrayNode = objectMapper.createArrayNode();
        String[] tocHashArr = tocHashStr.split(",");

        // 批量获取所有目录版本的内容
        Map<String, CourseTocDO> map = courseTocDataService.getByHashes(tocHashArr);
        for(String tocHash: tocHashArr) {
            try {
                CourseTocDO courseTocDO = map.get(tocHash);
                if (courseTocDO == null) {
                    // 数据不一致：目录哈希在数据库中不存在
                    throw StatusCode.TOC_USER_TOC_NOT_FOUND.exception();
                }
                arrayNode.add(objectMapper.readTree(courseTocDO.getToc()));
            } catch (IOException e) {
                throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
            }
        }

        return arrayNode;
    }

    /**
     * 获取用户在指定课程下特定索引位置的目录内容
     * 
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param tocIndex 目录索引（从1开始）
     * @return 指定索引位置的目录JSON字符串，如果用户目录不存在则返回null
     * @throws BusinessException 当课程不存在或索引越界时抛出异常
     */
    public String getToc(long userId, long courseId, int tocIndex) {
        CourseDO courseDO = validateCourseExists(courseId);

        UserCourseTocDO userCourseTocDO = userCourseTocDataService.getByUserAndCourse(userId, courseId);
        String tocStr = "";
        if (userCourseTocDO == null) return null;

        tocStr = userCourseTocDO.getToc();

        ArrayNode arrayNode = objectMapper.createArrayNode();
        String[] tocHashArr = tocStr.split(",");

        validateTocIndex(tocIndex, tocHashArr);

        CourseTocDO courseTocDO = courseTocDataService.get(tocHashArr[tocIndex - 1]);
        return courseTocDO.getToc();
    }

    /**
     * 将帖子内容选择添加到用户课程目录的指定路径下
     * 
     * 业务流程：
     * 1. 验证帖子和课程的有效性
     * 2. 创建包含帖子内容的子节点
     * 3. 获取用户当前目录并减少旧版本引用计数
     * 4. 更新目录结构并保存新版本
     * 5. 增加新版本引用计数并更新用户目录指向
     * 
     * @param userId 用户ID
     * @param path 目录路径，格式：{tocIndex}-{nodePath}，如 "1-chapter1-section1"
     * @param courseId 课程ID
     * @param postId 帖子ID（必须是内容类型，不能是文章类型）
     * @throws BusinessException 当课程、帖子不存在或帖子类型无效时抛出异常
     */
    @Transactional
    public void choose(long userId, String path, long courseId, long postId) {
        // validate - 先验证课程，再验证帖子
        CourseDO courseDO = validateCourseExists(courseId);
        PostDO postDO = validatePostForContents(postId);

        // 创建childNode
        ObjectNode childNode = objectMapper.createObjectNode();
        Arrays.stream(postDO.getContent().split(",")).forEach(id->childNode.putObject((id)));
        childNode.put(systemProperties.getContents().getChosenField(), postId);

        String[] pathParts = path.split("-", 2);
        int tocIndex = Integer.parseInt(pathParts[0]);

        // get user toc hash
        UserCourseTocDO userCourseTocDO = validateUserTocExists(userId, courseId);

        String[] tocHashArr = userCourseTocDO.getToc().split(",");
        validateTocIndex(tocIndex, tocHashArr);

        // 使用通用方法完成目录更新
        getCurrentTocAndUpdate(tocHashArr, tocIndex, userCourseTocDO, 
            currentTocContent -> updateContents(currentTocContent, pathParts[1], childNode));
    }

    /**
     * 取消选择用户课程目录指定路径下的内容
     * 
     * 将指定路径下的内容清空，用空的JSON对象替换原有内容。
     * 采用与choose相同的引用计数管理机制。
     * 
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param path 目录路径，格式：{tocIndex}-{nodePath}
     * @throws BusinessException 当课程不存在、用户目录不存在或索引越界时抛出异常
     */
    public void unchoose(long userId, long courseId, String path) {
        CourseDO courseDO = validateCourseExists(courseId);

        String[] pathParts = path.split("-", 2);
        int tocIndex = Integer.parseInt(pathParts[0]);

        // get user toc hash
        UserCourseTocDO userCourseTocDO = validateUserTocExists(userId, courseId);

        String[] tocHashArr = userCourseTocDO.getToc().split(",");
        validateTocIndex(tocIndex, tocHashArr);

        // 使用通用方法完成目录更新
        getCurrentTocAndUpdate(tocHashArr, tocIndex, userCourseTocDO,
            currentTocContent -> updateContents(currentTocContent, pathParts[1], objectMapper.createObjectNode()));
    }

    /**
     * 管理帖子在指定路径下的置顶状态
     * 
     * 置顶功能说明：
     * - 置顶的帖子存储在目录节点的特殊字段中（配置为 "^"）
     * - 每个节点最多可以置顶配置数量的帖子（默认10个）
     * - 支持添加和取消置顶操作
     * 
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param path 目录路径，格式：{tocIndex}-{nodePath}
     * @param postId 要置顶的帖子ID
     * @param add true=添加置顶，false=取消置顶
     * @throws BusinessException 当课程不存在、置顶数量超限等情况时抛出异常
     */
    public void pin(long userId, long courseId, String path, long postId, boolean add) {
        CourseDO courseDO = validateCourseExists(courseId);

        String[] pathParts = path.split("-", 2);
        int tocIndex = Integer.parseInt(pathParts[0]);

        // get user toc hash
        UserCourseTocDO userCourseTocDO = validateUserTocExists(userId, courseId);

        String[] tocHashArr = userCourseTocDO.getToc().split(",");
        validateTocIndex(tocIndex, tocHashArr);

        // 使用通用方法完成目录更新
        getCurrentTocAndUpdate(tocHashArr, tocIndex, userCourseTocDO,
            currentTocContent -> modifyPinOfContents(currentTocContent, pathParts[1], postId, add));
    }

    /**
     * 更新目录结构中指定路径的节点内容
     * 
     * 核心的目录更新逻辑，支持嵌套路径的导航和节点替换。
     * 在替换节点时会保留原节点的置顶信息。
     * 
     * @param contents 当前目录的JSON字符串
     * @param path 节点路径，支持嵌套如 "chapter1-section1-lesson1"
     * @param newNode 要设置的新节点内容
     * @return 更新后的目录JSON字符串
     * @throws BusinessException 当JSON处理失败时抛出异常
     */
    private String updateContents(String contents, String path, ObjectNode newNode) {
        try {
            ObjectNode rootNode = (ObjectNode)objectMapper.readTree(contents);
            String[] pathParts = path.split("-");

            ObjectNode node = rootNode;
            for (int i = 0; i < pathParts.length - 1; i++) {
                String part = pathParts[i];
                if (!node.has(part)) {
                    node.set(part, objectMapper.createObjectNode()); // 如果路径不存在，创建
                }
                node = (ObjectNode) node.get(part);
            }

            // 设置或替换目标节点
            String finalPart = pathParts[pathParts.length - 1];
            // 设置之前置顶的帖子
            JsonNode finalNode = node.get(finalPart);
            if (finalNode != null && finalNode.has(systemProperties.getContents().getPinField())) {
                newNode.put(systemProperties.getContents().getPinField(), node.get(finalPart).get(systemProperties.getContents().getPinField()));
            }
            node.set(finalPart, newNode);
            return objectMapper.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    /**
     * 管理目录节点的置顶帖子列表
     * 
     * 置顶帖子存储在节点的特殊字段中，支持添加和删除操作。
     * 实现了重复检查和数量限制控制。
     * 
     * @param contents 当前目录的JSON字符串
     * @param path 节点路径
     * @param value 帖子ID
     * @param add true=添加置顶，false=取消置顶
     * @return 更新后的目录JSON字符串
     * @throws BusinessException 当JSON处理失败或置顶数量超限时抛出异常
     */
    private String modifyPinOfContents(String contents, String path, long value, boolean add) {
        try {
            ObjectNode rootNode = (ObjectNode)objectMapper.readTree(contents);
            ObjectNode node = rootNode;

            String[] pathParts = path.split("-");

            for (int i = 0; i <= pathParts.length - 1; i++) {
                String part = pathParts[i];
                if (!node.has(part)) {
                    node.set(part, objectMapper.createObjectNode()); // 如果路径不存在，创建
                }
                node = (ObjectNode) node.get(part);
            }

            // 设置或替换目标节点
            ArrayNode pinedArray = ((ArrayNode)node.get(systemProperties.getContents().getPinField()));
            if (pinedArray == null) {
                pinedArray = objectMapper.createArrayNode();
                node.put(systemProperties.getContents().getPinField(), pinedArray);
            }
            if (add) {
                boolean exist = false;
                for (int i = 0; i < pinedArray.size(); i++) {
                    if (pinedArray.get(i).asInt() == value) {
                        exist = true;
                    }
                }
                if (pinedArray.size() >= systemProperties.getContents().getMaxPinnedItems()) {
                    throw StatusCode.TOC_PINNED_ITEMS_LIMIT_EXCEEDED.exception();
                }
                if (!exist) pinedArray.add(value);
            } else {
                for (int i = 0; i < pinedArray.size(); i++) {
                    if (pinedArray.get(i).asInt() == value) {
                        pinedArray.remove(i);
                        break;
                    }
                }
            }
            return objectMapper.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }
}
