package com.cj.zoo.service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

import com.cj.zoo.entity.Image;

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
