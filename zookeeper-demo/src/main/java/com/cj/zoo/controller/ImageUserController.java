package com.cj.zoo.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cj.zoo.service.ZkCuratorService;

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
