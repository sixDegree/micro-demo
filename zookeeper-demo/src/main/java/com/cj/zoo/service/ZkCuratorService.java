package com.cj.zoo.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;

public class ZkCuratorService {

	private CuratorFramework zkClient;
	
	@Value("${zk.server}")
	private String connectString;
	
	@Value("${zk.namespace}")
	private String namespace;

	private PathChildrenCache childCache;
	
	public CuratorFramework getZkClient() {
		return zkClient;
	}

	public void setZkClient(CuratorFramework zkClient) {
		this.zkClient = zkClient;
	}

	@PostConstruct
	public void init(){
		System.out.println("ZK Service init");
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(3000, 3);
		zkClient=CuratorFrameworkFactory.builder()
				.connectString(connectString)
				.retryPolicy(retryPolicy)
				.sessionTimeoutMs(10000)
				.connectionTimeoutMs(10000)
				.namespace(namespace)
				.build();
		zkClient.start();
	}
	
	@PreDestroy
	public void close(){
		if(zkClient!=null)
			zkClient.close();
	}
	
	public void addPathChildrenWatcher(String watchPath,boolean createIfNotExist,PathChildrenCacheListener listener) throws Exception{
		if(createIfNotExist){
			if(zkClient.checkExists().forPath(watchPath)==null){
				zkClient.create().creatingParentContainersIfNeeded().forPath(watchPath);
			}
		}
		childCache = new PathChildrenCache(zkClient, watchPath, true);
		System.out.println("Add Listener...");
		childCache.getListenable().addListener(listener);
		System.out.println("Start Cache...");
		childCache.start(StartMode.POST_INITIALIZED_EVENT); 	// 异步初始化 -- initial trigger watcher: Type.INITIALIZED
	}
	
	public String create(String path,byte[] data) throws Exception{
		return zkClient.create()
				.creatingParentsIfNeeded()
				.withMode(CreateMode.PERSISTENT_SEQUENTIAL)
				.forPath(path,data);
	}
	
	public void delete(String path) throws Exception{
		zkClient.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
	}
	
	public Stat update(String path,byte[] data) throws Exception{
		return zkClient.setData().forPath(path,data);
	}
	
	public Stat checkExists(String path) throws Exception{
		return zkClient.checkExists().forPath(path);
	}
	
}
