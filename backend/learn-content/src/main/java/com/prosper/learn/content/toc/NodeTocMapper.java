package com.prosper.learn.content.toc;

import org.apache.ibatis.annotations.*;
import java.util.Map;

@Mapper
public interface NodeTocMapper {

    @Select("SELECT * FROM node_toc WHERE hash = #{hash}")
    NodeTocDO get(String hash);

    @Select({"<script>SELECT * FROM node_toc where hash in " +
            "<foreach item='hash' collection='hashes' open='(' separator=', ' close=')'>#{hash}</foreach>" +
            "</script>"})
    @MapKey("hash")
    Map<String, NodeTocDO> getByHashes(String[] hashes);

    @Insert("INSERT INTO node_toc(hash, toc, ref_count) VALUES (#{hash}, #{toc}, #{refCount})")
    int insert(NodeTocDO nodeTocDO);

    @Update("UPDATE node_toc SET ref_count = ref_count + #{n} where hash = #{hash}")
    void incrRef(String hash, int n);
}
