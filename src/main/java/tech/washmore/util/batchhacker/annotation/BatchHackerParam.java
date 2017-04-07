package tech.washmore.util.batchhacker.annotation;

import java.lang.annotation.*;

/**
 * 当被拦截方法有多个list类型的参数时,需要使用此注解指明其中需要分割批处理的参数
 *
 * @author Washmore
 * @version V1.0
 * @since 2017/04/06
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface BatchHackerParam {
}
