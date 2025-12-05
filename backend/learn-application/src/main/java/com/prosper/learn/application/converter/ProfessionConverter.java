package com.prosper.learn.application.converter;

import com.prosper.learn.dto.response.ProfessionDTO;
import com.prosper.learn.persistence.dataobject.ProfessionDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface ProfessionConverter {
    
    @Named("toDTO")
    ProfessionDTO toDTO(ProfessionDO professionDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<ProfessionDTO> toDTO(List<ProfessionDO> professionDOList);
}