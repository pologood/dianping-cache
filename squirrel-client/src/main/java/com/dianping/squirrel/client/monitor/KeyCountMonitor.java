package com.dianping.squirrel.client.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Event;
import com.dianping.squirrel.common.config.ConfigManagerLoader;

public class KeyCountMonitor {

	private static Logger logger = LoggerFactory.getLogger(KeyCountMonitor.class);

	private static final String keyCountRangeConfig = ConfigManagerLoader.getConfigManager()
			.getStringValue("squirrel-client.monitor.keycount.range", "4,8,16,32,64,128,256");

	private static int[] keyCountRangeArray;

	private static final boolean enableMonitor = ConfigManagerLoader.getConfigManager()
			.getBooleanValue("squirrel-client.monitor.keycount.enable", true);

	private static class KeyCountHolder {
		public static final KeyCountMonitor INSTANCE = new KeyCountMonitor();
	}

	public static KeyCountMonitor getInstance() {
		return KeyCountHolder.INSTANCE;
	}

	private KeyCountMonitor() {
		if (enableMonitor) {
			init();
		}
	}

	private void init() {
		keyCountRangeArray = initRangeArray(keyCountRangeConfig);
	}

	private int[] initRangeArray(String rangeConfig) {
		String[] range = rangeConfig.split(",");
		int end = Integer.valueOf(range[range.length - 1]);
		int[] rangeArray = new int[end];
		int rangeIndex = 0;
		for (int i = 0; i < end; i++) {
			if (range.length > rangeIndex) {
				int value = Integer.valueOf(range[rangeIndex]);
				if (i >= value) {
					rangeIndex++;
				}
				rangeArray[i] = value;
			}
		}
		return rangeArray;
	}

	public void logKeyCount(String cacheType, String category, String eventName, int count) {
		if (enableMonitor && cacheType != null && !"web".equalsIgnoreCase(cacheType)) {
			try {
				log(count, keyCountRangeArray, "Squirrel." + cacheType + ".multi.keyCount");
			} catch (Throwable t) {
				logger.warn("error while logging key count:" + t.getMessage());
			}
		}
	}

	private void log(int size, int[] rangeArray, String eventName) {
		if (rangeArray != null && rangeArray.length > 0) {
			String value = null;
			if (rangeArray.length > size) {
				value = "<=" + rangeArray[size];
			} else {
				value = ">=" + rangeArray[rangeArray.length - 1];
			}
			Cat.getProducer().logEvent(eventName, value, Event.SUCCESS, String.valueOf(size));
		}
	}
}
