package com.prosper.learn.content.toc;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.exception.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 节点目录数据服务
 */
@Service
public class NodeTocDataService extends AbstractDataService<NodeTocDO, NodeTocMapper, String> {

    @Autowired
    private NodeTocMapper nodeTocMapper;

    @Override
    protected NodeTocMapper mapper() {
        return nodeTocMapper;
    }

    @Override
    protected String getCacheName() {
        return "nodeTocs";
    }

    @Override
    protected String getEntityName() {
        return "NodeToc";
    }

    @Override
    protected String getEntityId(NodeTocDO entity) {
        return entity.getHash();
    }

    @Override
    protected NodeTocDO getByIdFromMapper(NodeTocMapper mapper, String id) {
        return nodeTocMapper.get(id);
    }

    @Override
    protected List<NodeTocDO> getByIdsFromMapper(NodeTocMapper mapper, Collection<String> ids) {
        return List.of(); // NodeTocMapper 没有按 String 集合批量查询的方法
    }

    @CacheEvict(value = "nodeTocs", key = "#hash")
    public void incrRef(String hash, int n) {
        nodeTocMapper.incrRef(hash, n);
    }

    /**
     * 验证并获取节点目录
     *
     * @param hash 目录hash
     * @return 节点目录实体
     * @throws com.prosper.learn.shared.domain.exception.BusinessException 当目录不存在时抛出 NODE_TOC_NOT_FOUND
     */
    @Override
    public NodeTocDO validateAndGet(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            throw StatusCode.INVALID_PARAMETER.exception("目录hash不能为空");
        }

        NodeTocDO toc = getById(hash);
        if (toc == null) {
            throw StatusCode.COURSE_TOC_NOT_FOUND.exception();
        }

        return toc;
    }

    /**
     * 根据hash获取目录
     */
    public NodeTocDO get(String hash) {
        return nodeTocMapper.get(hash);
    }

    /**
     * 插入目录
     */
    public void insert(NodeTocDO nodeTocDO) {
        nodeTocMapper.insert(nodeTocDO);
    }

    /**
     * 根据hash数组获取目录映射
     */
    public Map<String, NodeTocDO> getByHashes(String[] hashes) {
        return nodeTocMapper.getByHashes(hashes);
    }

    @Override
    protected Map<String, NodeTocDO> getMapByIdsFromMapper(NodeTocMapper mapper, Collection<String> ids) {
        // 可以使用现有的getByHashes方法
        String[] hashArray = ids.toArray(new String[0]);
        return nodeTocMapper.getByHashes(hashArray);
    }

    @Override
    protected int deleteByIdFromMapper(NodeTocMapper mapper, String id) {
        return 0;
    }
}