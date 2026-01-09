package com.prosper.learn.infrastructure.image;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图片上传Mapper
 */
@Mapper
public interface ImageUploadMapper {

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM image_uploads WHERE id = #{id}")
    ImageUploadDO getById(long id);

    /**
     * 根据URL查询
     */
    @Select("SELECT * FROM image_uploads WHERE file_url = #{fileUrl}")
    ImageUploadDO getByFileUrl(String fileUrl);

    /**
     * 根据引用类型和引用ID查询
     */
    @Select("SELECT * FROM image_uploads WHERE ref_type = #{refType} AND ref_id = #{refId}")
    List<ImageUploadDO> getByRef(String refType, long refId);

    /**
     * 根据用户ID、引用类型和状态查询
     */
    @Select("SELECT * FROM image_uploads WHERE user_id = #{userId} AND ref_type = #{refType} AND status = #{status} LIMIT 1")
    ImageUploadDO getByUserIdAndRefTypeAndStatus(long userId, String refType, int status);

    /**
     * 查询未使用且超过指定时间的图片
     */
    @Select("SELECT * FROM image_uploads WHERE status = #{status} AND created_at < #{createdAt}")
    List<ImageUploadDO> getByStatusAndCreatedAtBefore(int status, LocalDateTime createdAt);

    /**
     * 插入图片记录
     */
    @Insert("INSERT INTO image_uploads(user_id, file_name, file_size, file_url, ref_type, ref_id, status, created_at, used_at) " +
            "VALUES (#{userId}, #{fileName}, #{fileSize}, #{fileUrl}, #{refType}, #{refId}, #{status}, #{createdAt}, #{usedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ImageUploadDO imageUpload);

    /**
     * 更新图片记录
     */
    @Update("UPDATE image_uploads SET ref_type = #{refType}, ref_id = #{refId}, status = #{status}, " +
            "used_at = #{usedAt}, created_at = #{createdAt} WHERE id = #{id}")
    int update(ImageUploadDO imageUpload);

    /**
     * 删除图片记录
     */
    @Delete("DELETE FROM image_uploads WHERE id = #{id}")
    int delete(long id);

    /**
     * 根据用户ID查询上传历史
     */
    @Select("SELECT * FROM image_uploads WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<ImageUploadDO> getByUserId(long userId, int limit);
}
