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
import java.util.Map;

/**
 * BatchHacker切点拦截器
 *
 * @author Washmore
 * @version V1.0
 * @since 2017/04/06
 */
@Aspect
public class BatchHackerInterceptor {
    private static final int BATCH_SIZE = 500;

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
            LOGGER.error("出参不是int!调用原生的方法{}", this.getFullMethodName(method));
            return pjp.proceed();
        }

        if (args == null || args.length == 0) {
            LOGGER.error("入参长度为0或没有入参!调用原生的方法{}", this.getFullMethodName(method));
            return pjp.proceed();
        }
        if (args.length == 1) {
            if (args[0] == null) {
                LOGGER.info("唯一的参数为null!调用原生的方法{}", this.getFullMethodName(method));
                return pjp.proceed();
            }
            if (args[0] instanceof List) {
                return handleOneParamMethod(pjp);
            }
            if (args[0] instanceof Map) {
                return handleMapParamMethod(pjp);
            }
            LOGGER.info("唯一的参数不为List或Map!调用原生的方法{}", this.getFullMethodName(method));
            return pjp.proceed();
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
            LOGGER.error("多个List参数中没有找到BatchHackerParam注解!调用原生的方法{}", this.getFullMethodName(method));
            return pjp.proceed();
        }

        if (annCount == 1) {
            return handleMuitParamMethod(pjp, index);
        }

        LOGGER.error("多个List参数中存在大于1个BatchHackerParam注解!调用原生的方法{}", this.getFullMethodName(method));
        return pjp.proceed();
    }

    private Object handleMapParamMethod(ProceedingJoinPoint pjp) throws Throwable {

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        //获取被拦截的方法
        Method method = signature.getMethod();
        //获取被拦截的方法参数
        Object[] args = pjp.getArgs();

        Map map = (Map) args[0];
        if (map.size() == 0) {
            LOGGER.error("Map参数长度为0!调用原生的方法{}", this.getFullMethodName(method));
            return pjp.proceed();
        }

        int listCount = 0;
        Object key = null;
        for (Object k : map.keySet()) {
            if (map.get(k) instanceof List) {
                listCount++;
                key = k;
            }
        }

        if (listCount == 0) {
            LOGGER.error("Map参数中不含有List类型的value!调用原生的方法{}", this.getFullMethodName(method));
            return pjp.proceed();
        }

        if (listCount == 1) {
            return foreach(pjp, key, null);
        }

        LOGGER.error("Map参数中含有大于1个的List类型的value!调用原生的方法{}", this.getFullMethodName(method));
        return pjp.proceed();
    }

    private Object foreach(ProceedingJoinPoint pjp, Object key, Integer index) throws Throwable {

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        //获取被拦截的方法
        Method method = signature.getMethod();
        if ((key == null && index == null) || (key != null && index != null)) {
            LOGGER.error("foreach参数错误key:{},index:{}!调用原生的方法{}", key, index, this.getFullMethodName(method));
            return pjp.proceed();
        }
        //获取被拦截的方法参数
        Object[] args = pjp.getArgs();

        List coll = null;
        Map map = null;

        if (key != null) {
            map = (Map) args[0];
            coll = (List) map.get(key);
        } else if (index != null) {
            coll = (List) args[index];
        }

        if (coll == null || coll.size() == 0 || coll.size() < BATCH_SIZE) {
            LOGGER.error("批处理数据List长度不符:{}!调用原生的方法{}", coll == null ? 0 : coll.size(), this.getFullMethodName(method));
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

        int i = 0;
        int count = 0;

        do {
            int start = i;
            int end = i + batchSize < coll.size() ? i + batchSize : coll.size();

            List subColl = coll.subList(start, end);

            if (key != null) {
                map.put(key, subColl);
                args[0] = map;
            } else if (index != null) {
                args[index] = subColl;
            }

            count += (Integer) pjp.proceed(args);

            i = i + batchSize;
        } while (i < coll.size());

        return count;
    }


    private Object handleOneParamMethod(ProceedingJoinPoint pjp) throws Throwable {
        return handleMuitParamMethod(pjp, 0);
    }

    private Object handleMuitParamMethod(ProceedingJoinPoint pjp, int i) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        return foreach(pjp, null, i);
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
