package com.twicemax.content.shared.revision;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ContentRevisionMapper {

    /** 主键查询。 */
    @Select("SELECT * FROM content_revision WHERE id = #{id}")
    ContentRevisionDO getById(@Param("id") long id);

    /**
     * 该 content 下当前 revision_no 最大值；用于生成下一个 revision_no。
     * 事务内调用。新 content 无任何记录时返回 NULL（调用方按 0 处理 → 下一个为 1）。
     */
    @Select("SELECT MAX(revision_no) FROM content_revision " +
            "WHERE content_type = #{contentType} AND content_id = #{contentId}")
    Integer maxRevisionNo(@Param("contentType") String contentType,
                          @Param("contentId") long contentId);

    /**
     * 该 content 下最近一次 revision（按 id DESC），不限 status。
     * 用于 submit 时对比 hash 实现去重。
     */
    @Select("SELECT * FROM content_revision " +
            "WHERE content_type = #{contentType} AND content_id = #{contentId} " +
            "ORDER BY id DESC LIMIT 1")
    ContentRevisionDO getLatest(@Param("contentType") String contentType,
                                @Param("contentId") long contentId);

    /**
     * 列出某 content 的所有 revision（按 revision_no DESC）。
     * 历史浏览/调试使用，前期可能不会被业务调用。
     */
    @Select("SELECT * FROM content_revision " +
            "WHERE content_type = #{contentType} AND content_id = #{contentId} " +
            "ORDER BY revision_no DESC")
    List<ContentRevisionDO> listByContent(@Param("contentType") String contentType,
                                          @Param("contentId") long contentId);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO content_revision " +
            "(content_type, content_id, revision_no, status, payload, hash, " +
            " reject_reason, author_id, reviewer_id, reviewed_at) " +
            "VALUES " +
            "(#{contentType}, #{contentId}, #{revisionNo}, #{status}, #{payload}, #{hash}, " +
            " #{rejectReason}, #{authorId}, #{reviewerId}, #{reviewedAt})")
    int insert(ContentRevisionDO revision);

    /**
     * 状态流转：SUBMITTED → PUBLISHED / REJECTED / WITHDRAWN。
     * reviewer_id / reviewed_at / reject_reason 由调用方填好后整体写入。
     */
    @Update("UPDATE content_revision SET " +
            "status = #{status}, " +
            "reviewer_id = #{reviewerId}, " +
            "reviewed_at = #{reviewedAt}, " +
            "reject_reason = #{rejectReason} " +
            "WHERE id = #{id}")
    int updateStatus(ContentRevisionDO revision);
}
