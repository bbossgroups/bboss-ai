/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.frameworkset.spi.ai.memory.coordination;

import org.frameworkset.nosql.redis.RedisTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Distributed {@link PeriodicGate} backed by redis. Enables cross-node
 * coordination of periodic background work (memory maintenance, skill curator, etc.).
 */
public final class RedisStoreBackedPeriodicGate implements PeriodicGate {

    private static final Logger log = LoggerFactory.getLogger(RedisStoreBackedPeriodicGate.class);

    private static final String KEY_PREFIX = "coordination:periodic:";

    /**
     * 原子校验并认领周期性槽位的 Lua 脚本：
     * 读取上次认领时间，若距当前时间的间隔不小于 minGap，则将当前时间写回并返回 1，否则返回 0。
     * KEYS[1] 槽位 key
     * ARGV[1] 当前时间戳（毫秒）
     * ARGV[2] 最小间隔（毫秒）
     */
    private static final String CLAIM_SCRIPT =
            "local last = redis.call('GET', KEYS[1]) " +
            "local now = tonumber(ARGV[1]) " +
            "local minGap = tonumber(ARGV[2]) " +
            "if last and (now - tonumber(last)) < minGap then return 0 end " +
            "redis.call('SET', KEYS[1], now) " +
            "return 1";

    private String redis;

    public RedisStoreBackedPeriodicGate(String redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryClaim(String name, Duration minGap) {
        if (name == null || name.isEmpty() || minGap == null) {
            return false;
        }
        String key = KEY_PREFIX + name;
        long now = System.currentTimeMillis();
        long gapMillis = minGap.toMillis();
        try {
            RedisTool redisTool = RedisTool.getInstance(redis);
            Object result = redisTool.eval(CLAIM_SCRIPT, 1, key,
                    String.valueOf(now), String.valueOf(gapMillis));
            return result != null && Long.parseLong(result.toString()) == 1L;
        } catch (RuntimeException e) {
            log.warn("Failed to claim periodic gate {}: {}", name, e.getMessage());
            return false;
        }
    }
}