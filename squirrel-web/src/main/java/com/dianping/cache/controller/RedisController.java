package com.dianping.cache.controller;

import com.dianping.cache.controller.vo.*;
import com.dianping.cache.entity.CacheConfiguration;
import com.dianping.cache.entity.CacheKeyConfiguration;
import com.dianping.cache.entity.RedisStats;
import com.dianping.cache.monitor.highcharts.ChartsBuilder;
import com.dianping.cache.monitor.highcharts.HighChartsWrapper;
import com.dianping.cache.monitor.statsdata.RedisClusterData;
import com.dianping.cache.monitor.statsdata.RedisStatsData;
import com.dianping.cache.scale.ScaleException;
import com.dianping.cache.scale.cluster.redis.RedisCluster;
import com.dianping.cache.scale.cluster.redis.RedisManager;
import com.dianping.cache.scale.cluster.redis.RedisScaler;
import com.dianping.cache.scale.cluster.redis.ReshardPlan;
import com.dianping.cache.service.CacheConfigurationService;
import com.dianping.cache.service.CacheKeyConfigurationService;
import com.dianping.cache.service.RedisService;
import com.dianping.cache.service.ReshardService;
import com.dianping.squirrel.service.AuthService;
import com.dianping.squirrel.task.RedisReshardTask;
import com.dianping.squirrel.task.TaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Controller
public class RedisController extends AbstractSidebarController{


	@Autowired
	private RedisService redisService;

	@Autowired
	private ReshardService reshardService;

	@Autowired
	private CacheKeyConfigurationService cacheKeyConfigurationService;

	@Autowired
	private CacheConfigurationService cacheConfigurationService;

	@Autowired
	private AuthService authService;

	private String subside;

	private String currentCluster;

	@RequestMapping(value = "/redis/dashboard")
	public ModelAndView viewClusterDashBoard(){
		return new ModelAndView("monitor/redisdashboard",createViewMap());
	}

	/**
	 * @deprecated
	 * @return
     */
	@RequestMapping(value = "/redis/dashboardinfo")
	@ResponseBody
	public List<RedisClusterData> getDashboard(){
		return RedisDataUtil.getClusterData();
	}

	@RequestMapping(value = "/redis/data/dashboard")
	@ResponseBody
	public RedisDashBoardData getRedisDashboard(){
		return new RedisDashBoardData(RedisManager.getClusterCache().values());
	}
	@RequestMapping(value = "/redis/data/history")
	@ResponseBody
	public List<HighChartsWrapper> getRedisHistory(String cluster,long endTime){
		long start = (endTime - TimeUnit.MILLISECONDS.convert(120, TimeUnit.MINUTES))/1000;
		long end = endTime/1000;
		RedisCluster redisCluster = RedisManager.getRedisCluster(cluster);
		List<RedisStats> data = redisService.findByServerWithInterval(cluster, start, end);
		RedisStatsData statsData = new RedisStatsData(data);
		return ChartsBuilder.buildRedisStatsCharts(statsData);
	}
	/**
	 * @deprecated
	 * @return
	 */
	@RequestMapping(value = "/redis/serverinfo")
	public ModelAndView viewRedisServerInfo(){
		subside = "dashboard";
		return new ModelAndView("monitor/redisserverinfo",createViewMap());
	}
	/**
	 * @deprecated
	 * @return
	 */
	@RequestMapping(value = "/redis/serverinfodata")
	@ResponseBody
	public Map<String, Object> getRedisServerInfo(String address){
		return RedisDataUtil.getRedisServerData(address);
	}
	
	@RequestMapping(value = "/redis")
	public ModelAndView redis(){
		subside = "redis";
		return new ModelAndView("cluster/redis",createViewMap());
	}

	@RequestMapping(value = "/redis/{cluster}")
	public ModelAndView getRedisDetail(@PathVariable("cluster") String cluster){
		subside = "redis";
		currentCluster = cluster;
		return new ModelAndView("cluster/redisdetail",createViewMap());
	}

	@RequestMapping(value = "/redis/{cluster}/edit")
	public ModelAndView edit(@PathVariable("cluster") String cluster){
		subside = "redis";
		currentCluster = cluster;
		return new ModelAndView("cluster/edit",createViewMap());
	}

	@RequestMapping(value = "/redis/{cluster}/modifypassword")
	public boolean password(){
		//authService.
		return true;
	}

	@RequestMapping(value = "/redis/{cluster}/authorize")
	public boolean authorize(@PathVariable(value = "cluster") String cluster,@RequestParam String application) throws Exception {
		authService.authorize(application,cluster);
		return true;
	}

	@RequestMapping(value = "/redis/editdata")
	@ResponseBody
	public CacheConfiguration editRedis(@RequestParam String swimlane){
		CacheConfiguration configuration = cacheConfigurationService.findWithSwimLane(currentCluster,swimlane);
		return configuration;
	}

	@RequestMapping(value = "/redis/detail")
	@ResponseBody
	public Map<String, Object> getRedisDetailData(){
		List<CacheKeyConfiguration> categorys = cacheKeyConfigurationService.findByCacheType(currentCluster);
		Map<String,Object> result = new HashMap<String, Object>();
		RedisCluster redisCluster =  RedisManager.getRedisCluster(currentCluster);
		RedisDashBoardData data = new RedisDashBoardData();
		RedisDashBoardData.SimpleAnalysisData simpleAnalysisData = data.new SimpleAnalysisData(redisCluster);
		simpleAnalysisData.analysis();
		result.put("data",simpleAnalysisData);
		result.put("categorys",categorys);
		return result;
	}

	@RequestMapping(value = "/redis/data/applications")
	@ResponseBody
	public List<String> getApplications(String cluster) throws Exception {
		return authService.getAuthorizedApplications(cluster);
	}

	@RequestMapping(value = "/redis/data/auth")
	@ResponseBody
	public void getAuth(String cluster) throws Exception {
		// authService.getAuth(cluster);
	}

	@RequestMapping(value = "/redis/historydata")
	@ResponseBody
	public List<HighChartsWrapper> getRedisHistoryData(String address,long endTime){
		long start = (endTime - TimeUnit.MILLISECONDS.convert(120, TimeUnit.MINUTES))/1000;
		long end = endTime/1000;
		List<RedisStats> data = redisService.findByServerWithInterval(address, start, end);
		RedisStatsData statsData = new RedisStatsData(data);
		return ChartsBuilder.buildRedisStatsCharts(statsData);
	}

	@RequestMapping(value = "/redis/auth/setPassword")
	@ResponseBody
	public void setPassword(@RequestBody AuthParams authParams){

	}


	@RequestMapping(value = "/redis/auth/authorize")
	@ResponseBody
	public void authorize(@RequestBody AuthParams authParams) throws Exception {
		authService.authorize(authParams.getApplication(),authParams.getResource());
	}

	@RequestMapping(value = "/redis/auth/unauthorize")
	@ResponseBody
	public void unauthorize(@RequestBody AuthParams authParams) throws Exception {
		authService.unauthorize(authParams.getApplication(),authParams.getResource());
	}

	@RequestMapping(value = "/redis/reshard")
	@ResponseBody
	public void reshard(@RequestBody RedisReshardParams redisReshardParams) {

		ReshardPlan reshardPlan = reshardService.createReshardPlan(redisReshardParams.getCluster(), redisReshardParams.getSrcNodes(),
				redisReshardParams.getDesNodes(), redisReshardParams.isAverage());
		RedisReshardTask task = new RedisReshardTask(reshardPlan);

		TaskManager.submit(task);
	}

	@RequestMapping(value = "/redis/failover")
	@ResponseBody
	public boolean failover(@RequestBody RedisScaleParams redisScaleParams){
		return RedisManager.failover(redisScaleParams.getCluster(),redisScaleParams.getSlaveAddress());
	}

	@RequestMapping(value = "/redis/addslave")
	@ResponseBody
	public void addSlave(@RequestBody RedisScaleParams redisScaleParams){
		RedisScaler.addSlave(redisScaleParams.getCluster(),redisScaleParams.getMasterAddress());
	}

	@RequestMapping(value = "/redis/new")
	@ResponseBody
	public boolean newCluster(@RequestBody NewClusterParams newClusterParams){
		//RedisScaler.addSlave(redisScaleParams.getCluster(),redisScaleParams.getMasterAddress());
		return false;
	}


	@RequestMapping(value = "/redis/deleteslave")
	@ResponseBody
	public void delSlave(@RequestBody RedisScaleParams redisScaleParams) throws ScaleException{
		RedisScaler.removeSlave(redisScaleParams.getCluster(),redisScaleParams.getSlaveAddress());
	}

	@RequestMapping(value = "/redis/refreshcache")
	@ResponseBody
	public List<RedisClusterData> refreshClusterCache(String cluster) throws ScaleException{
		RedisManager.refreshCache(cluster);
		return RedisDataUtil.getClusterData();
	}


	@Override
	protected String getSide() {
		return "cluster";
	}

	@Override
	public String getSubSide() {
		return subside;
	}

}
