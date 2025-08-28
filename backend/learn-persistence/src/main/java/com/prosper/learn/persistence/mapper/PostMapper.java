package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.PostDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PostMapper {

    @Select("SELECT * FROM post WHERE id = #{id}")
    PostDO get(long id);

    @Select({"<script>SELECT * FROM post where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<PostDO> getByIds(List<Long> ids);

    @Select({"<script>SELECT * FROM post where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, PostDO> getMapByIds(Collection<Long> ids);

    @Select("SELECT * FROM post " +
            "WHERE node_id = #{nodeId} and state = #{state} " +
            "order by created_at desc limit #{limit}")
    List<PostDO> getListByNode(long nodeId, int limit, int state);

    @Select("SELECT * FROM post " +
            "WHERE node_id = #{nodeId} and id < #{lastId} and state = #{state} " +
            "order by id desc limit #{limit}")
    List<PostDO> getListByLastId(long nodeId, long lastId, int limit, int state);

    @Select("SELECT * FROM post " +
            "WHERE creator = #{userId} and type = 2 and id < #{lastId} " +
            "order by id desc limit #{count}")
    List<PostDO> getArticleListByUser(long userId, long lastId, int count);

    @Select("SELECT * FROM post " +
            "WHERE creator = #{userId} and type = 1 and id < #{lastId} " +
            "order by id desc limit #{count}")
    List<PostDO> getContentsListByUser(long userId, long lastId, int count);

    @Insert("INSERT INTO post" +
            "(node_id, creator, type, content, once, twice, helpful, comment_count, state, score) " +
            "VALUES " +
            "(#{nodeId}, #{creator}, #{type}, #{content}, #{once}, #{twice}, #{helpful}, #{commentCount}, #{state}, 0.0)")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PostDO posting);

    @Update("UPDATE post " +
            "SET " +
            "node_id = #{nodeId}, content = #{content}, once = #{once}, twice = #{twice}, " +
            "helpful = #{helpful}, comment_count=#{commentCount}, state=#{state} where id = #{id}")
    void update(PostDO posting);

    // 新增分数相关方法
    @Update("UPDATE post SET score = #{score}, score_calculated_at = NOW() WHERE id = #{id}")
    int updateScore(long id, double score);

    @Select("SELECT * FROM post WHERE node_id = #{nodeId} AND state = #{state} " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<PostDO> getListByNodeAndScore(long nodeId, int limit, int state);

    @Select("SELECT * FROM post WHERE node_id = #{nodeId} AND state = #{state} AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<PostDO> getListByNodeAndScoreAndPaginated(long nodeId, double lastScore, long lastId, int limit, int state);

    @Select("SELECT id, once, twice, helpful, created_at FROM post WHERE state = #{state}")
    List<PostDO> getAllPostsForScoreCalculation(int state);

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
