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
    /**
     * 触发自动分批操作的阀值;默认取2000(稍小于mybatis的限制)
     *
     * @return
     */
    int batchLimit() default 2000;

    /**
     * 自动分批操作每批次的数量;默认取500
     *
     * @return
     */
    int batchSize() default 500;
}
