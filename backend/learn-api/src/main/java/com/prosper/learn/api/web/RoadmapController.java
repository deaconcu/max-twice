package com.prosper.learn.api.web;

import com.prosper.learn.api.client.RoadmapClient;
import com.prosper.learn.domain.service.RoadmapService;
import com.prosper.learn.domain.service.ScoreCalculationService;
import com.prosper.learn.domain.service.UpvoteService;
import com.prosper.learn.domain.service.UserRoadmapService;
import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.RoadmapDTO;
import com.prosper.learn.persistence.mapper.RoadmapMapper;
import com.prosper.learn.persistence.mapper.UserProfileMapper;
import com.prosper.learn.persistence.mapper.UpvoteMapper;
import com.prosper.learn.persistence.mapper.UserRoadmapMapper;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.dataobject.UserProfileDO;
import com.prosper.learn.domain.util.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
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
@RestController
@RequiredArgsConstructor
public class RoadmapController implements RoadmapClient {

    private final RoadmapService roadmapService;
    private final ScoreCalculationService scoreCalculationService;
    private final RoadmapMapper roadmapMapper;
    private final UserProfileMapper userProfileMapper;
    private final UpvoteMapper upvoteMapper;
    private final UserRoadmapMapper userRoadmapMapper;
    private final UpvoteService upvoteService;
    private final UserRoadmapService userRoadmapService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Response<List<RoadmapDTO>> getListByProfession(Integer professionId, Integer lastId) {
        try {
            if (!StpUtil.isLogin()) {
                return Response.fail("用户未登录");
            }

            List<RoadmapDO> roadmapList = new ArrayList<>();
            int limit = 20; // 默认每页20条

            List<Integer> pinnedRoadmapIds = new ArrayList<>();
            if (lastId == null || lastId == 0) {
                // 获取当前用户的置顶路线
                int userId = StpUtil.getLoginIdAsInt();
                UserProfileDO userProfile = userProfileMapper.getById(userId);

                if (userProfile != null && userProfile.getRoadmapPin() != null) {
                    // 解析 roadmapPin JSON: {"1": [26, 28, 27, 37, 15, 18, 19]}
                    Map<String, List<Integer>> pinMap = objectMapper.readValue(
                        userProfile.getRoadmapPin(), new TypeReference<>() {}
                    );

                    List<Integer> professionPins = pinMap.get(professionId.toString());
                    if (professionPins != null && !professionPins.isEmpty()) {
                        pinnedRoadmapIds = professionPins;
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
            int userId = StpUtil.getLoginIdAsInt();

            // 批量设置 upvoted 和 pinned 状态
            if (!dtoList.isEmpty()) {
                // 1. 批量查询点赞状态
                List<Integer> roadmapIds = dtoList.stream()
                    .map(dto -> dto.getId())
                    .collect(Collectors.toList());

                Set<Integer> upvotedIds = upvoteService.getUpvotedRoadmapIds(roadmapIds, userId);


                // 2. 获取置顶状态
                Set<Integer> pinnedIds = new HashSet<>();
                if (lastId == null || lastId == 0) {
                    // 首页时，pinnedRoadmapIds 就是置顶的ID
                    pinnedIds.addAll(pinnedRoadmapIds);
                } else {
                    // 分页时需要重新获取置顶信息
                    UserProfileDO userProfile = userProfileMapper.getById(userId);
                    if (userProfile != null && userProfile.getRoadmapPin() != null) {
                        try {
                            Map<String, List<Integer>> pinMap = objectMapper.readValue(
                                userProfile.getRoadmapPin(), new TypeReference<>() {}
                            );
                            List<Integer> professionPins = pinMap.get(professionId.toString());
                            if (professionPins != null) {
                                pinnedIds.addAll(professionPins);
                            }
                        } catch (Exception e) {
                            // 忽略解析错误
                        }
                    }
                }

                // 3. 批量查询学习状态
                List<Integer> learningRoadmapIds = userRoadmapMapper.getBatchLearningStatus((long) userId, roadmapIds);
                Set<Integer> learningIds = new HashSet<>(learningRoadmapIds);

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
                    try {
                        String formattedContent = roadmapService.parseContentToGraphFormat(dto.getContent());
                        dto.setContent(formattedContent);
                    } catch (Exception e) {
                        // 如果转换失败，保持原始内容
                    }
                }
            }

            return Response.success(dtoList);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.fail("获取路线图列表失败");
        }
    }

    @Override
    public Response<Void> putById(Long id, String content) {
        try {
            // 验证用户是否登录
            if (!StpUtil.isLogin()) {
                return Response.fail("用户未登录");
            }

            // 验证content格式
            if (!RoadmapService.isValidContentFormat(content)) {
                return Response.fail("路线图内容格式不正确");
            }

            RoadmapDO roadmapDO = roadmapMapper.get(id.intValue());
            if (roadmapDO == null) {
                return Response.fail("路线图不存在");
            }

            // 检查权限（简单起见，只允许创建者修改）
            int currentUserId = StpUtil.getLoginIdAsInt();
            if (roadmapDO.getCreatorId() != currentUserId) {
                return Response.fail("无权限修改此路线图");
            }

            roadmapDO.setContent(content);
            roadmapDO.setContentHash(RoadmapService.calculateContentHash(content));
            roadmapDO.setUpdatedAt(LocalDateTime.now());

            roadmapMapper.update(roadmapDO);
            return Response.success(null);
        } catch (Exception e) {
            return Response.fail("更新路线图失败: " + e.getMessage());
        }
    }

    @Override
            // 获取最新的roadmap数据
    public Response<Object> upvote(Long id) {
        try {
            // 验证用户是否登录
            if (!StpUtil.isLogin()) {
                return Response.fail("用户未登录");
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
        } catch (Exception e) {
            e.printStackTrace();
            return Response.fail("点赞操作失败");
        }
    }

    @Override
    public Response<Long> post(Long professionId, String content, String description) {
        try {
            // 验证用户是否登录
            if (!StpUtil.isLogin()) {
                return Response.fail("用户未登录");
            }

            // 验证content格式
            if (!RoadmapService.isValidContentFormat(content)) {
                return Response.fail("路线图内容格式不正确");
            }

            int userId = StpUtil.getLoginIdAsInt();

            // 创建新的路线图
            RoadmapDO roadmapDO = new RoadmapDO();
            roadmapDO.setProfessionId(professionId.intValue());
            roadmapDO.setCreatorId(userId);
            roadmapDO.setContent(content);
            roadmapDO.setDescription(description);
            roadmapDO.setContentHash(RoadmapService.calculateContentHash(content));
            roadmapDO.setVote(0);
            roadmapDO.setComment(0);
            roadmapDO.setCreatedAt(LocalDateTime.now());
            roadmapDO.setUpdatedAt(LocalDateTime.now());

            roadmapMapper.insert(roadmapDO);
            return Response.success((long) roadmapDO.getId());
        } catch (Exception e) {
            return Response.fail("创建路线图失败: " + e.getMessage());
        }
    }

    @Override
    public Response<RoadmapDTO> getById(Long id) {
        try {
            int userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : 0;
            RoadmapDTO roadmapDTO = roadmapService.getById(id.intValue(), userId);

            if (roadmapDTO == null) {
                return Response.fail("路线图不存在");
            }

            // 转换 content 格式
            if (roadmapDTO.getContent() != null) {
                try {
                    String formattedContent = roadmapService.parseContentToGraphFormat(roadmapDTO.getContent());
                    roadmapDTO.setContent(formattedContent);
                } catch (Exception e) {
                    // 如果转换失败，保持原始内容
                    // 或者可以设置为空字符串，根据业务需求决定
                }
            }

            return Response.success(roadmapDTO);
        } catch (Exception e) {
            return Response.fail("获取路线图详情失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> pin(int professionId, int roadmapId) {
        try {
            // 验证用户是否登录
            if (!StpUtil.isLogin()) {
                return Response.fail("用户未登录");
            }

            int userId = StpUtil.getLoginIdAsInt();

            // 获取用户配置
            UserProfileDO userProfile = userProfileMapper.getById(userId);
            Map<String, List<Integer>> pinMap = new HashMap<>();

            // 解析现有的置顶配置
            if (userProfile != null && userProfile.getRoadmapPin() != null) {
                try {
                    pinMap = objectMapper.readValue(
                        userProfile.getRoadmapPin(),
                        new TypeReference<Map<String, List<Integer>>>() {}
                    );
                } catch (Exception e) {
                    // 如果解析失败，使用空的 Map
                    pinMap = new HashMap<>();
                }
            }

            String professionKey = String.valueOf(professionId);
            List<Integer> professionPins = pinMap.getOrDefault(professionKey, new ArrayList<>());

            boolean isPinned = professionPins.contains(roadmapId);
            String message;

            if (isPinned) {
                // 如果已置顶，则取消置顶
                professionPins.remove(Integer.valueOf(roadmapId));
                message = "unpinned";
            } else {
                // 如果未置顶，则添加置顶（最多19个，保证加上至少1个非置顶的总共20个）
                if (professionPins.size() >= 19) {
                    return Response.fail("最多只能置顶19个路线图");
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
            String updatedPinJson = objectMapper.writeValueAsString(pinMap);

            if (userProfile == null) {
                // 如果用户配置不存在，创建新的
                userProfile = new UserProfileDO();
                userProfile.setId(userId);
                userProfile.setRoadmapPin(updatedPinJson);
                userProfile.setSubscription(""); // 设置默认值
                userProfileMapper.insert(userProfile);
            } else {
                // 更新现有配置
                userProfileMapper.updateRoadmapPin(userId, updatedPinJson);
            }

            return Response.success(message);
        } catch (Exception e) {
            return Response.fail("操作失败: " + e.getMessage());
        }
    }
}
