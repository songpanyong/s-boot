package com.guohuai.component.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.guohuai.component.exception.SETException;

@SuppressWarnings("static-access")
public class DateUtil {
	
	/**
	 * String yyyymmdd to Date
	 * @param dateString
	 * @return
	 */
	public static Date stringToDate(String dateString){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");  
		Date date = null;
		try {
			date = sdf.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}
	
	/**
	 * 获取当前日期的第二天
	 * @param date
	 * @return
	 */
	public static Date getNextDay(Date date){
	    Calendar calendar = new GregorianCalendar(); 
	    calendar.setTime(date);
	    calendar.add(calendar.DATE,1);//把日期往后增加一天.整数往后推,负数往前移动 
	    date=calendar.getTime();//这个时间就是日期往后推一天的结果
		return date;
	}
	
	/**
	 * 获取当前日期的前一天
	 * @param date
	 * @return
	 */
	public static Date getLastDay(Date date){
	    Calendar calendar = new GregorianCalendar(); 
	    calendar.setTime(date);
	    calendar.add(calendar.DATE,-1);//把日期往后增加一天.整数往后推,负数往前移动 
	    date=calendar.getTime();//这个时间就是日期往后推一天的结果
		return date;
	}
	
	/**
	 * 获取指定类型的上一天时间
	 * @param date
	 * @return
	 */
	public static String getLastDay(String pattern){
	    Calendar calendar = new GregorianCalendar(); 
	    calendar.setTime(calendar.getTime());
	    calendar.add(calendar.DATE,-1);//把日期往后增加一天.整数往后推,负数往前移动 
	    return format(calendar.getTimeInMillis(),pattern);//这个时间就是日期往后推一天的结果
	}
	
	public static String format(long timestamp, String pattern) {
		return new SimpleDateFormat(pattern).format(new Date(timestamp));
	}
	
	public static Date parseDate(String date, String pattern) {
		try {
			return new SimpleDateFormat(pattern).parse(date);
		} catch (ParseException e) {
			throw SETException.getException(10002, date);
		}
	}
	/**
	 * 获取当前日期 格式:XXXX-XX-XX
	 * 
	 * @param date
	 * @return
	 */
	public static java.sql.Date getCurrSqlDate(Timestamp time) {
		String sdate = format(time.getTime(), Constant.fomat);
		return new java.sql.Date(parseDate(sdate, Constant.fomat).getTime());
	}
	
	public static void main(String[] args) {
		System.out.println(getLastDay("yyyy-MM-dd"));
	}
	
	/**
	 * 判断最新通知订单和最新异常订单时间差
	 * @param sendTimeInterval
	 * @param receiveTime
	 * @param receiveTime2
	 * @return
	 */
	public static boolean judgeTimeDifference(Timestamp lastReceiveTime, Timestamp newReceiveTime,
			int sendTimeInterval) {
		// Timestamp转Date
		Date lastReceiveDate = new Date(lastReceiveTime.getTime());
		// Date转Calendar
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(lastReceiveDate);
		// Calendar加上时间
		calendar.add(Calendar.HOUR, +sendTimeInterval);
		// Calendar转Timestamp
		Timestamp time = new Timestamp(calendar.getTimeInMillis());
		// 判断是否过期，true就是超过
		if (newReceiveTime.after(time)) {
			return true;
		}
		return false;
	}
}