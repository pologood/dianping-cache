package com.dianping.cache.service.impl;

import com.dianping.cache.scale.ScaleException;
import com.dianping.cache.scale.cluster.redis.RedisCluster;
import com.dianping.cache.scale.cluster.redis.RedisManager;
import com.dianping.cache.scale.cluster.redis.RedisServer;
import com.dianping.cache.scale.cluster.redis.ReshardPlan;
import com.dianping.cache.service.ReshardService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dp on 16/1/7.
 */
public class ReshardServiceImpl implements ReshardService{
    @Override
    public ReshardPlan createReshardPlan(String cluster,List<String> srcNodes,List<String> desNodes,boolean isAverage) {
        ReshardPlan reshardPlan;
        List<RedisServer> srcNodeServer = RedisManager.getServerInClusterCache(cluster,srcNodes);
        if(isAverage){
            int totalSlots = 0,avgSlots;
            for(RedisServer server : srcNodeServer){
                totalSlots += server.getSlotSize();
            }
            avgSlots = totalSlots / srcNodeServer.size();

            List<String> more = new ArrayList<String>();
            List<String> less = new ArrayList<String>();

            for(RedisServer server : srcNodeServer){
                if(server.getSlotSize() > avgSlots){
                    more.add(server.getAddress());
                }else{
                    less.add(server.getAddress());
                }
            }
            reshardPlan = new ReshardPlan(cluster,more,less,true);
        }else{
            desNodes.removeAll(srcNodes);
            reshardPlan = new ReshardPlan(cluster,srcNodes,desNodes,false);
        }

        return reshardPlan;
    }

    @Override
    public ReshardPlan findLastReshardPlan() {
        return null;
    }

    @Override
    public ReshardPlan getPlanByPlanId(int id) {
        return null;
    }

    @Override
    public void updateReshardPlan(ReshardPlan reshardPlan) {

    }

    @Override
    public void stopPlan(int id) {

    }

    @Override
    public void cancelPlan(int id) {

    }
}
