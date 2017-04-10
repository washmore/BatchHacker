## BatchHacker.jar功能介绍
mybatis框架在执行批量插入或者更新的时候限制入参List长度为2100以内，随着业务数据的增长，List长度总有超过这个数的时候，如果你嫌修改以前的业务代码麻烦的话，此时就有这个jar包的用武之地了，在不侵入代码的前提下，突破mybatis批处理数据量2100个的限制，另一个应用场景是，即使没有用到mybatis，也可以作为自动分批提交的工具，防止一次性提交数据过大导致数据库自动提高锁级别

### Maven配置
1.在Maven配置文件中profiles节点新增
```
        <profile>
            <id>washmore</id>
            <repositories>
                <repository>
                    <id>public</id>
                    <url>http://maven.washmore.tech/nexus/content/repositories/public</url>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <id>central</id>
                    <url>http://maven.washmore.tech/nexus/content/repositories/public</url>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </pluginRepository>
            </pluginRepositories>
        </profile>
```
2.在项目中引入最新版本的Maven依赖
```
    <dependency>
      <groupId>tech.washmore</groupId>
      <artifactId>util.batchhacker</artifactId>
      <version>1.3-SNAPSHOT</version>
    </dependency>
```

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

2.然后在批量操作的方法上增加@BatchHacker注解即可，如果入参中有多个List的话，则需要在批量数据List参数上额外增加@BatchHackerParam注解;  
入参形式支持:  
a.单个List类型参数;  
b.多个参数，其中一个为List类型;  
c.多个参数，含多个List类型的参数，其中一个List参数带有@BatchHackerParam注解;  
d.单个Map类型参数，其中有且仅有一个key对应的value为List类型;  
出参形式只支持Integer(int)  


#### tips:
这个玩意还有个缺陷，this关键字调用的方法无法增强，因为没有被AOP切到，Spring AOP官方给出的建议也是在应用AOP的时候尽量避免内部方法调用的出现，硬是要破解也是可以的，将this替换为从Spring环境中拿出来的bean再进行方法调用，但是这样一来需要修改原逻辑代码，就违背了使用这个小插件的初衷了!当然，你也可以将这一个过程使用注解+AOP实现，不过，那又是另一个小插件的故事了(根据注解自动代理/增强内部方法调用)...有兴趣的可以自行尝试(＾Ｕ＾)ノ~ＹＯ


