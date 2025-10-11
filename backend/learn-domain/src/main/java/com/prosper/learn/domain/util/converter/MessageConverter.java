package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.message.MessageDTO;
import com.prosper.learn.persistence.dataobject.MessageDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageConverter {
    
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "senderId")
    @Mapping(target = "receiverId")
    @Mapping(target = "content")
    @Mapping(target = "type")
    @Mapping(target = "createdAt")
    MessageDTO toDTO(MessageDO messageDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<MessageDTO> toDTO(List<MessageDO> messageDOList);
}