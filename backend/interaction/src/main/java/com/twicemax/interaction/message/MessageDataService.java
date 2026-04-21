package com.twicemax.interaction.message;

import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息数据服务
 * 负责消息数据的 CRUD
 *
 * 无缓存：消息是即时数据，缓存意义不大
 */
@Service
@RequiredArgsConstructor
public class MessageDataService {

    private final MessageMapper messageMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询消息
     */
    public MessageDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return messageMapper.getById(id);
    }

    /**
     * 按类型查询消息列表
     */
    public List<MessageDO> listByType(int type, long receiverId, Long lastId, int limit) {
        return messageMapper.listByType(type, receiverId, lastId, limit);
    }

    /**
     * 按分类查询消息列表
     * @param category 消息分类 1=互动消息, 2=系统消息, 3=私信
     */
    public List<MessageDO> listByCategory(long receiverId, int category, Long lastId, int limit) {
        return messageMapper.listByCategory(receiverId, category, lastId, limit);
    }

    /**
     * 查询全部消息（互动+系统，category IN (1, 2)）
     */
    public List<MessageDO> listAllMessages(long receiverId, Long lastId, int limit) {
        return messageMapper.listAllMessages(receiverId, lastId, limit);
    }

    /**
     * 统计未读消息数量
     */
    public int countUnreadMessages(long receiverId, long lastViewedMessageId) {
        return messageMapper.countUnreadMessages(receiverId, lastViewedMessageId);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证并获取消息
     */
    public MessageDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("消息ID无效");
        }
        MessageDO message = getById(id);
        if (message == null) {
            throw StatusCode.MESSAGE_NOT_FOUND.exception();
        }
        return message;
    }

    // ==================== 写入方法 ====================

    /**
     * 插入消息
     */
    public void insert(MessageDO messageDO) {
        messageMapper.insert(messageDO);
    }

    /**
     * 更新消息
     */
    public void update(MessageDO messageDO) {
        if (messageDO == null || messageDO.getId() == null) {
            throw new IllegalArgumentException("Message or message ID cannot be null");
        }
        messageMapper.update(messageDO);
    }
}
