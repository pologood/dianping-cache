package com.dianping.squirrel.common.lifecycle;

import com.dianping.squirrel.client.config.StoreClientConfig;

public interface Configurable {

    public void configure(StoreClientConfig config);
    
}
