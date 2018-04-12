package com.guohuai.component.util;

import java.net.InetAddress;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("static-access")
public class IPUtil {
	
	/**
	 * 获取本机ip
	 * @return
	 */
	public static String getHostIP() {
		String localip = "";
		InetAddress ia = null;
        try {
            ia=ia.getLocalHost();
            localip=ia.getHostAddress();
            log.info("本机的ip是 ："+localip);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		return localip;
	}
	
	/**
	 * 获取本机名称
	 * @return
	 */
	public static String getHostName() {
		String localname = "";
		InetAddress ia=null;
        try {
            ia=ia.getLocalHost();
            localname=ia.getHostName();
            log.info("本机名称是：{}", localname);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		return localname;
	}
}