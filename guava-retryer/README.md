# [使用Guava retryer优雅的实现接口重调机制](https://www.cnblogs.com/jianzh5/p/6651799.html)

　　API 接口调用异常, 网络异常在我们日常开发中经常会遇到，这种情况下我们需要先重试几次调用才能将其标识为错误并在确认错误之后发送异常提醒。guava-retry可以灵活的实现这一功能。Guava retryer在支持重试次数和重试频度控制基础上，能够兼容支持多个异常或者自定义实体对象的重试源定义，让重试功能有更多的灵活性。Guava Retryer也是线程安全的，入口调用逻辑采用的是Java.util.concurrent.Callable的call方法。

　　使用Guava retryer 很简单，我们只要做以下几步：

　　**Step1、引入Guava-retry**　　

```
<guava-retry.version>2.0.0</guava-retry.version>
<dependency>
      <groupId>com.github.rholder</groupId>
      <artifactId>guava-retrying</artifactId>
      <version>${guava-retry.version}</version>
</dependency>
```

　　**Step2、定义实现Callable接口的方法，以便Guava retryer能够调用**

```
/**
    * @desc 更新可代理报销人接口
    * @author jianzhang11
    * @date 2017/3/31 15:17
    */
   private static Callable<Boolean> updateReimAgentsCall = new Callable<Boolean>() {
       @Override
       public Boolean call() throws Exception {
           String url = ConfigureUtil.get(OaConstants.OA_REIM_AGENT);
           String result = HttpMethod.post(url, new ArrayList<BasicNameValuePair>());
           if(StringUtils.isEmpty(result)){
              throw new RemoteException("获取OA可报销代理人接口异常");
           }
           List<OAReimAgents> oaReimAgents = JSON.parseArray(result, OAReimAgents.class);
           if(CollectionUtils.isNotEmpty(oaReimAgents)){
               CacheUtil.put(Constants.REIM_AGENT_KEY,oaReimAgents);
               return true;
           }
           return false;
       }
   };
```

 　　**Step3、定义Retry对象并设置相关策略**

```
Retryer<Boolean> retryer = RetryerBuilder
                .<Boolean>newBuilder()
                //抛出runtime异常、checked异常时都会重试，但是抛出error不会重试。
                .retryIfException()
                //返回false也需要重试
                .retryIfResult(Predicates.equalTo(false))
                //重调策略
                .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS))
                //尝试次数
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
 
        try {
            retryer.call(updateReimAgentsCall);
        } catch (ExecutionException e) {
//            e.printStackTrace();
        } catch (RetryException e) {
            logger.error("更新可代理报销人异常,需要发送提醒邮件");
        }
```

 　　简单三步就能使用Guava Retryer优雅的实现重调方法。



 　　接下来对其进行详细说明：　　

　　**RetryerBuilder**是一个factory创建者，可以定制设置重试源且可以支持多个重试源，可以配置重试次数或重试超时时间，以及可以配置等待时间间隔，创建重试者Retryer实例。

　　RetryerBuilder的重试源支持Exception异常对象 和自定义断言对象，通过retryIfException 和retryIfResult设置，同时支持多个且能兼容。

　　**retryIfException**，抛出runtime异常、checked异常时都会重试，但是抛出error不会重试。

　　**retryIfRuntimeException**只会在抛runtime异常的时候才重试，checked异常和error都不重试。

　　**retryIfExceptionOfType**允许我们只在发生特定异常的时候才重试，比如NullPointerException和IllegalStateException都属于runtime异常，也包括自定义的error

　　如：　　

```
 .retryIfExceptionOfType(Error.class)// 只在抛出error重试
```

　　当然我们还可以在只有出现指定的异常的时候才重试，如：　　

```
.retryIfExceptionOfType(IllegalStateException.class)   

.retryIfExceptionOfType(NullPointerException.class) 
```

　　或者通过Predicate实现

```
.retryIfException(Predicates.or(Predicates.instanceOf(NullPointerException.class),
		Predicates.instanceOf(IllegalStateException.class))) 
```

　　retryIfResult可以指定你的Callable方法在返回值的时候进行重试，如　　

```
// 返回false重试  
.retryIfResult(Predicates.equalTo(false))   
//以_error结尾才重试  
.retryIfResult(Predicates.containsPattern("_error$"))  
```

　　当发生重试之后，假如我们需要做一些额外的处理动作，比如发个告警邮件啥的，那么可以使用RetryListener。每次重试之后，guava-retrying会自动回调我们注册的监听。可以注册多个RetryListener，会按照注册顺序依次调用。

```
import com.github.rholder.retry.Attempt;  
import com.github.rholder.retry.RetryListener;  
  
import java.util.concurrent.ExecutionException;  
  
public class MyRetryListener<Boolean> implements RetryListener {  
  
    @Override  
    public <Boolean> void onRetry(Attempt<Boolean> attempt) {  
  
        // 第几次重试,(注意:第一次重试其实是第一次调用)  
        System.out.print("[retry]time=" + attempt.getAttemptNumber());  
  
        // 距离第一次重试的延迟  
        System.out.print(",delay=" + attempt.getDelaySinceFirstAttempt());  
  
        // 重试结果: 是异常终止, 还是正常返回  
        System.out.print(",hasException=" + attempt.hasException());  
        System.out.print(",hasResult=" + attempt.hasResult());  
  
        // 是什么原因导致异常  
        if (attempt.hasException()) {  
            System.out.print(",causeBy=" + attempt.getExceptionCause().toString());  
        } else {  
            // 正常返回时的结果  
            System.out.print(",result=" + attempt.getResult());  
        }  
  
        // bad practice: 增加了额外的异常处理代码  
        try {  
            Boolean result = attempt.get();  
            System.out.print(",rude get=" + result);  
        } catch (ExecutionException e) {  
            System.err.println("this attempt produce exception." + e.getCause().toString());  
        }  
  
        System.out.println();  
    }  
}
```

　　接下来在Retry对象中指定监听：　　

```
.withRetryListener(new MyRetryListener<>())  
```

　　效果如下：

![img](https://raw.githubusercontent.com/yang-zhijiang/learngit/master/guava-retryer/README-img.png)

 

 