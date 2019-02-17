package com.cj.zoo.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cj.zoo.entity.Image;
import com.cj.zoo.service.ImageService;
import com.cj.zoo.service.ZkCuratorService;

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

	
	/* ------ Test -------- */
	
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
