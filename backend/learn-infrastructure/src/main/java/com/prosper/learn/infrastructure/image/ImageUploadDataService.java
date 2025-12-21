package com.prosper.learn.infrastructure.image;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 图片上传数据服务
 */
@Service
public class ImageUploadDataService extends AbstractDataService<ImageUploadDO, ImageUploadMapper, Long> {

    @Autowired
    private ImageUploadMapper imageUploadMapper;

    @Override
    protected ImageUploadMapper mapper() {
        return imageUploadMapper;
    }

    @Override
    protected String getCacheName() {
        return "imageUploads";
    }

    @Override
    protected String getEntityName() {
        return "ImageUpload";
    }

    @Override
    protected Long getEntityId(ImageUploadDO entity) {
        return entity.getId();
    }

    @Override
    protected ImageUploadDO getByIdFromMapper(ImageUploadMapper mapper, Long id) {
        return mapper.getById(id);
    }

    @Override
    protected List<ImageUploadDO> getByIdsFromMapper(ImageUploadMapper mapper, Collection<Long> ids) {
        return List.of(); // 图片上传不需要批量按ID查询
    }

    @Override
    protected Map<Long, ImageUploadDO> getMapByIdsFromMapper(ImageUploadMapper mapper, Collection<Long> ids) {
        return Map.of(); // 图片上传不需要批量按ID查询
    }

    @Override
    protected int deleteByIdFromMapper(ImageUploadMapper mapper, Long id) {
        return mapper.delete(id);
    }

    /**
     * 根据URL查询图片
     */
    public ImageUploadDO getByFileUrl(String fileUrl) {
        return imageUploadMapper.getByFileUrl(fileUrl);
    }

    /**
     * 根据引用类型和引用ID查询图片列表
     */
    public List<ImageUploadDO> getByRef(String refType, Long refId) {
        return imageUploadMapper.getByRef(refType, refId);
    }

    /**
     * 根据用户ID、引用类型和状态查询
     */
    public ImageUploadDO getByUserIdAndRefTypeAndStatus(Long userId, String refType, Integer status) {
        return imageUploadMapper.getByUserIdAndRefTypeAndStatus(userId, refType, status);
    }

    /**
     * 查询未使用且超过指定时间的图片
     */
    public List<ImageUploadDO> getByStatusAndCreatedAtBefore(Integer status, LocalDateTime createdAt) {
        return imageUploadMapper.getByStatusAndCreatedAtBefore(status, createdAt);
    }

    /**
     * 插入图片记录
     */
    public void insert(ImageUploadDO imageUpload) {
        imageUploadMapper.insert(imageUpload);
    }

    /**
     * 更新图片记录
     */
    public void update(ImageUploadDO imageUpload) {
        imageUploadMapper.update(imageUpload);
    }

    /**
     * 删除图片记录
     */
    public void delete(Long id) {
        imageUploadMapper.delete(id);
    }

    /**
     * 查询用户上传历史
     */
    public List<ImageUploadDO> getByUserId(Long userId, int limit) {
        return imageUploadMapper.getByUserId(userId, limit);
    }
}
