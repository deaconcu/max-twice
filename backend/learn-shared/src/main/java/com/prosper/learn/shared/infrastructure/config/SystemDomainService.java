package com.prosper.learn.shared.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.shared.domain.exception.StatusCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * 系统配置领域服务
 *
 * 职责：
 * 1. 应用启动时加载系统配置到内存
 * 2. 提供分类数据查询（课程分类、职业分类）
 * 3. 提供分类验证功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemDomainService {

    private final SystemDataService systemDataService;
    private final ObjectMapper objectMapper;

    // 内存缓存 - 课程分类
    private volatile JsonNode courseCategoriesCache;
    // 内存缓存 - 职业分类
    private volatile JsonNode professionCategoriesCache;

    // 课程主分类 ID 集合（用于快速验证）
    private volatile Set<Integer> courseMainCategoryIds;
    // 课程分类映射：mainCategoryId -> Set<subCategoryId>
    private volatile Map<Integer, Set<Integer>> courseSubCategoryMap;

    // 职业主分类 ID 集合（用于快速验证）
    private volatile Set<Integer> professionMainCategoryIds;
    // 职业分类映射：mainCategoryId -> Set<subCategoryId>
    private volatile Map<Integer, Set<Integer>> professionSubCategoryMap;

    /**
     * 应用启动时加载配置到内存
     */
    @PostConstruct
    public void init() {
        try {
            loadCourseCategories();
            loadProfessionCategories();
            log.info("系统配置加载成功");
        } catch (Exception e) {
            log.error("系统配置加载失败", e);
            // 不抛出异常，避免应用启动失败
        }
    }

    /**
     * 加载课程分类配置
     */
    private void loadCourseCategories() {
        try {
            String configValue = systemDataService.getValue("courseCategories");
            if (configValue == null) {
                log.warn("系统配置 课程分类配置未找到");
                return;
            }

            JsonNode categoryNode = objectMapper.readTree(configValue);

            // 处理嵌套的 courseCategories 结构
            if (categoryNode.has("courseCategories")) {
                categoryNode = categoryNode.get("courseCategories");
            }

            courseCategoriesCache = categoryNode;

            // 构建快速查询索引
            buildCourseCategoryIndex(categoryNode);

            log.info("系统配置 课程分类加载完成: {} 个主分类", courseMainCategoryIds.size());
        } catch (IOException e) {
            log.error("系统配置 课程分类配置解析失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 加载职业分类配置
     */
    private void loadProfessionCategories() {
        try {
            String configValue = systemDataService.getValue("professionCategories");
            if (configValue == null) {
                log.warn("系统配置 职业分类配置未找到");
                return;
            }

            JsonNode categoryNode = objectMapper.readTree(configValue);

            // 处理嵌套的 professionCategories 结构
            if (categoryNode.has("professionCategories")) {
                categoryNode = categoryNode.get("professionCategories");
            }

            professionCategoriesCache = categoryNode;

            // 构建快速查询索引
            buildProfessionCategoryIndex(categoryNode);

            log.info("系统配置 职业分类加载完成: {} 个主分类", professionMainCategoryIds.size());
        } catch (IOException e) {
            log.error("系统配置 职业分类配置解析失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 构建课程分类索引（用于快速验证）
     *
     * 数据结构示例：
     * {
     *   "mainCategories": [{"id": 1, "name": "主分类1"}],
     *   "categoryMapping": [
     *     {
     *       "mainCategoryId": 1,
     *       "subcategories": [{"id": 1, "name": "子分类1"}]
     *     }
     *   ]
     * }
     */
    private void buildCourseCategoryIndex(JsonNode categoryNode) {
        Set<Integer> mainCategoryIds = new HashSet<>();
        Map<Integer, Set<Integer>> subCategoryMap = new HashMap<>();

        try {
            // 解析主分类
            JsonNode mainCategories = categoryNode.get("mainCategories");
            if (mainCategories != null && mainCategories.isArray()) {
                for (JsonNode category : mainCategories) {
                    int id = category.get("id").asInt();
                    mainCategoryIds.add(id);
                }
            }

            // 解析子分类映射
            JsonNode categoryMapping = categoryNode.get("categoryMapping");
            if (categoryMapping != null && categoryMapping.isArray()) {
                for (JsonNode mapping : categoryMapping) {
                    int mainCategoryId = mapping.get("mainCategoryId").asInt();
                    JsonNode subCategories = mapping.get("subcategories");

                    Set<Integer> subCategoryIds = new HashSet<>();
                    if (subCategories != null && subCategories.isArray()) {
                        for (JsonNode subCategory : subCategories) {
                            int subId = subCategory.get("id").asInt();
                            subCategoryIds.add(subId);
                        }
                    }

                    subCategoryMap.put(mainCategoryId, subCategoryIds);
                }
            }

            // 原子更新
            this.courseMainCategoryIds = mainCategoryIds;
            this.courseSubCategoryMap = subCategoryMap;

        } catch (Exception e) {
            log.error("系统配置 构建课程分类索引失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 构建职业分类索引（用于快速验证）
     *
     * 数据结构与课程分类相同
     */
    private void buildProfessionCategoryIndex(JsonNode categoryNode) {
        Set<Integer> mainCategoryIds = new HashSet<>();
        Map<Integer, Set<Integer>> subCategoryMap = new HashMap<>();

        try {
            // 解析主分类
            JsonNode mainCategories = categoryNode.get("mainCategories");
            if (mainCategories != null && mainCategories.isArray()) {
                for (JsonNode category : mainCategories) {
                    int id = category.get("id").asInt();
                    mainCategoryIds.add(id);
                }
            }

            // 解析子分类映射
            JsonNode categoryMapping = categoryNode.get("categoryMapping");
            if (categoryMapping != null && categoryMapping.isArray()) {
                for (JsonNode mapping : categoryMapping) {
                    int mainCategoryId = mapping.get("mainCategoryId").asInt();
                    JsonNode subCategories = mapping.get("subcategories");

                    Set<Integer> subCategoryIds = new HashSet<>();
                    if (subCategories != null && subCategories.isArray()) {
                        for (JsonNode subCategory : subCategories) {
                            int subId = subCategory.get("id").asInt();
                            subCategoryIds.add(subId);
                        }
                    }

                    subCategoryMap.put(mainCategoryId, subCategoryIds);
                }
            }

            // 原子更新
            this.professionMainCategoryIds = mainCategoryIds;
            this.professionSubCategoryMap = subCategoryMap;

        } catch (Exception e) {
            log.error("系统配置 构建职业分类索引失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 获取课程分类数据（已解析的 JsonNode）
     */
    public JsonNode getCourseCategories() {
        if (courseCategoriesCache == null) {
            throw StatusCode.SYSTEM_ERROR.exception("课程分类配置未加载");
        }
        return courseCategoriesCache;
    }

    /**
     * 获取职业分类数据（已解析的 JsonNode）
     */
    public JsonNode getProfessionCategories() {
        if (professionCategoriesCache == null) {
            throw StatusCode.SYSTEM_ERROR.exception("职业分类配置未加载");
        }
        return professionCategoriesCache;
    }

    /**
     * 验证课程分类是否有效
     *
     * @param mainCategoryId 主分类ID
     * @param subCategoryId 子分类ID
     * @throws com.prosper.learn.shared.domain.exception.BizException 分类无效时抛出
     */
    public void validateCourseCategory(Integer mainCategoryId, Integer subCategoryId) {
        if (courseMainCategoryIds == null || courseSubCategoryMap == null) {
            throw StatusCode.SYSTEM_ERROR.exception("课程分类配置未加载");
        }

        // 验证主分类
        if (!courseMainCategoryIds.contains(mainCategoryId)) {
            throw StatusCode.COURSE_CATEGORY_INVALID.exception("主分类不存在: " + mainCategoryId);
        }

        // 验证子分类
        Set<Integer> subCategoryIds = courseSubCategoryMap.get(mainCategoryId);
        if (subCategoryIds == null || !subCategoryIds.contains(subCategoryId)) {
            throw StatusCode.COURSE_CATEGORY_INVALID.exception(
                "子分类不存在: mainCategoryId=" + mainCategoryId + ", subCategoryId=" + subCategoryId);
        }
    }

    /**
     * 验证职业分类是否有效
     *
     * @param mainCategoryId 主分类ID
     * @param subCategoryId 子分类ID
     * @throws com.prosper.learn.shared.domain.exception.BizException 分类无效时抛出
     */
    public void validateProfessionCategory(Integer mainCategoryId, Integer subCategoryId) {
        if (professionMainCategoryIds == null || professionSubCategoryMap == null) {
            throw StatusCode.SYSTEM_ERROR.exception("职业分类配置未加载");
        }

        // 验证主分类
        if (!professionMainCategoryIds.contains(mainCategoryId)) {
            throw StatusCode.PROFESSION_CATEGORY_INVALID.exception("主分类不存在: " + mainCategoryId);
        }

        // 验证子分类
        Set<Integer> subCategoryIds = professionSubCategoryMap.get(mainCategoryId);
        if (subCategoryIds == null || !subCategoryIds.contains(subCategoryId)) {
            throw StatusCode.PROFESSION_CATEGORY_INVALID.exception(
                "子分类不存在: mainCategoryId=" + mainCategoryId + ", subCategoryId=" + subCategoryId);
        }
    }

    /**
     * 重新加载配置（供管理接口调用）
     */
    public void reload() {
        log.info("系统配置 重新加载中...");
        loadCourseCategories();
        loadProfessionCategories();
        log.info("系统配置 重新加载完成");
    }
}
