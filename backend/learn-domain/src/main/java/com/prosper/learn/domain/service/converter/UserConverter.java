package com.prosper.learn.domain.service.converter;

import com.prosper.learn.domain.service.data.CourseDataService;
import com.prosper.learn.domain.service.data.FollowDataService;
import com.prosper.learn.domain.service.data.UserProfileDataService;
import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.dto.response.old.UserDTOV0;
import com.prosper.learn.dto.response.SubscriptionDTO;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.dataobject.UserProfileDO;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class UserConverter {

    @Autowired
    protected UserProfileDataService userProfileDataService;
    @Autowired
    protected CourseDataService courseDataService;
    @Autowired
    protected FollowDataService followDataService;
    
    @Named("toDTO")
    public abstract UserDTO toDTO(UserDO userDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    public abstract List<UserDTO> toDTO(List<UserDO> userDOList);

    @Named("toDTOV1")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "biography")
    public abstract UserDTO toDTOV1(UserDO userDO);
    
    @IterableMapping(qualifiedByName = "toDTOV1")
    public abstract List<UserDTO> toDTOV1(List<UserDO> userDOList);

    public UserDTO toDTOV1(UserDO userDO, boolean followed) {
        if (userDO == null) return null;

        UserDTO dto = toDTOV1(userDO);
        dto.setFollowed(followed);
        return dto;
    }
    
    @Named("toDTOV2")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "subscriptions", source = "id", qualifiedByName = "getSubscriptions")
    public abstract UserDTO toDTOV2(UserDO userDO);
    
    @IterableMapping(qualifiedByName = "toDTOV2")
    public abstract List<UserDTO> toDTOV2(List<UserDO> userDOList);

    @Named("toDTOV4")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    public abstract UserDTO toDTOV4(UserDO userDO);

    @IterableMapping(qualifiedByName = "toDTOV4")
    public abstract List<UserDTO> toDTOV4(List<UserDO> userDOList);
    
    @Named("getSubscriptions")
    public SubscriptionDTO[] getSubscriptions(Long id) {
        UserProfileDO userProfileDO = userProfileDataService.getById(id);

        if (userProfileDO != null && userProfileDO.getSubscription() != null
                && !userProfileDO.getSubscription().trim().isEmpty()) {
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