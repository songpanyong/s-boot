package com.guohuai.component.common;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.guohuai.basic.component.ext.web.BaseController;
import com.guohuai.basic.component.ext.web.PageResp;

public class TemplateQueryController<T,R extends JpaSpecificationExecutor<T>> extends BaseController{
	
	@Autowired
	R repo;
	

	/**
	 * 根据RSQL表达式进行查询.
	 * 
	 * @param spec
	 * @return
	 */
	@RequestMapping(path="/spec", produces="application/json")
	public List<T> findBySpec(@RequestParam("filter") String spec) {
		return repo.findAll(RsqlSpecification.<T>rsql(spec));
	}
	
	@RequestMapping(path="/pagespec", produces="application/json")
	public PageResp<T> findBySpec(@RequestParam("filter") String spec, 
			@RequestParam(defaultValue="1") int page, 
			@RequestParam(defaultValue="10") int rows, 
			@RequestParam(defaultValue="") String sortby, @RequestParam(defaultValue="desc") String sortDir) {
		Sort sort = null;
		if (sortby.length()>0) {
			sort = new Sort(Sort.Direction.fromString(sortDir), sortby);
		}
		PageRequest pageInfo = null;
		if (sort != null) {
			pageInfo = new PageRequest(page-1, rows, sort);
		} else {
			pageInfo = new PageRequest(page-1, rows);
		}
		Page<T> aa = repo.findAll(RsqlSpecification.<T>rsql(spec), pageInfo);
		PageResp<T> resp =new PageResp<T>();
		resp.setTotal(aa.getTotalElements());
		resp.setRows(aa.getContent());
		return resp;
	}
	
}
