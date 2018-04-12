package com.guohuai.boot.account.service;

import java.math.BigDecimal;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.guohuai.account.api.request.AccountTransferRequest;
import com.guohuai.account.api.request.entity.TradeEvent;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.account.component.util.EventTypeEnum;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.PlatformInfoDao;
import com.guohuai.boot.account.entity.PlatformInfoEntity;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;

/**
 * AccountTransferRequest 校验
 * @author xueyunlong
 * @date 2018/1/27 13:42
 */
@Slf4j
@Service
public class AccountTransferRequestCheckService {
	
	@Autowired
	AccountInfoDao accountInfoDao;
	@Autowired
	PlatformInfoDao platformInfoDao;

    /**
     * 通用校验收单参数
     *
     * @param req 交易请求参数
     * @return AccountTransResponse 校验结果
     */
    private static AccountTransResponse checkTransParam(AccountTransferRequest req) {
        AccountTransResponse resp = new AccountTransResponse();
        resp.setReturnCode(Constant.FAIL);
        if (StringUtil.isEmpty(req.getOrderNo())) {
            resp.setReturnCode("9019");
            return resp;
        }
        if (StringUtil.isEmpty(req.getRequestNo())) {
            resp.setReturnCode("9018");
            return resp;
        }
        if (StringUtil.isEmpty(req.getSystemSource())) {
            resp.setReturnCode("9019");
            return resp;
        }
        if (CollectionUtils.isEmpty(req.getEventList())) {
            resp.setReturnCode("9069");
            return resp;
        }
        resp.setReturnCode(Constant.SUCCESS);
        resp.setErrorMessage("交易成功");
        return resp;
    }

    /**
     * 验证轧差
     *
     * @param accountTransferRequest 请求
     * @return AccountTransResponse
     */
    public AccountTransResponse checkNettingParam(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getPublisherUserOid())) {
            resp.setReturnCode("9066");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getProductAccountNo())) {
            resp.setReturnCode("9070");
            return resp;
        }

        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
        
        return checkTransParam(accountTransferRequest);
    }

    /**
     * 验证红包
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse checkUseRedPacketParam(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getUserOid())) {
            resp.setReturnCode("9016");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
        
        return checkTransParam(accountTransferRequest);
    }
    
    /**
     * 验证返佣
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse checkRebateParam(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getUserOid())) {
            resp.setReturnCode("9016");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
       
        return checkTransParam(accountTransferRequest);
    }
    
    /**
     * 转换-实时兑付 参数校验:主要校验发行人、发行人产品户
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse checkRedeemT0Param(AccountTransferRequest accountTransferRequest) {
    	 AccountTransResponse resp = new AccountTransResponse();
         if (StringUtil.isEmpty(accountTransferRequest.getUserOid())) {
             resp.setReturnCode("9016");
             return resp;
         }
         if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
             resp.setReturnCode("9027");
             return resp;
         }
        
        if (StringUtil.isEmpty(accountTransferRequest.getProductAccountNo())) {
            resp.setReturnCode("9073"); //发行人产品户不能为空
            return resp;
        }
        return checkTransParam(accountTransferRequest);
    }
    
    /**
     * 非实时兑付,发行人归集户只有一个，查库而不是参数传入，但是发行人要传
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse checkRedeemT1Param(AccountTransferRequest accountTransferRequest) {
   	 AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getUserOid())) {
            resp.setReturnCode("9016");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
       
       if (StringUtil.isEmpty(accountTransferRequest.getPublisherUserOid())) {
           resp.setReturnCode("9066"); //发行人不能为空
           return resp;
       }
       return checkTransParam(accountTransferRequest);
   }
    
    /**
     * 转换-实时投资 参数校验:主要校验发行人、发行人产品户
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse checkInvestT0Param(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getUserOid())) {
            resp.setReturnCode("9016");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
        
        if (StringUtil.isEmpty(accountTransferRequest.getProductAccountNo())) {
            resp.setReturnCode("9073"); //发行人产品户不能为空
            return resp;
        }
        return checkTransParam(accountTransferRequest);
    }
    
    /**
     * 非实时投资 参数校验:主要校验发行人、发行人（发行人归集户查库）
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse checkInvestT1Param(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getUserOid())) {
            resp.setReturnCode("9016");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
        
        if (StringUtil.isEmpty(accountTransferRequest.getPublisherUserOid())) {
            resp.setReturnCode("9066"); //发行人不能为空
            return resp;
        }
        
        return checkTransParam(accountTransferRequest);
    }
    
    /**
     * 转账 参数校验，校验入款账户、出款账户不能为空
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse checkTransferParam(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getUserOid())) {
            resp.setReturnCode("9016");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
       
        if (StringUtil.isEmpty(accountTransferRequest.getInputAccountNo())) {
            resp.setReturnCode("9071");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getOutputAccountNo())) {
            resp.setReturnCode("9072");
            return resp;
        }
        return checkTransParam(accountTransferRequest);
    }
    
    /**
     * 解冻参数校验
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse checkUnfreezeParam(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getUserOid())) {
            resp.setReturnCode("9016");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
       
        return checkTransParam(accountTransferRequest);
    }
    
    /**
     * 转账 参数校验：入款账户不能为空
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse checkTransferInpuptAccountNoParam(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
        
        if (StringUtil.isEmpty(accountTransferRequest.getInputAccountNo())) {
            resp.setReturnCode("9071");
            return resp;
        }
        return checkTransParam(accountTransferRequest);
    }
    
    /**
     * 转账 参数校验：出款账户不能为空
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse checkTransferOutputAccountNoParam(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
        
        if (StringUtil.isEmpty(accountTransferRequest.getOutputAccountNo())) {
            resp.setReturnCode("9072");
            return resp;
        }
        
        return checkTransParam(accountTransferRequest);
    }
    
    /**
     * 参数校验：发行人、发行人产品户（入款账户）
     * @param accountTransferRequest 
     * @param orderTypeCode 订单交易类型(必填)
     * @return
     */
    public AccountTransResponse checkPublisherInputNoParam(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
       
        if (StringUtil.isEmpty(accountTransferRequest.getPublisherUserOid())) {
            resp.setReturnCode("9066"); //发行人不能为空
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getInputAccountNo())) {
            resp.setReturnCode("9071"); //发行人产品户（传入转账入款账户字段中）
            return resp;
        }
        return checkTransParam(accountTransferRequest);
    }
    
    /**
     * 参数校验：发行人、发行人产品户（出款账户）
     * @param accountTransferRequest
     * @param orderTypeCode 订单交易类型(必填)
     * @return
     */
    public AccountTransResponse checkPublisherOutputNoParam(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
       
        if (StringUtil.isEmpty(accountTransferRequest.getPublisherUserOid())) {
            resp.setReturnCode("9066"); //发行人不能为空
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getOutputAccountNo())) {
            resp.setReturnCode("9072"); //发行人产品户（传入转账出款账户字段中）
            return resp;
        }
        return checkTransParam(accountTransferRequest);
    }
    
    /**
     * 参数校验：发行人、发行人产品户
     * @param accountTransferRequest 
     * @param orderTypeCode 订单交易类型(必填)
     * @return
     */
    public AccountTransResponse checkPublisherProductNoParam(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
        
        if (StringUtil.isEmpty(accountTransferRequest.getPublisherUserOid())) {
            resp.setReturnCode("9066"); //发行人不能为空
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getProductAccountNo())) {
            resp.setReturnCode("9071"); //发行人产品户
            return resp;
        }
        return checkTransParam(accountTransferRequest);
    }

    /**
     * 申购T0退款
     * @param accountTransferRequest
     * @param orderTypeCode
     * @return
     */
	public AccountTransResponse checkReFundInvestT0Param(
			AccountTransferRequest accountTransferRequest) {
		AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
        
        if (StringUtil.isEmpty(accountTransferRequest.getOrigOrderNo())) {
            resp.setReturnCode("9017");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getUserOid())) {
            resp.setReturnCode("9016");
            return resp;
        }
        return checkTransParam(accountTransferRequest);
	}
	
	/**
	 * 申购T1退款
	 * @param accountTransferRequest
	 * @param orderTypeCode
	 * @return
	 */
	public AccountTransResponse checkReFundInvestT1Param(AccountTransferRequest accountTransferRequest) {
		AccountTransResponse resp = new AccountTransResponse();
        if (StringUtil.isEmpty(accountTransferRequest.getOrderType())) {
            resp.setReturnCode("9027");
            return resp;
        }
        
        if (StringUtil.isEmpty(accountTransferRequest.getOrigOrderNo())) {
            resp.setReturnCode("9017");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getUserOid())) {
            resp.setReturnCode("9016");
            return resp;
        }
        if (StringUtil.isEmpty(accountTransferRequest.getPublisherUserOid())) {
            resp.setReturnCode("9066"); //发行人不能为空
            return resp;
        }
        return checkTransParam(accountTransferRequest);
	}

	/**
	 * 校验出账账户余额是够充足
	 * 解决基本户转出时考虑提现冻结户余额问题
	 * 可转账金额=基本户-提现冻结户
	 * @param accountTransferRequest
	 * @return
	 */
	public AccountTransResponse checkTransferOutputAccountBalance(
			AccountTransferRequest accountTransferRequest) {
		AccountTransResponse resp = new AccountTransResponse();
		resp.setReturnCode(Constant.SUCCESS);
		String publishUserOid = accountTransferRequest.getPublisherUserOid();
		String userOid = publishUserOid;
		for(TradeEvent event : accountTransferRequest.getEventList()){
			if(EventTypeEnum.TRANSFER_PUBLISHER_BASIC.getCode().equals(event.getEventType())){
				//查询发行人账户信息
				userOid = publishUserOid;
			}
			if(EventTypeEnum.TRANSFER_PLATFORM_BASIC.getCode().equals(event.getEventType())){
				//查询平台庄户信息
				PlatformInfoEntity platform = platformInfoDao.findFirst();
				userOid = platform.getUserOid();
			}
			BigDecimal balance = accountInfoDao.findAvaliableBalanceByUserOid(userOid);
			if(balance.compareTo(event.getBalance())<0){
				resp.setReturnCode("9062");
	            return resp;
			}
		}
		return resp;
	}
    
}
