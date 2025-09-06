package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.message.MessageDTO;
import com.prosper.learn.persistence.dataobject.MessageDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageConverter {
    
    @Named("toDTO")
    MessageDTO toDTO(MessageDO messageDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<MessageDTO> toDTO(List<MessageDO> messageDOList);
    
    MessageDO toMessageDO(MessageDTO messageDTO);
    
    List<MessageDO> toMessageDO(List<MessageDTO> messageDTOList);
}