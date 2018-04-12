package com.guohuai.boot.account.service;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountTransferRequest;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.account.api.response.BaseResponse;
import com.guohuai.component.common.AccountEerrorDefineConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * 交易定单
 * @author xueyunlong
 * @date 2018/1/27 13:39
 */
@Slf4j
@Service
public class TradeOrderService {

    @Autowired
    private AccountAcceptOrderService accountAcceptOrderService;
    @Autowired
    private  AccountTransferRequestCheckService accountTransferRequestCheckService;
    /**
     * 轧差
     * @param req 交易请求参数
     * @return 交易结果
     */
    public AccountTransResponse netting(AccountTransferRequest req) {
        log.info("轧差接收定单 ：{}" + JSONObject.toJSONString(req));
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(req, transResp);
        // 校验收单参数
        transResp = accountTransferRequestCheckService.checkNettingParam(req);
       if(!BaseResponse.isSuccess(transResp)){
           transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
            return transResp;
        }
       	transResp = accountAcceptOrderService.acceptOrder(req);
       	if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }

    /**
     * 使用红包
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse useRedPacket(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        transResp = accountTransferRequestCheckService.checkUseRedPacketParam(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    /**
     * 返佣
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse rebate(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        transResp = accountTransferRequestCheckService.checkRebateParam(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 转换-实时兑付
     * 出：发行人产品户-参数：productAccountNo
     * 入：投资人续投冻结户
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse redeemT0Change(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkRedeemT0Param(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 转换-实时投资
     * 出：投资人续投冻结户
     * 入：发行人产品户-参数：productAccountNo
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse investT0Change(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkInvestT0Param(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 续投-实时兑付
     * 出：发行人产品户-参数：productAccountNo
 	 * 入：资人续投冻结户
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse redeemT0Continued(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkRedeemT0Param(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 续投-非实时兑付
     * 出：发行人归集户
 	 * 入：投资人续投冻结户
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse redeemT1Continued(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkRedeemT1Param(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    
    /**
     * 续投-实时投资
     * 出：投资人续投冻结户
 	 * 入：发行人产品户-参数：productAccountNo
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse investT0Continued(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkInvestT0Param(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 续投-非实时投资
     * 出：投资人续投冻结户
 	 * 入：发行人归集户
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse investT1Continued(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkInvestT1Param(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 续投解冻
     * 出：投资人续投冻结户
 	 * 入：投资人基本户
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse unfreezeContinued(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkUnfreezeParam(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 平台基本户转账
     * 出：平台基本户-查库
	 * 入：平台备付金-参数inputAccountNo
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse transferPlatformBasic(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkTransferInpuptAccountNoParam(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        // 校验基本户余额是否充足
        checkResp = accountTransferRequestCheckService.checkTransferOutputAccountBalance(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 平台备付金转账
     * 出：平台备付金-参数outputAccountNo
	 * 入：平台基本户-查库
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse transferPlatformPayment(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkTransferOutputAccountNoParam(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 发行人基本户转账
     * 出：发行人基本户-查库 
	 * 入：发行人产品户-参数inputAccountNo
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse transferPublisherBasic(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkPublisherInputNoParam(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        // 校验基本户余额是否充足
        checkResp = accountTransferRequestCheckService.checkTransferOutputAccountBalance(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 发行人产品户转账
     * 出：发行人产品户-参数outputAccountNo
	 * 入：发行人基本户-查库 
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse transferPublisherProduct(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkPublisherOutputNoParam(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 轧差-入款
     * 出：发行人归集户-查库
	 * 入：发行人产品户-参数productAccountNo
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse nettingIncome(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkPublisherProductNoParam(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 轧差-出款
     * 出：发行人产品户-参数productAccountNo
	 * 入：发行人归集户-查库
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse nettingOutcome(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        AccountTransResponse checkResp = accountTransferRequestCheckService.checkPublisherProductNoParam(accountTransferRequest);
        transResp.setReturnCode(checkResp.getReturnCode());
        transResp.setErrorMessage(checkResp.getErrorMessage());
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 投资t+0
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse investT0(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        transResp = accountTransferRequestCheckService.checkInvestT0Param(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 投资t+1
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse investT1(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        transResp = accountTransferRequestCheckService.checkInvestT1Param(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 赎回t+0
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse redeemT0(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        transResp = accountTransferRequestCheckService.checkRedeemT0Param(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }
    
    /**
     * 赎回t+1
     * @param accountTransferRequest
     * @return
     */
    public AccountTransResponse redeemT1(AccountTransferRequest accountTransferRequest) {
        AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        transResp = accountTransferRequestCheckService.checkRedeemT1Param(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
    }

    /**
     * 申购T+0退款
     * @param accountTransferRequest
     * @return
     */
	public AccountTransResponse reFundInvestT0(AccountTransferRequest accountTransferRequest) {
		AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        transResp = accountTransferRequestCheckService.checkReFundInvestT0Param(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
	}
	
	/**
     * 申购T+1退款
     * @param accountTransferRequest
     * @return
     */
	public AccountTransResponse reFundInvestT1(AccountTransferRequest accountTransferRequest) {
		AccountTransResponse transResp = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransferRequest, transResp);
        // 校验收单参数
        transResp = accountTransferRequestCheckService.checkReFundInvestT1Param(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
            return transResp;
        }
        transResp = accountAcceptOrderService.acceptOrder(accountTransferRequest);
        if(!BaseResponse.isSuccess(transResp)){
            transResp.setErrorMessage(AccountEerrorDefineConfig.define.get(transResp.getReturnCode()));
             log.info("账户接收定单，校验参数信息失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
             return transResp;
         }
        return transResp;
	}
}
