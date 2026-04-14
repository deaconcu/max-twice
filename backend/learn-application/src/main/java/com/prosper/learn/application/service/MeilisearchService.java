package com.prosper.learn.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.model.Searchable;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.role.RoleDO;
import com.prosper.learn.content.role.RoleDataService;
import com.prosper.learn.infrastructure.datasource.DataSourceContextHolder;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Meilisearch 搜索服务
 *
 * 索引按语言分开：
 * - 业务索引：zh_courses, en_courses, zh_nodes, en_nodes, zh_roles, en_roles
 * - 用户索引：users（共享，不分语言）
 */
@Slf4j
@Service
public class MeilisearchService {

    @Autowired(required = false)
    private Client meilisearchClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CourseDataService courseDataService;
    @Autowired
    private NodeDataService nodeDataService;
    @Autowired
    private UserDataService userDataService;
    @Autowired
    private RoleDataService roleDataService;

    // 基础索引名（业务索引会加语言前缀）
    private static final String INDEX_COURSES = "courses";
    private static final String INDEX_NODES = "nodes";
    private static final String INDEX_ROLES = "roles";
    // 用户索引不分语言
    private static final String INDEX_USERS = "users";

    // ========== 索引名生成（带语言前缀）==========

    private String coursesIndex() {
        return DataSourceContextHolder.getLanguage() + "_" + INDEX_COURSES;
    }

    private String nodesIndex() {
        return DataSourceContextHolder.getLanguage() + "_" + INDEX_NODES;
    }

    private String rolesIndex() {
        return DataSourceContextHolder.getLanguage() + "_" + INDEX_ROLES;
    }

    // ========== 初始化 ==========

    /**
     * 初始化索引（为每种语言创建业务索引）
     */
    public void initializeIndexes() {
        if (meilisearchClient == null) {
            log.info("Meilisearch 未启用，跳过初始化");
            return;
        }
        try {
            // 为每种语言创建业务索引
            DataSourceContextHolder.forEachLanguage(lang -> {
                createIndexIfNotExists(coursesIndex(), "id");
                createIndexIfNotExists(nodesIndex(), "id");
                createIndexIfNotExists(rolesIndex(), "id");

                configureIndex(coursesIndex(), new String[]{"name", "description"});
                configureIndex(nodesIndex(), new String[]{"name", "description"});
                configureIndex(rolesIndex(), new String[]{"name", "description"});

                log.info("[{}] Meilisearch 业务索引初始化完成", lang);
            });

            // 用户索引（共享）
            createIndexIfNotExists(INDEX_USERS, "id");
            configureIndex(INDEX_USERS, new String[]{"name"});

            log.info("Meilisearch 索引初始化完成");
        } catch (Exception e) {
            log.error("Meilisearch 索引初始化失败", e);
        }
    }

    private void createIndexIfNotExists(String indexName, String primaryKey) {
        try {
            meilisearchClient.getIndex(indexName);
        } catch (Exception e) {
            try {
                meilisearchClient.createIndex(indexName, primaryKey);
                log.info("Meilisearch 创建索引: {}", indexName);
            } catch (Exception ex) {
                log.error("Meilisearch 创建索引失败: {}", indexName, ex);
            }
        }
    }

    private void configureIndex(String indexName, String[] searchableAttributes) {
        try {
            Index index = meilisearchClient.index(indexName);
            index.updateSearchableAttributesSettings(searchableAttributes);
            index.updateSortableAttributesSettings(new String[]{"id"});
        } catch (Exception e) {
            log.error("Meilisearch 配置索引失败: {}", indexName, e);
        }
    }

    // ========== 全量同步 ==========

    /**
     * 全量同步（为每种语言同步业务数据）
     */
    public void syncAll() {
        if (meilisearchClient == null) return;
        log.info("Meilisearch 开始全量同步...");
        long start = System.currentTimeMillis();

        // 为每种语言同步业务数据
        DataSourceContextHolder.forEachLanguage(lang -> {
            int courses = syncAllCourses();
            int nodes = syncAllNodes();
            int roles = syncAllRoles();
            log.info("[{}] Meilisearch 同步完成，课程: {}，节点: {}，角色: {}",
                lang, courses, nodes, roles);
        });

        // 用户是共享的，只同步一次
        int users = syncAllUsers();

        log.info("Meilisearch 全量同步完成，耗时 {}ms，用户: {}",
            System.currentTimeMillis() - start, users);
    }

    public int syncAllCourses() {
        if (meilisearchClient == null) return 0;
        String indexName = coursesIndex();
        try {
            log.info("Meilisearch 同步课程到索引 {}...", indexName);
            meilisearchClient.deleteIndex(indexName);
            createIndexIfNotExists(indexName, "id");
            configureIndex(indexName, new String[]{"name", "description"});

            int total = 0;
            Long lastId = null;
            while (true) {
                List<CourseDO> list = courseDataService.listByState(Enums.ContentState.PUBLISHED.value(), lastId, 1000);
                if (list.isEmpty()) break;

                bulkIndexCourses(list);
                total += list.size();
                lastId = list.get(list.size() - 1).getId();

                if (total % 1000 == 0) {
                    log.info("Meilisearch 已同步 {} 个课程", total);
                }

                if (list.size() < 20) break;
            }
            log.info("Meilisearch 课程同步完成，共 {} 个", total);
            return total;
        } catch (Exception e) {
            log.error("Meilisearch 同步课程失败", e);
            return 0;
        }
    }

    public int syncAllNodes() {
        if (meilisearchClient == null) return 0;
        String indexName = nodesIndex();
        try {
            log.info("Meilisearch 同步节点到索引 {}...", indexName);
            meilisearchClient.deleteIndex(indexName);
            createIndexIfNotExists(indexName, "id");
            configureIndex(indexName, new String[]{"name", "description"});

            int total = 0;
            Long lastId = null;
            while (true) {
                List<NodeDO> list = nodeDataService.listByState(Enums.ContentState.PUBLISHED.value(), lastId, 1000, true);
                if (list.isEmpty()) break;

                bulkIndexNodes(list);
                total += list.size();
                lastId = list.get(list.size() - 1).getId();

                if (total % 1000 == 0) {
                    log.info("Meilisearch 已同步 {} 个节点", total);
                }

                if (list.size() < 1000) break;
            }
            log.info("Meilisearch 节点同步完成，共 {} 个", total);
            return total;
        } catch (Exception e) {
            log.error("Meilisearch 同步节点失败", e);
            return 0;
        }
    }

    public int syncAllUsers() {
        if (meilisearchClient == null) return 0;
        try {
            log.info("Meilisearch 同步用户...");
            meilisearchClient.deleteIndex(INDEX_USERS);
            createIndexIfNotExists(INDEX_USERS, "id");
            configureIndex(INDEX_USERS, new String[]{"name"});

            int total = 0;
            Long lastId = null;
            while (true) {
                List<UserDO> list = userDataService.listByState(Enums.UserState.ACTIVE.value(), lastId, 1000);
                if (list.isEmpty()) break;

                bulkIndexUsers(list);
                total += list.size();
                lastId = list.get(list.size() - 1).getId();

                if (total % 1000 == 0) {
                    log.info("Meilisearch 已同步 {} 个用户", total);
                }

                if (list.size() < 1000) break;
            }
            log.info("Meilisearch 用户同步完成，共 {} 个", total);
            return total;
        } catch (Exception e) {
            log.error("Meilisearch 同步用户失败", e);
            return 0;
        }
    }

    public int syncAllRoles() {
        if (meilisearchClient == null) return 0;
        String indexName = rolesIndex();
        try {
            log.info("Meilisearch 同步角色到索引 {}...", indexName);
            meilisearchClient.deleteIndex(indexName);
            createIndexIfNotExists(indexName, "id");
            configureIndex(indexName, new String[]{"name", "description"});

            int total = 0;
            Long lastId = null;
            while (true) {
                List<RoleDO> list = roleDataService.listByState(
                        Enums.ContentState.PUBLISHED.value(), lastId, 1000);
                if (list.isEmpty()) break;

                bulkIndexRoles(list);
                total += list.size();
                lastId = list.get(list.size() - 1).getId();

                if (total % 1000 == 0) {
                    log.info("Meilisearch 已同步 {} 个角色", total);
                }

                if (list.size() < 1000) break;
            }
            log.info("Meilisearch 角色同步完成，共 {} 个", total);
            return total;
        } catch (Exception e) {
            log.error("Meilisearch 同步角色失败", e);
            return 0;
        }
    }

    private void bulkIndexCourses(List<CourseDO> courses) throws Exception {
        List<Map<String, Object>> docs = new ArrayList<>();
        for (CourseDO c : courses) {
            docs.add(Map.of(
                "id", c.getId(),
                "name", c.getName(),
                "description", c.getDescription() != null ? c.getDescription() : ""
            ));
        }
        meilisearchClient.index(coursesIndex()).addDocuments(objectMapper.writeValueAsString(docs));
    }

    private void bulkIndexNodes(List<NodeDO> nodes) throws Exception {
        List<Map<String, Object>> docs = new ArrayList<>();
        for (NodeDO n : nodes) {
            docs.add(Map.of(
                "id", n.getId(),
                "name", n.getName(),
                "description", n.getDescription() != null ? n.getDescription() : ""
            ));
        }
        meilisearchClient.index(nodesIndex()).addDocuments(objectMapper.writeValueAsString(docs));
    }

    private void bulkIndexUsers(List<UserDO> users) throws Exception {
        List<Map<String, Object>> docs = new ArrayList<>();
        for (UserDO u : users) {
            docs.add(Map.of("id", u.getId(), "name", u.getName()));
        }
        meilisearchClient.index(INDEX_USERS).addDocuments(objectMapper.writeValueAsString(docs));
    }

    private void bulkIndexRoles(List<RoleDO> roles) throws Exception {
        List<Map<String, Object>> docs = new ArrayList<>();
        for (RoleDO p : roles) {
            docs.add(Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "description", p.getDescription() != null ? p.getDescription() : ""
            ));
        }
        meilisearchClient.index(rolesIndex()).addDocuments(objectMapper.writeValueAsString(docs));
    }

    // ========== 实时同步 ==========

    /**
     * 索引课程（异步，需传递语言上下文）
     */
    @Async
    public void indexCourse(CourseDO course, String language) {
        if (meilisearchClient == null) return;
        DataSourceContextHolder.setLanguage(language);
        try {
            if (course.getState() != Enums.ContentState.PUBLISHED.value()) {
                deleteCourseInternal(course.getId());
                return;
            }
            Map<String, Object> doc = Map.of(
                "id", course.getId(),
                "name", course.getName(),
                "description", course.getDescription() != null ? course.getDescription() : ""
            );
            meilisearchClient.index(coursesIndex()).addDocuments(objectMapper.writeValueAsString(List.of(doc)));
        } catch (Exception e) {
            log.error("[{}] Meilisearch 索引课程失败: {}", language, course.getId(), e);
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    @Async
    public void deleteCourse(Long id, String language) {
        if (meilisearchClient == null) return;
        DataSourceContextHolder.setLanguage(language);
        try {
            deleteCourseInternal(id);
        } catch (Exception e) {
            log.error("[{}] Meilisearch 删除课程失败: {}", language, id, e);
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    private void deleteCourseInternal(Long id) {
        meilisearchClient.index(coursesIndex()).deleteDocument(String.valueOf(id));
    }

    @Async
    public void indexNode(NodeDO node, String language) {
        if (meilisearchClient == null) return;
        DataSourceContextHolder.setLanguage(language);
        try {
            if (node.getState() != Enums.ContentState.PUBLISHED.value()) {
                deleteNodeInternal(node.getId());
                return;
            }
            Map<String, Object> doc = Map.of(
                "id", node.getId(),
                "name", node.getName(),
                "description", node.getDescription() != null ? node.getDescription() : ""
            );
            meilisearchClient.index(nodesIndex()).addDocuments(objectMapper.writeValueAsString(List.of(doc)));
        } catch (Exception e) {
            log.error("[{}] Meilisearch 索引节点失败: {}", language, node.getId(), e);
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    @Async
    public void deleteNode(Long id, String language) {
        if (meilisearchClient == null) return;
        DataSourceContextHolder.setLanguage(language);
        try {
            deleteNodeInternal(id);
        } catch (Exception e) {
            log.error("[{}] Meilisearch 删除节点失败: {}", language, id, e);
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    private void deleteNodeInternal(Long id) {
        meilisearchClient.index(nodesIndex()).deleteDocument(String.valueOf(id));
    }

    @Async
    public void indexUser(UserDO user) {
        if (meilisearchClient == null) return;
        try {
            Map<String, Object> doc = Map.of("id", user.getId(), "name", user.getName());
            meilisearchClient.index(INDEX_USERS).addDocuments(objectMapper.writeValueAsString(List.of(doc)));
        } catch (Exception e) {
            log.error("Meilisearch 索引用户失败: {}", user.getId(), e);
        }
    }

    @Async
    public void deleteUser(Long id) {
        if (meilisearchClient == null) return;
        try {
            meilisearchClient.index(INDEX_USERS).deleteDocument(String.valueOf(id));
        } catch (Exception e) {
            log.error("Meilisearch 删除用户失败: {}", id, e);
        }
    }

    @Async
    public void indexRole(RoleDO roleDO, String language) {
        if (meilisearchClient == null) return;
        DataSourceContextHolder.setLanguage(language);
        try {
            if (roleDO.getState() != Enums.ContentState.PUBLISHED.value()) {
                deleteRoleInternal(roleDO.getId());
                return;
            }
            Map<String, Object> doc = Map.of(
                "id", roleDO.getId(),
                "name", roleDO.getName(),
                "description", roleDO.getDescription() != null ? roleDO.getDescription() : ""
            );
            meilisearchClient.index(rolesIndex()).addDocuments(objectMapper.writeValueAsString(List.of(doc)));
        } catch (Exception e) {
            log.error("[{}] Meilisearch 索引角色失败: {}", language, roleDO.getId(), e);
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    @Async
    public void deleteRole(Long id, String language) {
        if (meilisearchClient == null) return;
        DataSourceContextHolder.setLanguage(language);
        try {
            deleteRoleInternal(id);
        } catch (Exception e) {
            log.error("[{}] Meilisearch 删除角色失败: {}", language, id, e);
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    private void deleteRoleInternal(Long id) {
        meilisearchClient.index(rolesIndex()).deleteDocument(String.valueOf(id));
    }

    // ========== 搜索 ==========

    public Searchable searchCourses(String query, int limit, int offset) {
        if (meilisearchClient == null) return null;
        try {
            return meilisearchClient.index(coursesIndex())
                .search(SearchRequest.builder().q(query).limit(limit).offset(offset).build());
        } catch (Exception e) {
            log.error("Meilisearch 搜索课程失败", e);
            return null;
        }
    }

    public Searchable searchNodes(String query, int limit, int offset) {
        if (meilisearchClient == null) return null;
        try {
            return meilisearchClient.index(nodesIndex())
                .search(SearchRequest.builder().q(query).limit(limit).offset(offset).build());
        } catch (Exception e) {
            log.error("Meilisearch 搜索节点失败", e);
            return null;
        }
    }

    public Searchable searchUsers(String query, int limit, int offset) {
        if (meilisearchClient == null) return null;
        try {
            return meilisearchClient.index(INDEX_USERS)
                .search(SearchRequest.builder().q(query).limit(limit).offset(offset).build());
        } catch (Exception e) {
            log.error("Meilisearch 搜索用户失败", e);
            return null;
        }
    }

    public Searchable searchRoles(String query, int limit, int offset) {
        if (meilisearchClient == null) return null;
        try {
            return meilisearchClient.index(rolesIndex())
                .search(SearchRequest.builder().q(query).limit(limit).offset(offset).build());
        } catch (Exception e) {
            log.error("Meilisearch 搜索角色失败", e);
            return null;
        }
    }
}
