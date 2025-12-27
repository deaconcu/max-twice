package com.prosper.learn.web.architecture;

import com.prosper.learn.shared.common.validator.ConfigurableSize;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.validation.constraints.*;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

/**
 * DTO 验证注解架构测试
 *
 * 目的：确保所有 Request DTO 的字段都正确添加了验证注解
 * 防止开发者忘记添加 @NotBlank, @NotNull 等注解
 *
 * 说明：只检查 Create*Request，因为：
 * - Create*Request: 创建操作，所有字段通常是必填的
 * - Update*Request: 更新操作，字段可能是可选的（部分更新）
 * - Query*Request: 查询操作，字段通常是可选的筛选条件
 */
class DtoValidationArchTest {

    private final JavaClasses dtoClasses = new ClassFileImporter()
            .importPackages("com.prosper.learn.application.dto.request");

    /**
     * 规则1: Create*Request 中的 String 类型字段必须有验证注解
     *
     * 要求：String 字段必须至少有以下之一
     * - @NotBlank (推荐，同时验证非空和非空白)
     * - @NotNull (允许空字符串)
     * - @Pattern (有格式要求时)
     * - @Size / @ConfigurableSize (可选字段，只限制长度)
     */
    @Test
    void create_request_string_fields_should_have_validation_annotations() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().haveSimpleNameStartingWith("Create")
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Request")
                .and().haveRawType(String.class)
                .and().areNotStatic()
                .should().beAnnotatedWith(NotBlank.class)
                .orShould().beAnnotatedWith(NotNull.class)
                .orShould().beAnnotatedWith(Pattern.class)
                .orShould().beAnnotatedWith(Size.class)
                .orShould().beAnnotatedWith(ConfigurableSize.class)
                .because("Create Request 的 String 字段都应该有验证注解，@NotBlank/@NotNull 表示必填，@Size/@ConfigurableSize 表示可选");

        rule.check(dtoClasses);
    }

    /**
     * 规则2: Create*Request 中的 String 字段应该有长度限制
     *
     * 说明：这个规则检查是否有长度限制注解
     */
    @Test
    void create_request_string_fields_should_have_size_limit() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().haveSimpleNameStartingWith("Create")
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Request")
                .and().haveRawType(String.class)
                .and().areNotStatic()
                .and().areAnnotatedWith(NotBlank.class)
                .should().beAnnotatedWith(Size.class)
                .orShould().beAnnotatedWith(ConfigurableSize.class)
                .because("String 字段应该有长度限制，防止过长的输入");

        rule.check(dtoClasses);
    }

    /**
     * 规则3: Create*Request 中的 Integer 字段应该有 @NotNull
     */
    @Test
    void create_request_number_fields_should_have_not_null() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().haveSimpleNameStartingWith("Create")
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Request")
                .and().haveRawType(Integer.class)
                .and().areNotStatic()
                .should().beAnnotatedWith(NotNull.class)
                .because("Create Request 的 Integer 字段应该标注 @NotNull，明确是否允许空值");

        rule.check(dtoClasses);
    }
}
