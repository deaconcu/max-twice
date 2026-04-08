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

    private static final String INDEX_COURSES = "courses";
    private static final String INDEX_NODES = "nodes";
    private static final String INDEX_USERS = "users";
    private static final String INDEX_ROLES = "roles";

    // ========== 初始化 ==========

    public void initializeIndexes() {
        if (meilisearchClient == null) {
            log.info("Meilisearch 未启用，跳过初始化");
            return;
        }
        try {
            createIndexIfNotExists(INDEX_COURSES, "id");
            createIndexIfNotExists(INDEX_NODES, "id");
            createIndexIfNotExists(INDEX_USERS, "id");
            createIndexIfNotExists(INDEX_ROLES, "id");

            configureIndex(INDEX_COURSES, new String[]{"name", "description"});
            configureIndex(INDEX_NODES, new String[]{"name", "description"});
            configureIndex(INDEX_USERS, new String[]{"name"});
            configureIndex(INDEX_ROLES, new String[]{"name", "description"});

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

    public void syncAll() {
        if (meilisearchClient == null) return;
        log.info("Meilisearch 开始全量同步...");
        long start = System.currentTimeMillis();

        int courses = syncAllCourses();
        int nodes = syncAllNodes();
        int users = syncAllUsers();
        int roles = syncAllRoles();

        log.info("Meilisearch 全量同步完成，耗时 {}ms，课程: {}，节点: {}，用户: {}，角色: {}",
            System.currentTimeMillis() - start, courses, nodes, users, roles);
    }

    public int syncAllCourses() {
        if (meilisearchClient == null) return 0;
        try {
            log.info("Meilisearch 同步课程...");
            meilisearchClient.deleteIndex(INDEX_COURSES);
            createIndexIfNotExists(INDEX_COURSES, "id");
            configureIndex(INDEX_COURSES, new String[]{"name", "description"});

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
        try {
            log.info("Meilisearch 同步节点...");
            meilisearchClient.deleteIndex(INDEX_NODES);
            createIndexIfNotExists(INDEX_NODES, "id");
            configureIndex(INDEX_NODES, new String[]{"name", "description"});

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
        try {
            log.info("Meilisearch 同步角色...");
            meilisearchClient.deleteIndex(INDEX_ROLES);
            createIndexIfNotExists(INDEX_ROLES, "id");
            configureIndex(INDEX_ROLES, new String[]{"name", "description"});

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
        meilisearchClient.index(INDEX_COURSES).addDocuments(objectMapper.writeValueAsString(docs));
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
        meilisearchClient.index(INDEX_NODES).addDocuments(objectMapper.writeValueAsString(docs));
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
        meilisearchClient.index(INDEX_ROLES).addDocuments(objectMapper.writeValueAsString(docs));
    }

    // ========== 实时同步 ==========

    @Async
    public void indexCourse(CourseDO course) {
        if (meilisearchClient == null) return;
        if (course.getState() != Enums.ContentState.PUBLISHED.value()) {
            deleteCourse(course.getId());
            return;
        }
        try {
            Map<String, Object> doc = Map.of(
                "id", course.getId(),
                "name", course.getName(),
                "description", course.getDescription() != null ? course.getDescription() : ""
            );
            meilisearchClient.index(INDEX_COURSES).addDocuments(objectMapper.writeValueAsString(List.of(doc)));
        } catch (Exception e) {
            log.error("Meilisearch 索引课程失败: {}", course.getId(), e);
        }
    }

    @Async
    public void deleteCourse(Long id) {
        if (meilisearchClient == null) return;
        try {
            meilisearchClient.index(INDEX_COURSES).deleteDocument(String.valueOf(id));
        } catch (Exception e) {
            log.error("Meilisearch 删除课程失败: {}", id, e);
        }
    }

    @Async
    public void indexNode(NodeDO node) {
        if (meilisearchClient == null) return;
        if (node.getState() != Enums.ContentState.PUBLISHED.value()) {
            deleteNode(node.getId());
            return;
        }
        try {
            Map<String, Object> doc = Map.of(
                "id", node.getId(),
                "name", node.getName(),
                "description", node.getDescription() != null ? node.getDescription() : ""
            );
            meilisearchClient.index(INDEX_NODES).addDocuments(objectMapper.writeValueAsString(List.of(doc)));
        } catch (Exception e) {
            log.error("Meilisearch 索引节点失败: {}", node.getId(), e);
        }
    }

    @Async
    public void deleteNode(Long id) {
        if (meilisearchClient == null) return;
        try {
            meilisearchClient.index(INDEX_NODES).deleteDocument(String.valueOf(id));
        } catch (Exception e) {
            log.error("Meilisearch 删除节点失败: {}", id, e);
        }
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
    public void indexRole(RoleDO roleDO) {
        if (meilisearchClient == null) return;
        if (roleDO.getState() != Enums.ContentState.PUBLISHED.value()) {
            deleteRole(roleDO.getId());
            return;
        }
        try {
            Map<String, Object> doc = Map.of(
                "id", roleDO.getId(),
                "name", roleDO.getName(),
                "description", roleDO.getDescription() != null ? roleDO.getDescription() : ""
            );
            meilisearchClient.index(INDEX_ROLES).addDocuments(objectMapper.writeValueAsString(List.of(doc)));
        } catch (Exception e) {
            log.error("Meilisearch 索引角色失败: {}", roleDO.getId(), e);
        }
    }

    @Async
    public void deleteRole(Long id) {
        if (meilisearchClient == null) return;
        try {
            meilisearchClient.index(INDEX_ROLES).deleteDocument(String.valueOf(id));
        } catch (Exception e) {
            log.error("Meilisearch 删除角色失败: {}", id, e);
        }
    }

    // ========== 搜索 ==========

    public Searchable searchCourses(String query, int limit, int offset) {
        if (meilisearchClient == null) return null;
        try {
            return meilisearchClient.index(INDEX_COURSES)
                .search(SearchRequest.builder().q(query).limit(limit).offset(offset).build());
        } catch (Exception e) {
            log.error("Meilisearch 搜索课程失败", e);
            return null;
        }
    }

    public Searchable searchNodes(String query, int limit, int offset) {
        if (meilisearchClient == null) return null;
        try {
            return meilisearchClient.index(INDEX_NODES)
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
            return meilisearchClient.index(INDEX_ROLES)
                .search(SearchRequest.builder().q(query).limit(limit).offset(offset).build());
        } catch (Exception e) {
            log.error("Meilisearch 搜索角色失败", e);
            return null;
        }
    }
}
