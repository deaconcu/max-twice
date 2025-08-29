package com.prosper.learn.api.v1.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.RoadmapService;
import com.prosper.learn.domain.service.ScoreCalculationService;
import com.prosper.learn.domain.service.UpvoteService;
import com.prosper.learn.dto.RoadmapDTO;
import com.prosper.learn.persistence.mapper.RoadmapMapper;
import com.prosper.learn.persistence.mapper.UserProfileMapper;
import com.prosper.learn.persistence.mapper.UserRoadmapMapper;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.dataobject.UserProfileDO;
import com.prosper.learn.domain.util.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
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

/**
 * 路线图接口
 * 从RoadmapClient迁移而来
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoadmapsController {

    private final RoadmapService roadmapService;
    private final ScoreCalculationService scoreCalculationService;
    private final RoadmapMapper roadmapMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRoadmapMapper userRoadmapMapper;
    private final UpvoteService upvoteService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取职业路线图
     * 映射: GET /roadmap/list/{professionId} → GET /api/v1/professions/{professionId}/roadmaps?lastId=123
     */
    @GetMapping("/professions/{professionId}/roadmaps")
    public ApiResponse<List<RoadmapDTO>> getRoadmapsByProfession(
            @PathVariable Long professionId, 
            @RequestParam(required = false, defaultValue = "0") Long lastId) {
        
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        List<RoadmapDO> roadmapList = new ArrayList<>();
        int limit = 20;

        List<Long> pinnedRoadmapIds = new ArrayList<>();
        if (lastId == null || lastId == 0) {
            int userId = StpUtil.getLoginIdAsInt();
            UserProfileDO userProfile = userProfileMapper.getById(userId);

            if (userProfile != null && userProfile.getRoadmapPin() != null) {
                Map<String, List<Long>> pinMap = null;
                try {
                    pinMap = objectMapper.readValue(userProfile.getRoadmapPin(), new TypeReference<>() {});
                } catch (JsonProcessingException e) {
                    throw ErrorCode.SYSTEM_ERROR.exception(e);
                }

                List<Long> professionPins = pinMap.get(professionId.toString());
                if (professionPins != null && !professionPins.isEmpty()) {
                    pinnedRoadmapIds = professionPins;
                }
            }

            if (!pinnedRoadmapIds.isEmpty()) {
                List<RoadmapDO> pinnedRoadmaps = roadmapMapper.getByIds(pinnedRoadmapIds);
                roadmapList.addAll(pinnedRoadmaps);
            }

            int remainingLimit = limit - roadmapList.size();
            if (remainingLimit > 0) {
                List<RoadmapDO> otherRoadmaps = roadmapMapper.getListByProfessionExcludingOrderByScore(
                    professionId, 0, remainingLimit, pinnedRoadmapIds);
                roadmapList.addAll(otherRoadmaps);
            }
        } else {
            RoadmapDO lastRoadmap = roadmapMapper.get(lastId);
            if (lastRoadmap != null) {
                roadmapList = roadmapMapper.getListByProfessionAfterScoreExcluding(
                        professionId, lastRoadmap.getScore(), lastId, limit, null);
            }
        }

        List<RoadmapDTO> dtoList = Converter.INSTANCE.toRoadMapDTO(roadmapList);

        long userId = StpUtil.getLoginIdAsLong();

        if (!dtoList.isEmpty()) {
            List<Long> roadmapIds = dtoList.stream()
                .map(RoadmapDTO::getId)
                .collect(Collectors.toList());

            Set<Long> upvotedIds = upvoteService.getUpvotedRoadmapIds(roadmapIds, userId);

            Set<Long> pinnedIds = new HashSet<>();
            if (lastId == null || lastId == 0) {
                pinnedIds.addAll(pinnedRoadmapIds);
            } else {
                UserProfileDO userProfile = userProfileMapper.getById(userId);
                if (userProfile != null && userProfile.getRoadmapPin() != null) {
                    Map<String, List<Long>> pinMap = null;
                    try {
                        pinMap = objectMapper.readValue(userProfile.getRoadmapPin(), new TypeReference<>() {});
                    } catch (JsonProcessingException e) {
                        throw ErrorCode.SYSTEM_ERROR.exception(e);
                    }
                    List<Long> professionPins = pinMap.get(professionId.toString());
                    if (professionPins != null) {
                        pinnedIds.addAll(professionPins);
                    }
                }
            }

            List<Long> learningRoadmapIds = userRoadmapMapper.getBatchLearningStatus(userId, roadmapIds);
            Set<Long> learningIds = new HashSet<>(learningRoadmapIds);

            for (RoadmapDTO dto : dtoList) {
                dto.setUpvoted(upvotedIds.contains(dto.getId()));
                dto.setPinned(pinnedIds.contains(dto.getId()));
                dto.setLearning(learningIds.contains(dto.getId()));
            }
        }

        for (RoadmapDTO dto : dtoList) {
            if (dto.getContent() != null) {
                String formattedContent = roadmapService.parseContentToGraphFormat(dto.getContent(), userId);
                dto.setContent(formattedContent);
            }
        }

        return ApiResponse.success(dtoList);
    }

    /**
     * 更新路线图
     * 映射: PUT /roadmap/{id} → PUT /api/v1/roadmaps/{id}
     */
    @PutMapping("/roadmaps/{id}")
    public ApiResponse<Void> updateRoadmap(@PathVariable Long id, @RequestParam String content) {
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        if (!RoadmapService.isValidContentFormat(content)) {
            throw ErrorCode.ROADMAP_CONTENT_INVALID.exception();
        }

        RoadmapDO roadmapDO = roadmapMapper.get(id.intValue());
        if (roadmapDO == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        int currentUserId = StpUtil.getLoginIdAsInt();
        if (roadmapDO.getCreatorId() != currentUserId) {
            throw ErrorCode.PERMISSION_DENIED.exception();
        }

        roadmapDO.setContent(content);
        roadmapDO.setContentHash(RoadmapService.calculateContentHash(content));
        roadmapDO.setUpdatedAt(LocalDateTime.now());

        roadmapMapper.update(roadmapDO);
        return ApiResponse.success();
    }

    /**
     * 路线图点赞
     * 映射: PUT /roadmap/{id}/upvote → PUT /api/v1/roadmaps/{id}/upvote
     */
    @PutMapping("/roadmaps/{id}/upvote")
    public ApiResponse<Object> upvoteRoadmap(@PathVariable Long id) {
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        int userId = StpUtil.getLoginIdAsInt();
        boolean voted = upvoteService.upvoteRoadmap(id.intValue(), userId);

        int voteDelta = voted ? 1 : -1;
        roadmapMapper.updateVoteCount(id.intValue(), voteDelta);

        RoadmapDO roadmapDO = roadmapMapper.get(id.intValue());

        scoreCalculationService.checkAndUpdateRoadmapScore(roadmapDO);
        roadmapDO = roadmapMapper.get(id.intValue());

        RoadmapDTO roadmapDTO = Converter.INSTANCE.toRoadMapDTO(roadmapDO);
        roadmapDTO.setUpvoted(voted);

        return ApiResponse.success(roadmapDTO);
    }

    /**
     * 创建路线图
     * 映射: POST /roadmap → POST /api/v1/roadmaps
     */
    @PostMapping("/roadmaps")
    public ApiResponse<Long> createRoadmap(
            @RequestParam Long professionId, 
            @RequestParam String content, 
            @RequestParam String description) {
        
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        if (!RoadmapService.isValidContentFormat(content)) {
            throw ErrorCode.ROADMAP_CONTENT_INVALID.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();

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
        return ApiResponse.success(roadmapDO.getId());
    }

    /**
     * 获取路线图详情
     * 映射: GET /roadmap/{id} → GET /api/v1/roadmaps/{id}
     */
    @GetMapping("/roadmaps/{id}")
    public ApiResponse<RoadmapDTO> getRoadmap(@PathVariable Long id) {
        int userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : 0;
        RoadmapDTO roadmapDTO = roadmapService.getById(id.intValue(), userId);

        if (roadmapDTO == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        if (roadmapDTO.getContent() != null) {
            String formattedContent = roadmapService.parseContentToGraphFormat(roadmapDTO.getContent(), userId);
            roadmapDTO.setContent(formattedContent);
        }

        return ApiResponse.success(roadmapDTO);
    }

    /**
     * 置顶路线图
     * 映射: POST /roadmap/pin → POST /api/v1/roadmaps/pin
     */
    @PostMapping("/roadmaps/pin")
    public ApiResponse<Object> pinRoadmap(@RequestParam Long professionId, @RequestParam Long roadmapId) {
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();

        UserProfileDO userProfile = userProfileMapper.getById(userId);
        Map<String, List<Long>> pinMap = new HashMap<>();

        if (userProfile != null && userProfile.getRoadmapPin() != null) {
            try {
                pinMap = objectMapper.readValue(userProfile.getRoadmapPin(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw ErrorCode.SYSTEM_ERROR.exception(e);
            }
        }

        String professionKey = String.valueOf(professionId);
        List<Long> professionPins = pinMap.getOrDefault(professionKey, new ArrayList<>());

        boolean isPinned = professionPins.contains(roadmapId);
        String message;

        if (isPinned) {
            professionPins.remove(roadmapId);
            message = "unpinned";
        } else {
            if (professionPins.size() >= 19) {
                throw ErrorCode.ROADMAP_PIN_LIMIT_EXCEEDED.exception();
            }
            professionPins.add(roadmapId);
            message = "pinned";
        }

        if (professionPins.isEmpty()) {
            pinMap.remove(professionKey);
        } else {
            pinMap.put(professionKey, professionPins);
        }

        String updatedPinJson = null;
        try {
            updatedPinJson = objectMapper.writeValueAsString(pinMap);
        } catch (JsonProcessingException e) {
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }

        if (userProfile == null) {
            userProfile = new UserProfileDO();
            userProfile.setUserId(userId);
            userProfile.setRoadmapPin(updatedPinJson);
            userProfile.setSubscription("");
            userProfileMapper.insert(userProfile);
        } else {
            userProfileMapper.updateRoadmapPin(userId, updatedPinJson);
        }

        return ApiResponse.success(message);
    }
}