package com.guava.retryer.guavaretryer;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;

import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.Date;
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
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
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






        Retryer<Boolean> retryerBuildTask = RetryerBuilder.<Boolean>newBuilder()
                //.retryIfExceptionOfType(IOException.class)
                .retryIfException()
                .retryIfResult(Predicates.equalTo(false))
                // 返回false重试
                .retryIfResult(Predicates.equalTo(false))
                .withWaitStrategy(WaitStrategies.fixedWait(1,TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(5))
                .build();

        System.out.println("begin..." + df.format(new Date()));

        try {
            retryerBuildTask.call(buildTask());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("end..." + df.format(new Date()));



    }

    private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss,SSS");

    private static Callable<Boolean> buildTask() {
        return new Callable<Boolean>() {
            private int i = 0;

            @Override
            public Boolean call() throws Exception {
                System.out.println("called");
                i++;
                if (i == 3) {
                    return true;
                    //throw new IOException();
                } else {
                    //throw new NullPointerException();
                    return false;

                }
            }
        };
    }

}
