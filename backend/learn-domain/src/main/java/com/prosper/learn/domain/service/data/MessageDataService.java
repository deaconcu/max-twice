package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.MessageDO;
import com.prosper.learn.persistence.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 消息数据服务
 */
@Service
public class MessageDataService extends AbstractDataService<MessageDO, MessageMapper, Long> {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    protected MessageMapper mapper() {
        return messageMapper;
    }

    @Override
    protected String getCacheName() {
        return "messages";
    }

    @Override
    protected String getEntityName() {
        return "Message";
    }

    @Override
    protected Long getEntityId(MessageDO entity) {
        return entity.getId();
    }

    @Override
    protected MessageDO getByIdFromMapper(MessageMapper mapper, Long id) {
        return messageMapper.getById(id);
    }

    @Override
    protected List<MessageDO> getByIdsFromMapper(MessageMapper mapper, Collection<Long> ids) {
        return List.of(); // MessageMapper没有批量查询方法
    }

    @Override
    protected Map<Long, MessageDO> getMapByIdsFromMapper(MessageMapper mapper, Collection<Long> ids) {
        return Map.of(); // MessageMapper没有批量查询方法
    }

    @Override
    protected int deleteByIdFromMapper(MessageMapper mapper, Long id) {
        return 0;
    }

    /**
     * 更新消息
     */
    @CacheEvict(value = "messages", key = "#messageDO.id")
    public void update(MessageDO messageDO) {
        messageMapper.update(messageDO);
    }

    /**
     * 插入消息
     */
    public void insert(MessageDO messageDO) {
        messageMapper.insert(messageDO);
    }

    /**
     * 根据类型分页查询消息
     */
    public List<MessageDO> listByPull(int type, long lastId, int limit) {
        return messageMapper.listByPull(type, lastId, limit);
    }

    /**
     * 根据用户查询消息列表
     */
    public List<MessageDO> getListByUser(int type, long senderId, long receiverId, long lastId, int limit) {
        return messageMapper.getListByUser(type, senderId, receiverId, lastId, limit);
    }

    /**
     * 获取用户对话消息
     */
    public List<MessageDO> getConversationByUser(long senderId, long receiverId, long lastId, int limit) {
        return messageMapper.getConversationByUser(senderId, receiverId, lastId, limit);
    }

    /**
     * 获取系统消息列表
     */
    public List<MessageDO> getSystemListByUser(long receiverId, long lastId, int limit) {
        return messageMapper.getSystemListByUser(receiverId, lastId, limit);
    }

    /**
     * 获取系统消息详情列表
     */
    public List<MessageDO> getSystemItemListByUser(int type, long receiverId, long lastId, int limit) {
        return messageMapper.getSystemItemListByUser(type, receiverId, lastId, limit);
    }

    /**
     * 获取课程申请消息列表
     */
    public List<MessageDO> getApplyCourseListByUser(long senderId, long lastId, int limit) {
        return messageMapper.getApplyCourseListByUser(senderId, lastId, limit);
    }

    /**
     * 获取课程申请消息列表（分页）
     */
    public List<MessageDO> getApplyCourseList(int offset, int limit) {
        return messageMapper.getApplyCourseList(offset, limit);
    }

    /**
     * 获取课程申请消息数量
     */
    public long getApplyCourseCount() {
        return messageMapper.getApplyCourseCount();
    }
}