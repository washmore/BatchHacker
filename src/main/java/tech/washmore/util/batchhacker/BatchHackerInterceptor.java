package tech.washmore.util.batchhacker;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.washmore.util.batchhacker.annotation.BatchHacker;
import tech.washmore.util.batchhacker.annotation.BatchHackerParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * BatchHacker切点拦截器
 *
 * @author Washmore
 * @version V1.0
 * @since 2017/04/06
 */
@Aspect
public class BatchHackerInterceptor {
    private static final int BATCH_SIZE = 2000;
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchHackerInterceptor.class);

    @Pointcut("@annotation(tech.washmore.util.batchhacker.annotation.BatchHacker)")
    public void pointCut() {
    }

    @Around("pointCut()")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        //获取被拦截的方法
        Method method = signature.getMethod();
        Class<?> result = method.getReturnType();
        //获取被拦截的方法参数
        Object[] args = pjp.getArgs();

        if (!(Integer.class.getName().equals(result.getName()) || "int".equalsIgnoreCase(result.getName()))) {
            LOGGER.error("出參不為int!调用原生的方法{}", this.getFullMethodName(method));
            return pjp.proceed();
        }

        if (args == null || args.length == 0) {
            LOGGER.error("入参长度为0或没有入参!调用原生的方法{}", this.getFullMethodName(method));
            return pjp.proceed();
        }
        if (args.length == 1 && args[0] instanceof List) {
            return handleOneParamMethod(pjp);
        }

        int listCount = 0;
        int index = 0;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof List) {
                listCount++;
                index = i;
            }
        }

        if (listCount == 0) {
            LOGGER.error("多个参数中没有找到List存在!调用原生的方法{}", this.getFullMethodName(method));
            return pjp.proceed();
        }
        if (listCount == 1) {
            return handleMuitParamMethod(pjp, index);
        }

        int annCount = 0;

        Annotation[][] paramsAnoAry = method.getParameterAnnotations();

        if (paramsAnoAry != null && paramsAnoAry.length > 0) {
            for (int i = 0; i < paramsAnoAry.length; i++) {
                Annotation[] annotationAry = paramsAnoAry[i];
                if (annotationAry != null && annotationAry.length > 0) {
                    for (Annotation annotation : annotationAry) {
                        if (annotation instanceof BatchHackerParam && args[i] instanceof List) {
                            annCount++;
                            index = i;
                        }
                    }
                }
            }
        }

        if (annCount == 0) {
            LOGGER.error("多个List参数中没有找到BatchParam注解存在!调用原生的方法{}", this.getFullMethodName(method));
            return pjp.proceed();
        }
        if (annCount == 1) {
            return handleMuitParamMethod(pjp, index);
        }

        LOGGER.error("多个List参数中存在多个BatchParam注解!调用原生的方法{}", this.getFullMethodName(method));
        return pjp.proceed();
    }

    private Object handleOneParamMethod(ProceedingJoinPoint pjp) throws Throwable {
        return handleMuitParamMethod(pjp, 0);
    }

    private Object handleMuitParamMethod(ProceedingJoinPoint pjp, int i) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        //获取被拦截的方法
        Method method = signature.getMethod();
        //获取被拦截的方法参数
        Object[] args = pjp.getArgs();

        List coll = (List) args[i];
        if (coll == null || coll.size() == 0 || coll.size() < BATCH_SIZE) {
            LOGGER.error("List参数长度不符:{}!调用原生的方法{}", coll == null ? 0 : coll.size(), this.getFullMethodName(method));
            return pjp.proceed();
        }

        BatchHacker ann = method.getAnnotation(BatchHacker.class);
        int batchSize = ann.batchSize();

        if (batchSize <= 0) {
            LOGGER.error("BatchHacker注解的batchSize值不在有效范围,应该为({},{}]的整数!调用原生的方法{}", 0, BATCH_SIZE, this.getFullMethodName(method));
            return pjp.proceed();
        }
        if (batchSize > BATCH_SIZE) {
            batchSize = BATCH_SIZE;
        }

        int index = 0;
        int count = 0;

        do {
            int start = index;
            int end = index + batchSize < coll.size() ? index + batchSize : coll.size();

            List subColl = coll.subList(start, end);

            args[i] = subColl;

            count += (Integer) pjp.proceed(args);

            index = index + batchSize;
        } while (index < coll.size());

        return count;
    }

    private String getFullMethodName(Method method) {
        if (method == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        Class cls = method.getDeclaringClass();
        if (cls != null) {
            sb.append(cls.getName());
            sb.append(":");
        }
        sb.append(method.getName());
        return sb.toString();
    }
}
