
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


## 应用示例

### Description

Two Parts:
- Part 1: FileServer -> CRUD files
- Part 2: ClientUser -> Auto download from FileServer / delete local file depends on the files updates on FileServer.

- admin(`/admin`): FileServer
	+ upload file: POST `/images` -> upload(MultipartFile photo)
		* save file with name `{image.id}.{image.type}` on local fileLocation 
		* save file information on DB
		* add zk node: `/{image.id}_{PERSISTENT_SEQUENTIAL}`: `ADD:{image.id}:{image.type}:{image.originalName}`
	+ delete file: DEL `images/{id}` -> delete(id)
		* delete file information on DB
		* add zk node: `/{image.id}_{PERSISTENT_SEQUENTIAL}`: `DEL:{image.id}:{image.type}:{image.originalName}`
	+ list all files information: GET `/images` -> list()
	+ get file information by id: GET `/images/{id}` -> get(id)
	+ config:
		* fileLocation: `/Users/cj/space/java/admin-uploads`
		* zk
			+ server: 127.0.0.1:2181
			+ namespace: demo

- user(`/user`): Client User
	+ get local file: GET `/images/{filename}` -> get(filename)
	+ zk listen `/`:`CHILD_ADDED`: 
		* `ADD:{filename}:{extension}:{originalName}` -> download file from fileServer to local,then del this child node
		* `DEL:{filename}:{extension}:{originalName}` -> delete local file,then del this child node
	+ config:
		* local fileLocation `/Users/cj/space/java/user-downloads`
		* remote fileServer `http://localhost:8080/zookeeper-demo/admin/images`
		* zk
			+ server: 127.0.0.1:2181
			+ namespace: demo
- verify:
```bash
# upload: POST /admin/images
$ curl -i -F 'photo=@/Users/cj/Pictures/design/极光2.jpg' -X POST http://localhost:8080/zookeeper-demo/admin/images 

# list: GET /admin/images
$ curl -i -H "Content-Type: application/json" http://localhost:8080/zookeeper-demo/admin/images

# get: GET /admin/images/{id}
$ curl -i -H "Content-Type: application/json" http://localhost:8080/zookeeper-demo/admin/images/1d0fa647-9dbe-410a-8d9c-0e1c973a98e2

# delete: DELETE /admin/images/{id}
$ curl -i -H "Content-Type: application/json" -X DELETE http://localhost:8080/zookeeper-demo/admin/images/1d0fa647-9dbe-410a-8d9c-0e1c973a98e2

# visit: 
# /user/images/1d0fa647-9dbe-410a-8d9c-0e1c973a98e2.jpg

# More (for test)
# /light.jpg
# /admin/images
# /admin/images/test1/虫洞.gif
# /admin/images/test2/极光1.jpg
```	

### Dependencies

pom.xml

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-test</artifactId>
	<scope>test</scope>
</dependency>

<!-- Curator (include zookeeper) -->
<!-- Note: need to change zookeeper version,the beta version zookeeper has issues -->
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
	<exclusions>
		<exclusion>
			  <groupId>org.slf4j</groupId>
			  <artifactId>slf4j-api</artifactId>
		</exclusion>
		<exclusion>
			 <groupId>org.slf4j</groupId>
			 <artifactId>slf4j-log4j12</artifactId>
		</exclusion>
	</exclusions>
</dependency>

<!-- FileUpload -->
<dependency>
    <groupId>commons-fileupload</groupId>
    <artifactId>commons-fileupload</artifactId>
    <version>1.3.1</version>
</dependency>
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.5</version>
</dependency>
```

### Config

application.yml
```
server:
  port: 8080
  servlet:
    context-path: /zookeeper-demo
  
zk:
  server: 127.0.0.1:2181
  namespace: demo

# for admin part:  
admin:
  fileLocation: /Users/cj/space/java/admin-uploads

# for user part:
user:
  fileLocation: /Users/cj/space/java/user-downloads
  fileServer: http://localhost:8080/zookeeper-demo/admin/images
```

### Admin(FileServer) Part

ImageAdminController.java

```java
@RestController
@RequestMapping("/admin/images")
public class ImageAdminController {

	@Value("${admin.fileLocation}")
	private String fileLocation;
	
	@Autowired
	private ImageService imageService;
	
	@Resource(name="zkService")
	private ZkCuratorService zkService;
	
	@PostMapping
	public Object upload(@RequestParam(value="photo") MultipartFile photo) throws Exception{
 
		System.out.println("Name:"+photo.getName());							// photo
		System.out.println("OriginalFilename:"+photo.getOriginalFilename());	// 极光2.jpg
		System.out.println("Size:"+photo.getSize());							// 186316
		System.out.println("ContentType:"+photo.getContentType());				// image/jpeg
		
        Image image=new Image(UUID.randomUUID().toString(),
        		photo.getOriginalFilename(),"Active");
        String destFilePath=this.fileLocation+"/"+image.getId()+"."+image.getType();
        System.out.println("dest:"+destFilePath);
        FileUtils.copyToFile(photo.getInputStream(), new File(destFilePath));
        boolean result=this.imageService.save(image);
        
        // zookeeper
        String data="ADD:"+image.getId()+":"+image.getType()+":"+image.getOriginalName();
        String zkPath=zkService.create("/"+image.getId(), data.getBytes());
        System.out.println("create zkPath(for ADD):"+zkPath);
        
		return ResponseEntity.ok(result);
	}
	
	@DeleteMapping("/{id}")
	public Object delete(@PathVariable("id") String id) throws Exception{
		Image image=this.imageService.delete(id);
		
		// zookeeper
		if(image!=null){
			String data="DEL:"+image.getId()+":"+image.getType()+":"+image.getOriginalName();
	        String zkPath=zkService.create("/"+image.getId(), data.getBytes());
	        System.out.println("create zkPath(for DEL):"+zkPath);
		}
        
		return ResponseEntity.ok(image!=null);
	}
	
	@GetMapping
	public Object list(){
		return ResponseEntity.ok(this.imageService.list());
	}
	
	@GetMapping("/{id}")
	public Object get(@PathVariable("id")String id) throws IOException{
		Image image=this.imageService.get(id);
		if(image==null || !"Active".equals(image.getStatus()) 
				|| StringUtils.isEmpty(image.getType()) || "Unknow".equals(image.getType()))
			return ResponseEntity.ok("Not Available!");
		
		FileInputStream in=new FileInputStream(this.fileLocation+"/"+image.getId()+"."+image.getType());
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_JPEG)
				.contentType(MediaType.IMAGE_PNG)
				.contentType(MediaType.IMAGE_GIF)
				.body(IOUtils.toByteArray(in));
	}

	/* ------ For Test: -------- */
	@GetMapping(value="/test1/{name}"
			,produces={MediaType.IMAGE_JPEG_VALUE,MediaType.IMAGE_PNG_VALUE,MediaType.IMAGE_GIF_VALUE})
	public Object getImageByName1(@PathVariable("name") String filename) throws IOException{
		String filePath="/Users/cj/Pictures/design";
		FileInputStream in = new FileInputStream(filePath+"/"+filename);
		return IOUtils.toByteArray(in);	// or ResponseEntity.ok(IOUtils.toByteArray(in));
	}
	@GetMapping(value="/test2/{name}")
	public Object getImageByName2(@PathVariable("name") String filename) throws IOException{
		String filePath="/Users/cj/Pictures/design";
		FileInputStream in = new FileInputStream(filePath+"/"+filename);
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_JPEG)
				.contentType(MediaType.IMAGE_PNG)
				.contentType(MediaType.IMAGE_GIF)
				.body(IOUtils.toByteArray(in));
	}
	@PostConstruct
	private void init() throws Exception{
		System.out.println("init:"+this.fileLocation);
		FileUtils.forceMkdir(new File(this.fileLocation));
		
		String id="1d0fa647-9dbe-410a-8d9c-0e1c973a98e2";
    	Image image=new Image(id,"light.jpg","Active");
    	FileUtils.copyFile(new File("/Users/cj/Pictures/design/极光1.jpg"), new File(this.fileLocation+"/"+id+".jpg"));
    	this.imageService.save(image);
    	
    	// zookeeper
    	String data="ADD:"+image.getId()+":"+image.getType()+":"+image.getOriginalName();
        String zkPath=zkService.create("/"+image.getId(), data.getBytes());
        System.out.println("create zkPath(for ADD):"+zkPath);
	}
	@PreDestroy
	private void clear() throws Exception{
		FileUtils.cleanDirectory(new File(this.fileLocation));
		// zookeeper
		zkService.delete("/");
	}
}
```

### User(ClientUser) Part

ImageUserController

```java
@RestController
@RequestMapping("/user/images")
public class ImageUserController {
	
	@Value("${user.fileLocation}")
	private String fileLocation;
	
	@Value("${user.fileServer}")
	private String fileServer;
	
	@Resource(name="zkClientService")
//	@Resource(name="zkService")
	private ZkCuratorService zkService;
	
	@GetMapping("/{filename}")
	public Object get(@PathVariable("filename") String filename) throws IOException{
		FileInputStream in=new FileInputStream(this.fileLocation+"/"+filename);
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_JPEG)
				.contentType(MediaType.IMAGE_PNG)
				.contentType(MediaType.IMAGE_GIF)
				.body(IOUtils.toByteArray(in));
	}
	
	@PostConstruct
	public void init() throws Exception{
		PathChildrenCacheListener listener=new PathChildrenCacheListener(){
			@Override
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) 
					throws Exception {
				System.out.println("Get Event:"+event.getType());
				
				if(event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)){
					ChildData child=event.getData();
					if(child==null)
						return;
					String data=new String(child.getData());
					String array[]=data.split(":");
					String filename= array[1]+"."+array[2];
					System.out.println(child.getPath()+":"+data);
					
					if("ADD".equals(array[0])){
						try{
							Thread.sleep(3000);		// ! for test and connect refused exception
							FileUtils.copyURLToFile(new URL(fileServer+"/"+filename)
									, new File(fileLocation+"/"+filename));
							zkService.delete(child.getPath());
						}catch(ConnectException ex){
							System.out.println(ex.getMessage());
						}
					}else if("DEL".equals(array[0])){
						try{
							FileUtils.forceDelete(new File(fileLocation+"/"+filename));
						}catch(FileNotFoundException ex){
							System.out.println("not exist:"+fileLocation+"/"+filename);
						}
						zkService.delete(child.getPath());
					}
				}
			}
		};
		zkService.addPathChildrenWatcher("/", true, listener);
	}
	
	@PreDestroy
	public void clear() throws Exception{
		FileUtils.cleanDirectory(new File(this.fileLocation));
		// zookeeper
		zkService.delete("/");
	}
}
```

### Common: Service & Entity

1. ImageService
```java
@Service
public class ImageService {
	private final ConcurrentMap<String,Image> images=new ConcurrentHashMap<String,Image>();
    
    public boolean save(Image image){
        return images.put(image.getId(),image)==null;
    }
    public Image delete(String id){
    	Image image=images.get(id);
    	if(image==null)
    		return null;
    	image.setStatus("InActive");
    	images.replace(id, image);
    	return image;
    }
    public Image get(String id){
    	return images.get(id);
    }
    public Collection<Image> list(){
        return images.values();
    }   
}
```

2. ZkCuratorService
```java
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
```

3. Entity: Image
```java
public class Image {
	private String id;
	private String originalName;
	private String type;
	private String status;
	
	public Image(){}
	
	public Image(String id,String originalName,String status){
		this.id=id;
		this.originalName=originalName;
		this.status=status;
		int pos=originalName.lastIndexOf(".");
		this.type=(pos!=-1?originalName.substring(pos+1):"Unknow");
	}

	/* getter & setter .... */
}
```

### main

```java
@SpringBootApplication
@Configuration
public class App {
    public static void main( String[] args ){
        SpringApplication.run(App.class, args);
    }
    
    @Bean(name="zkService")
    public ZkCuratorService zkService(){
    	return new ZkCuratorService();
    }
    
    @Bean(name="zkClientService")
    public ZkCuratorService zkClientService(){
    	return new ZkCuratorService();
    }
}
```

