## BatchHacker.jar功能介绍
mybatis框架在执行批量插入或者更新的时候限制入参List长度为2100以内,随着业务数据的增长,List长度总有超过这个数的时候,如果你嫌修改以前的业务代码麻烦的话,此时就有这个jar包的用武之地了

## 使用方法
1.首先将AOP增强拦截器托管给spring

xml声明式:
在spring配置文件中增加一行
```
    <import resource="classpath*:batchhacker/spring-aop.xml"/>
  ```

注解式:
新建类BatchHackerConfig类:
```
@Configuration
@ImportResource("classpath*:batchhacker/spring-aop.xml")
public class BatchHackerConfig {
}
```

2.然后在批量操作的方法上增加@BatchHacker注解即可,如果入参中有多个List的话,则需要在批量数据List参数上额外增加@BatchHackerParam注解;


