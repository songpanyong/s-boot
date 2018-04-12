package com.guohuai.component.util;

import java.io.Serializable;

import lombok.Data;
@Data
public class PageBase implements Serializable {

	/**
	* @Fields serialVersionUID : TODO
	*/
	private static final long serialVersionUID = 6472174806239578499L;
	
	private int page;
	private int rows;
	/**
	 * 排序字段
	 */
	private String sortField;
	/**
	 * 排序标记,asc,desc
	 */
	private String sort;
}
