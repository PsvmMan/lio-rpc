package com.gt.lio.cluster.invoker.parallel;

import com.gt.lio.cluster.client.ClientInvoker;
import com.gt.lio.cluster.directory.ServiceDirectory;
import com.gt.lio.cluster.invoker.AbstractClusterInvoker;
import com.gt.lio.cluster.loadbalance.LoadBalance;
import com.gt.lio.common.utils.RequestIdGenerator;
import com.gt.lio.config.model.LioReferenceMethodMetadata;
import com.gt.lio.protocol.body.RequestMessage;
import com.gt.lio.protocol.body.ResponseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.gt.lio.common.utils.RequestIdGenerator.DEFAULT_KEY;

/**
 * 并发调用，返回第一个执行成功的结果，如果都失败，返回最后一个错误信息
 */
public class ParallelClusterInvoker extends AbstractClusterInvoker {

    // 自定义线程池
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    static {
        //注册钩子销毁线程池
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
        }));
    }

    public ParallelClusterInvoker(ServiceDirectory serviceDirectory, Map<String, LioReferenceMethodMetadata> methods) {
        super(serviceDirectory, methods);
    }

    @Override
    public ResponseMessage invoke(RequestMessage req, List<ClientInvoker> clientInvokers, LoadBalance loadBalance, LioReferenceMethodMetadata methodMetadata) {

        if (clientInvokers == null || clientInvokers.isEmpty()) {
            return new ResponseMessage(new RuntimeException("没有可用服务节点"));
        }

        // 超时时间
        long timeout = methodMetadata.getTimeout();

        // 并发数
        int parallelNumber = methodMetadata.getParallelNumber();
        List<ClientInvoker> selected = new ArrayList<>();
        if(parallelNumber >= clientInvokers.size()){
            for (ClientInvoker invoker : clientInvokers) {
                if(invoker.isAvailable()){
                    selected.add(invoker);
                }
            }
        }else {
            ClientInvoker clientInvoker = select(clientInvokers, req, loadBalance);
            selected.add(clientInvoker);
            int index = clientInvokers.indexOf(clientInvoker);
            for (int i = 1; i < clientInvokers.size(); i++) {
                ClientInvoker invoker = clientInvokers.get((++index) % clientInvokers.size());
                if(invoker.isAvailable()){
                    selected.add(invoker);
                }
                if(selected.size() >= parallelNumber){
                    break;
                }
            }
        }

        if(selected.isEmpty()){
            return new ResponseMessage(new RuntimeException("没有可用服务节点"));
        }

        int total = selected.size();
        final AtomicInteger failCount = new AtomicInteger(0);
        CompletableFuture<CompletableFuture> future = new CompletableFuture<>();

        // 并发调用, 设置统一的请求序号
        long requestId = RequestIdGenerator.nextId(DEFAULT_KEY);

        // 并发发起调用
        for (ClientInvoker invoker : selected) {
            executor.execute(() -> {
                try {
                    // 这里执行特别快，会尽快释放线程资源
                    CompletableFuture<Object> resultFuture = invoker.invoke(req, requestId, total);

                    // 设置结果，如果请求是不需要响应的，resultFuture会为null
                    // 只要没有报错，只要有一个请求成功，就直接返回成功
                    if(!future.isDone()){
                        future.complete(resultFuture);
                    }
                } catch (Throwable t) {

                    // 统计失败次数
                    int failed = failCount.incrementAndGet();

                    // 所有都失败后才放入错误响应
                    if (failed >= total && !future.isDone()) {
                        future.completeExceptionally(t);
                    }
                }
            });
        }

        try {
            // 获取CompletableFuture结果
            CompletableFuture completableFuture = future.get();

            // 如果需要响应的，获取结果
            if(completableFuture != null){
                if(timeout > 0){
                    return (ResponseMessage)completableFuture.get(timeout, TimeUnit.MILLISECONDS);
                }
                return (ResponseMessage)completableFuture.get();
            }else { // 不需要响应的，直接返回空响应
                return new ResponseMessage();
            }
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ResponseMessage(new RuntimeException("调用过程中被中断", e));
        }catch (TimeoutException e) {
            return new ResponseMessage(new RuntimeException("调用超时", e));
        }catch (Throwable e) {
            return new ResponseMessage(e.getCause());
        }
    }
}
