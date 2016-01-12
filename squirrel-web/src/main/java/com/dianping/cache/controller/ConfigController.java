package com.dianping.cache.controller;

import com.dianping.cache.controller.dto.ConfigurationParams;
import com.dianping.cache.entity.CacheConfiguration;
import com.dianping.cache.service.CacheConfigurationService;
import com.dianping.cache.service.CacheKeyConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Created by dp on 16/1/11.
 */
@Controller
public class ConfigController extends AbstractCacheController {

    @Autowired
    private CacheKeyConfigurationService cacheKeyConfigurationService;

    @Autowired
    private CacheConfigurationService cacheConfigurationService;

    private String subside;

    @RequestMapping(value = "/config/cluster")
    public ModelAndView clusterConfig() {
        subside = "clusterconfig";
        return new ModelAndView("cache/config", createViewMap());
    }

    @RequestMapping(value = "/config/category")
    public ModelAndView categoryConfig() {
        subside = "categoryconfig";
        return new ModelAndView("cache/key", createViewMap());
    }

    @RequestMapping(value = "/config/cluster/edit")
    public ModelAndView editConfig() {

        return new ModelAndView("cache/configedit");
    }

    @RequestMapping(value = "/config/configuration/1")
    @ResponseBody
    public CacheConfiguration getConfig(@RequestBody ConfigurationParams configurationParams) {
        return cacheConfigurationService.findWithSwimLane(configurationParams.getCacheKey(), configurationParams.getSwimlane());
    }

    @RequestMapping(value = "/config/cluster/findAll")
    @ResponseBody
    public List<CacheConfiguration> getConfigurations() {
        return cacheConfigurationService.findAll();
    }

    @RequestMapping(value = "/config/cluster/update")
    @ResponseBody
    public void update() {

    }

    @Override
    protected String getSide() {
        return "config";
    }

    @Override
    public String getSubSide() {
        return subside;
    }
}