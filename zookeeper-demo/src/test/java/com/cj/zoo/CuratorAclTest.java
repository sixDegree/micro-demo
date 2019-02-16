package com.cj.zoo;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.AuthInfo;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CuratorAclTest {

	public static String connectString="127.0.0.1:2181";
	public static String namespace="micro";
	public static String zkPath="/bb";
	private CuratorFramework zkClient=null;
	
	private ACL aclAdmin=null;
	private ACL aclRW=null;
	private ACL aclCD=null;
	
	
	@Before
	public void init() throws NoSuchAlgorithmException{
		
		Id id0=new Id("digest",DigestAuthenticationProvider.generateDigest("id00:admin"));
		Id id1=new Id("digest",DigestAuthenticationProvider.generateDigest("id01:12345"));
		Id id2=new Id("digest",DigestAuthenticationProvider.generateDigest("id02:12345"));
		
		aclAdmin=new ACL(Perms.ADMIN, id0);
		aclRW=new ACL(Perms.READ|Perms.WRITE,id1);
		aclCD=new ACL(Perms.CREATE|Perms.DELETE,id2);
	}
	
	@After
	public void close(){
		if(zkClient!=null){
			zkClient.close();
			
			CuratorFrameworkState state=zkClient.getState();
			System.out.println("Close zkClient :"+state.name());
		}
	}
	
	/********* ACL **************/
	
	/*
	 * 1. aclProvider/setAcl
	 * 
	 * same as zkClient:
	 * setAcl path acl
	 * 
	 * 2. authorization
	 * 
	 * same as zkClient:
	 * addauth sechema auth
	 * eg:
	 * addauth digest id02:12345
	 * 
	 * 3. Perm
	 * Perms.ADMIN	-- 可以修改节点权限(setAcl)
	 * Perms.READ	-- 可读取节点（ls,get）
	 * Perms.WRITE	-- 可修改节点内容 (set)
	 * Perms.CREATE -- 可创建节点 (create)
	 * Persm.DELETE -- 可删除节点 (delete)
	 * 
	 * 4. getAcl -- 无需认权限证
	 * 
	 * */
	@Test
	public void aclProviderTest() throws Exception{
		
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(3000, 3);
		zkClient=CuratorFrameworkFactory.builder()
				//.aclProvider(new DefaultACLProvider())	// default
				.aclProvider(new ACLProvider() {	// Note: 在节点创建时触发调用，为新建的节点设置权限
					@Override
					public List<ACL> getDefaultAcl() {
						System.out.println("setting default acl...");
						return ZooDefs.Ids.OPEN_ACL_UNSAFE;
					}
					@Override
					public List<ACL> getAclForPath(String path) {
						System.out.println("setting acl...");
						// 为zkPath节点和其以下的所有子节点设置acl
						if(path.startsWith("/"+namespace+zkPath)){	// note: namespace+zkPath
							System.out.println("setting aclAdmin,aclRW,aclCD...");
							return Arrays.asList(aclAdmin,aclRW,aclCD);
						}
						else
							return ZooDefs.Ids.OPEN_ACL_UNSAFE;
					}
				})
				.connectString(connectString)
				.retryPolicy(retryPolicy)
				.sessionTimeoutMs(100000)
				.connectionTimeoutMs(100000)
				.namespace(namespace)
				.build();
		
		zkClient.start();
		
		CuratorFrameworkState state=zkClient.getState();
		System.out.println("Init zkClient :"+state.name());
		
		// case 1 -- Create -zkPath --> Success
		aclCreateTest();
	
		// case 2 -- Read and Write -zkPath	--> NoAuthException!!
//		aclReadWriteTest();
		
		// case 3 -- Delete -zkPath --> NoAuthException!!
		aclDeleteTest();
		
		// case 4 -- Create -zkPath --> Success
		aclCreateTest();
		
		System.out.println("child case...");
		
		// case 5 -- Delete and Create -zkPath child --> NoAuthException!!
//		aclDeleteChildTest();
//		aclCreateChildTest();
		
		// case 6 -- Read and Write -zkPath child	--> NoAuthException!!
//		aclReadWriteChildTest();
		
	}
	
	@Test
	public void authorizationTest() throws Exception{
		
		List<AuthInfo> authInfos = new ArrayList<AuthInfo>();
		authInfos.add(new AuthInfo("digest", "id00:admin".getBytes()));	// admin：修改权限认证
//		authInfos.add(new AuthInfo("digest", "id01:12345".getBytes()));	// rw:读写节点认证
		authInfos.add(new AuthInfo("digest", "id02:12345".getBytes()));	// cr:增删节点认证
				
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(3000, 3);
		zkClient=CuratorFrameworkFactory.builder()
				// Note: 
				// 不设置aclProvider，等同.aclProvider(new DefaultACLProvider())，即使用ZooDefs.Ids.OPEN_ACL_UNSAFE
				// 对于新创建的节点，acl为ZooDefs.Ids.OPEN_ACL_UNSAFE
//				.authorization(authInfos)	// 附上访问权限信息AuthInfo，用于访问节点认证
				.connectString(connectString)
				.retryPolicy(retryPolicy)
				.sessionTimeoutMs(100000)
				.connectionTimeoutMs(100000)
				.namespace(namespace)
				.build();
		
		zkClient.start();
		
		CuratorFrameworkState state=zkClient.getState();
		System.out.println("Init zkClient :"+state.name());
		
		//get acl -- 无需权限
		getACLTest();
		
		// set acl -zkPath 
		// --> 拥有admin权限认证时才可
		setACLTest();
		
//		getChildACLTest();
		
		// case 1: read & write -zkPath
		// --> 需拥有rw权限
		aclReadWriteTest();
		
		// case 2: create -zkPath child	
		// --> 需拥有c权限才可创建，新创建的节点未设置acl，则使用默认acl{world,anyone}
		aclCreateChildTest();
		
		// case 3: read & write -zkPath child
		aclReadWriteChildTest();
		// case 4: delete -zkPath child
		aclDeleteChildTest();
		
		// case 4: delete -zkPath
		// --> 需拥有d权限才可删除
		aclDeleteTest();
		
	}
	
	/************** SET/GET ACL **************/
	
	public void setACLTest() throws Exception{
		
		if(zkClient.checkExists().forPath(zkPath)!=null){
			System.out.println("Set ACL");
			Stat stat=zkClient.setACL().withACL(Arrays.asList(aclAdmin,aclRW,aclCD)).forPath(zkPath);
			System.out.println(stat.getVersion());
		}else{
			System.out.println("Create and Set ACL");
			zkClient.create().creatingParentsIfNeeded().withACL(Arrays.asList(aclAdmin,aclRW,aclCD)).forPath(zkPath,"access test".getBytes());
		}
		
		getACLTest();
	}
	
	public void getACLTest() throws Exception{
		
		if(zkClient.checkExists().forPath(zkPath)==null){
			System.out.println(zkPath+" not exist!");
			return;
		}
		
		System.out.println("list acl:");
		List<ACL> aclList=zkClient.getACL().forPath(zkPath);
		for(ACL acl:aclList)
			System.out.println(acl);
	}
	
	public void getChildACLTest() throws Exception{
		System.out.println("list child acl:");
		List<String> childList=zkClient.getChildren().forPath(zkPath);
		for(String child:childList){
			List<ACL> aclList=zkClient.getACL().forPath(zkPath+"/"+child);
			System.out.println(child+" acl: ");
			for(ACL acl:aclList)
				System.out.println("\t"+acl);
		}
	}
	
	/************ CRUD **************/
	
	public void aclReadWriteTest() throws Exception{
		
		System.out.println("aclReadWriteTest....");
		
		if(zkClient.checkExists().forPath(zkPath)==null){
			System.out.println(zkPath+" not exist!");
			return;
		}
		
		//read
		System.out.println("read..."+zkPath);
		byte[] data=zkClient.getData().forPath(zkPath);
		if(data!=null)
			System.out.println(new String(data));
		
		//write
		System.out.println("write..."+zkPath);
		Stat stat=zkClient.setData().forPath(zkPath,"access test-acl".getBytes());
		System.out.println(stat.getVersion());
	}
	
	public void aclCreateTest() throws Exception{
		System.out.println("acl create test...");
		
		if(zkClient.checkExists().forPath(zkPath)!=null){
			System.out.println(zkPath+" already exist!");
		}else{
			//create
			System.out.println("create..."+zkPath);
			zkClient.create().creatingParentsIfNeeded().forPath(zkPath,"access test-new".getBytes());
		}
		//print acl
		System.out.println("list acl..."+zkPath);
		List<ACL> aclList=zkClient.getACL().forPath(zkPath);
		for(ACL acl:aclList){
			System.out.println(acl);
		}
	}
	
	public void aclDeleteTest() throws Exception{
		
		System.out.println("aclDeleteTest....");
		
		if(zkClient.checkExists().forPath(zkPath)==null){
			System.out.println(zkPath+" not exist!");
		}else{
			//delete
			System.out.println("delete..."+zkPath);
			zkClient.delete().deletingChildrenIfNeeded().forPath(zkPath);
		}
	}
	
	public void aclReadWriteChildTest() throws Exception{
		
		System.out.println("aclReadWriteChildTest....");
		
		String accessPath=zkPath+"/a";
		
		//read
		System.out.println("read..."+accessPath);
		byte[] data=zkClient.getData().forPath(accessPath);
		if(data!=null)
			System.out.println(new String(data));
		
		//write
		System.out.println("write..."+accessPath);
		Stat stat=zkClient.setData().forPath(accessPath,"aaa-acl".getBytes());
		System.out.println(stat.getVersion());
	}
	
	public void aclCreateChildTest() throws Exception{
		System.out.println("aclCreateChildTest....");
		String accessPath=zkPath+"/a";
		
		if(zkClient.checkExists().forPath(accessPath)!=null){
			System.out.println(accessPath+" already exist!");
		}else{
			//create
			System.out.println("create..."+accessPath);
			zkClient.create().creatingParentsIfNeeded().forPath(accessPath,"aaa-new".getBytes());
		}
		
		//print acl
		System.out.println("list acl..."+accessPath);
		List<ACL> aclList=zkClient.getACL().forPath(accessPath);
		for(ACL acl:aclList){
			System.out.println(acl);
		}
	}
	
	public void aclDeleteChildTest() throws Exception{
		
		System.out.println("aclDeleteChildTest....");
		
		String accessPath=zkPath+"/a";
		
		if(zkClient.checkExists().forPath(accessPath)==null){
			System.out.println(accessPath+" not exist!");
		}else{
			//delete
			System.out.println("delete..."+accessPath);
			zkClient.delete().deletingChildrenIfNeeded().forPath(accessPath);
		}
		
	}
	
}
