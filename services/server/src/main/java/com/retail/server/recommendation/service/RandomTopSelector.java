package com.retail.server.recommendation.service;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomTopSelector {

    public <T> List<T> select(List<T> rankedItems, int topM, int topK) {
        if (CollectionUtils.isEmpty(rankedItems) || topK <= 0) {
            return List.of();
        }
        int poolSize = Math.min(Math.max(topK, topM), rankedItems.size());
        List<T> pool = new ArrayList<>(rankedItems.subList(0, poolSize));
        Collections.shuffle(pool, ThreadLocalRandom.current());
        return pool.subList(0, Math.min(topK, pool.size()));
    }
}
