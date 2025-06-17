package com.gt.lio.cluster.client;

import com.gt.lio.common.callback.RpcCallback;
import com.gt.lio.common.spi.LioServiceLoader;
import com.gt.lio.common.utils.RequestIdGenerator;
import com.gt.lio.common.utils.ResponseHolder;
import com.gt.lio.compression.Compression;
import com.gt.lio.config.ProtocolConfig;
import com.gt.lio.config.model.LioReferenceMethodMetadata;
import com.gt.lio.protocol.ProtocolMessage;
import com.gt.lio.protocol.body.RequestMessage;
import com.gt.lio.protocol.body.ResponseMessage;
import com.gt.lio.protocol.header.DefaultProtocolHeader;
import com.gt.lio.register.ServiceInstance;
import com.gt.lio.remote.ClientFactory;
import com.gt.lio.remote.param.ClientStartParam;
import com.gt.lio.remote.TransportClient;
import com.gt.lio.serialization.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static com.gt.lio.config.model.LioReferenceMethodMetadata.DEFAULT;
import static com.gt.lio.protocol.ProtocolConstants.BUSINESS_MESSAGE;
import static com.gt.lio.protocol.ProtocolConstants.NO_COMPRESSED;
import static com.gt.lio.remote.RemoteConstants.DEFAULT_DELIMITER;

public class ClientInvoker {

    private static final Logger logger = LoggerFactory.getLogger(ClientInvoker.class);

    public static final Integer DEFAULT_CONNECTIONS = 20;

    // 权重
    private Integer weight;

    // 客户端列表
    private List<TransportClient>  clients;

    private volatile boolean isAvailable = true;

    // 线程池名称
    private Byte threadPoolName;

    // 序列化类型
    private String serializationType;

    // 序列化代码
    private Byte serializationCode;

    // 方法配置列表
    private Map<String, LioReferenceMethodMetadata> methods;

    private String protocol;

    private String host;

    private Integer port;

    private String address;

    public ClientInvoker(ServiceInstance instance, Integer connections,
                         Map<String, LioReferenceMethodMetadata> methods, Map<String, ProtocolConfig> protocols){

        this.methods = methods;

        // 初始化客户端
        initClients(instance, connections, protocols);

    }

    private void initClients(ServiceInstance instance, Integer connections, Map<String, ProtocolConfig> protocols){

        // 线程池的编码
        Integer threadPoolNameTemp = (Integer) instance.getMetadata().get("threadPoolName");
        this.threadPoolName = threadPoolNameTemp.byteValue();

        // 序列化名称
        this.serializationType = (String) instance.getMetadata().get("serialization");

        // 序列化编码
        Integer serializationCodeTemp = (Integer)instance.getMetadata().get("serializationCode");
        this.serializationCode = serializationCodeTemp.byteValue();

        // 获取权重
        this.weight = (Integer) instance.getMetadata().get("weight");

        // 获取协议
        this.protocol = instance.getProtocol();
        ProtocolConfig protocolConfig = protocols.get(this.protocol);

        // 获取IP
        this.host = instance.getHost();

        // 获取端口
        this.port = instance.getPort();

        this.address = instance.getHost() + ":" + instance.getPort();

        // 传输类型
        String remote = (String) instance.getMetadata().get("remote");

        // 构建客户端启动参数
        ClientStartParam startParam = new ClientStartParam();
        startParam.setProtocol(protocol);
        startParam.setHost(host);
        startParam.setPort(port);
        startParam.setHeartbeatWriteTimeout(protocolConfig == null ? null : protocolConfig.getHeartbeatWriteTimeout());

        // 获取客户端工厂
        ClientFactory clientFactory = LioServiceLoader.getServiceLoader(ClientFactory.class).getService(remote);
        clients = new CopyOnWriteArrayList<>();
        for (int i = 0; i < connections; i++) {
            TransportClient client = clientFactory.createClient(startParam);
            clients.add(client);
        }

    }

    public ResponseMessage invoke(RequestMessage req){

        // 获取当前请求方法配置信息
        LioReferenceMethodMetadata methodMetadata = methods.getOrDefault(req.getMethodKey(), methods.get(DEFAULT));

        try {
            // 选择传输客户端
            TransportClient transportClient = getTransportClient();

            // 如果需要等待响应
            if(methodMetadata.isRespond()){
                String localAddress = transportClient.getLocalAddress();
                String remoteAddress = transportClient.getRemoteAddress();
                String key = remoteAddress + DEFAULT_DELIMITER + localAddress;
                long requestId = RequestIdGenerator.nextId(key);

                // 构建协议消息
                ProtocolMessage protocolMessage = buildProtocolMessage(req, requestId, methodMetadata);

                // 注册响应等待器
                CompletableFuture<Object> future = ResponseHolder.register(key + DEFAULT_DELIMITER + requestId);
                transportClient.send(protocolMessage);

                // 如果是异步
                if(methodMetadata.isAsync()){
                    asyncDeal(future, methodMetadata.getCallback());
                    return new ResponseMessage();
                }

                // 超时控制
                Long timeoutTemp = methodMetadata.getTimeout();
                if(timeoutTemp > 0){
                    return (ResponseMessage) future.get(timeoutTemp, TimeUnit.MILLISECONDS);
                }
                return (ResponseMessage) future.get();

            }else {
                // 不需要响应，直接发送，这时候无论是同步还是异步都是直接返回
                ProtocolMessage protocolMessage = buildProtocolMessage(req, 0, methodMetadata);
                transportClient.send(protocolMessage);
                return new ResponseMessage();
            }

        }catch (TimeoutException e){
            return new ResponseMessage(new TimeoutException("调用超时"));
        }catch (Throwable e){
            return new ResponseMessage(e);
        }
    }

    public CompletableFuture<Object> invoke(RequestMessage req, Long requestId, int count){

        // 注册响应等待器
        CompletableFuture<Object> future = null;

        try {

            // 获取当前请求方法配置信息
            LioReferenceMethodMetadata methodMetadata = methods.getOrDefault(req.getMethodKey(), methods.get(DEFAULT));

            // 选择传输客户端
            TransportClient transportClient = getTransportClient();

            // 构建协议消息
            ProtocolMessage protocolMessage = buildProtocolMessage(req, requestId, methodMetadata);

            // 如果需要等待响应
            if(methodMetadata.isRespond()){

                future = ResponseHolder.register(requestId.toString(), count);

                // 如果是异步
                if(methodMetadata.isAsync()){
                    asyncDeal(future, methodMetadata.getCallback());
                    return null;
                }
            }

            // 发送消息
            transportClient.send(protocolMessage);

        }catch (Throwable e){
            throw new RuntimeException(e);
        }

        return future;
    }

    private void asyncDeal(CompletableFuture<Object> future, RpcCallback callback){
        future.whenComplete((response, throwable) -> {
            if(response != null && response instanceof ResponseMessage){
                ResponseMessage responseMessage = (ResponseMessage) response;
                if(responseMessage.getException() != null){
                    callback.onFailure(responseMessage.getException());
                }else {
                    callback.onSuccess(responseMessage.getResult());
                }
            }
        });
    }

    private TransportClient getTransportClient(){

        if (clients.isEmpty()) {
            this.isAvailable = false;
            throw new IllegalStateException("没有可用的 TransportClient 连接到远程服务[" + address + "]");
        }

        int maxTries = clients.size(); // 最多尝试次数
        TransportClient transportClient = null;
        int index = 0;

        for (int i = 0; i < maxTries; i++) {

            if(i == 0){
                index = ThreadLocalRandom.current().nextInt(clients.size());
            }
            transportClient = clients.get(index);

            if (transportClient.isAvailable()) {
                return transportClient;
            } else if(transportClient.isClosed()){
                // 移除
                clients.remove(transportClient);
                index = index % clients.size();
            }else {
                index = (index + 1) % clients.size();
            }
        }

        if (clients.isEmpty()) {
            this.isAvailable = false;
        }

        throw new IllegalStateException("没有可用的 TransportClient 连接到远程服务[" + address + "]");
    }

    private ProtocolMessage buildProtocolMessage(RequestMessage req, long requestId, LioReferenceMethodMetadata methodMetadata) throws IOException {

        Boolean isRespond = methodMetadata.isRespond();

        Boolean isCompressed = methodMetadata.isCompressed();

        String compressionName = methodMetadata.getCompressionType();

        Byte compressionType = NO_COMPRESSED;
        if(isCompressed){
            compressionType = LioServiceLoader.getServiceLoader(Compression.class).getCodeByServiceName(compressionName);
        }

        ProtocolMessage protocolMessage = new ProtocolMessage();
        DefaultProtocolHeader header = new DefaultProtocolHeader(BUSINESS_MESSAGE, isRespond, serializationCode,
                isCompressed, compressionType, threadPoolName, requestId);
        protocolMessage.setHeader(header);

        Serialization serialization = LioServiceLoader.getServiceLoader(Serialization.class).getService(serializationType);
        byte[] body = serialization.serialize(req);

        if(header.isCompressed()){
            Compression compression = LioServiceLoader.getServiceLoader(Compression.class).getService(compressionName);
            body = compression.compress(body);
        }

        protocolMessage.setBody(body);

        return protocolMessage;
    }

    public void destroy(){
        if(clients != null){
            clients.forEach(client -> {
                if(client != null){
                    client.close();
                }
            });
            clients.clear();
            if(logger.isInfoEnabled()){
                logger.info("destroy client invoker success, the server information[ protocol: " + protocol + ", host: " + host + ", port: " + port + " ]");
            }
        }
    }


    public boolean isAvailable(){
        return isAvailable;
    }

    public int getWeight() {
        return weight;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }
}
