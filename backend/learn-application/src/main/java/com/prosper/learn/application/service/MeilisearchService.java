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
import com.prosper.learn.content.profession.ProfessionDO;
import com.prosper.learn.content.profession.ProfessionDataService;
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
    private ProfessionDataService professionDataService;

    private static final String INDEX_COURSES = "courses";
    private static final String INDEX_NODES = "nodes";
    private static final String INDEX_USERS = "users";
    private static final String INDEX_PROFESSIONS = "professions";

    // ========== 初始化 ==========

    public void initializeIndexes() {
        if (meilisearchClient == null) {
            log.info("Meilisearch is disabled, skipping initialization");
            return;
        }
        try {
            createIndexIfNotExists(INDEX_COURSES, "id");
            createIndexIfNotExists(INDEX_NODES, "id");
            createIndexIfNotExists(INDEX_USERS, "id");
            createIndexIfNotExists(INDEX_PROFESSIONS, "id");

            configureIndex(INDEX_COURSES, new String[]{"name", "description"});
            configureIndex(INDEX_NODES, new String[]{"name", "description"});
            configureIndex(INDEX_USERS, new String[]{"name"});
            configureIndex(INDEX_PROFESSIONS, new String[]{"name", "description"});

            log.info("Meilisearch indexes initialized");
        } catch (Exception e) {
            log.error("Failed to initialize indexes", e);
        }
    }

    private void createIndexIfNotExists(String indexName, String primaryKey) {
        try {
            meilisearchClient.getIndex(indexName);
        } catch (Exception e) {
            try {
                meilisearchClient.createIndex(indexName, primaryKey);
                log.info("Created index: {}", indexName);
            } catch (Exception ex) {
                log.error("Failed to create index: {}", indexName, ex);
            }
        }
    }

    private void configureIndex(String indexName, String[] searchableAttributes) {
        try {
            Index index = meilisearchClient.index(indexName);
            index.updateSearchableAttributesSettings(searchableAttributes);
            index.updateSortableAttributesSettings(new String[]{"id"});
        } catch (Exception e) {
            log.error("Failed to configure index: {}", indexName, e);
        }
    }

    // ========== 全量同步 ==========

    public void syncAll() {
        if (meilisearchClient == null) return;
        log.info("Starting full sync...");
        long start = System.currentTimeMillis();

        int courses = syncAllCourses();
        int nodes = syncAllNodes();
        int users = syncAllUsers();
        int professions = syncAllProfessions();

        log.info("Full sync done in {}ms. Courses: {}, Nodes: {}, Users: {}, Professions: {}",
            System.currentTimeMillis() - start, courses, nodes, users, professions);
    }

    public int syncAllCourses() {
        if (meilisearchClient == null) return 0;
        try {
            log.info("Syncing courses...");
            meilisearchClient.deleteIndex(INDEX_COURSES);
            createIndexIfNotExists(INDEX_COURSES, "id");
            configureIndex(INDEX_COURSES, new String[]{"name", "description"});

            int total = 0;
            Long lastId = null;
            while (true) {
                List<CourseDO> list = courseDataService.listByStateAndLastId(Enums.ContentState.PUBLISHED, lastId);
                if (list.isEmpty()) break;

                bulkIndexCourses(list);
                total += list.size();
                lastId = list.get(list.size() - 1).getId();

                if (total % 1000 == 0) {
                    log.info("Synced {} courses", total);
                }

                if (list.size() < 20) break;
            }
            log.info("Finished syncing {} courses", total);
            return total;
        } catch (Exception e) {
            log.error("Failed to sync courses", e);
            return 0;
        }
    }

    public int syncAllNodes() {
        if (meilisearchClient == null) return 0;
        try {
            log.info("Syncing nodes...");
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
                    log.info("Synced {} nodes", total);
                }

                if (list.size() < 1000) break;
            }
            log.info("Finished syncing {} nodes", total);
            return total;
        } catch (Exception e) {
            log.error("Failed to sync nodes", e);
            return 0;
        }
    }

    public int syncAllUsers() {
        if (meilisearchClient == null) return 0;
        try {
            log.info("Syncing users...");
            meilisearchClient.deleteIndex(INDEX_USERS);
            createIndexIfNotExists(INDEX_USERS, "id");
            configureIndex(INDEX_USERS, new String[]{"name"});

            int total = 0;
            Long lastId = null;
            while (true) {
                List<UserDO> list = userDataService.listByStateAndLastId(Enums.UserState.ACTIVE.value(), lastId, 1000);
                if (list.isEmpty()) break;

                bulkIndexUsers(list);
                total += list.size();
                lastId = list.get(list.size() - 1).getId();

                if (total % 1000 == 0) {
                    log.info("Synced {} users", total);
                }

                if (list.size() < 1000) break;
            }
            log.info("Finished syncing {} users", total);
            return total;
        } catch (Exception e) {
            log.error("Failed to sync users", e);
            return 0;
        }
    }

    public int syncAllProfessions() {
        if (meilisearchClient == null) return 0;
        try {
            log.info("Syncing professions...");
            meilisearchClient.deleteIndex(INDEX_PROFESSIONS);
            createIndexIfNotExists(INDEX_PROFESSIONS, "id");
            configureIndex(INDEX_PROFESSIONS, new String[]{"name", "description"});

            int total = 0;
            Long lastId = null;
            while (true) {
                List<ProfessionDO> list = professionDataService.listByStateAndLastId(
                        Enums.ContentState.PUBLISHED.value(), lastId, 1000);
                if (list.isEmpty()) break;

                bulkIndexProfessions(list);
                total += list.size();
                lastId = list.get(list.size() - 1).getId();

                if (total % 1000 == 0) {
                    log.info("Synced {} professions", total);
                }

                if (list.size() < 1000) break;
            }
            log.info("Finished syncing {} professions", total);
            return total;
        } catch (Exception e) {
            log.error("Failed to sync professions", e);
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

    private void bulkIndexProfessions(List<ProfessionDO> professions) throws Exception {
        List<Map<String, Object>> docs = new ArrayList<>();
        for (ProfessionDO p : professions) {
            docs.add(Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "description", p.getDescription() != null ? p.getDescription() : ""
            ));
        }
        meilisearchClient.index(INDEX_PROFESSIONS).addDocuments(objectMapper.writeValueAsString(docs));
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
            log.error("Failed to index course: {}", course.getId(), e);
        }
    }

    @Async
    public void deleteCourse(Long id) {
        if (meilisearchClient == null) return;
        try {
            meilisearchClient.index(INDEX_COURSES).deleteDocument(String.valueOf(id));
        } catch (Exception e) {
            log.error("Failed to delete course: {}", id, e);
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
            log.error("Failed to index node: {}", node.getId(), e);
        }
    }

    @Async
    public void deleteNode(Long id) {
        if (meilisearchClient == null) return;
        try {
            meilisearchClient.index(INDEX_NODES).deleteDocument(String.valueOf(id));
        } catch (Exception e) {
            log.error("Failed to delete node: {}", id, e);
        }
    }

    @Async
    public void indexUser(UserDO user) {
        if (meilisearchClient == null) return;
        try {
            Map<String, Object> doc = Map.of("id", user.getId(), "name", user.getName());
            meilisearchClient.index(INDEX_USERS).addDocuments(objectMapper.writeValueAsString(List.of(doc)));
        } catch (Exception e) {
            log.error("Failed to index user: {}", user.getId(), e);
        }
    }

    @Async
    public void deleteUser(Long id) {
        if (meilisearchClient == null) return;
        try {
            meilisearchClient.index(INDEX_USERS).deleteDocument(String.valueOf(id));
        } catch (Exception e) {
            log.error("Failed to delete user: {}", id, e);
        }
    }

    @Async
    public void indexProfession(ProfessionDO profession) {
        if (meilisearchClient == null) return;
        if (profession.getState() != Enums.ContentState.PUBLISHED.value()) {
            deleteProfession(profession.getId());
            return;
        }
        try {
            Map<String, Object> doc = Map.of(
                "id", profession.getId(),
                "name", profession.getName(),
                "description", profession.getDescription() != null ? profession.getDescription() : ""
            );
            meilisearchClient.index(INDEX_PROFESSIONS).addDocuments(objectMapper.writeValueAsString(List.of(doc)));
        } catch (Exception e) {
            log.error("Failed to index profession: {}", profession.getId(), e);
        }
    }

    @Async
    public void deleteProfession(Long id) {
        if (meilisearchClient == null) return;
        try {
            meilisearchClient.index(INDEX_PROFESSIONS).deleteDocument(String.valueOf(id));
        } catch (Exception e) {
            log.error("Failed to delete profession: {}", id, e);
        }
    }

    // ========== 搜索 ==========

    public Searchable searchCourses(String query, int limit, int offset) {
        if (meilisearchClient == null) return null;
        try {
            return meilisearchClient.index(INDEX_COURSES)
                .search(SearchRequest.builder().q(query).limit(limit).offset(offset).build());
        } catch (Exception e) {
            log.error("Search courses failed", e);
            return null;
        }
    }

    public Searchable searchNodes(String query, int limit, int offset) {
        if (meilisearchClient == null) return null;
        try {
            return meilisearchClient.index(INDEX_NODES)
                .search(SearchRequest.builder().q(query).limit(limit).offset(offset).build());
        } catch (Exception e) {
            log.error("Search nodes failed", e);
            return null;
        }
    }

    public Searchable searchUsers(String query, int limit, int offset) {
        if (meilisearchClient == null) return null;
        try {
            return meilisearchClient.index(INDEX_USERS)
                .search(SearchRequest.builder().q(query).limit(limit).offset(offset).build());
        } catch (Exception e) {
            log.error("Search users failed", e);
            return null;
        }
    }

    public Searchable searchProfessions(String query, int limit, int offset) {
        if (meilisearchClient == null) return null;
        try {
            return meilisearchClient.index(INDEX_PROFESSIONS)
                .search(SearchRequest.builder().q(query).limit(limit).offset(offset).build());
        } catch (Exception e) {
            log.error("Search professions failed", e);
            return null;
        }
    }
}
