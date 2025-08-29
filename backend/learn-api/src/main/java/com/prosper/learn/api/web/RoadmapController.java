package com.prosper.learn.api.web;

import com.prosper.learn.api.client.RoadmapClient;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.business.RoadmapService;
import com.prosper.learn.domain.service.basic.ScoreCalculationService;
import com.prosper.learn.domain.service.business.UpvoteService;
import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.RoadmapDTO;
import com.prosper.learn.persistence.mapper.RoadmapMapper;
import com.prosper.learn.persistence.mapper.UserProfileMapper;
import com.prosper.learn.persistence.mapper.UserRoadmapMapper;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.dataobject.UserProfileDO;
import com.prosper.learn.domain.util.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Slf4j
//@RestController
@RequiredArgsConstructor
public class RoadmapController implements RoadmapClient {

    private final RoadmapService roadmapService;
    private final ScoreCalculationService scoreCalculationService;
    private final RoadmapMapper roadmapMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRoadmapMapper userRoadmapMapper;
    private final UpvoteService upvoteService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Response<List<RoadmapDTO>> getListByProfession(Long professionId, Long lastId) {
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        List<RoadmapDO> roadmapList = new ArrayList<>();
        int limit = 20; // 默认每页20条

        List<Long> pinnedRoadmapIds = new ArrayList<>();
        if (lastId == null || lastId == 0) {
            // 获取当前用户的置顶路线
            int userId = StpUtil.getLoginIdAsInt();
            UserProfileDO userProfile = userProfileMapper.getById(userId);

            if (userProfile != null && userProfile.getRoadmapPin() != null) {
                try {
                    // 解析 roadmapPin JSON: {"1": [26, 28, 27, 37, 15, 18, 19]}
                    Map<String, List<Long>> pinMap = objectMapper.readValue(
                        userProfile.getRoadmapPin(), new TypeReference<>() {}
                    );

                    List<Long> professionPins = pinMap.get(professionId.toString());
                    if (professionPins != null && !professionPins.isEmpty()) {
                        pinnedRoadmapIds = professionPins;
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse roadmap pin config for userId: {}", StpUtil.getLoginIdAsInt(), e);
                    throw ErrorCode.JSON_PARSE_ERROR.exception(e);
                }
            }

            // 先获取置顶的路线图
            if (!pinnedRoadmapIds.isEmpty()) {
                List<RoadmapDO> pinnedRoadmaps = roadmapMapper.getByIds(pinnedRoadmapIds);
                roadmapList.addAll(pinnedRoadmaps);
            }

            // 如果置顶的路线图不够20个，补充其他路线图（按score排序）
            int remainingLimit = limit - roadmapList.size();
            if (remainingLimit > 0) {
                List<RoadmapDO> otherRoadmaps = roadmapMapper.getListByProfessionExcludingOrderByScore(
                    professionId, 0, remainingLimit, pinnedRoadmapIds);
                roadmapList.addAll(otherRoadmaps);
            }
        } else {
            // 分页查询，按score排序
            // 根据lastId获取对应的score值
            RoadmapDO lastRoadmap = roadmapMapper.get(lastId);
            if (lastRoadmap != null) {
                roadmapList = roadmapMapper.getListByProfessionAfterScoreExcluding(
                        professionId, lastRoadmap.getScore(), lastId, limit, null);
            }
        }

        List<RoadmapDTO> dtoList = Converter.INSTANCE.toRoadMapDTO(roadmapList);

        // 获取当前用户ID，用于批量查询状态
        long userId = StpUtil.getLoginIdAsLong();

        // 批量设置 upvoted 和 pinned 状态
        if (!dtoList.isEmpty()) {
            // 1. 批量查询点赞状态
            List<Long> roadmapIds = dtoList.stream()
                .map(RoadmapDTO::getId)
                .collect(Collectors.toList());

            Set<Long> upvotedIds = upvoteService.getUpvotedRoadmapIds(roadmapIds, userId);

            // 2. 获取置顶状态
            Set<Long> pinnedIds = new HashSet<>();
            if (lastId == null || lastId == 0) {
                // 首页时，pinnedRoadmapIds 就是置顶的ID
                pinnedIds.addAll(pinnedRoadmapIds);
            } else {
                // 分页时需要重新获取置顶信息
                UserProfileDO userProfile = userProfileMapper.getById(userId);
                if (userProfile != null && userProfile.getRoadmapPin() != null) {
                    try {
                        Map<String, List<Long>> pinMap = objectMapper.readValue(
                            userProfile.getRoadmapPin(), new TypeReference<>() {}
                        );
                        List<Long> professionPins = pinMap.get(professionId.toString());
                        if (professionPins != null) {
                            pinnedIds.addAll(professionPins);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse pin config for pagination", e);
                        throw ErrorCode.JSON_PARSE_ERROR.exception(e);
                    }
                }
            }

            // 3. 批量查询学习状态
            List<Long> learningRoadmapIds = userRoadmapMapper.getBatchLearningStatus(userId, roadmapIds);
            Set<Long> learningIds = new HashSet<>(learningRoadmapIds);

            // 4. 设置状态
            for (RoadmapDTO dto : dtoList) {
                dto.setUpvoted(upvotedIds.contains(dto.getId()));
                dto.setPinned(pinnedIds.contains(dto.getId()));
                dto.setLearning(learningIds.contains(dto.getId()));
            }
        }

        // 转换每个路线图的 content 格式
        for (RoadmapDTO dto : dtoList) {
            if (dto.getContent() != null) {
                String formattedContent = roadmapService.parseContentToGraphFormat(dto.getContent(), userId);
                dto.setContent(formattedContent);
            }
        }

        return Response.success(dtoList);
    }

    @Override
    public Response<Void> putById(Long id, String content) {
        // 验证用户是否登录
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        // 验证content格式
        if (!RoadmapService.isValidContentFormat(content)) {
            throw ErrorCode.ROADMAP_CONTENT_INVALID.exception();
        }

        RoadmapDO roadmapDO = roadmapMapper.get(id.intValue());
        if (roadmapDO == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        // 检查权限（简单起见，只允许创建者修改）
        int currentUserId = StpUtil.getLoginIdAsInt();
        if (roadmapDO.getCreatorId() != currentUserId) {
            throw ErrorCode.PERMISSION_DENIED.exception();
        }

        roadmapDO.setContent(content);
        roadmapDO.setContentHash(RoadmapService.calculateContentHash(content));
        roadmapDO.setUpdatedAt(LocalDateTime.now());

        roadmapMapper.update(roadmapDO);
        return Response.success(null);
    }

    @Override
    public Response<Object> upvote(Long id) {
        // 验证用户是否登录
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        int userId = StpUtil.getLoginIdAsInt();
        // 使用点赞帖子的方法，类型为1（once）
        boolean voted = upvoteService.upvoteRoadmap(id.intValue(), userId);

        int voteDelta = voted ? 1 : -1;
        roadmapMapper.updateVoteCount(id.intValue(), voteDelta);

        // 获取最新的roadmap数据
        RoadmapDO roadmapDO = roadmapMapper.get(id.intValue());

        // 实时更新评分
        try {
            scoreCalculationService.checkAndUpdateRoadmapScore(roadmapDO);
            // 重新获取更新后的数据
            roadmapDO = roadmapMapper.get(id.intValue());
        } catch (Exception e) {
            // 评分更新失败不应该影响点赞操作
            log.warn("更新roadmap评分失败: roadmapId={}", id, e);
        }

        RoadmapDTO roadmapDTO = Converter.INSTANCE.toRoadMapDTO(roadmapDO);
        roadmapDTO.setUpvoted(voted);

        return Response.success(roadmapDTO);
    }

    @Override
    public Response<Long> post(Long professionId, String content, String description) {
        // 验证用户是否登录
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        // 验证content格式
        if (!RoadmapService.isValidContentFormat(content)) {
            throw ErrorCode.ROADMAP_CONTENT_INVALID.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();

        // 创建新的路线图
        RoadmapDO roadmapDO = new RoadmapDO();
        roadmapDO.setProfessionId(professionId);
        roadmapDO.setCreatorId(userId);
        roadmapDO.setContent(content);
        roadmapDO.setDescription(description);
        roadmapDO.setContentHash(RoadmapService.calculateContentHash(content));
        roadmapDO.setVote(0);
        roadmapDO.setComment(0);
        roadmapDO.setCreatedAt(LocalDateTime.now());
        roadmapDO.setUpdatedAt(LocalDateTime.now());

        roadmapMapper.insert(roadmapDO);
        return Response.success(roadmapDO.getId());
    }

    @Override
    public Response<RoadmapDTO> getById(Long id) {
        int userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : 0;
        RoadmapDTO roadmapDTO = roadmapService.getById(id.intValue(), userId);

        if (roadmapDTO == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        // 转换 content 格式
        if (roadmapDTO.getContent() != null) {
            String formattedContent = roadmapService.parseContentToGraphFormat(roadmapDTO.getContent(), userId);
            roadmapDTO.setContent(formattedContent);
        }

        return Response.success(roadmapDTO);
    }

    @Override
    public Response<Object> pin(Long professionId, Long roadmapId) {
        // 验证用户是否登录
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();

        // 获取用户配置
        UserProfileDO userProfile = userProfileMapper.getById(userId);
        Map<String, List<Long>> pinMap = new HashMap<>();

        // 解析现有的置顶配置
        if (userProfile != null && userProfile.getRoadmapPin() != null) {
            try {
                pinMap = objectMapper.readValue(userProfile.getRoadmapPin(), new TypeReference<>() {});
            } catch (Exception e) {
                // 如果解析失败，使用空的 Map
                log.warn("Failed to parse roadmap pin config for userId: {}", userId, e);
                throw ErrorCode.JSON_PARSE_ERROR.exception(e);
            }
        }

        String professionKey = String.valueOf(professionId);
        List<Long> professionPins = pinMap.getOrDefault(professionKey, new ArrayList<>());

        boolean isPinned = professionPins.contains(roadmapId);
        String message;

        if (isPinned) {
            // 如果已置顶，则取消置顶
            professionPins.remove(roadmapId);
            message = "unpinned";
        } else {
            // 如果未置顶，则添加置顶（最多19个，保证加上至少1个非置顶的总共20个）
            if (professionPins.size() >= 19) {
                throw ErrorCode.ROADMAP_PIN_LIMIT_EXCEEDED.exception();
            }
            professionPins.add(roadmapId);
            message = "pinned";
        }

        // 更新 pinMap
        if (professionPins.isEmpty()) {
            pinMap.remove(professionKey);
        } else {
            pinMap.put(professionKey, professionPins);
        }

        // 序列化并更新数据库
        String updatedPinJson;
        try {
            updatedPinJson = objectMapper.writeValueAsString(pinMap);
        } catch (Exception e) {
            log.error("Failed to serialize pin config for userId: {}", userId, e);
            throw ErrorCode.JSON_PARSE_ERROR.exception(e);
        }

        if (userProfile == null) {
            // 如果用户配置不存在，创建新的
            userProfile = new UserProfileDO();
            userProfile.setUserId(userId);
            userProfile.setRoadmapPin(updatedPinJson);
            userProfile.setSubscription(""); // 设置默认值
            userProfileMapper.insert(userProfile);
        } else {
            // 更新现有配置
            userProfileMapper.updateRoadmapPin(userId, updatedPinJson);
        }

        return Response.success(message);
    }
}
