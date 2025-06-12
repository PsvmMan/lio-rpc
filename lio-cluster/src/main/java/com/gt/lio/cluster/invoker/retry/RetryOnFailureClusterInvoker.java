package com.gt.lio.cluster.invoker.retry;

import com.gt.lio.cluster.client.ClientInvoker;
import com.gt.lio.cluster.directory.ServiceDirectory;
import com.gt.lio.cluster.invoker.AbstractClusterInvoker;
import com.gt.lio.cluster.loadbalance.LoadBalance;
import com.gt.lio.config.model.LioReferenceMethodMetadata;
import com.gt.lio.protocol.body.RequestMessage;
import com.gt.lio.protocol.body.ResponseMessage;
import java.util.List;
import java.util.Map;

/**
 * 失败重试调用，直至最后一次失败，返回最后一次失败的错误信息
 */
public class RetryOnFailureClusterInvoker extends AbstractClusterInvoker{

    public RetryOnFailureClusterInvoker(ServiceDirectory serviceDirectory, Map<String, LioReferenceMethodMetadata> methods) {
        super(serviceDirectory, methods);
    }

    @Override
    public ResponseMessage invoke(RequestMessage req, List<ClientInvoker> clientInvokers, LoadBalance loadBalance, LioReferenceMethodMetadata methodMetadata) {

        // 重试次数
        int retries = methodMetadata.getRetries();

        ClientInvoker currentInvoker = null;

        int index = 0;

        for (int i = 0; i <= retries; i++) {
            try {

                if (i == 0) {
                    currentInvoker = loadBalance.select(clientInvokers, req);
                    index = clientInvokers.indexOf(currentInvoker);
                }else {
                    currentInvoker = clientInvokers.get((index + 1) % clientInvokers.size());
                }

                if (currentInvoker == null) {
                    return new ResponseMessage(new RuntimeException("没有可用服务节点"));
                }

                // 返回结果
                ResponseMessage responseMessage = currentInvoker.invoke(req);
                if (responseMessage.getException() != null) {
                    // 如果不是最后一个重试次数，记录失败节点并重试
                    if (i < retries) {
                        continue;
                    }
                    // 最后一次失败，返回错误信息
                    return responseMessage;
                }
                return responseMessage;

            } catch (Throwable e) {

                // 如果不是最后一个重试次数，记录失败节点并重试
                if (i < retries) {
                    continue;
                }

                // 最后一次失败，返回错误信息
                return new ResponseMessage(e);
            }
        }

        return new ResponseMessage(new RuntimeException("调用失败，未知错误"));

    }
}
