package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.ProfessionAdminDTO;
import com.prosper.learn.application.dto.response.ProfessionBriefDTO;
import com.prosper.learn.application.dto.response.ProfessionDTO;
import com.prosper.learn.content.profession.ProfessionDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface ProfessionConverter {

    @Named("toDTO")
    ProfessionDTO toDTO(ProfessionDO professionDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<ProfessionDTO> toDTO(List<ProfessionDO> professionDOList);

    @Named("toBriefDTO")
    ProfessionBriefDTO toBriefDTO(ProfessionDO professionDO);

    @IterableMapping(qualifiedByName = "toBriefDTO")
    List<ProfessionBriefDTO> toBriefDTO(List<ProfessionDO> professionDOList);

    @Named("toAdminDTO")
    ProfessionAdminDTO toAdminDTO(ProfessionDO professionDO);

    @IterableMapping(qualifiedByName = "toAdminDTO")
    List<ProfessionAdminDTO> toAdminDTO(List<ProfessionDO> professionDOList);
}