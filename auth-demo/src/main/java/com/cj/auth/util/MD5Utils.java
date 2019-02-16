package com.cj.auth.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

public class MD5Utils {
	 public static String getMD5Str(String strValue) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			String newstr = Base64.encodeBase64String(md5.digest(strValue.getBytes()));
			return newstr;
		} catch (NoSuchAlgorithmException e) {
			//e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return strValue;
	}
}
