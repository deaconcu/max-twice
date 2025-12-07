package com.prosper.learn.content.toc;

import org.apache.ibatis.annotations.*;
import java.util.Map;

@Mapper
public interface CourseTocMapper {

    @Select("SELECT * FROM course_toc WHERE hash = #{hash}")
    CourseTocDO get(String hash);

    @Select({"<script>SELECT * FROM course_toc where hash in " +
            "<foreach item='hash' collection='hashes' open='(' separator=', ' close=')'>#{hash}</foreach>" +
            "</script>"})
    @MapKey("hash")
    Map<String, CourseTocDO> getByHashes(String[] hashes);

    @Insert("INSERT INTO course_toc(hash, toc, ref_count) VALUES (#{hash}, #{toc}, #{refCount})")
    int insert(CourseTocDO courseTocDO);

    @Update("UPDATE course_toc SET ref_count = ref_count + #{n} where hash = #{hash}")
    void incrRef(String hash, int n);

    @Delete("DELETE FROM course_toc where id = #{id}")
    void delete(long id);
}
