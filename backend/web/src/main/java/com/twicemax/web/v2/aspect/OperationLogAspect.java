package com.twicemax.web.v2.aspect;

import com.twicemax.application.dto.response.OperationLogDTO;
import com.twicemax.application.service.OperationLogService;
import com.twicemax.infrastructure.context.RequestContext;
import com.twicemax.shared.domain.Enums.UserRole;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.util.IpUtils;
import com.twicemax.web.v2.annotation.OperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 操作日志切面
 * 自动记录标注了 @OperationLog 的方法调用
 *
 * 注意：日志记录失败会抛出异常，导致业务事务回滚（强一致性）
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(operationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        // 1. 获取当前用户信息（从 RequestContext 获取）
        UserDO currentUser = RequestContext.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("无法获取当前用户，操作日志记录失败");
        }

        // 2. 解析SpEL表达式
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = createEvaluationContext(method, args);

        // 解析支持SpEL表达式的字段
        String module = parseExpression(operationLog.module(), context, String.class);
        String operationType = parseExpression(operationLog.type(), context, String.class);
        String targetType = parseExpression(operationLog.targetType(), context, String.class);
        Long targetId = parseExpression(operationLog.targetId(), context, Long.class);
        String targetName = parseExpression(operationLog.targetName(), context, String.class);
        String reason = parseExpression(operationLog.reason(), context, String.class);

        // 3. 获取IP地址
        String ipAddress = IpUtils.getIpAddress();

        // 4. 构建日志DTO
        OperationLogDTO logDTO = OperationLogDTO.builder()
                .operatorId(currentUser.getId())
                .operatorName(currentUser.getName())
                .operatorRole(UserRole.fromName(currentUser.getRole()).getLevel())
                .module(module)
                .operationType(operationType)
                .operationLevel(operationLog.level().getCode())
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .reason(reason)
                .ipAddress(ipAddress)
                .build();

        // 5. 记录日志（失败会抛出异常）
        operationLogService.recordLog(logDTO);

        // 6. 执行原方法
        return joinPoint.proceed();
    }

    /**
     * 创建SpEL评估上下文
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        Parameter[] parameters = method.getParameters();

        // 将方法参数添加到上下文
        for (int i = 0; i < parameters.length; i++) {
            context.setVariable(parameters[i].getName(), args[i]);
        }

        return context;
    }

    /**
     * 解析SpEL表达式
     * 只有以 # 开头的字符串才作为 SpEL 表达式解析，否则直接返回原字符串
     */
    @SuppressWarnings("unchecked")
    private <T> T parseExpression(String expressionString, EvaluationContext context, Class<T> desiredResultType) {
        if (expressionString == null || expressionString.trim().isEmpty()) {
            return null;
        }

        // 只有包含 # 的字符串才作为 SpEL 表达式解析
        if (!expressionString.contains("#")) {
            // 纯字符串，直接返回
            if (desiredResultType == String.class) {
                return (T) expressionString;
            }
            // 尝试转换为目标类型
            if (desiredResultType == Long.class) {
                try {
                    return (T) Long.valueOf(expressionString);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        try {
            Expression expression = parser.parseExpression(expressionString);
            return expression.getValue(context, desiredResultType);
        } catch (Exception e) {
            log.warn("解析SpEL表达式失败: {}", expressionString, e);
            return null;
        }
    }
}
