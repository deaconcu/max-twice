package com.prosper.learn.content.toc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * 目录管理服务
 *
 * 负责管理用户节点目录的增删改查操作，包括：
 * - 目录结构的获取和创建
 * - 目录类型的帖子可以加入到左侧目录中
 *
 * 核心概念：
 * - 每个用户对每个节点都有独立的目录结构
 * - 目录内容采用引用计数机制管理，支持多用户共享相同的目录版本
 * - 目录以 JSON 格式存储，支持嵌套结构
 *
 * @author Claude
 * @since 2024-01-20
 */
@Service
@RequiredArgsConstructor
public class TocDomainService {

    /** JSON 对象映射器，用于目录结构的序列化和反序列化 */
    private final ObjectMapper objectMapper;

    /** 帖子数据访问接口 */
    private final PostDataService postDataService;

    /** 用户节点目录数据访问接口 */
    private final UserNodeTocDataService userNodeTocDataService;

    /** 节点目录数据访问接口，管理目录内容和引用计数 */
    private final NodeTocDataService nodeTocDataService;

    /** 内容管理相关配置属性 */
    private final SystemProperties systemProperties;

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
     * 检查指定用户在指定节点下是否已经创建了目录结构。
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @return 用户节点目录实体对象
     * @throws BusinessException 当用户目录不存在时抛出 TOC_USER_TOC_NOT_FOUND 异常
     */
    private UserNodeTocDO validateUserTocExists(long userId, long nodeId) {
        UserNodeTocDO userNodeTocDO = userNodeTocDataService.getByUserAndNode(userId, nodeId);
        if (userNodeTocDO == null) {
            throw StatusCode.TOC_USER_TOC_NOT_FOUND.exception();
        }
        return userNodeTocDO;
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
     * 更新用户节点目录
     *
     * 根据提供的索引数组重新组织用户的节点目录结构。
     * 索引数组中的每个值对应当前目录哈希数组中的位置（从1开始），
     * 特殊值0表示使用默认目录（只包含该节点）。
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param indexArray 索引数组字符串（逗号分隔），如 "1,2,0,3"
     *                   - 0: 使用默认目录
     *                   - 正整数: 使用现有目录数组中对应位置的哈希（从1开始）
     * @throws BusinessException 当节点不存在、用户目录不存在、索引无效等情况时抛出异常
     */
    @Transactional
    public void updateUserNodeToc(long userId, long nodeId, String indexArray) {
        // 1. 验证用户目录存在性
        UserNodeTocDO userNodeTocDO = validateUserTocExists(userId, nodeId);

        // 2. 解析当前目录哈希数组
        String toc = userNodeTocDO.getToc();
        String[] tocHashes = toc.split(",");

        // 3. 验证并解析索引数组
        int[] indexes = validateAndParseIndexArray(indexArray, 9, tocHashes.length);

        // 4. 生成默认目录结构
        ObjectNode defaultTocNode = objectMapper.createObjectNode();
        defaultTocNode.set(Long.toString(nodeId), objectMapper.createObjectNode());
        String defaultTocStr = defaultTocNode.toString();
        String defaultTocHash = Utils.hashSHA(defaultTocStr);

        // 5. 如果默认目录不存在则创建
        if (nodeTocDataService.get(defaultTocHash) == null) {
            nodeTocDataService.insert(new NodeTocDO(defaultTocHash, defaultTocStr));
        }

        // 6. 构建新的目录数组
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

        // 7. 增加默认目录的引用计数
        if (defaultCount > 0) {
            nodeTocDataService.incrRef(defaultTocHash, defaultCount);
        }

        // 8. 更新用户节点目录
        String newToc = String.join(",", newTocArr);
        userNodeTocDO.setToc(newToc);
        userNodeTocDataService.update(userNodeTocDO);
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
     * @param userNodeTocDO 用户节点目录对象
     * @param contentUpdater 内容更新函数，接收当前目录内容，返回新的目录内容
     */
    private void getCurrentTocAndUpdate(String[] tocHashArr, int tocIndex, UserNodeTocDO userNodeTocDO, 
                                       java.util.function.Function<String, String> contentUpdater) {
        // 1. 获取当前目录内容并减少引用计数
        NodeTocDO nodeTocDO = nodeTocDataService.get(tocHashArr[tocIndex - 1]);
        nodeTocDataService.incrRef(nodeTocDO.getHash(), -1);
        String currentTocContent = nodeTocDO.getToc();
        
        // 2. 应用内容更新操作
        String newTocContent = contentUpdater.apply(currentTocContent);
        
        // 3. 保存新版本并增加引用计数
        String hash = Utils.hashSHA(newTocContent);
        if (nodeTocDataService.get(hash) == null) {
            nodeTocDataService.insert(new NodeTocDO(hash, newTocContent));
        }
        nodeTocDataService.incrRef(hash, 1);

        // 4. 更新用户目录指向
        tocHashArr[tocIndex - 1] = hash;
        userNodeTocDO.setToc(String.join(",", tocHashArr));
        userNodeTocDataService.update(userNodeTocDO);
    }

    /**
     * 获取用户在指定节点下的完整目录结构
     *
     * 目录结构采用分层设计：
     * 1. 每个用户节点有一个目录哈希列表，支持多版本目录
     * 2. 每个哈希对应一个具体的目录JSON内容
     * 3. 目录内容使用引用计数管理，支持多用户共享
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param create 当用户目录不存在时是否自动创建
     * @return 目录结构的JSON数组，如果不存在且不创建则返回null
     */
    public ArrayNode getToc(long userId, long nodeId, boolean create) {
        UserNodeTocDO userNodeTocDO = userNodeTocDataService.getByUserAndNode(userId, nodeId);
        String tocHashStr = "";

        // 如果用户目录不存在且不需要创建，直接返回null
        if (userNodeTocDO == null && !create) return null;

        if (userNodeTocDO == null) {
            // 创建根目录结构：包含课程根节点的目录
            ObjectNode s = objectMapper.createObjectNode();
            ObjectNode rootNodeContent = objectMapper.createObjectNode();

            // 自动生成默认目录：查找该节点下 score 最高的 index 类型 Post
            List<PostDO> topPosts = postDataService.getListByNodeAndScore(
                    nodeId, 1, Enums.ContentState.PUBLISHED.value());

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

            s.put(Long.toString(nodeId), rootNodeContent);

            String tocStr = s.toString();
            tocHashStr = Utils.hashSHA(tocStr);

            // 检查是否已存在相同内容的目录，避免重复存储
            NodeTocDO nodeTocDO = nodeTocDataService.get(tocHashStr);
            if (nodeTocDO == null) {
                NodeTocDO newToc = new NodeTocDO();
                newToc.setHash(tocHashStr);
                newToc.setToc(tocStr);
                newToc.setRefCount(1);
                nodeTocDataService.insert(newToc);
            }

            // 为用户创建目录记录，指向刚创建的目录版本
            userNodeTocDO = new UserNodeTocDO();
            userNodeTocDO.setUserId(userId);
            userNodeTocDO.setNodeId(nodeId);
            userNodeTocDO.setToc(tocHashStr);

            userNodeTocDataService.insert(userNodeTocDO);
        } else {
            // 用户目录已存在，获取目录哈希列表
            tocHashStr = userNodeTocDO.getToc();
        }

        ArrayNode arrayNode = objectMapper.createArrayNode();
        String[] tocHashArr = tocHashStr.split(",");

        // 批量获取所有目录版本的内容
        Map<String, NodeTocDO> map = nodeTocDataService.getByHashes(tocHashArr);
        for(String tocHash: tocHashArr) {
            try {
                NodeTocDO nodeTocDO = map.get(tocHash);
                if (nodeTocDO == null) {
                    // 数据不一致：目录哈希在数据库中不存在
                    throw StatusCode.TOC_USER_TOC_NOT_FOUND.exception();
                }
                arrayNode.add(objectMapper.readTree(nodeTocDO.getToc()));
            } catch (IOException e) {
                throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
            }
        }

        return arrayNode;
    }

    /**
     * 获取用户在指定节点下特定索引位置的目录内容
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param tocIndex 目录索引（从1开始）
     * @return 指定索引位置的目录JSON字符串，如果用户目录不存在则返回null
     * @throws BusinessException 当索引越界时抛出异常
     */
    public String getToc(long userId, long nodeId, int tocIndex) {
        UserNodeTocDO userNodeTocDO = userNodeTocDataService.getByUserAndNode(userId, nodeId);
        String tocStr = "";
        if (userNodeTocDO == null) return null;

        tocStr = userNodeTocDO.getToc();

        ArrayNode arrayNode = objectMapper.createArrayNode();
        String[] tocHashArr = tocStr.split(",");

        validateTocIndex(tocIndex, tocHashArr);

        NodeTocDO nodeTocDO = nodeTocDataService.get(tocHashArr[tocIndex - 1]);
        return nodeTocDO.getToc();
    }

    /**
     * 将帖子内容选择添加到用户节点目录的指定路径下
     *
     * 业务流程：
     * 1. 验证帖子的有效性
     * 2. 创建包含帖子内容的子节点
     * 3. 获取用户当前目录并减少旧版本引用计数
     * 4. 更新目录结构并保存新版本
     * 5. 增加新版本引用计数并更新用户目录指向
     *
     * @param userId 用户ID
     * @param path 目录路径，格式：{tocIndex}-{nodePath}，如 "1-chapter1-section1"
     * @param nodeId 节点ID
     * @param postId 帖子ID（必须是内容类型，不能是文章类型）
     * @throws BusinessException 当帖子不存在或帖子类型无效时抛出异常
     */
    @Transactional
    public void choose(long userId, String path, long nodeId, long postId) {
        // validate - 验证帖子
        PostDO postDO = validatePostForContents(postId);

        // 创建childNode
        ObjectNode childNode = objectMapper.createObjectNode();
        Arrays.stream(postDO.getContent().split(",")).forEach(id->childNode.putObject((id)));
        childNode.put(systemProperties.getContents().getChosenField(), postId);

        String[] pathParts = path.split("-", 2);
        int tocIndex = Integer.parseInt(pathParts[0]);

        // get user toc hash
        UserNodeTocDO userNodeTocDO = validateUserTocExists(userId, nodeId);

        String[] tocHashArr = userNodeTocDO.getToc().split(",");
        validateTocIndex(tocIndex, tocHashArr);

        // 使用通用方法完成目录更新
        getCurrentTocAndUpdate(tocHashArr, tocIndex, userNodeTocDO, 
            currentTocContent -> updateContents(currentTocContent, pathParts[1], childNode));
    }

    /**
     * 取消选择用户节点目录指定路径下的内容
     *
     * 将指定路径下的内容清空，用空的JSON对象替换原有内容。
     * 采用与choose相同的引用计数管理机制。
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param path 目录路径，格式：{tocIndex}-{nodePath}
     * @throws BusinessException 当用户目录不存在或索引越界时抛出异常
     */
    public void unchoose(long userId, long nodeId, String path) {
        String[] pathParts = path.split("-", 2);
        int tocIndex = Integer.parseInt(pathParts[0]);

        // get user toc hash
        UserNodeTocDO userNodeTocDO = validateUserTocExists(userId, nodeId);

        String[] tocHashArr = userNodeTocDO.getToc().split(",");
        validateTocIndex(tocIndex, tocHashArr);

        // 使用通用方法完成目录更新
        getCurrentTocAndUpdate(tocHashArr, tocIndex, userNodeTocDO,
            currentTocContent -> updateContents(currentTocContent, pathParts[1], objectMapper.createObjectNode()));
    }


    /**
     * 更新目录结构中指定路径的节点内容
     *
     * 核心的目录更新逻辑，支持嵌套路径的导航和节点替换。
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
            node.set(finalPart, newNode);
            return objectMapper.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    /**
     * 批量获取用户节点目录内容（目录索引固定为1）
     *
     * @param userId 用户ID
     * @param nodeIds 节点ID列表
     * @return Map<nodeId, tocContent> 节点ID到目录内容的映射
     */
    public Map<Long, String> batchGetToc(long userId, List<Long> nodeIds) {
        Map<Long, String> result = new HashMap<>();

        if (nodeIds == null || nodeIds.isEmpty()) {
            return result;
        }

        // 1. 批量查询用户节点目录
        Map<Long, UserNodeTocDO> userTocMap = userNodeTocDataService.getByUserAndNodes(userId, nodeIds);

        // 2. 收集所有需要查询的 hash（固定使用索引1）
        Set<String> hashesToFetch = new HashSet<>();
        Map<Long, String> nodeIdToHash = new HashMap<>();

        for (Map.Entry<Long, UserNodeTocDO> entry : userTocMap.entrySet()) {
            Long nodeId = entry.getKey();
            UserNodeTocDO userNodeTocDO = entry.getValue();

            if (userNodeTocDO == null || userNodeTocDO.getToc() == null) {
                continue;
            }

            String[] tocHashArr = userNodeTocDO.getToc().split(",");

            // 索引1对应数组下标0
            if (tocHashArr.length > 0) {
                String hash = tocHashArr[0];
                hashesToFetch.add(hash);
                nodeIdToHash.put(nodeId, hash);
            }
        }

        // 3. 批量查询 node_toc
        if (!hashesToFetch.isEmpty()) {
            Map<String, NodeTocDO> nodeTocMap = nodeTocDataService.getByHashes(
                hashesToFetch.toArray(new String[0])
            );

            // 4. 组装结果
            for (Map.Entry<Long, String> entry : nodeIdToHash.entrySet()) {
                Long nodeId = entry.getKey();
                String hash = entry.getValue();
                NodeTocDO nodeTocDO = nodeTocMap.get(hash);

                if (nodeTocDO != null && nodeTocDO.getToc() != null) {
                    result.put(nodeId, nodeTocDO.getToc());
                }
            }
        }

        return result;
    }

}
