package tech.washmore.util.batchhacker.annotation;

import java.lang.annotation.*;


/**
 * BatchHacker切点,当批处理sql传入list参数元素过多时(mybatis中2100)使用此注解
 *
 * @author Washmore
 * @version V1.0
 * @since 2017/04/06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface BatchHacker {
    int batchSize() default 500;
}
