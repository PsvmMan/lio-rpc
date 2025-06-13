package com.gt.lio.cluster.loadbalance;

import com.gt.lio.cluster.client.ClientInvoker;
import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.protocol.body.RequestMessage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@SPIService("consistentHash")
public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    // 默认虚拟节点数量
    private final int virtualNodeCount;

    // 哈希函数
    private final HashFunction hashFunction;

    // key 提取器，用于从请求中提取唯一标识
    private final Function<RequestMessage, String> keyExtractor;

    // 缓存 selector，key 是 invokers 的 identityHashCode
    private final Map<Integer, ConsistentHashSelector> selectorCache = new ConcurrentHashMap<>();

    public ConsistentHashLoadBalance() {
        this(160, defaultHashFunction(), defaultKeyExtractor());
    }

    public ConsistentHashLoadBalance(int virtualNodeCount) {
        this(virtualNodeCount, defaultHashFunction(), defaultKeyExtractor());
    }

    public ConsistentHashLoadBalance(int virtualNodeCount,
                                     HashFunction hashFunction,
                                     Function<RequestMessage, String> keyExtractor) {
        this.virtualNodeCount = virtualNodeCount;
        this.hashFunction = hashFunction;
        this.keyExtractor = keyExtractor;
    }

    @Override
    public ClientInvoker doSelect(List<ClientInvoker> clientInvokers, RequestMessage req) {

        // 提取 key
        String key = keyExtractor.apply(req);

        // 获取 invokers 的唯一标识
        int identityHashCode = System.identityHashCode(clientInvokers);

        // 查找或创建 selector
        ConsistentHashSelector selector = selectorCache.get(identityHashCode);
        if (selector == null || selector.size() != clientInvokers.size()) {
            selectorCache.putIfAbsent(identityHashCode, new ConsistentHashSelector(clientInvokers, virtualNodeCount, hashFunction));
            selector = selectorCache.get(identityHashCode);
        }

        // 使用 selector 返回索引，再从 clientInvokers 中获取实际 invoker
        return clientInvokers.get(selector.select(key));
    }

    /**
     * 默认 key 提取逻辑
     */
    private static Function<RequestMessage, String> defaultKeyExtractor() {
        return req -> req.getServiceName() + req.getMethodKey() + System.nanoTime();
    }

    /**
     * 默认哈希算法
     */
    private static HashFunction defaultHashFunction() {
        return new MurmurHash();
    }

    /**
     * 哈希函数接口（便于扩展其他算法）
     */
    @FunctionalInterface
    public interface HashFunction {
        long hash(String input);
    }

    public static class MurmurHash implements HashFunction {

        private static final int SEED = 137;

        @Override
        public long hash(String input) {
            return murmurHash3(input.getBytes(StandardCharsets.UTF_8), SEED);
        }

        /**
         * MurmurHash3_x86_32 算法实现（32-bit 输出）
         */
        public int murmurHash3(byte[] data, int seed) {
            final int c1 = 0xcc9e2d51;
            final int c2 = 0x1b873593;

            final int r1 = 15;
            final int r2 = 13;
            final int m = 5;
            final int n = 0xe6546b64;

            int hash = seed;
            int length = data.length;
            int roundedEnd = (length / 4) * 4;
            int k = 0;

            // 处理完整 4 字节块
            for (int i = 0; i < roundedEnd; i += 4) {
                k = (data[i] & 0xff) |
                        ((data[i + 1] & 0xff) << 8) |
                        ((data[i + 2] & 0xff) << 16) |
                        ((data[i + 3] & 0xff) << 24);

                k *= c1;
                k = Integer.rotateLeft(k, r1);
                k *= c2;

                hash ^= k;
                hash = Integer.rotateLeft(hash, r2) * m + n;
            }

            // 处理剩余字节
            int tail = 0;
            switch (length - roundedEnd) {
                case 3:
                    tail |= (data[roundedEnd + 2] & 0xff) << 16;
                case 2:
                    tail |= (data[roundedEnd + 1] & 0xff) << 8;
                case 1:
                    tail |= (data[roundedEnd] & 0xff);
                    tail *= c1;
                    tail = Integer.rotateLeft(tail, r1);
                    tail *= c2;
                    hash ^= tail;
            }

            // 最终混合
            hash ^= length;
            hash ^= (hash >>> 16);
            hash *= 0x85ebca6b;
            hash ^= (hash >>> 13);
            hash *= 0xc2b2ae35;
            hash ^= (hash >>> 16);

            return hash;
        }
    }

    /**
     * 哈希环选择器（优化为仅存储索引）
     */
    static class ConsistentHashSelector {
        private final TreeMap<Long, Integer> virtualNodes; // 存储的是索引
        private final int size;

        public ConsistentHashSelector(List<ClientInvoker> invokers, int virtualNodeCount, HashFunction hashFunction) {
            this.size = invokers.size();
            this.virtualNodes = new TreeMap<>();

            // 外层循环是虚拟节点编号，内层是 invoker 列表
            for (int i = 0; i < virtualNodeCount; i++) {
                for (int idx = 0; idx < invokers.size(); idx++) {
                    ClientInvoker invoker = invokers.get(idx);
                    String virtualKey = invoker.getAddress() + "-" + i;
                    long hash = hashFunction.hash(virtualKey);
                    virtualNodes.put(hash, idx); // 存储索引
                }
            }
        }

        public int size() {
            return size;
        }

        public int select(String key) {
            if (virtualNodes.isEmpty()) {
                return -1;
            }

            long hash = hashFunction.hash(key);

            // 找到第一个大于等于当前 hash 的节点
            Map.Entry<Long, Integer> entry = virtualNodes.ceilingEntry(hash);
            if (entry == null) {
                entry = virtualNodes.firstEntry(); // 环状结构回绕
            }

            return entry.getValue(); // 返回索引
        }

        private final HashFunction hashFunction = defaultHashFunction();
    }
}
