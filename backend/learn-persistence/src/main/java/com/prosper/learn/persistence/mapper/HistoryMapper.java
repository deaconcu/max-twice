package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.HistoryDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface HistoryMapper {

    @Insert("INSERT INTO history(postingId, content) VALUES (#{postingId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(HistoryDO history);
}
