package com.dianping.cache.alarm.event;

import com.dianping.cache.alarm.memcache.MemcacheEvent;
import com.dianping.cache.alarm.redis.RedisEvent;

/**
 * Created by lvshiyun on 15/11/29.
 */
public interface EventFactory {
    MemcacheEvent createMemcacheEvent();
    RedisEvent createRedisEvent();
}
