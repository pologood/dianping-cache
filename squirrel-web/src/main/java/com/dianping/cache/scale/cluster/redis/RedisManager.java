package com.dianping.cache.scale.cluster.redis;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.dianping.cache.entity.CacheConfiguration;
import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisClusterException;

import com.dianping.cache.service.CacheConfigurationService;
import com.dianping.cache.support.spring.SpringLocator;
import com.dianping.cache.util.ParseServersUtil;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class RedisManager {
	
	private static Logger logger = Logger.getLogger(RedisManager.class);

	public static final String SLOT_IN_TRANSITION_IDENTIFIER = "[";
	public static final String SLOT_IMPORTING_IDENTIFIER = "--<--";
	public static final String SLOT_MIGRATING_IDENTIFIER = "-->--";
	public static final long CLUSTER_SLEEP_INTERVAL = 50;
	public static final int CLUSTER_DEFAULT_TIMEOUT = 300;
	public static final int CLUSTER_DEFAULT_DB = 0;
	public static final String UNIX_LINE_SEPARATOR = "\n";
	public static final String WINDOWS_LINE_SEPARATOR = "\r\n";
	public static final String COLON_SEPARATOR = ":";

	private static final Map<String,RedisCluster> clusterCache = new TreeMap<String, RedisCluster>();

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	static {
		refreshCache();
	}
	
	public static void create(List<RedisNode> nodes) {
		
	}
	
	public static void addMaster(String cluster,String master){
		RedisCluster rc = getRedisCluster(cluster);
		RedisServer rs = new RedisServer(master);
		joinCluster(rc, rs);
		refreshCache(cluster);
	}

	public static void addSlaveToMaster(String master, String slave) {
		try {
			RedisServer m = new RedisServer(master);
			RedisServer s = new RedisServer(slave);
			if(!checkPort(s,60000)){
				return;
			}
			String masterNodeId = getNodeId(m);
			Jedis mJedis = new Jedis(m.getIp(), m.getPort());
			mJedis.clusterMeet(s.getIp(), s.getPort());
			waitForClusterReady(master);
			Jedis sJedis = new Jedis(s.getIp(), s.getPort());
			sJedis.clusterReplicate(masterNodeId);
			sJedis.close();
		} catch (Exception e) {
			logger.error("Add slave to " + master + " error ! Please check : "
					+ slave + "\n" + e);
		} finally {
		}
	}
	private static boolean checkPort(RedisServer redisServer,long timeout) throws InterruptedException {
		Jedis jedis = new Jedis(redisServer.getIp(),redisServer.getPort());
		JedisConnectionException ex = null;
		long wait = 0;
		while (wait < timeout){
			try {
				if (jedis.ping().contains("PONG")){
					jedis.close();
					return true;
				}
			}catch (JedisConnectionException e){
				Thread.sleep(1000);
				wait += 1000;
				ex = e;
			}
		}
		logger.error("TimeOut for wait " + redisServer.getAddress() + " response .",ex);
		return false;
	}
	public static boolean removeServer(String cluster,String address) {
		refreshCache(cluster);
		RedisCluster rc = clusterCache.get(cluster);
		RedisServer nodeToDelete = rc.getServer(address);
		if(nodeToDelete == null){
			return false;
		}
		Jedis deleteNode = new Jedis(nodeToDelete.getIp(),nodeToDelete.getPort());
		String deleteNodeId = nodeToDelete.getId();
		List<RedisServer> allNodesOfCluster = rc.getAllAliveServer();

		// check if the node to delete is a master
		boolean isMaster = false;
		if (nodeToDelete.isMaster()) {
			isMaster = true;
		}

		// the node to delete is slave
		if (!isMaster) {
			for (RedisServer node : allNodesOfCluster) {
				// a node cannot `forget` itself
				if (!node.equals(nodeToDelete)) {
					Jedis conn = new Jedis(node.getIp(), node.getPort());
					conn.clusterForget(deleteNodeId);
					conn.close();
				}
			}
			deleteNode.close();
			refreshCache(cluster);
			return true;
		}
		//TODO   delete master node 
		deleteNode.close();
		refreshCache(cluster);
		return false;// master
	}

	public static boolean joinCluster(RedisCluster cluster, RedisServer server ) {
			RedisServer rsInCluster = cluster.getAllAliveServer().get(0);
	        Jedis clusterJedis = new Jedis(rsInCluster.getIp(), rsInCluster.getPort());
	        clusterJedis.clusterMeet(server.getIp(), server.getPort());

	        List<RedisServer> clusterNodes = cluster.getAllAliveServer();

	        boolean joinOk = false;
	        long sleepTime = 0;
	        while (!joinOk && sleepTime < CLUSTER_DEFAULT_TIMEOUT) {
	            joinOk = true;
	            for (RedisServer rs : clusterNodes) {
	                if (!isNodeKnown(rs, server)) {
	                    joinOk = false;
	                    break;
	                }
	            }
	            if (joinOk) {
	                break;
	            }
	            try {
	                TimeUnit.MILLISECONDS.sleep(CLUSTER_SLEEP_INTERVAL);
	            } catch (InterruptedException e) {
	                throw new JedisClusterException("joinCluster timeout.", e);
	            }
	            sleepTime += CLUSTER_SLEEP_INTERVAL;
	        }
	        clusterJedis.close();
	        return joinOk;
	}

	public static String getNodeId(RedisServer server) {
		Jedis jedis = new Jedis(server.getIp(), server.getPort());
		String[] clusterInfo = splitClusterInfo(jedis.clusterNodes());
		for (String lineInfo : clusterInfo) {
			if (lineInfo.contains("myself")) {
				jedis.close();
				return lineInfo.split(" ")[0];
			}
		}
		jedis.close();
		return null;
	}

	private static boolean isNodeKnown(RedisServer srcNodeInfo,RedisServer targetNodeInfo) {
		Jedis srcNode = new Jedis(srcNodeInfo.getIp(), srcNodeInfo.getPort());
		String targetNodeId = getNodeId(targetNodeInfo);
		String[] clusterInfo = srcNode.clusterNodes()
				.split(UNIX_LINE_SEPARATOR);
		for (String infoLine : clusterInfo) {
			if (infoLine.contains(targetNodeId)) {
				srcNode.close();
				return true;
			}
		}
		srcNode.close();
		return false;
	}

	public static List<RedisServer> getAllNodesOfCluster(RedisServer server) {
		Jedis node = new Jedis(server.getIp(), server.getPort());
		List<RedisServer> clusterNodeList = new ArrayList<RedisServer>();
		String[] clusterNodesOutput = node.clusterNodes().split(
				UNIX_LINE_SEPARATOR);
		for (String infoLine : clusterNodesOutput) {
			String[] hostAndPort = infoLine.split(" ")[1].split(":");
			RedisServer rs = new RedisServer(hostAndPort[0],
					Integer.valueOf(hostAndPort[1]));
			clusterNodeList.add(rs);
		}
		return clusterNodeList;
	}

	private static String[] splitClusterInfo(String str) {
		if (str != null) {
			str = str.replaceAll("\r\n", "#");
			str = str.replaceAll("\n", "#");
			return str.split("#");
		}
		return null;
	}
	
	public static RedisCluster getRedisCluster(String cluster){
		if(clusterCache.containsKey(cluster)){
			return clusterCache.get(cluster);
		}
		refreshCache(cluster);
		return clusterCache.get(cluster);
	}
	
	public static void refreshCache(String cluster){
		CacheConfigurationService cacheConfigurationService = SpringLocator.getBean("cacheConfigurationService");
		List<String> serverList = ParseServersUtil.parseRedisServers(cacheConfigurationService.find(cluster).getServers());
		RedisCluster rc = new RedisCluster(serverList);
		for (RedisNode node : rc.getNodes()) {
			node.getMaster().loadRedisInfo();
			if (node.getSlave() != null)
				node.getSlave().loadRedisInfo();
		}
		clusterCache.put(cluster, rc);
	}
	
    public static void waitForClusterReady(String clusterRandomAddress) {
        boolean clusterOk = false;
        while (!clusterOk) {
            clusterOk = true;
            List<RedisServer> serverList = getAllNodesOfCluster(new RedisServer(clusterRandomAddress));
            for (RedisServer rs : serverList) {
                Jedis jedis = new Jedis(rs.getIp(), rs.getPort());
                String clusterInfo = jedis.clusterInfo();
                String firstLine = clusterInfo.split(UNIX_LINE_SEPARATOR)[0];
                String[] firstLineArr = firstLine.trim().split(COLON_SEPARATOR);
                if (firstLineArr[0].contains("cluster_state") &&
                        firstLineArr[1].contains("ok")) {
                    jedis.close();
                    continue;
                }
                jedis.close();
                clusterOk = false;
                break;
            }
            if (clusterOk) {
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(CLUSTER_SLEEP_INTERVAL);
            } catch (InterruptedException e) {
                throw new JedisClusterException("waitForClusterReady", e);
            }
        }
    }

	private static void refreshCache(){
		scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				CacheConfigurationService cacheConfigurationService = SpringLocator.getBean("cacheConfigurationService");
				List<CacheConfiguration> configurations = cacheConfigurationService.findAll();
				Map<String,RedisCluster> tempClusterCache = new TreeMap<String, RedisCluster>();
				for (CacheConfiguration configuration : configurations) {
					if (configuration.getCacheKey().startsWith("redis") &&
							"".equals(configuration.getSwimlane())) {
						String url = configuration.getServers();
						List<String> servers = ParseServersUtil.parseRedisServers(url);
						RedisCluster cluster = new RedisCluster(servers);
						for (RedisNode node : cluster.getNodes()) {
							node.getMaster().loadRedisInfo();
							if (node.getSlave() != null)
								node.getSlave().loadRedisInfo();
						}
						tempClusterCache.put(configuration.getCacheKey(), cluster);
					}
				}
				clusterCache.clear();
				clusterCache.putAll(tempClusterCache);
			}
		},15,30,TimeUnit.SECONDS);
	}

	public static Map<String,RedisCluster> getClusterCache() {
		return clusterCache;
	}
}