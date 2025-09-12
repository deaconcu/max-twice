package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.MemoryCardVersionDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MemoryCardVersionMapper {

    @Select("SELECT * FROM memory_card_version WHERE id = #{id}")
    MemoryCardVersionDO get(long id);

    @Select({"<script>SELECT * FROM memory_card_version WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<MemoryCardVersionDO> getByIds(List<Long> ids);

    @Select({"<script>SELECT * FROM memory_card_version WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, MemoryCardVersionDO> getMapByIds(Collection<Long> ids);

    @Select("SELECT * FROM memory_card_version WHERE card_id = #{cardId} " +
            "ORDER BY version DESC")
    List<MemoryCardVersionDO> getVersionsByCard(long cardId);

    @Select("SELECT * FROM memory_card_version WHERE card_id = #{cardId} AND is_active = 1")
    MemoryCardVersionDO getActiveVersionByCard(long cardId);

    @Select("SELECT * FROM memory_card_version WHERE card_id = #{cardId} AND version = #{version}")
    MemoryCardVersionDO getVersionByCardAndVersion(long cardId, int version);

    @Select("SELECT * FROM memory_card_version WHERE content_hash = #{contentHash}")
    List<MemoryCardVersionDO> getByContentHash(String contentHash);

    @Insert("INSERT INTO memory_card_version " +
            "(card_id, version, creator_id, front, back, content_hash, is_active) " +
            "VALUES " +
            "(#{cardId}, #{version}, #{creatorId}, #{front}, #{back}, #{contentHash}, #{isActive})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MemoryCardVersionDO version);

    @Update("UPDATE memory_card_version SET is_active = #{isActive} WHERE id = #{id}")
    int updateActiveStatus(long id, boolean isActive);

    @Update("UPDATE memory_card_version SET is_active = 0 WHERE card_id = #{cardId}")
    int deactivateAllVersions(long cardId);

    @Select("SELECT MAX(version) FROM memory_card_version WHERE card_id = #{cardId}")
    Integer getMaxVersionByCard(long cardId);

    @Select("SELECT COUNT(*) FROM memory_card_version WHERE card_id = #{cardId}")
    int countByCard(long cardId);

}