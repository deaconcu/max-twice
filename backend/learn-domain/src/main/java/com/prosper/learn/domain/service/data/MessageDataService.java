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

    /**
     * 更新消息
     */
    @CacheEvict(value = "messages", key = "#messageDO.id")
    public void update(MessageDO messageDO) {
        messageMapper.update(messageDO);
    }
}