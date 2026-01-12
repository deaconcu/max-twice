package com.prosper.learn.web.v1.aspect;

import com.prosper.learn.application.dto.response.OperationLogDTO;
import com.prosper.learn.application.service.OperationLogService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.util.IpUtils;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.web.v1.annotation.OperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(operationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        // 1. 执行原方法
        Object result = joinPoint.proceed();

        try {
            // 2. 获取当前用户信息
            UserDO currentUser = getCurrentUser(joinPoint);
            if (currentUser == null) {
                log.warn("无法获取当前用户，跳过操作日志记录");
                return result;
            }

            // 3. 解析SpEL表达式
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();

            EvaluationContext context = createEvaluationContext(method, args);

            // 解析操作类型（支持SpEL表达式）
            String operationType = parseOperationType(operationLog.type(), context);
            Long targetId = parseExpression(operationLog.targetId(), context, Long.class);
            String targetName = parseExpression(operationLog.targetName(), context, String.class);
            String reason = parseExpression(operationLog.reason(), context, String.class);

            // 4. 获取IP地址
            String ipAddress = IpUtils.getIpAddress();

            // 5. 构建日志DTO
            OperationLogDTO logDTO = OperationLogDTO.builder()
                    .operatorId(currentUser.getId())
                    .operatorName(currentUser.getName())
                    .operatorRole(currentUser.getRole())
                    .module(operationLog.module())
                    .operationType(operationType)
                    .operationLevel(operationLog.level().getCode())
                    .targetType(operationLog.targetType())
                    .targetId(targetId)
                    .targetName(targetName)
                    .reason(reason)
                    .ipAddress(ipAddress)
                    .build();

            // 6. 异步记录日志
            operationLogService.recordLog(logDTO);

        } catch (Exception e) {
            log.error("操作日志记录失败，但不影响主业务", e);
            // 日志记录失败不应影响主业务
        }

        return result;
    }

    /**
     * 从方法参数中获取当前用户
     */
    private UserDO getCurrentUser(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();

        // 查找标注了 @CurrentUser 的参数
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(CurrentUser.class)) {
                Object arg = args[i];
                if (arg instanceof UserDO) {
                    return (UserDO) arg;
                }
            }
        }

        return null;
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
     * 解析操作类型（支持SpEL表达式）
     */
    private String parseOperationType(String typeExpression, EvaluationContext context) {
        // 如果包含 # 符号，说明是SpEL表达式
        if (typeExpression != null && typeExpression.contains("#")) {
            return parseExpression(typeExpression, context, String.class);
        }
        // 否则直接返回字符串
        return typeExpression;
    }

    /**
     * 解析SpEL表达式
     */
    private <T> T parseExpression(String expressionString, EvaluationContext context, Class<T> desiredResultType) {
        if (expressionString == null || expressionString.trim().isEmpty()) {
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
