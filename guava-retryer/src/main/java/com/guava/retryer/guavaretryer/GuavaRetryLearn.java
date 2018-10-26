package com.guava.retryer.guavaretryer;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;
import com.sun.javaws.CacheUtil;
import org.springframework.util.StringUtils;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author yzjiang
 * @description
 * @date 2018/10/26 0026 15:39
 */
public class GuavaRetryLearn {

    public static void main(String [] args){

        Boolean openRetry = false;
        int timeout = 0;
        Callable<Boolean> updateReimAgentsCall = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (timeout < 1) {
                    return false;
                } else {
                    return true;
                }
            }
        };


        Retryer<Boolean> retryer = RetryerBuilder
                .<Boolean>newBuilder()
                //抛出runtime异常、checked异常时都会重试，但是抛出error不会重试。
                .retryIfException()
                //返回false也需要重试
                .retryIfResult(Predicates.equalTo(false))
                // 返回false重试
                .retryIfResult(Predicates.equalTo(false))
                //重调策略
                .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS))
                //尝试次数
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withRetryListener(new MyRetryListener<>())
                .build();

        try {
            retryer.call(updateReimAgentsCall);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (RetryException e) {
            System.out.println("操作错误，需要发送提醒邮件");
        }
    }
}
