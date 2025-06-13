package com.gt.lio.cluster.loadbalance;

import com.gt.lio.cluster.client.ClientInvoker;
import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.protocol.body.RequestMessage;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SPIService("weightedRandom")
public class WeightedRandomLoadBalance extends AbstractLoadBalance{


    public WeightedRandomLoadBalance() {

    }

    @Override
    public ClientInvoker doSelect(List<ClientInvoker> clientInvokers, RequestMessage req) {

        // 总权重、是否权重相同、权重值数组
        int totalWeight = 0;
        boolean sameWeight = true;
        int[] cumulativeWeights = new int[clientInvokers.size()];

        for(int i = 0; i < clientInvokers.size(); i++){
            ClientInvoker clientInvoker = clientInvokers.get(i);
            totalWeight += clientInvoker.getWeight();
            if(sameWeight & i > 0 && clientInvokers.get(i - 1).getWeight() != clientInvoker.getWeight()){
                sameWeight = false;
            }
            if(i == 0){
                cumulativeWeights[i] = clientInvoker.getWeight();
            }else {
                cumulativeWeights[i] = cumulativeWeights[i - 1] + clientInvoker.getWeight();
            }
        }

        // 如果总权重为0 或 权重相同，则直接随机返回
        if (totalWeight <= 0 || sameWeight) {
            return clientInvokers.get(ThreadLocalRandom.current().nextInt(clientInvokers.size()));
        }

        // 获取随机数
        int offset = ThreadLocalRandom.current().nextInt(totalWeight);

        // 使用自定义二分查找定位区间
        int index = findWeightedIndex(cumulativeWeights, offset);

        return clientInvokers.get(index);
    }


    /**
     * 在 cumulativeWeights 中查找第一个 >= offset 的索引位置
     */
    private int findWeightedIndex(int[] cumulativeWeights, int offset) {

        int left = 0;
        int right = cumulativeWeights.length - 1;

        // 如果 offset 比所有元素都小，返回第一个
        if (offset < cumulativeWeights[0]) {
            return 0;
        }

        while (left <= right) {

            int mid = (left + right) >>> 1;  // 防止溢出
            int midVal = cumulativeWeights[mid];

            if (midVal == offset) {
                return mid;
            } else if (midVal < offset) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        // 此时 left 是第一个 >= offset 的位置
        return left;
    }
}
