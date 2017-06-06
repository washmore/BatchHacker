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

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchHackerInterceptor.class);

    /**
     * 切面配置
     */
    @Pointcut("@annotation(tech.washmore.util.batchhacker.annotation.BatchHacker)")
    public void pointCut() {
    }

    /**
     * 切面环绕增强
     *
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("pointCut()")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        //获取被拦截的方法
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        //获取被拦截的方法返回值类型
        Class<?> resultType = method.getReturnType();
        //获取被拦截的方法参数
        Object[] args = pjp.getArgs();
        //检查入参和出参
        if (!this.preCheckArgsAndReturnTypes(method, resultType, args)) {
            return pjp.proceed();
        }
        //单入参处理
        if (args.length == 1) {
            return this.handleTheOnlyArg(pjp, method, args[0]);
        }
        //多入参处理
        return this.handleMultiArgs(pjp, method, args);
    }

    /**
     * 检查入参和出参是否符合要求
     *
     * @param method
     * @param resultType
     * @param args
     * @return
     * @throws Throwable
     */
    private boolean preCheckArgsAndReturnTypes(Method method, Class<?> resultType, Object[] args) throws Throwable {
        //出参必须为int或者对应的包装类型Integer
        if (!(Integer.class.getName().equals(resultType.getName()) || "int".equalsIgnoreCase(resultType.getName()))) {
            LOGGER.error("出参不是int!调用原生的方法{}", this.getFullMethodName(method));
            return false;
        }

        //入参列表长度必须大于0(必须要有参数)
        if (args == null || args.length == 0) {
            LOGGER.error("入参长度为0或没有入参!调用原生的方法{}", this.getFullMethodName(method));
            return false;
        }

        return true;
    }

    /**
     * 处理单入参的情形
     *
     * @param pjp
     * @param method
     * @param theOnlyArg
     * @return
     * @throws Throwable
     */
    private Object handleTheOnlyArg(ProceedingJoinPoint pjp, Method method, Object theOnlyArg) throws Throwable {

        if (!preCheckTheOnlyArg(method, theOnlyArg)) {
            return pjp.proceed();
        }

        if (theOnlyArg instanceof List) {
            return this.handleTheOnlyList(pjp, method, theOnlyArg);
        } else {
            //theOnlyArg instanceof Map
            return this.handleTheOnlyMap(pjp, method, theOnlyArg);
        }
    }

    /**
     * 检查单入参是否符合要求
     *
     * @param method
     * @param theOnlyArg
     * @return
     */
    private boolean preCheckTheOnlyArg(Method method, Object theOnlyArg) {
        //唯一参数不能为空
        if (theOnlyArg == null) {
            LOGGER.error("唯一的参数为null!调用原生的方法{}", this.getFullMethodName(method));
            return false;
        }
        //唯一参数类型必须为List或者Map
        if (!(theOnlyArg instanceof List || theOnlyArg instanceof Map)) {
            LOGGER.error("唯一的参数不为List或Map!调用原生的方法{}", this.getFullMethodName(method));
            return false;
        }

        return true;
    }

    /**
     * 处理单List类型入参的情形
     *
     * @param pjp
     * @param method
     * @param theOnlyList
     * @return
     * @throws Throwable
     */
    private Object handleTheOnlyList(ProceedingJoinPoint pjp, Method method, Object theOnlyList) throws Throwable {
        //单List类型入参 为 多入参且包含唯一有效List类型入参的一种特殊情形
        return this.handleTheMultiArgsForTheOnlyValidList(pjp, method, new Object[]{theOnlyList}, 0);
    }

    /**
     * 处理单Map类型入参的情形
     *
     * @param pjp
     * @param method
     * @param theOnlyMap
     * @return
     * @throws Throwable
     */
    private Object handleTheOnlyMap(ProceedingJoinPoint pjp, Method method, Object theOnlyMap) throws Throwable {
        Object key = this.getKeyOfListFromTheOnlyMap(pjp, method, theOnlyMap);
        if (key == null) {
            return pjp.proceed();
        }
        return handleTheOnlyMapForTheOnlyValidList(pjp, method, new Object[]{theOnlyMap}, key);
    }

    /**
     * 检查单Map类型入参的情况下是否是仅包含一个List类型的Value;
     *
     * @param pjp
     * @param method
     * @param theOnlyMap
     * @return
     * @throws Throwable
     */
    private Object getKeyOfListFromTheOnlyMap(ProceedingJoinPoint pjp, Method method, Object theOnlyMap) throws Throwable {
        Map map = (Map) theOnlyMap;
        if (map.size() == 0) {
            LOGGER.error("Map参数长度为0!调用原生的方法{}", this.getFullMethodName(method));
            return null;
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
            return null;
        }

        if (listCount > 1) {
            LOGGER.error("Map参数中含有大于1个的List类型的value!调用原生的方法{}", this.getFullMethodName(method));
            return null;
        }

        return key;
    }

    /**
     * 处理单Map入参且仅包含一个有效List类型value的情形
     *
     * @param pjp
     * @param method
     * @param args
     * @param key
     * @return
     * @throws Throwable
     */
    private Object handleTheOnlyMapForTheOnlyValidList(ProceedingJoinPoint pjp, Method method, Object[] args, Object key) throws Throwable {
        List data = (List) ((Map) args[0]).get(key);

        BatchHacker ann = method.getAnnotation(BatchHacker.class);
        if (!checkBatchHackerAnnFields(method, data, ann)) {
            return pjp.proceed();
        }

        return foreachProceedOfTheOnlyMap(pjp, args, key, data, ann.batchSize());
    }

    /**
     * 针对单Map入参的关键性批量处理
     *
     * @param pjp
     * @param args
     * @param key
     * @param data
     * @param batchSize
     * @return
     * @throws Throwable
     */
    private Object foreachProceedOfTheOnlyMap(ProceedingJoinPoint pjp, Object[] args, Object key, List data, int batchSize) throws Throwable {
        int i = 0;
        int count = 0;

        do {
            int start = i;
            int end = i + batchSize < data.size() ? i + batchSize : data.size();
            List subColl = data.subList(start, end);

            ((Map) args[0]).put(key, subColl);

            count += (Integer) pjp.proceed(args);
            i = i + batchSize;
        } while (i < data.size());

        return count;
    }

    /**
     * 处理多入参的情形
     *
     * @param pjp
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    private Object handleMultiArgs(ProceedingJoinPoint pjp, Method method, Object[] args) throws Throwable {
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
            //多入参中仅有一个List类型参数
            return this.handleTheMultiArgsForTheOnlyValidList(pjp, method, args, index);
        }

        return this.handleMultiListArgs(pjp, method, args);
    }

    /**
     * 处理多入参且包含多个List类型入参的情形
     *
     * @param pjp
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    private Object handleMultiListArgs(ProceedingJoinPoint pjp, Method method, Object[] args) throws Throwable {

        int index = this.getIndexOfTheAnnoListFromMultiListArgs(pjp, method, args);
        if (index < 0) {
            return pjp.proceed();
        }
        //含有多个List类型入参但是其中仅有一个标记了@BatchHacker注解
        return this.handleTheMultiArgsForTheOnlyValidList(pjp, method, args, index);
    }

    /**
     * 检查多List类型入参的情况下是否是用了@BatchHackerParam进行唯一有效性List标注
     *
     * @param pjp
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    private int getIndexOfTheAnnoListFromMultiListArgs(ProceedingJoinPoint pjp, Method method, Object[] args) throws Throwable {
        int annCount = 0;
        int index = 0;

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
            return -1;
        }

        if (annCount > 1) {
            LOGGER.error("多个List参数中存在大于1个BatchHackerParam注解!调用原生的方法{}", this.getFullMethodName(method));
            return -1;
        }

        return index;
    }

    /**
     * 处理多入参且包含唯一有效List类型入参的情形(只含有一个List类型入参或者含有多个List类型入参但是其中仅有一个标记了@BatchHacker注解)
     *
     * @param pjp
     * @param method
     * @param args
     * @param index
     * @return
     * @throws Throwable
     */
    private Object handleTheMultiArgsForTheOnlyValidList(ProceedingJoinPoint pjp, Method method, Object[] args, int index) throws Throwable {

        List data = (List) args[index];

        BatchHacker ann = method.getAnnotation(BatchHacker.class);
        if (!checkBatchHackerAnnFields(method, data, ann)) {
            return pjp.proceed();
        }

        return foreachProceedOfMultiArgs(pjp, args, index, data, ann.batchSize());
    }

    /**
     * 针对单有效List类型入参的关键性批量处理
     *
     * @param pjp
     * @param args
     * @param index
     * @param data
     * @param batchSize
     * @return
     * @throws Throwable
     */
    private Object foreachProceedOfMultiArgs(ProceedingJoinPoint pjp, Object[] args, int index, List data, int batchSize) throws Throwable {
        int i = 0;
        int count = 0;

        do {
            int start = i;
            int end = i + batchSize < data.size() ? i + batchSize : data.size();
            List subColl = data.subList(start, end);

            args[index] = subColl;

            count += (Integer) pjp.proceed(args);
            i = i + batchSize;
        } while (i < data.size());

        return count;
    }


    /**
     * 检查@BatchHacker注解使用是否符合要求
     *
     * @param method
     * @param data
     * @param ann
     * @return
     * @throws Throwable
     */
    private boolean checkBatchHackerAnnFields(Method method, List data, BatchHacker ann) throws Throwable {

        int batchSize = ann.batchSize();
        int batchLimit = ann.batchLimit();
        if (batchSize > batchLimit) {
            batchSize = batchLimit;
        }

        if (batchLimit <= 0) {
            LOGGER.error("BatchHacker注解的batchLimit值{}不在有效范围,应该为大于0的整数!调用原生的方法{}", batchLimit, this.getFullMethodName(method));
            return false;
        }

        if (data == null || data.size() == 0 || data.size() < batchLimit) {
            LOGGER.error("批处理数据List长度不符:{}!调用原生的方法{}", data == null ? 0 : data.size(), this.getFullMethodName(method));
            return false;
        }

        if (batchSize <= 0) {
            LOGGER.error("BatchHacker注解的batchSize值{}不在有效范围,应该为({},{}]的整数!调用原生的方法{}", batchSize, 0, batchLimit, this.getFullMethodName(method));
            return false;
        }

        return true;
    }


    /**
     * 获取完全限定方法名
     *
     * @param method
     * @return
     */
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
