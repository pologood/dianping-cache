package com.dianping.cache.controller.vo;

import java.util.List;

/**
 * Created by dp on 16/1/6.
 */
public class RedisReshardParams {
    private String cluster;
    private boolean isAverage;
    private boolean speed;
    private List<String> srcNodes;
    private List<String> desNodes;

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public boolean isAverage() {
        return isAverage;
    }

    public void setIsAverage(boolean average) {
        isAverage = average;
    }

    public List<String> getSrcNodes() {
        return srcNodes;
    }

    public void setSrcNodes(List<String> srcNodes) {
        this.srcNodes = srcNodes;
    }

    public List<String> getDesNodes() {
        return desNodes;
    }

    public void setDesNodes(List<String> desNodes) {
        this.desNodes = desNodes;
    }

    public boolean isSpeed() {
        return speed;
    }

    public void setSpeed(boolean speed) {
        this.speed = speed;
    }
}
