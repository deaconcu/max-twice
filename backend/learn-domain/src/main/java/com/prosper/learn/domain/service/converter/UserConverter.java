package com.prosper.learn.domain.service.converter;

import com.prosper.learn.domain.service.data.CourseDataService;
import com.prosper.learn.domain.service.data.FollowDataService;
import com.prosper.learn.domain.service.data.UserProfileDataService;
import com.prosper.learn.dto.response.old.UserDTOV0;
import com.prosper.learn.dto.response.SubscriptionDTO;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.dataobject.UserProfileDO;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        uses = {UserProfileDataService.class, CourseDataService.class, FollowDataService.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConverter {
    
    @Named("toDTO")
    UserDTOV0 toDTO(UserDO userDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<UserDTOV0> toDTO(List<UserDO> userDOList);

    @Named("toDTOV1")
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "biography")
    UserDTOV0 toDTOV1(UserDO userDO);
    
    @IterableMapping(qualifiedByName = "toDTOV1")
    List<UserDTOV0> toDTOV1(List<UserDO> userDOList);
    
    @Named("toDTOV2")
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "subscriptions", source = "id", qualifiedByName = "getSubscriptions")
    UserDTOV0 toDTOV2(UserDO userDO);
    
    @IterableMapping(qualifiedByName = "toDTOV2")
    List<UserDTOV0> toDTOV2(List<UserDO> userDOList);
    
    @Named("toDTOV3")
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "biography")
    UserDTOV0 toDTOV3(UserDO userDO);
    
    @IterableMapping(qualifiedByName = "toDTOV3")
    List<UserDTOV0> toDTOV3(List<UserDO> userDOList);
    
    @Named("toDTOV4")
    @Mapping(target = "id")
    @Mapping(target = "name")
    UserDTOV0 toDTOV4(UserDO userDO);
    
    @IterableMapping(qualifiedByName = "toDTOV4")
    List<UserDTOV0> toDTOV4(List<UserDO> userDOList);
    
    @Named("getSubscriptions")
    default SubscriptionDTO[] getSubscriptions(
            Long userId,
            @Context UserProfileDataService userProfileDataService,
            @Context CourseDataService courseDataService) {
        UserProfileDO userProfileDO = userProfileDataService.getById(userId);

        if (userProfileDO != null && userProfileDO.getSubscription() != null && !userProfileDO.getSubscription().trim().isEmpty()) {
            List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .map(Long::parseLong).collect(Collectors.toCollection(ArrayList::new));
            List<CourseDO> courseDOList = courseDataService.getByIds(ids);
            SubscriptionDTO[] subscriptionDTOS = new SubscriptionDTO[courseDOList.size()];
            int i = 0;
            for (CourseDO courseDO : courseDOList) {
                subscriptionDTOS[i++] = new SubscriptionDTO(courseDO.getId(), courseDO.getName());
            }
            return subscriptionDTOS;
        } else {
            return new SubscriptionDTO[0];
        }
    }
}