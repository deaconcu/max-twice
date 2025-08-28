package com.prosper.learn.api.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.api.client.PostClient;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.dto.PostDTO;
import com.prosper.learn.dto.Response;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Utils;
import com.prosper.learn.domain.service.PostingService;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class PostController implements PostClient {

    private final PostMapper postMapper;
    private final ObjectMapper objectMapper;
    private final NodeMapper nodeMapper;
    private final PostingService postService;

    @Override
    public Response create(PostDTO posting) {
        if (posting.getType() == Enums.PostType.contents.value) {
            try {
                NodeDO nodeDO = nodeMapper.getById(posting.getNodeId());
                List<String> nodeNames = objectMapper.readValue(posting.getContent(), new TypeReference<>() {});
                String[] ids = new String[nodeNames.size()];
                for (int i = 0; i < nodeNames.size(); i ++) {
                    NodeDO node = new NodeDO();
                    node.setName(nodeNames.get(i));
                    node.setDescription("");
                    node.setRoot(0l);
                    node.setCourseId(nodeDO.getCourseId());
                    node.setCreatedAt(Utils.getLocalDateTime());
                    node.setUpdatedAt(Utils.getLocalDateTime());
                    nodeMapper.insert(node);
                    ids[i] = Long.toString(node.getId());
                }
                posting.setContent(String.join(",", ids));

            } catch (JsonProcessingException e) {
                log.error("Failed to process JSON content", e);
                throw ErrorCode.JSON_PARSE_ERROR.exception(e);
            }
        }

        posting.setCreatorId(0l);
        posting.setCreatedAt(Utils.getTimeString());
        postMapper.insert(Converter.INSTANCE.toPostDO(posting));
        return Response.success;
    }

    @Override
    public Response modify(PostDTO posting) {
        PostDO postDO = postMapper.get(posting.getId());
        if (postDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        postDO.setContent(posting.getContent());
        postDO.setUpdatedAt(Utils.getLocalDateTime());
        postMapper.update(postDO);
        return Response.success;
    }

    @Override
    public Response delete(Long id) {
        PostDO postDO = postMapper.get(id);
        if (postDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        postDO.setState(Enums.PostState.deleted.value);
        postDO.setUpdatedAt(Utils.getLocalDateTime());
        postMapper.update(postDO);
        return Response.success;
    }

    @Override
    public PostDTO get(Long id) {
        return Converter.INSTANCE.toPostDTO(postService.get(id));
    }

    @Override
    public List<PostDTO> getPostings(Long nodeId) {
        int count = 3;
        List<PostDO> postings = postMapper.getListByNode(nodeId, count, Enums.PostState.approved.value);
        postings.forEach(postingDO -> postService.idToName(postingDO));
        return Converter.INSTANCE.toPostDTO(postings);
    }

    @Override
    public List<PostDTO> getByLastId(Long nodeId, Long lastPostingId) {
        int count = 2;
        List<PostDO> postings = postMapper.getListByLastId(nodeId, lastPostingId, count, Enums.PostState.approved.value);
        postings.forEach(postingDO -> postService.idToName(postingDO));
        return Converter.INSTANCE.toPostDTO(postings);
    }

    @Override
    public Response<Object> get(List<Long> ids, Long nodeId, Long lastPostingId) {
        if (ids != null && ids.size() > 0) {
            List<PostDO> postings = postMapper.getByIds(ids);
            postings.forEach(postingDO -> postService.idToName(postingDO));
            return new Response<>(Converter.INSTANCE.toPostDTO(postings));
        } else if (nodeId > 0){
            int count = 2;
            List<PostDO> postings = postMapper.getListByLastId(nodeId, lastPostingId, count, Enums.PostState.approved.value);
            postings.forEach(postingDO -> postService.idToName(postingDO));
            return new Response<>(Converter.INSTANCE.toPostDTO(postings));
        }
        throw ErrorCode.SYSTEM_ERROR.exception();
    }

    @Override
    public Response<List<PostDTO>> getCensorList() {
        List<PostDO> postDOList = postMapper.getListByState(Enums.PostState.approved.value, 200);
        for (PostDO postDO : postDOList) {
            if (postDO.getType() == Enums.PostType.contents.value) {
                postService.idToName(postDO);
            }
        }
        return new Response<>(Converter.INSTANCE.toPostDTO(postDOList));
    }

    @Override
    public Response<Object> approve(Long id, boolean approve) {
        PostDO postDO = postMapper.get(id);
        if (postDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        if (approve && postDO.getState() != Enums.PostState.approved.value) {
            postDO.setState(Enums.CommentState.approved.value);
            postMapper.update(postDO);
        }
        if (!approve && postDO.getState() != Enums.CommentState.deleted.value) {
            postDO.setState(Enums.CommentState.deleted.value);
            postMapper.update(postDO);
        }
        return new Response(Converter.INSTANCE.toPostDTO(postDO));
    }

    // 选择一个目录
    /*
    @Override
    public Response choose(int postingId, int courseId, String currentPath, int userId) {
        ContentsDO contentsDO = contentsMapper.getByUser(1);
        PostDO postDO = postMapper.get(postingId);

        if (currentPath.isEmpty()) {
            String contents = postDO.getContent();
            ObjectNode node = objectMapper.createObjectNode();
            ObjectNode emptyNode = objectMapper.createObjectNode();
            for (String s : contents.split(",")) {
                node.set(s, emptyNode);
            }
            String jsonString = "";
            try {
                jsonString = objectMapper.writeValueAsString(node);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize JSON", e);
                throw ErrorCode.JSON_PARSE_ERROR.exception(e);
            }

            if (contentsDO == null) {
                contentsDO = new ContentsDO();
                contentsDO.setUserId(1);
                contentsDO.setContents(jsonString);
                contentsDO.setCTime(Utils.getLocalDateTime());
                contentsDO.setUTime(Utils.getLocalDateTime());
                contentsMapper.insert(contentsDO);
            } else {
                contentsDO.setContents(jsonString);
                contentsMapper.update(contentsDO);
            }
            return Response.success;
        }

        if (contentsDO == null) {
            return Response.success;
        }

        String contentsStr = contentsDO.getContents();

        try {
            JsonNode rootNode = objectMapper.readTree(contentsStr);
            JsonNode currNode = rootNode.at(currentPath);
            if (currNode.isObject()) {
                ObjectNode obj = (ObjectNode) currNode;
                obj.removeAll();

                ObjectNode emptyNode = objectMapper.createObjectNode();
                for (String s : postDO.getContent().split(",")) {
                    obj.set(s, emptyNode);
                }
            }

            String jsonString = objectMapper.writeValueAsString(rootNode);
            contentsDO.setContents(jsonString);
            contentsMapper.update(contentsDO);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize JSON content", e);
            throw ErrorCode.JSON_PARSE_ERROR.exception(e);
        }
        return Response.success;
    }
    */

    /**
     * 获取基于分数排序的文章列表
     * @param nodeId 节点ID
     * @param limit 返回数量限制
     * @return 按分数排序的文章列表
     */
    @GetMapping("/post/list/by-score")
    public Response<List<PostDTO>> getPostsByScore(@RequestParam int nodeId,
                                                  @RequestParam(defaultValue = "10") int limit) {
        List<PostDO> postings = postService.getListByScore(nodeId, limit);
        return new Response<>(Converter.INSTANCE.toPostDTO(postings));
    }

    /**
     * 基于分数的分页查询
     * @param nodeId 节点ID
     * @param lastScore 上一页最后一个文章的分数（可选）
     * @param lastId 上一页最后一个文章的ID（可选）
     * @param limit 返回数量限制
     * @return 按分数排序的文章列表
     */
    @GetMapping("/post/list/by-score/pagination")
    public Response<List<PostDTO>> getPostsByScoreWithPagination(@RequestParam int nodeId,
                                                                @RequestParam(required = false) Double lastScore,
                                                                @RequestParam(defaultValue = "0") int lastId,
                                                                @RequestParam(defaultValue = "10") int limit) {
        List<PostDO> postings = postService.getListByScoreWithPagination(nodeId, lastScore, lastId, limit);
        return new Response<>(Converter.INSTANCE.toPostDTO(postings));
    }
}
