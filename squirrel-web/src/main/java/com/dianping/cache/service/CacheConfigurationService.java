/**
 * Project: cache-server
 * 
 * File Created at 2010-10-15
 * $Id$
 * 
 * Copyright 2010 Dianping.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Dianping Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Dianping.com.
 */
package com.dianping.cache.service;

import com.dianping.avatar.exception.DuplicatedIdentityException;
import com.dianping.cache.entity.CacheConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CacheKeyConfigurationService
 * @author danson.liu
 *
 */
@Transactional
public interface CacheConfigurationService {
	
	/**
	 * retrieve all configurations
	 * @return
	 */
	List<CacheConfiguration> findAll();
	
	CacheConfiguration find(String key);

	CacheConfiguration findWithSwimLane(String key,String swimlane);
	
	/**
	 * 批量清除，仅清除Java端
	 * @param category
	 */
	void clearByCategory(String category);
	
	/**
	 * 批量清除指定机器或组的缓存，仅清除Java端
	 * @param category
	 * @param serverOrGroup
	 */
	void clearByCategory(String category, String serverOrGroup);
	
	/**
	 * 仅在JAVA端清除缓存(可调整为同时清除.net缓存，但尚无需求，可在.net后台管理系统操作)
	 * @param cacheType
	 * @param key
	 */
	void clearByKey(String cacheType, String key);

	CacheConfiguration create(CacheConfiguration config) throws DuplicatedIdentityException;
	
	CacheConfiguration update(CacheConfiguration config);
	
	void delete(String key);

	void deleteWithSwimLane(String key,String swimlane);
	
	void incVersion(String category);
	
	void pushCategoryConfig(String category, String serverOrGroup);
	
	void migrate();

	String getPassword(String cacheKey);
}
