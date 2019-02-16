
# zookeeper-demo


## Summary

### dependency

curator-recipes (included zookeeper)
```xml
<!-- Change zookeeper version ( beta version may have issues.) -->
<dependency>
	<groupId>org.apache.curator</groupId>
	<artifactId>curator-recipes</artifactId>
	<version>4.0.1</version>
	<exclusions>
		<exclusion>
			 <groupId>org.apache.zookeeper</groupId>
			 <artifactId>zookeeper</artifactId>
		</exclusion>
	</exclusions>
</dependency>
<dependency>
	<groupId>org.apache.zookeeper</groupId>
	<artifactId>zookeeper</artifactId>
	<version>3.4.6</version>
	<!-- <exclusions>
		<exclusion>
			  <groupId>org.slf4j</groupId>
			  <artifactId>slf4j-api</artifactId>
		</exclusion>
		<exclusion>
			 <groupId>org.slf4j</groupId>
			 <artifactId>slf4j-log4j12</artifactId>
		</exclusion>
	</exclusions> -->
</dependency>
```

### zkClient

```java
CuratorFramework zkClient = CuratorFrameworkFactory.builder()...build();
zkClient.start();
//...
zkClient.close();
```
key properties:
- connectString 服务器列表，格式host1:port1,host2:port2
- namespace 命名空间
- retryPolicy 重试策略,内建有四种重试策略,也可以自行实现RetryPolicy接口(eg: `ExponentialBackoffRetry(3000, 3)`)
- sessionTimeoutMs 会话超时时间，单位毫秒，默认60000ms
- connectionTimeoutMs 连接创建超时时间，单位毫秒，默认60000ms
	

### CRUD

- Create: `create()`
	+ creatingParentContainersIfNeeded() 自动递归创建所有所需的父节点
	+ withMode(CreateMode.xxx) 指定创建模式
		* CreateMode.PERSISTENT 持久化 (default)
		* CreateMode.PERSISTENT_SEQUENTIAL 持久化并且带序列号
		* CreateMode.EPHEMERAL 临时
		* CreateMode.EPHEMERAL_SEQUENTIAL 临时并且带序列号
	+ withACL(Ids.xxx) 
		* Ids.OPEN_ACL_UNSAFE (default)
		* Ids.CREATOR_ALL_ACL
		* Ids.READ_ACL_UNSAFE
		* Ids.ANYONE_ID_UNSAFE
		* Ids.AUTH_IDS
	+ eg:
		* `String createdPath=zkClient.create().creatingParentsIfNeeded().forPath(path,dataBytes)`
		* `String createdPath=zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).withACL(Ids.OPEN_ACL_UNSAFE).forPath(path,dataBytes)`
- Read: `getData()`
	+ 不存在则抛出`NoNodeException`
	+ 返回值是`byte[]`
	+ `storingStatIn(stat)` 获取到该节点的stat
	+ eg:
		* `byte[] data=zkClient.getData().forPath(path)`
		* `byte[] data=zkClient.getData().storingStatIn(stat).forPath(path)` 
- Update: `setData()` 
	+ 不存在则抛出`NoNodeException`
	+ 返回一个`Stat`实例
	+ `withVersion(num)` 强制指定版本进行更新
	+ eg:
		* `Stat stat=zkClient.setData().forPath(path,dataBytes)`
		* `Stat stat=zkClient.setData().withVersion(0).forPath(path,dataBytes)` (Version不匹配则抛出`BadVersionException`)
- Delete: `delete()`
	+ 不存在则抛出`NoNodeException`
	+ 只能删除叶子节点
	+ `guaranteed()` 保障措施, 如果删除失败，那么在后端还是继续会删除，直到成功
	+ `deletingChildrenIfNeeded()` 	递归删除其所有的子节点
	+ `withVersion(num)` 强制指定版本进行删除
	+ eg:
		* `zkClient.delete().guaranteed().deletingChildrenIfNeeded().withVersion(12).forPath(path)`  
- More: 
	+ `checkExists()` 检查节点是否存在，不存在则返回为`null`，eg:
		* `Stat stat=zkClient.checkExists().forPath(path)`
		* `client.checkExists().creatingParentContainersIfNeeded().forPath(path)`
	+ `getChildren()` 获取某个节点的所有子节点路径,eg:
		* `List<String> childrenPathList=zkClient.getChildren().forPath(zkPath)`

		
### Watch

#### `usingWatcher(watcher)`: 

+ Target: 监听当前节点，对子节点无效!
+ Times: 只触发一次，监听完毕后就销毁
+ Watcher: `interface CuratorWatcher`
+ Usage:
	* `getData().usingWatcher(watcher).forPath(watchPath)`
		- 监听节点(watchPath)需存在，否则报`NoNodeException`
		- 可监听到的EventType: `NodeDeleted`,`NodeDataChanged`
	* `checkExists().usingWatcher(watcher).forPath(watchPath)`
		- 监听节点(watchPath)可不存在
		- 可监听到的EventType: `NodeCreated`,`NodeDeleted`,`NodeDataChanged`


```java
public interface CuratorWatcher{
	public void process(WatchedEvent event) throws Exception;	// event.getPath(),getType(),getState(),...
}
```
	
#### `NodeCache`:

+ Target: 监听当前节点，对子节点无效！
+ Times: 一次注册，n次监听 (监听节点可不存在)
+ Listener: `interface NodeCacheListener`

```java
public interface NodeCacheListener{
	public void nodeChanged() throws Exception;
}
```

```java
NodeCache nodeCache = new NodeCache(zkClient, watchPath);
nodeCache.getListenable().addListener(new NodeCacheListener() {
	@Override
	public void nodeChanged() throws Exception {
		// ChildData data=nodeCache.getCurrentData();	 ChildData#getPath(),getData(),getStat() -- get watchPath node information
	}
});
nodeCache.start(true); 						// 启动（buildInitial: true/false）, 注：启动不会触发watcher
ChildData data=nodeCache.getCurrentData();	// get watchPath node information
```	

#### `PathChildrenCache`: 

+ Target: 监听节点的一级子节点的CUD，注：
	* 监听节点不会触发watcher，监听节点可不存在
	* 若监听节点删除，则监听失效，子节点的变化将不会触发watcher
+ Times: 一次注册，n次监听
+ Listener: `interface PathChildrenCacheListener`

```java
public interface PathChildrenCacheListener{
	 public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception;
}
```

```java
PathChildrenCache pathChildrenCache=new PathChildrenCache(zkClient, watchPath, true);	// cacheData:true/false (true则Client能够获取到节点数据内容)
pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		// event.getType()			// Type.CHILD_ADDED,CHILD_UPDATED,CHILD_REMOVED,CONNECTION_SUSPENDED,CONNECTION_RECONNECTED,CONNECTION_LOST,INITIALIZED
		// event.getData()			// ChildData, ChildData#getPath,getData,getStat	-- 获取触发Event的节点信息
		// event.getInitialData()	// List<ChildData>	 -- get initial data when triggered Type.INITIALIZED				
	}
});
pathChildrenCache.start(StartMode.BUILD_INITIAL_CACHE);						// 启动
// StartMode 为初始的cache设置暖场方式
// StartMode.NORMAL						异步初始化(default mode), Note: initial not trigger watcher
// StartMode.POST_INITIALIZED_EVENT		异步初始化, Cache初始化数据后发送一个PathChildrenCacheEvent.Type#INITIALIZED事件
// StartMode.BUILD_INITIAL_CACHE		同步初始化, start方法返回之前调用rebuild(), Note: initial not trigger watcher

List<ChildData> childDataList= pathChildrenCache.getCurrentData();			//  get watch node (watchPath's child nodes) information
```	

#### `TreeCache`: 

+ Target: 监听节点和该节点的所有子节点 （创建时可设置`maxDepth`）
+ Times: 一次注册，n次监听
+ Listener: `interface TreeCacheListener`

```java
public interface TreeCacheListener{
	public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception;
}
```

```java
TreeCache treeCache=new TreeCache(zkClient, watchPath,false);		// cacheData：true/false
treeCache.getListenable().addListener(new TreeCacheListener() {
	@Override
	public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
		// event.getType()		Type.NODE_ADDED,NODE_UPDATED,NODE_REMOVED,CONNECTION_SUSPENDED,CONNECTION_RECONNECTED,CONNECTION_LOST,INITIALIZED
		// event.getData() 		ChildData,ChildData#getPath,getData,getStat -- 获取触发Event的节点信息
	}
});
treeCache.start();															// 启动
ChildData childData=treeCache.getCurrentData(watchPath);					// get watch node (watchPath node) information
Map<String,ChildData> childMap=treeCache.getCurrentChildren(watchPath);		// get watch node (watchPath's child nodes) information
```		


### ACL 权限控制

| schema | id | 
|:-|:-|
| world | 只有一个ID：`anyone` (任何人都拥有所有权限) |
| ip | ip地址或ip段 |
| auth | "", 使用已添加认证的用户认证,eg: `digest:username:password` |
| digest | `用户名:加密密码` 通常是`username:BASE64(SHA-1(username:password))` |

- 使用：`schema`:`id`:`permission` 来标识
	+ `schema`:`id`
		* `schema`: 权限模式(鉴权的策略)
		* `id`: 授权对象,即权限赋予的用户或者一个实体
	+ `permission`: 权限
		* CREATE : 	c 	可以创建子节点
		* DELETE : 	d 	可以删除子节点（仅下一级节点）
		* READ : 	r 	可以读取节点数据及显示子节点列表
		* WRITE : 	w 	可以设置节点数据
		* ADMIN :	a 	可以设置节点访问控制列表权限
- 特性：
	+ ZooKeeper的权限控制是基于每个znode节点的，需要对每个节点设置权限
	+ 每个znode支持设置多种权限控制方案和多个权限
	+ 子节点不会继承父节点的权限，客户端无权访问某节点，但可能可以访问它的子节点
	

### cmd示例：

+ world
```bash
# setAcl <path> world:anyone:<permission>
$ setAcl /node1 world:anyone:cdrwa
$ getAcl /node1
```

+ ip
```bash
# setAcl <path> ip:<ip>:<permission>
$ setAcl /node2 ip:192.168.100.1:cdrwa
$ getAcl /node2
```

+ auth
```bash
# addauth digest <user>:<password> 				# 添加认证用户
# setAcl <path> auth:<user>:<permission> 		# 设置ACL
$ addauth digest tom:123456
$ setAcl /node3 auth:tom:cdrwa
```

+ digest
```bash
# echo -n <user>:<password> | openssl dgst -binary -sha1 | openssl base64   # password：经SHA1及BASE64处理的密文
# setAcl <path> digest:<user>:<password>:<permission>
$ echo -n tom:123456 | openssl dgst -binary -sha1 | openssl base64
3YvKnq60bERLJOlabQFeB1f+/n0=
$ setAcl /node4 digest:tom:3YvKnq60bERLJOlabQFeB1f+/n0=:cdrwa
```

#### Curator

+ `new ACL(perms,id)`
	* parameter1: `Perms.xxx`
		 + Perms.ADMIN	-- 可以修改节点权限(setAcl)
		 + Perms.READ	-- 可读取节点（ls,get）
		 + Perms.WRITE	-- 可修改节点内容 (set)
		 + Perms.CREATE -- 可创建节点 (create)
		 + Perms.DELETE -- 可删除节点 (delete)
	* parameter2: `new org.apache.zookeeper.data.Id(String schema,String id)`
	* eg: 
		+ `Id id1=new Id("digest",DigestAuthenticationProvider.generateDigest("id01:12345"));`
		+ `ACL aclRW=new ACL(Perms.READ|Perms.WRITE,id1);`
	* Util: `org.apache.zookeeper.ZooDefs.Ids` 提供一些常用的Id和ACL实例
		- `ANYONE_ID_UNSAFE = new Id("world", "anyone")`
		- `AUTH_IDS = new Id("auth", "")`
		- `OPEN_ACL_UNSAFE = new ArrayList<ACL>(Collections.singletonList(new ACL(Perms.ALL, ANYONE_ID_UNSAFE)))`
		- `CREATOR_ALL_ACL = new ArrayList<ACL>(Collections.singletonList(new ACL(Perms.ALL, AUTH_IDS)))`
		- `READ_ACL_UNSAFE = new ArrayList<ACL>(Collections.singletonList(new ACL(Perms.READ, ANYONE_ID_UNSAFE)))`
		
+ `.aclProvider(aclProvider)` same as zkClient cmd `setAcl path acl` 
	* `interface ACLProvider`
		+ `public List<ACL> getDefaultAcl();`
		+ `public List<ACL> getAclForPath(String path);`
	* `.aclProvider(new DefaultACLProvider())` default,使用ZooDefs.Ids.OPEN_ACL_UNSAFE
	* `.aclProvider(new ACLProvider(){ ... })` override getDefaultAcl,getAclForPath	

+ `.authorization(authInfoList)` : same as zkClient cmd `addauth sechema auth` ( eg: `addauth digest id02:12345` )
	* `new AuthInfo(String scheme, byte[] auth)`
	* eg: `new AuthInfo("digest", "id01:12345".getBytes())`
	
+ `.withACL()`: same as zkClient cmd `setAcl path acl` 
	+ `setACL().withACL(aclList).forPath(path)`
	+ `create().withACL(aclList).forPath(path)`

+ Note:
	* `setACL()` 需要Admin权限 (Perms.ADMIN)
	* `getACL()` 无需认权限证
