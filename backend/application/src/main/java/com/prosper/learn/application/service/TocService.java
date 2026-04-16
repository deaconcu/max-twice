package com.prosper.learn.application.service;

import com.prosper.learn.content.toc.TocDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 目录应用服务
 *
 * 负责跨域协调、事务管理、DTO转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TocService {

    private final TocDomainService tocDomainService;

    /**
     * 更新用户节点目录
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param indexArray 索引数组字符串（逗号分隔）
     */
    @Transactional
    public void updateUserNodeToc(long userId, long nodeId, String indexArray) {
        tocDomainService.updateUserNodeToc(userId, nodeId, indexArray);
        log.info("用户 {} 更新节点 {} 的目录", userId, nodeId);
    }
}
