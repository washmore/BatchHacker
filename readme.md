## BatchHacker.jar功能介绍
mybatis框架在执行批量插入或者更新的时候限制入参List长度为2100以内,随着业务数据的增长,List长度总有超过这个数的时候,如果你嫌修改以前的业务代码麻烦的话,此时就有这个jar包的用武之地了

### 使用方法
1.首先将AOP增强拦截器托管给spring  
使用xml声明式:  
在spring配置文件中增加一行代码
```
    <import resource="classpath*:batchhacker/spring-aop.xml"/>
  ```
或者使用注解式:  
新建类BatchHackerConfig类(确保此类能被自动扫描到):  
```
@Configuration
@ImportResource("classpath*:batchhacker/spring-aop.xml")
public class BatchHackerConfig {
}
```

2.然后在批量操作的方法上增加@BatchHacker注解即可,如果入参中有多个List的话,则需要在批量数据List参数上额外增加@BatchHackerParam注解;  
入参形式支持:  
a.单个List类型参数;  
b.多个参数，其中一个为List类型;  
c.多个参数，含多个List类型的参数，其中一个List参数带有@BatchHackerParam注解;  
d.单个Map类型参数，其中有且仅有一个key对应的value为List类型;  
出参形式只支持Integer(int)  


