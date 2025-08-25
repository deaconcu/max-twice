package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.PostDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PostMapper {

    @Select("SELECT * FROM post WHERE id = #{id}")
    PostDO get(@Param("id") int id);

    @Select({"<script>SELECT * FROM post where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<PostDO> getByIds(@Param("ids") List<Integer> ids);

    @Select({"<script>SELECT * FROM post where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Integer, PostDO> getMapByIds(@Param("ids") Collection<Integer> ids);

    @Select("SELECT * FROM post " +
            "WHERE nodeId = #{nodeId} and state = #{state} " +
            "order by ctime desc limit #{limit}")
    List<PostDO> getListByNode(@Param("nodeId") int nodeId, @Param("limit") int limit, @Param("state") int state);

    @Select("SELECT * FROM post " +
            "WHERE nodeId = #{nodeId} and id < #{lastId} and state = #{state} " +
            "order by id desc limit #{limit}")
    List<PostDO> getListByLastId(@Param("nodeId") int nodeId, @Param("lastId") int lastId, int limit, @Param("state") int state);

    @Select("SELECT * FROM post " +
            "WHERE creator = #{userId} and type = 2 and id < #{lastId} " +
            "order by id desc limit #{count}")
    List<PostDO> getArticleListByUser(int userId, int lastId, int count);

    @Select("SELECT * FROM post " +
            "WHERE creator = #{userId} and type = 1 and id < #{lastId} " +
            "order by id desc limit #{count}")
    List<PostDO> getContentsListByUser(int userId, int lastId, int count);

    @Insert("INSERT INTO post" +
            "(nodeId, creator, type, content, once, twice, helpful, commentCount, state, score) " +
            "VALUES " +
            "(#{nodeId}, #{creator}, #{type}, #{content}, #{once}, #{twice}, #{helpful}, #{commentCount}, #{state}, 0.0)")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PostDO posting);

    @Update("UPDATE post " +
            "SET " +
            "nodeId = #{nodeId}, content = #{content}, once = #{once}, twice = #{twice}, " +
            "helpful = #{helpful}, commentCount=#{commentCount}, state=#{state} where id = #{id}")
    void update(PostDO posting);

    // 新增分数相关方法
    @Update("UPDATE post SET score = #{score}, score_calculated_at = NOW() WHERE id = #{id}")
    int updateScore(@Param("id") int id, @Param("score") Double score);

    @Select("SELECT * FROM post WHERE nodeId = #{nodeId} AND state = #{state} " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<PostDO> getListByNodeAndScore(@Param("nodeId") int nodeId, @Param("limit") int limit, @Param("state") int state);

    @Select("SELECT * FROM post WHERE nodeId = #{nodeId} AND state = #{state} AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<PostDO> getListByNodeAndScoreAndPaginated(@Param("nodeId") int nodeId,
                                                   @Param("lastScore") Double lastScore,
                                                   @Param("lastId") int lastId,
                                                   @Param("limit") int limit,
                                                   @Param("state") int state);

    @Select("SELECT id, once, twice, helpful, cTime FROM post WHERE state = #{state}")
    List<PostDO> getAllPostsForScoreCalculation(@Param("state") int state);

    @Select("SELECT * FROM post where state = #{state} order by id limit #{count}")
    List<PostDO> getListByState(int state, int count);
    
    /**
     * 统计活跃文章数量（state=1表示已发布状态）
     * 
     * @return 文章总数
     */
    @Select("SELECT COUNT(*) FROM post WHERE state = 1")
    Long countActiveArticles();
}
