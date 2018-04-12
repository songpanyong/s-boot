package com.guohuai.boot.account.controller;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;

import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.Like;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.CreatePasswordRequest;
import com.guohuai.account.api.request.CreateUserRequest;
import com.guohuai.account.api.request.ModifyPasswordRequest;
import com.guohuai.account.api.request.SaveUserForm;
import com.guohuai.account.api.request.UserQueryRequest;
import com.guohuai.account.api.request.ValidatePasswordRequest;
import com.guohuai.account.api.response.CreatePasswordResponse;
import com.guohuai.account.api.response.CreateUserResponse;
import com.guohuai.account.api.response.FinanceUserResp;
import com.guohuai.account.api.response.ModifyPasswordResponse;
import com.guohuai.account.api.response.UserListResponse;
import com.guohuai.account.api.response.ValidatePasswordResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.ext.web.PageResp;
import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.UserInfoService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/account/user")
public class UserInfoController {

	@Autowired
	private UserInfoService userInfoService;

	/**
	 * 创建用户
	 */
	@RequestMapping(value = "/add",method = RequestMethod.POST)
	public CreateUserResponse addUser(@RequestBody CreateUserRequest req) {
		CreateUserResponse resp = userInfoService.addUser(req); 
		return resp;
	}
	
	/**
	 * 设置密码
	 */
	@RequestMapping(value = "/setpassword",method = RequestMethod.POST)
	public CreatePasswordResponse setPassword(@RequestBody CreatePasswordRequest req) {
		CreatePasswordResponse resp = userInfoService.setPassword(req); 
		return resp;
	}
	
	/**
	 * 修改密码
	 */
	@RequestMapping(value = "/modifypassword",method = RequestMethod.POST)
	public ModifyPasswordResponse modifyPassword(@RequestBody ModifyPasswordRequest req) {
		ModifyPasswordResponse resp = userInfoService.modifyPassword(req); 
		return resp;
	}
	
	/**
	 * 校验密码
	 */
	@RequestMapping(value = "/validatepassword",method = RequestMethod.POST)
	public ValidatePasswordResponse validatePassword(@RequestBody ValidatePasswordRequest req) {
		ValidatePasswordResponse resp = userInfoService.validatePassword(req); 
		return resp;
	}
	
	/**
	 * 查询用户
	 */
	@RequestMapping(value = "/userlist",method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<UserListResponse> userQueryList(@RequestBody UserQueryRequest req){
		Specification<UserInfoEntity> spec = new Specification<UserInfoEntity>() {
			@Override
			public Predicate toPredicate(Root<UserInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.isNotNull(root.get("userOid").as(String.class));
			}
		};
		spec = Specifications.where(spec);
		
		final String systemUid = req.getSystemUid();
		final String systemSource = req.getSystemSource();
		final String userType = req.getUserType(); 
		
		if(!StringUtil.isEmpty(systemUid)) {
			Specification<UserInfoEntity> systemUidSpec = new Specification<UserInfoEntity>() {
				public Predicate toPredicate(Root<UserInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
					return cb.equal(root.get("systemUid").as(String.class), systemUid);
				}
			};
			
			spec = Specifications.where(spec).and(systemUidSpec);
		}
		if(!StringUtil.isEmpty(systemSource)) {
			Specification<UserInfoEntity> systemSourceSpec = new Specification<UserInfoEntity>() {
				public Predicate toPredicate(Root<UserInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
					return cb.equal(root.get("systemSource").as(String.class), systemSource);
				}
			};
			spec = Specifications.where(spec).and(systemSourceSpec);
		}
		if(!StringUtil.isEmpty(userType)) {
			Specification<UserInfoEntity> userTypeSpec = new Specification<UserInfoEntity>() {
				public Predicate toPredicate(Root<UserInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
					return cb.equal(root.get("userType").as(String.class), userType);
				}
			};
			spec = Specifications.where(spec).and(userTypeSpec);
		}
		
		UserListResponse resp = userInfoService.userQueryList(spec);
		
		return new ResponseEntity<UserListResponse>(resp, HttpStatus.OK);
	}
	
	 /**
     * 新加用户
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ResponseBody
    public CreateUserResponse save(SaveUserForm form) {
    	CreateUserResponse resp = this.userInfoService.save(form);
		return resp;
    }


    /**
     * 更新用户
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    public CreateUserResponse update(SaveUserForm form) {
    	CreateUserResponse resp = this.userInfoService.update(form);
		return resp;
    }
    
    /**
     * 详情
     */
    @RequestMapping(value = "/detail", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<FinanceUserResp> detail(@RequestParam(required = true) String oid) {
    	FinanceUserResp pr = this.userInfoService.read(oid);
        return new ResponseEntity<FinanceUserResp>(pr, HttpStatus.OK);
    }

    /**
     * 查询用户列表
     */
    @RequestMapping(value = "/list", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<PageResp<FinanceUserResp>> list(HttpServletRequest request,
    		@And({@Spec(path = "userOid", params = "userOid", spec = Like.class),
				@Spec(path = "name", params = "name", spec = Like.class),
				@Spec(path = "idCard", params = "idCard", spec = Like.class),
				@Spec(path = "phone", params = "phone", spec = Like.class),
				@Spec(path = "userType", params = "userType", spec = Equal.class) }) Specification<UserInfoEntity> spec,
			@RequestParam int page, @RequestParam int rows,
			@RequestParam(required = false, defaultValue = "updateTime") String sort,
			@RequestParam(required = false, defaultValue = "desc") String order) {

		if (page < 1) {
            page = 1;
        }
        if (rows < 1) {
            rows = 1;
        }

        Direction sortDirection = Direction.DESC;
        if (!"desc".equals(order)) {
            sortDirection = Direction.ASC;
        }
        
        spec = Specifications.where(spec);

        Pageable pageable = new PageRequest(page - 1, rows, new Sort(new Order(sortDirection, sort)));
        PageResp<FinanceUserResp> rep = this.userInfoService.list(spec, pageable);
        return new ResponseEntity<PageResp<FinanceUserResp>>(rep, HttpStatus.OK);
    }
    
    /**
     * 查询用户下拉列表
     */
    @RequestMapping(value = "/getUsers", method = {RequestMethod.POST})
    public @ResponseBody ResponseEntity<Response> getUsers() {
        List<JSONObject> jsonList = userInfoService.getUsers();
        Response r = new Response();
        r.with("rows", jsonList);
        return new ResponseEntity<Response>(r, HttpStatus.OK);
    }
    

    /**
	 * 修改用户手机号
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/modifyPhone",method = RequestMethod.POST)
	public  CreateUserResponse modifyPhone (@RequestBody  CreateUserRequest request) {
		log.info("修改用户手机号请求 CreateUserRequest =[{}] ",JSONObject.toJSON(request));
		CreateUserResponse orderResponse = new CreateUserResponse();
		orderResponse = userInfoService.modifyPhone(request);
		log.info("修改用户手机号返回FindBindRequest =[{}] ",JSONObject.toJSON(orderResponse));
		return orderResponse;
	}
}
