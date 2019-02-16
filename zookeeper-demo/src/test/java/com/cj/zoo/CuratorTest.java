package com.cj.zoo;


import java.util.List;
import java.util.Map;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CuratorTest {

	public static String connectString="127.0.0.1:2181";
	public static String namespace="micro";
	public static String zkPath="/test";
	private CuratorFramework zkClient=null;
	
	@Before
	public void init(){
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(3000, 3);
		
		zkClient=CuratorFrameworkFactory.builder()
				.connectString(connectString)
				.retryPolicy(retryPolicy)
				.sessionTimeoutMs(100000)
				.connectionTimeoutMs(100000)
				.namespace(namespace)
				.build();
		zkClient.start();
		
		CuratorFrameworkState state=zkClient.getState();
		System.out.println("Init zkClient :"+state.name());
	}
	
	@After
	public void close(){
		if(zkClient!=null){
			zkClient.close();
			
			CuratorFrameworkState state=zkClient.getState();
			System.out.println("Close zkClient :"+state.name());
		}
	}
	
	
	/********* CRUD **************/
	
	@Test
	public void createTest() throws Exception{
		String result=zkClient.create()
				.creatingParentsIfNeeded()
				.withMode(CreateMode.PERSISTENT)	//default: PERSISTENT
				.withACL(Ids.OPEN_ACL_UNSAFE)		//default: Ids.OPEN_ACL_UNSAFE
				.forPath(zkPath+"/a","aaa".getBytes())
				;
		
		System.out.println("create: "+result);	// print created path
	}
	
	@Test
	public void createChildrenTest() throws Exception {
		zkClient.create().creatingParentsIfNeeded().forPath(zkPath+"/a/01/02");
	}
	
	@Test
	public void checkExistTest() throws Exception{
		Stat stat=zkClient.checkExists().forPath(zkPath+"/a");
		System.out.println(stat);
		
		// 如果不存在则为空
		stat=zkClient.checkExists().forPath(zkPath+"/aa");	//null
		System.out.println(stat);
	}
	
	@Test
	public void getDataTest() throws Exception{
		
		byte[] data=zkClient.getData().forPath(zkPath+"/a");
		System.out.println(new String(data));
		
		// NoNodeException
		data=zkClient.getData().forPath(zkPath+"/aa");
		if(data!=null)
			System.out.println(new String(data));
	}
	
	@Test
	public void getStatTest() throws Exception{
		Stat stat = new Stat();
		byte[] data=zkClient.getData().storingStatIn(stat).forPath(zkPath+"/a");
		if(data!=null){
			System.out.println("data: "+new String(data));
			System.out.println("version: "+stat.getVersion());
		}
		
		//NoNodeException
		data=zkClient.getData().storingStatIn(stat).forPath(zkPath+"/aa");	
		if(data!=null){
			System.out.println("data: "+new String(data));
			System.out.println("version: "+stat.getVersion());
		}
	}
	
	@Test
	public void getChildrenTest() throws Exception{
		List<String> children=zkClient.getChildren().forPath(zkPath);
		for(String child:children){
			byte[] data=zkClient.getData().forPath(zkPath+"/"+child);	// note: need add parentPath
			System.out.println(child+":"+new String(data));
		}
	}
	
	@Test
	public void updateTest() throws Exception{
		Stat stat=zkClient.setData().forPath(zkPath+"/a","aaa-aaa".getBytes());
		System.out.println(stat.getVersion());
		
		byte[] data=zkClient.getData().forPath(zkPath+"/a");
		System.out.println(new String(data));
		
		//BadVersionException
		stat=zkClient.setData().withVersion(0).forPath(zkPath+"/a","aaa-bbb".getBytes());	
		System.out.println(stat.getVersion());
		
		//NoNodeException
//		zkClient.setData().forPath(zkPath+"/a/01","a01".getBytes());
	}
	
	@Test
	public void deleteTest() throws Exception{
		
		zkClient.delete()
			.guaranteed()					// 如果删除失败，那么在后端还是继续会删除，直到成功
			.deletingChildrenIfNeeded()		// 如果有子节点，就删除
			.forPath(zkPath+"/a/01");
		
		Stat stat=zkClient.checkExists().forPath(zkPath+"/a/01");
		System.out.println(zkPath+"/a/01 :"+(stat!=null?"Exist":"NotExist"));
		
		stat=zkClient.checkExists().forPath(zkPath+"/a/01/02");
		System.out.println(zkPath+"/a/01/02 :"+(stat!=null?"Exist":"NotExist"));
		
		// NoNodeException
		zkClient.delete().guaranteed().deletingChildrenIfNeeded().forPath(zkPath+"/aa");	
	}
	
	
	/********* Watch  **************/
	
	// usingWatcher - 监听只会触发一次，监听完毕后就销毁 －－ 对子节点无效！
	// getData(),checkExists() 方法可追加 Watcher
	// getData().usingWatcher -- 只监听该节点的变化（可监听到 NodeDeleted,NodeDataChanged)，监听节点(watchPath)需存在，不然会报NoNodeException
	// checkExists().usingWatcher -- 只监听该节点的变化（可监听到 NodeCreated,NodeDeleted,NodeDataChanged)，监听节点(watchPath)可不存在
	@Test
	public void usingWatcherTest() throws Exception{
		
		Stat stat=zkClient.checkExists().forPath(zkPath+"/b");
		if(stat==null)
			zkClient.create().creatingParentContainersIfNeeded().forPath(zkPath+"/b","bbb".getBytes());
		
		zkClient.getData().usingWatcher(new CuratorWatcher() {
			@Override
			public void process(WatchedEvent event) throws Exception {
				System.out.println("Path:"+event.getPath());
				System.out.println("Type:"+event.getType());
				System.out.println("State:"+event.getState());
				if(!event.getType().equals(EventType.NodeDeleted)){
					byte[] data=zkClient.getData().forPath(event.getPath());
					if(data!=null)
						System.out.println("Data:"+new String(data));
				}
			}
		}).forPath(zkPath+"/b");
		
		// case 1: node data change
//		// first - trigger event:NodeDataChanged
//		zkClient.setData().forPath(zkPath+"/b","bbb-bbb".getBytes());
//		// second - no watcher trigger
//		zkClient.setData().forPath(zkPath+"/b","bbb-ccc".getBytes());	
		
		// case 2: create children -- no trigger
//		zkClient.create().creatingParentsIfNeeded().forPath(zkPath+"/b/01/02","Hello".getBytes());
		
		// case 3: update children data -- no trigger
//		zkClient.setData().forPath(zkPath+"/b/01","b01".getBytes());
				
		// case 4: delete children -- no trigger
//		zkClient.delete().deletingChildrenIfNeeded().forPath(zkPath+"/b/01/02");
		
		// case 5: delete node -- trigger event: NodeDeleted
		zkClient.delete().deletingChildrenIfNeeded().forPath(zkPath+"/b");
		
		Thread.sleep(10000);
	}
	
	@Test
	public void usingWatcher2Test() throws Exception{
//		Stat stat=zkClient.checkExists().forPath(zkPath+"/b");
//		if(stat==null)
//			zkClient.create().creatingParentContainersIfNeeded().forPath(zkPath+"/b","bbb".getBytes());
		
		zkClient.checkExists().usingWatcher(new CuratorWatcher() {
			@Override
			public void process(WatchedEvent event) throws Exception {
				System.out.println("Path:"+event.getPath());
				System.out.println("Type:"+event.getType());
				System.out.println("State:"+event.getState());
				if(!event.getType().equals(EventType.NodeDeleted)){
					byte[] data=zkClient.getData().forPath(event.getPath());
					if(data!=null)
						System.out.println("Data:"+new String(data));
				}
			}
		}).forPath(zkPath+"/b");
		
		// case 1: node data change
//		// first - trigger event:NodeDataChanged
//		zkClient.setData().forPath(zkPath+"/b","bbb-bbb".getBytes());
//		// second - no watcher trigger
//		zkClient.setData().forPath(zkPath+"/b","bbb-ccc".getBytes());	
		
		// case 2: create children -- no trigger
//		zkClient.create().creatingParentsIfNeeded().forPath(zkPath+"/b/01/02","Hello".getBytes());
		
		// case 3: update children data -- no trigger
//		zkClient.setData().forPath(zkPath+"/b/01","b01".getBytes());
				
		// case 4: delete children -- no trigger
//		zkClient.delete().deletingChildrenIfNeeded().forPath(zkPath+"/b/01/02");
		
		// case 5: delete node -- trigger event: NodeDeleted
//		zkClient.delete().deletingChildrenIfNeeded().forPath(zkPath+"/b");
		
		// case 6: create node -- trigger event: NodeCreated
		zkClient.create().creatingParentContainersIfNeeded().forPath(zkPath+"/b","bbb".getBytes());
		
		Thread.sleep(10000);
	}
	
	// NodeCache 一次注册，n次监听 -- 监听当前节点，对子节点无效！（监听节点可不存在）
	@Test
	public void nodeCacheWatchTest() throws Exception{
		
		String watchPath=zkPath+"/c";
		if(zkClient.checkExists().forPath(watchPath)==null)
			zkClient.create().creatingParentContainersIfNeeded().forPath(watchPath,"ccc".getBytes());
		
		NodeCache nodeCache = new NodeCache(zkClient, watchPath);
		
		System.out.println("Add Listener...");
		nodeCache.getListenable().addListener(new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				System.out.println("Trigger Watcher...");
				ChildData data=nodeCache.getCurrentData();
				if(data!=null){
					System.out.println(data.getPath());
					System.out.println(new String(data.getData()));
					System.out.println(data.getStat().getVersion());
				}else {
					System.out.println("Empty!");
				}
			}
		});
		
		// start －－ 注意：不管是否cacheData，启动不会触发watcher
//		nodeCache.start();		// 不会cache节点数据
		nodeCache.start(true);	// cache节点数据 
		
		// check initial data
		ChildData data=nodeCache.getCurrentData();
		if(data!=null){
			System.out.println("节点初始化数据为：");
			System.out.println(data.getPath());
			System.out.println(new String(data.getData()));
			System.out.println(data.getStat().getVersion());
		}else {
			System.out.println("节点初始化数据为空！");
		}
		
		System.out.println("Begin Case 1 ...");
		// case 1: watchPath -- trigger watcher
		// node data change -- always trigger event:NodeDataChanged
		zkClient.setData().forPath(watchPath,"ccc-ccc".getBytes());
		Thread.sleep(2000);
		zkClient.setData().forPath(watchPath,"ccc-ddd".getBytes());	
		// delete node -- trigger event: NodeDeleted
		zkClient.delete().deletingChildrenIfNeeded().forPath(watchPath);
		// create node -- trigger event: NodeCreated
		zkClient.create().creatingParentContainersIfNeeded().forPath(watchPath,"ccc".getBytes());
				
		Thread.sleep(2000);
		
		System.out.println("Begin Case 2 ...");
		// case 2: children -- no trigger
		// create children
		zkClient.create().creatingParentsIfNeeded().forPath(watchPath+"/01/02","Hello".getBytes());
		// update children data
		zkClient.setData().forPath(watchPath+"/01","c01".getBytes());
		// delete children
		zkClient.delete().deletingChildrenIfNeeded().forPath(watchPath+"/01/02");
		
		Thread.sleep(10000);
		
		if(nodeCache!=null)
			nodeCache.close();
	}
	
	// PathChildrenCache 监听节点的一级子节点的CUD
	// Note: 监听节点不会触发watcher，监听节点可不存在；
	// 		 若监听节点删除，则监听失效，子节点的变化将不会触发watcher
	// startMode:
	//		Normal					- 异步方式缓存初始化信息, not trigger watcher
	//		POST_INITIALIZED_EVENT 	- 异步方式缓存初始化信息, trigger watcher (Type.INITIALIZED)
	//		BUILD_INITIAL_CACHE		- 同步方式缓存初始化信息, not trigger watcher
	@Test
	public void pathChildenCacheWatchTest() throws Exception{
		
		String watchPath=zkPath+"/d";
		if(zkClient.checkExists().forPath(watchPath)==null){
			zkClient.create().creatingParentContainersIfNeeded().forPath(watchPath,"ddd".getBytes());
		}
		if(zkClient.checkExists().forPath(watchPath+"/0")==null){
			zkClient.create().creatingParentContainersIfNeeded().forPath(watchPath+"/0","00".getBytes());
		}
		
		// cacheData:true/false
		PathChildrenCache childCache=new PathChildrenCache(zkClient, watchPath, true);
		
		System.out.println("Add Listener...");
		childCache.getListenable().addListener(new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
				System.out.println(event.getType());
				ChildData child=event.getData();
				if(child!=null)
					System.out.println(child.getPath()+":"+new String(child.getData())+",version:"+child.getStat().getVersion());
			
				if(event.getType().equals(Type.INITIALIZED)){
					System.out.println("watcher到的节点初始化信息：");
					List<ChildData> childDataList=event.getInitialData();
					for(ChildData c:childDataList){
						System.out.println(c.getPath()+":"+new String(c.getData())+",version:"+c.getStat().getVersion());
					}
				}
			}
		});
		
		System.out.println("Start Cache...");
//		childCache.start();		//default: StartMode.NORMAL,异步初始化	 -- initial data not trigger watcher
//		childCache.start(StartMode.POST_INITIALIZED_EVENT); 	// 异步初始化 -- initial trigger watcher: Type.INITIALIZED
		childCache.start(StartMode.BUILD_INITIAL_CACHE);		// 同步初始化 -- initial not trigger watcher
		
//		Thread.sleep(2000);
		List<ChildData> childDataList= childCache.getCurrentData();
		System.out.println("start 节点初始化信息：");
		for(ChildData child:childDataList){
			System.out.println(child.getPath()+":"+new String(child.getData())+",version:"+child.getStat().getVersion());
		}
		
		
		System.out.println("Begin Case 1...");
		// case 1: watchPath -- no trigger
		// node data change
//		zkClient.setData().forPath(watchPath,"ddd-ddd".getBytes());
//		// delete node	-- Note: 会导致watcher失效
//		zkClient.delete().deletingChildrenIfNeeded().forPath(watchPath);
//		// create node
//		zkClient.create().creatingParentContainersIfNeeded().forPath(watchPath,"ddd".getBytes());
//		Thread.sleep(2000);
		
		System.out.println("Begin Case 2...");
		// case 2: children -- trigger
		// create children -- trigger CHILD_ADDED -- Note: on trigger "/01" CHILD_ADD, won't trigger "/01/02" CHILD
		zkClient.create().creatingParentsIfNeeded().forPath(watchPath+"/01/02","Hello".getBytes());
		// update children data -- trigger CHILD_UPDATED
		zkClient.setData().forPath(watchPath+"/01","d01".getBytes());
		// delete children -- trigger CHILD_REMOVED
		zkClient.delete().deletingChildrenIfNeeded().forPath(watchPath+"/01");
		
		Thread.sleep(10000);
		
		if(childCache!=null)
			childCache.close();
		
	}
	
	// TreeCache 监听节点和该节点的所有子节点 （创建时可设置maxDepth）
	@Test
	public void treeCacheWatchTest() throws Exception{
		
		String watchPath=zkPath+"/e";
		if(zkClient.checkExists().forPath(watchPath)==null){
			zkClient.create().creatingParentContainersIfNeeded().forPath(watchPath,"eee".getBytes());
		}
		if(zkClient.checkExists().forPath(watchPath+"/0")==null){
			zkClient.create().creatingParentContainersIfNeeded().forPath(watchPath+"/0","00".getBytes());
		}
		
		System.out.println("Add Listener...");
		TreeCache treeCache=new TreeCache(zkClient, watchPath);		// default：not cacheData,initial data will trigger watcher: NODE_ADDED
		treeCache.getListenable().addListener(new TreeCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
				System.out.println("Watch:");
				System.out.println(event.getType());
				ChildData childData=event.getData();
				if(childData!=null)
					System.out.println(childData.getPath()+":"+new String(childData.getData())+",version:"+childData.getStat().getVersion());
			}
		});
		
		treeCache.start();
		
		System.out.println("Initial Data...");
		ChildData childData=treeCache.getCurrentData(watchPath);
		if(childData!=null)
			System.out.println("Current:"+childData.getPath()+":"+new String(childData.getData())+",version:"+childData.getStat().getVersion());
		Map<String,ChildData> childMap=treeCache.getCurrentChildren(watchPath);
		if(childMap!=null){
			for(String key:childMap.keySet()){
				System.out.println("Child:"+key+":"+childMap.get(key));
			}
		}
		
		System.out.println("Begin case 1...");
		// case 1: watchPath -- trigger
		// node data change -- trigger NODE_UPDATED
		zkClient.setData().forPath(watchPath,"eee-eee".getBytes());
		// delete node	-- trigger NODE_REMOVED 2 times for path '/test/e/0','/test/e'
		zkClient.delete().deletingChildrenIfNeeded().forPath(watchPath);
		// create node -- trigger NODE_ADDED
		zkClient.create().creatingParentContainersIfNeeded().forPath(watchPath,"eee".getBytes());
		Thread.sleep(2000);
		
		System.out.println("Begin Case 2...");
		// case 2: children -- trigger
		// create children -- trigger NODE_ADDED 2 times for path: '/test/e/01','/test/e/01/02'
		zkClient.create().creatingParentsIfNeeded().forPath(watchPath+"/01/02","Hello".getBytes());
		// update children data -- trigger NODE_UPDATED
		zkClient.setData().forPath(watchPath+"/01","d01".getBytes());
		// delete children -- trigger NODE_REMOVED 2 times for path: '/test/e/01/02','/test/e/01'
		zkClient.delete().deletingChildrenIfNeeded().forPath(watchPath+"/01");
		
		
		Thread.sleep(10000);
		
		if(treeCache!=null)
			treeCache.close();
	}

}
