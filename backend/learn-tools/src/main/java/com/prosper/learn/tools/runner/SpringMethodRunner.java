package com.prosper.learn.tools.runner;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;

/**
 * 通用的 Spring 方法执行器
 *
 * 使用和 Web 启动类相同的配置，但不启动 Web 容器
 *
 * 使用方式：
 * cd backend
 * mvn compile
 * mvn exec:java -pl learn-tools -Dexec.mainClass="com.prosper.learn.tools.runner.SpringMethodRunner" \
 *   -Dexec.args="com.prosper.learn.tools.migration.CourseRootNodeFixMigration execute"
 *
 * 或者在 IDEA 中运行此类的 main 方法，并配置 Program arguments:
 * com.prosper.learn.tools.migration.CourseRootNodeFixMigration execute
 */
@Slf4j
@SpringBootApplication
@ComponentScan(
    basePackages = {
        "com.prosper.learn.tools",                          // Tools 包
        "com.prosper.learn.content",                        // Content 相关
        "com.prosper.learn.shared.infrastructure.config",   // SystemProperties
        "com.prosper.learn.shared.common.utils"             // Utils 工具类
    }
)
@MapperScan(
    basePackages = "com.prosper.learn",
    annotationClass = Mapper.class
)
public class SpringMethodRunner {

    public static void main(String[] args) {
        if (args.length < 2) {
            log.error("========================================");
            log.error("用法错误！");
            log.error("========================================");
            log.error("需要提供参数: <Bean完整类名> <方法名> [参数1] [参数2] ...");
            log.error("");
            log.error("示例:");
            log.error("  com.prosper.learn.application.runner.RoadmapContentMigration migrateAllRoadmaps");
            log.error("========================================");
            System.exit(1);
            return;
        }

        String beanClassName = args[0];
        String methodName = args[1];

        log.info("========================================");
        log.info("Spring 方法执行器");
        log.info("========================================");
        log.info("目标类: {}", beanClassName);
        log.info("目标方法: {}", methodName);
        log.info("========================================");

        ConfigurableApplicationContext context = null;

        try {
            // 启动 Spring 容器（不启动 Web 服务器）
            log.info("正在启动 Spring 容器（非 Web 模式）...");
            log.info("配置文件：learn-application/src/main/resources/application.yml");
            log.info("");

            context = new SpringApplicationBuilder(SpringMethodRunner.class)
                    .web(WebApplicationType.NONE)  // 不启动 Web 容器
                    .run(args);

            log.info("✓ Spring 容器启动成功");
            log.info("");

            // 获取 Bean
            Class<?> beanClass = Class.forName(beanClassName);
            Object bean = context.getBean(beanClass);
            log.info("✓ 获取 Bean 成功: {}", bean.getClass().getSimpleName());
            log.info("");

            // 提取方法参数
            Object[] methodArgs = new Object[args.length - 2];
            System.arraycopy(args, 2, methodArgs, 0, args.length - 2);

            // 查找并执行方法
            Method method = findMethod(beanClass, methodName, methodArgs);
            if (method == null) {
                throw new NoSuchMethodException("未找到方法: " + methodName);
            }

            log.info("✓ 找到方法: {}", method.getName());
            log.info("");
            log.info("开始执行方法...");
            log.info("----------------------------------------");

            Object result = method.invoke(bean, methodArgs);

            log.info("----------------------------------------");
            log.info("✓ 方法执行完成");

            if (result != null) {
                log.info("");
                log.info("返回结果: {}", result);
            }

            log.info("");
            log.info("========================================");
            log.info("执行成功！");
            log.info("========================================");

            System.exit(0);

        } catch (Exception e) {
            log.error("========================================");
            log.error("执行失败！");
            log.error("========================================");
            log.error("错误信息:", e);
            System.exit(1);

        } finally {
            if (context != null) {
                context.close();
                log.info("Spring 容器已关闭");
            }
        }
    }

    /**
     * 查找匹配的方法
     */
    private static Method findMethod(Class<?> clazz, String methodName, Object[] args) {
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }

            Class<?>[] paramTypes = method.getParameterTypes();

            // 无参方法
            if (paramTypes.length == 0 && (args == null || args.length == 0)) {
                return method;
            }

            // 参数数量匹配
            if (args != null && paramTypes.length == args.length) {
                return method;
            }
        }

        return null;
    }
}
