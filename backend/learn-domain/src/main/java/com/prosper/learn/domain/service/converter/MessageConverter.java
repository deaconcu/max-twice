package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.message.MessageDTO;
import com.prosper.learn.persistence.dataobject.MessageDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class MessageConverter {
    
    @Named("toDTO")
    public abstract MessageDTO toDTO(MessageDO messageDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    public abstract List<MessageDTO> toDTO(List<MessageDO> messageDOList);
}