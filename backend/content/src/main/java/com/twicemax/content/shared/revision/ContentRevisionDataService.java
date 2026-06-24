package com.twicemax.content.shared.revision;

import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ContentRevision 数据服务。薄包装一层 Mapper，做参数校验与日志，不在这里做业务编排。
 * <p>
 * 业务编排（生成 revision_no、hash 去重、状态机流转、与主表 current/pending_revision_id 的协同）
 * 放在各内容类型的 DomainService 中（如 RoadmapDomainService）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentRevisionDataService {

    private final ContentRevisionMapper revisionMapper;

    public ContentRevisionDO getById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        return revisionMapper.getById(id);
    }

    public ContentRevisionDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("revision id 无效");
        }
        ContentRevisionDO revision = revisionMapper.getById(id);
        if (revision == null) {
            throw StatusCode.NOT_FOUND.exception("revision 不存在");
        }
        return revision;
    }

    /**
     * 计算下一个 revision_no（事务内调用以保证一致性）。
     * 当 content 无任何 revision 时返回 1。
     */
    public int nextRevisionNo(String contentType, long contentId) {
        Integer max = revisionMapper.maxRevisionNo(contentType, contentId);
        return max == null ? 1 : max + 1;
    }

    /** 该 content 最近一次 revision（按 id DESC），用于 hash 去重比对。 */
    public ContentRevisionDO getLatest(String contentType, long contentId) {
        return revisionMapper.getLatest(contentType, contentId);
    }

    public List<ContentRevisionDO> listByContent(String contentType, long contentId) {
        return revisionMapper.listByContent(contentType, contentId);
    }

    public void insert(ContentRevisionDO revision) {
        revisionMapper.insert(revision);
    }

    public void updateStatus(ContentRevisionDO revision) {
        if (revision == null || revision.getId() == null) {
            throw new IllegalArgumentException("revision or id cannot be null");
        }
        revisionMapper.updateStatus(revision);
    }
}
