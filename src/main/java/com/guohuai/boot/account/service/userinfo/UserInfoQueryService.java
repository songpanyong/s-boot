package com.guohuai.boot.account.service.userinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.ValidatePasswordRequest;
import com.guohuai.account.api.response.FinanceUserResp;
import com.guohuai.account.api.response.UserListResponse;
import com.guohuai.account.api.response.ValidatePasswordResponse;
import com.guohuai.account.api.response.entity.UserDto;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.ext.web.PageResp;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.util.MD5Utils;
import com.guohuai.component.util.Constant;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 用户信息查询服务
 * @author ZJ
 * @date 2018年1月19日 下午5:48:22
 * @version V1.0
 */
@Slf4j
@Service
@Transactional
public class UserInfoQueryService {
	@Autowired
	private UserInfoDao userInfoDao;

	/**
	 * 校验密码
	 * @param @param
	 *            req
	 * @param @return
	 * @return ValidatePasswordResponse
	 * @throws @Title:
	 *             validatePassword
	 * @Description:
	 */
	public ValidatePasswordResponse validatePassword(ValidatePasswordRequest req) {
		// 返回参数
		ValidatePasswordResponse resp = new ValidatePasswordResponse();
		try {
			log.info("校验密码参数,userOid:{},md5Password:{}", req.getUserOid(), MD5Utils.encryptByMd5(req.getPassword()));

			String userOid = req.getUserOid();
			String password = req.getPassword();

			if (StringUtil.isEmpty(userOid)) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("userOid不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(password)) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("密码不能为空");
				return resp;
			}

			UserInfoEntity entity = userInfoDao.findByUserOid(userOid);
			if (entity != null) {
				// 原密码加密和数据库的密码比较
				String encryptPassword = MD5Utils.encryptByMd5(password);
				if (encryptPassword.equals(entity.getPassword())) {
					resp.setUserOid(userOid);
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("密码校验成功");
					return resp;
				} else {
					resp.setReturnCode(Constant.FAIL);
					resp.setErrorMessage("密码校验失败");
					return resp;
				}
			} else {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("userOid不存在");
				return resp;
			}
		} catch (Exception e) {
			log.error("校验密码异常", e);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("校验密码异常");
			return resp;
		}
	}

	/**
	 * 根据userOid获取用户
	 * @param @param
	 *            userOid
	 * @param @return
	 * @return UserInfoEntity
	 * @throws @Title:
	 *             getAccountUserByUserOid
	 * @Description:
	 */
	public UserInfoEntity getAccountUserByUserOid(String userOid) {
		UserInfoEntity entity = userInfoDao.findByUserOid(userOid);
		return entity;
	}

	/**
	 * 查询用户
	 * @return
	 */
	public UserListResponse userQueryList(Specification<UserInfoEntity> spec) {
		List<UserInfoEntity> us = null;
		if (spec != null) {
			us = this.userInfoDao.findAll(spec);
		} else {
			us = this.userInfoDao.findAll();
		}

		if (null != us && us.size() != 0) {
			List<UserDto> infos = new ArrayList<UserDto>();
			for (UserInfoEntity entity : us) {
				UserDto tempEntity = new UserDto();
				BeanUtils.copyProperties(entity, tempEntity, new String[] { "createTime" });
				tempEntity.setCreateTime(DateUtil.format(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
				infos.add(tempEntity);
			}
			return new UserListResponse(infos);
		}
		return new UserListResponse();
	}

	public FinanceUserResp read(String oid) {
		log.info("获取用户详情,oid:{}", oid);
		UserInfoEntity userEntity = this.userInfoDao.findOne(oid);
		log.info("获取用户详情,UserInfoEntity:{}", JSON.toJSONString(userEntity));
		FinanceUserResp fur = userEntityTranforToResp(userEntity);
		fur.setReturnCode(Constant.SUCCESS);
		log.info("转化后用户详情,FinanceUserResp:{}", JSON.toJSONString(fur));

		return fur;
	}

	private FinanceUserResp userEntityTranforToResp(UserInfoEntity userEntity) {
		FinanceUserResp fur = new FinanceUserResp();
		fur.setOid(userEntity.getOid());
		fur.setUserType(userEntity.getUserType());
		fur.setUserOid(userEntity.getUserOid()); // 用户ID
		fur.setSystemUid(userEntity.getSystemUid()); // 用户业务系统ID
		fur.setSystemSource(userEntity.getSystemSource()); // 来源系统
		fur.setName(userEntity.getName()); // 姓名
		fur.setIdCard(userEntity.getIdCard()); // 身份证号
		fur.setBankName(userEntity.getBankName()); // 开户行
		fur.setCardNo(userEntity.getCardNo()); // 银行账号
		fur.setPhone(userEntity.getPhone()); // 手机号
		fur.setRemark(userEntity.getRemark()); // 备注
		fur.setUpdateTime(userEntity.getUpdateTime()); // 更新时间
		fur.setCreateTime(userEntity.getCreateTime()); // 创建时间
		return fur;
	}

	/**
	 * 获取用户列表
	 * @param spec
	 * @param pageable
	 * @return 如果返回的errCode属性等于0表示成功，否则表示失败，失败原因在errMessage里面体现
	 */
	public PageResp<FinanceUserResp> list(Specification<UserInfoEntity> spec, Pageable pageable) {
		log.info("获取用户列表,spec:{},pageable:{}", new Object[] { JSON.toJSONString(spec), JSON.toJSONString(pageable) });
		Page<UserInfoEntity> cas = this.userInfoDao.findAll(spec, pageable);
		PageResp<FinanceUserResp> pagesRep = new PageResp<FinanceUserResp>();
		if (cas != null && cas.getContent() != null && cas.getTotalElements() > 0) {
			List<FinanceUserResp> rows = new ArrayList<FinanceUserResp>();
			Object[] users = userInfoDao.findAllUserName();
			for (UserInfoEntity u : cas) {
				FinanceUserResp queryRep = userEntityTranforToResp(u);
				String name = u.getName();
				if (StringUtils.isEmpty(u.getName())) {
					for (Object user : users) {
						Object[] aname = (Object[]) user;
						String uid = aname[0].toString();
						String uname = aname[1].toString();
						if (!StringUtils.isEmpty(u.getUserOid()) && u.getUserOid().equals(uid)) {
							u.setName(uname);
						}
					}
				}
				queryRep.setName(name);
				rows.add(queryRep);
			}
			pagesRep.setRows(rows);
		}
		pagesRep.setTotal(cas.getTotalElements());
		return pagesRep;
	}

	public List<JSONObject> getUsers() {
		List<JSONObject> jsonObjList = new ArrayList<JSONObject>();
		List<UserInfoEntity> us = this.userInfoDao.findAll();
		if (us != null && us.size() > 0) {
			JSONObject jsonObj = null;
			for (UserInfoEntity u : us) {
				jsonObj = new JSONObject();
				jsonObj.put("oid", u.getOid());
				jsonObj.put("name", u.getName());
				jsonObjList.add(jsonObj);
			}
		}
		return jsonObjList;
	}

	public Map<String, UserInfoEntity> findUsers(final Set<String> userOids) {
		// 查询标签
		Specification<UserInfoEntity> spec = new Specification<UserInfoEntity>() {
			@Override
			public Predicate toPredicate(Root<UserInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				In<String> inUserOids = cb.in(root.get("userOid").as(String.class));
				for (String userOid : userOids) {
					inUserOids.value(userOid);
				}
				return inUserOids;
			}
		};
		spec = Specifications.where(spec);
		List<UserInfoEntity> us = userInfoDao.findAll(spec);
		Map<String, UserInfoEntity> userMap = new HashMap<String, UserInfoEntity>();
		if (us != null && us.size() > 0) {
			for (UserInfoEntity u : us) {
				userMap.put(u.getUserOid(), u);
			}
		}
		return userMap;
	}

	public List<UserInfoEntity> getUsers(final String phone) {
		Specification<UserInfoEntity> spec = new Specification<UserInfoEntity>() {
			@Override
			public Predicate toPredicate(Root<UserInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.like(root.get("phone").as(String.class), phone);
			}
		};
		spec = Specifications.where(spec);
		List<UserInfoEntity> us = userInfoDao.findAll(spec);

		return us;
	}

	/**
	 * 根据用户手机号获取用户信息
	 * @param phone
	 * @return
	 */
	public UserInfoEntity getAccountUserByPhone(String phone) {
		UserInfoEntity entity = userInfoDao.findByPhone(phone);
		return entity;
	}
}