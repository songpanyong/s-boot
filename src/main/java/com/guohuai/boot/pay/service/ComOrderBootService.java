package com.guohuai.boot.pay.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.CreateOrderRequest;
import com.guohuai.account.api.request.PublisherAccountInfoRequest;
import com.guohuai.account.api.response.AccountBalanceResponse;
import com.guohuai.account.api.response.CreateOrderResponse;
import com.guohuai.account.api.response.PublisherAccountBalanceResponse;
import com.guohuai.account.api.response.PublisherAccountInfoResponse;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.*;
import com.guohuai.boot.pay.dao.*;
import com.guohuai.boot.pay.form.OrderForm;
import com.guohuai.boot.pay.res.OrderVoRes;
import com.guohuai.boot.pay.vo.ChannelVo;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.component.util.*;
import com.guohuai.payadapter.component.CallBackEnum;
import com.guohuai.payadapter.redeem.CallBackDao;
import com.guohuai.payadapter.redeem.CallBackInfo;
import com.guohuai.settlement.api.SettlementSdk;
import com.guohuai.settlement.api.request.CheckInAccountOrderRequest;
import com.guohuai.settlement.api.request.DepositBankOrderSyncReq;
import com.guohuai.settlement.api.request.OrderAccountRequest;
import com.guohuai.settlement.api.request.WithdrawBankOrderSyncReq;
import com.guohuai.settlement.api.response.BaseResponse;
import com.guohuai.settlement.api.response.OrderAccountResponse;
import com.guohuai.settlement.api.response.OrderResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/**
 * 后台使用
 * @author guohuai
 *
 */
@Service
@Slf4j
public class ComOrderBootService {
    @Autowired
    private ComOrderDao comOrderDao;
    @Autowired
    private BankLogDao bankLogDao;
    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private ProtocolDao protocolDao;
    @Autowired
    private SeqGenerator seqGenerator;
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private AccountInfoService accountInfoService;
    @Autowired
    private AccOrderService accOrderService;
    @Autowired
    private AccountWithdrawalsService accountWithdrawalsService;
    @Autowired
    private AccountRechargeService accountRechargeService;
    @Autowired
    private AccountInfoDao accountInfoDao;
    @Autowired
	private AccOrderDao orderDao;
    @Autowired
	private ComChannelDao comChannelDao;
    @Autowired
   	private CallBackDao callbackDao;
    @Autowired
	SettlementSdk settlementSdk;

    @Autowired
	private UserInfoDao userInfoDao;
    @Autowired
	private PayTwoRedisUtil payTwoRedisUtil;

    public String genSn() {
        String sn = this.seqGenerator.next("SEL");
        return sn;
    }

    /**
     * 补单
     */
//    public OrderResponse feed(PaymentVo paymentVo) {
//        log.info("补单请求数据 paymentVo{},", JSONObject.toJSONString(paymentVo));
//        String channelNo = paymentVo.getChannelNo();
//        Timestamp time = new Timestamp(System.currentTimeMillis());
//        // ------获取开通协议
//        OrderResponse res = new OrderResponse();
////      ProtocolVo protocolVo = protocolDao.findOneByUserOidAndStatus(paymentVo.getUserOid(), ErrorDesEnum.ElELOCK.getCode());
//        //20180115支持用户绑定多张卡
//		ProtocolVo protocolVo = protocolDao.findOneByUserOidAndStatusAndCarNo(
//				paymentVo.getUserOid(), ErrorDesEnum.ElELOCK.getCode(), paymentVo.getCardNo());
//        if (protocolVo != null) {
//            // ---交付日志新增一条数据
//            BankLogVo bankLogVo = new BankLogVo();
//            bankLogVo.setUserOid(paymentVo.getUserOid());
//            bankLogVo.setPayNo(this.genSn());
//            bankLogVo.setOperatorTime(time);
//            bankLogVo.setCreateTime(time);
//            bankLogVo.setTradStatus(PayEnum.PAY0.getCode());
//            bankLogVo.setLaunchplatform(paymentVo.getLaunchplatform());
//            bankLogVo.setAmount(paymentVo.getAmount());
//            bankLogVo.setFee(BigDecimal.ZERO);
//            bankLogVo.setType(paymentVo.getType());
//            bankLogVo.setOrderNo(paymentVo.getOrderNo());
//            paymentVo.setPayNo(bankLogVo.getPayNo());
//            paymentVo.setUpTime(time);//存放对账时间
//            paymentDao.saveAndFlush(paymentVo);
//
//            bankLogVo.setSheetId(paymentVo.getOid());
//            bankLogDao.saveAndFlush(bankLogVo);
//
//            OrderVo orderVo = comOrderDao.findByorderNo(paymentVo.getOrderNo());
//
//            // ----调用-----
//            res = comOrderService.callPublishPayeeEvent(orderVo, protocolVo, paymentVo, bankLogVo, res, null);
//
//            log.info("提现订单实时返回结果{}，{}，渠道{}", res.getReturnCode(), res.getErrorMessage(), channelNo);
//            //金运通渠道同步返回结果推送业务系统,金运通赎回渠道:7,补单只走赎回
//            if ("7".equals(channelNo) || "8".equals(channelNo)) {
//                if (Constant.SUCCESS.equals(res.getReturnCode())) {
//                    comOrderService.pushResult(res);
//                }
//            }
//            log.info("补单返回数据{},", JSONObject.toJSONString(res));
//            return res;
//        } else {
//            res.setReturnCode(ErrorDesEnum.ELEMENTUN.getCode());
//            res.setErrorMessage(ErrorDesEnum.ELEMENTUN.getName());
//            return res;
//        }
//    }

    public OrderVo findOne(String oid) {
        return comOrderDao.findOne(oid);
    }

    public OrderVoRes page(OrderForm req) {
        log.info("{},订单查询,{},", req.getUserOid(), JSONObject.toJSONString(req));


        Page<OrderVo> listPage = comOrderDao.findAll(buildSpecification(req, 0),
                new PageRequest(req.getPage() - 1, req.getRows()));
        OrderVoRes res = new OrderVoRes();
        if (listPage != null && listPage.getSize() > 0) {
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (OrderVo vo : listPage.getContent()) {
                vo.setCardNo(DesPlus.decrypt(vo.getCardNo()));
                totalAmount = totalAmount.add(vo.getAmount());
                UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(vo.getUserOid());
                if(null!=userInfoEntity) {
                	vo.setPhone(userInfoEntity.getPhone());
                }
            }
            res.setRows(listPage.getContent());
            res.setTotalPage(listPage.getTotalPages());
            res.setPage(req.getPage());
            res.setRow(req.getRows());
            res.setTotal(listPage.getTotalElements());
            res.setTotalAccount(listPage.getTotalElements());
//			res.setTotalAmount(getTotalAmount(req));//20170227删除总金额查询
            return res;
        }
        return null;
    }

    /**
     * 获取总金额
     *
     * @param req
     * @return
     */
    public BigDecimal getTotalAmount(OrderForm req) {
        BigDecimal toam = BigDecimal.ZERO;
        String userOid = req.getUserOid();
        if (StringUtil.isEmpty(userOid)) {//null
            userOid = "";
        }
        String type = req.getType();
        if (StringUtil.isEmpty(type)) {//""
            type = "";
        }
        String realname = req.getRealName();
        if (StringUtil.isEmpty(realname)) {//""
            realname = "";
        } else {
            realname = "%" + realname + "%";
        }
        String orderNo = req.getOrderNo();
        if (StringUtil.isEmpty(orderNo)) {//""
            orderNo = "";
        } else {
            orderNo = "%" + orderNo + "%";
        }
        String payNo = req.getPayNo();
        if (StringUtil.isEmpty(payNo)) {//""
            payNo = "";
        } else {
            payNo = "%" + payNo + "%";
        }
        String status = req.getStatus();
        if (StringUtil.isEmpty(status)) {//""
            status = "";
        }
        String reconStatus = req.getReconStatus();
        if (StringUtil.isEmpty(reconStatus)) {//null
            reconStatus = "";
        }
        try {
            toam = comOrderDao.findAmount(userOid, type, realname, orderNo, payNo, status, reconStatus,
                    req.getLimitAmount(), req.getMaxAmount(), req.getBeginTime(), req.getEndTime());
        } catch (Exception e) {
            log.error("获取金额异常",e);
        }
        if (toam == null) {
            toam = BigDecimal.ZERO;
        }
        return toam;
    }

    // 导出数据
    public List<List<String>> data(OrderForm req) {
        log.info("{},导出订单查询参数,{},", req.getUserOid(), JSONObject.toJSONString(req));
        List<OrderVo> listPage = comOrderDao.findAll(buildSpecification(req, 0));
        List<List<String>> data = new ArrayList<List<String>>();
        if (!listPage.isEmpty()) {
            List<String> line = null;
            long index = 0;
            for (OrderVo vo : listPage) {
                line = new ArrayList<>();
                line.add(String.valueOf(++index));//序号
                line.add(vo.getOrderNo());//订单号
                if (!StringUtil.isEmpty(vo.getPayNo())) {//支付流水号
                    line.add(vo.getPayNo());
                } else {
                    line.add(null);
                }
                if (!StringUtil.isEmpty(vo.getType())) {//订单类型
                    if (vo.getType().equals("01"))
                        line.add("充值");
                    if (vo.getType().equals("02"))
                        line.add("提现");
                } else {
                    line.add(null);
                }
                if (!StringUtil.isEmpty(vo.getStatus())) {//订单状态
                    if (vo.getStatus().equals("0"))
                        line.add("未处理");
                    if (vo.getStatus().equals("1"))
                        line.add("交易成功");
                    if (vo.getStatus().equals("2"))
                        line.add("交易失败");
                    if (vo.getStatus().equals("3"))
                        line.add("交易处理中");
                    if (vo.getStatus().equals("4"))
                        line.add("超时");
                    if (vo.getStatus().equals("5"))
                        line.add("撤销");
                } else {
                    line.add(null);
                }
                line.add(AlgorithmUtil.getPrettyNumber(vo.getAmount() + ""));//订单金额(元)
                line.add(AlgorithmUtil.getPrettyNumber(vo.getFee() + ""));//手续费(元)
                line.add(vo.getSystemSource());
                line.add(vo.getRealName());
                UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(vo.getUserOid());
                if(null!=userInfoEntity) {
                    vo.setPhone(userInfoEntity.getPhone());
                }
                line.add(vo.getPhone());
                if (!StringUtil.isEmpty(vo.getUserType())) {//用户类型
                    if (vo.getUserType().equals("T1"))
                        line.add("投资人");
                    if (vo.getUserType().equals("T2"))
                        line.add("发行人");
                    if (vo.getUserType().equals("T3"))
                        line.add("平台");
                } else {
                    line.add(null);
                }
                if (!StringUtil.isEmpty(vo.getChannel())) {//支付通道
                    if (vo.getChannel().equals("1"))
                        line.add("快付通");
                    if (vo.getChannel().equals("2"))
                        line.add("平安银行");
                    if (vo.getChannel().equals("3"))
                        line.add("先锋代扣");
                    if (vo.getChannel().equals("4"))
                        line.add("先锋代付");
                    if (vo.getChannel().equals("5"))
                        line.add("南粤银行(代扣)");
                    if (vo.getChannel().equals("6"))
                        line.add("南粤银行(代付)");
                    if (vo.getChannel().equals("7"))
                        line.add("金运通(代付)");
                    if (vo.getChannel().equals("8"))
                        line.add("金运通(代扣)");
                    if (vo.getChannel().equals("9"))
                        line.add("金运通网关支付");
                    if (vo.getChannel().equals("10"))
                        line.add("宝付(认证支付)");
                    if (vo.getChannel().equals("11"))
                        line.add("宝付(代付)");
                    if (vo.getChannel().equals("12"))
                        line.add("宝付代扣");
                    if (vo.getChannel().equals("16"))
                        line.add("先锋(认证支付)");
                    if (vo.getChannel().equals("17"))
                    	line.add("先锋(代扣)");
                    if (vo.getChannel().equals("18"))
                        line.add("先锋(代付)");
                    if (vo.getChannel().equals("19"))
                        line.add("易宝(绑卡支付)");
                    if (vo.getChannel().equals("20"))
                    	line.add("易宝(代扣)");
                    if (vo.getChannel().equals(""))
                    	line.add("未知");
                } else {
                    line.add("未知");
                }
                line.add(timeSplitMs(vo.getReceiveTime()));//下单时间
                if (!StringUtil.isEmpty(DesPlus.decrypt(vo.getCardNo()))) {//银行账号
                    line.add(DesPlus.decrypt(vo.getCardNo()));
                } else {
                    line.add(null);
                }
                if (!StringUtil.isEmpty(vo.getFailDetail())) {//失败详情
                    line.add(vo.getFailDetail());
                } else {
                    line.add(null);
                }
                if (!StringUtil.isEmpty(vo.getReturnCode())) {//错误码
                    line.add(vo.getReturnCode());
                } else {
                    line.add(null);
                }
                if (!StringUtil.isEmpty(vo.getRemark())) {//备注
                    line.add(vo.getRemark());
                } else {
                    line.add(null);
                }
                if (!StringUtil.isEmpty(vo.getDescrib())) {//订单描述
                    line.add(vo.getDescrib());
                } else {
                    line.add(null);
                }
                line.add(vo.getUpdateTime() + "");//修改时间
                data.add(line);
//				log.info("data参数"+data);
            }
        }
        return data;
    }

    // 导出的标题
    public List<String> header() {
        List<String> header = new ArrayList<String>();
        header.add("序号");
        header.add("订单号");
        header.add("支付流水号");
        header.add("订单类型");
        header.add("订单状态");
        header.add("订单金额(元)");
        header.add("手续费(元)");
        header.add("系统来源");
        header.add("用户名称");
        header.add("用户账号");
        header.add("用户类型");
        header.add("支付通道");
        header.add("下单时间");
        header.add("银行账号");
        header.add("失败详情");
        header.add("错误码");
        header.add("备注");
        header.add("订单描述");
        header.add("修改时间");
        return header;
    }

    public Specification<OrderVo> buildSpecification(final OrderForm req, final int index) {
        Specification<OrderVo> spec = new Specification<OrderVo>() {
            @Override
            public Predicate toPredicate(Root<OrderVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> bigList = new ArrayList<Predicate>();
                List<Predicate> big2List = new ArrayList<Predicate>();
                if (!StringUtil.isEmpty(req.getUserOid())) {
                    bigList.add(cb.equal(root.get("userOid").as(String.class), req.getUserOid()));
                } else if (!StringUtil.isEmpty(req.getPhone())) {
                    UserInfoEntity userInfoEntity = userInfoService.getAccountUserByPhone(req.getPhone());
                    if (null == userInfoEntity) { //构造一个错误的userOid
                        bigList.add(cb.equal(root.get("userOid").as(String.class), StringUtil.uuid()));
                    } else {
                        bigList.add(cb.equal(root.get("userOid").as(String.class), userInfoEntity.getUserOid()));
                    }
                }
                if (!StringUtil.isEmpty(req.getType()))
                    bigList.add(cb.equal(root.get("type").as(String.class), req.getType()));
                if (!StringUtil.isEmpty(req.getRealName()))
                    bigList.add(cb.like(root.get("realName").as(String.class), "%" + req.getRealName() + "%"));

                if (!StringUtil.isEmpty(req.getOrderNo()))
                    bigList.add(cb.like(root.get("orderNo").as(String.class), "%" + req.getOrderNo() + "%"));
                if (!StringUtil.isEmpty(req.getPayNo()))
                    bigList.add(cb.like(root.get("payNo").as(String.class), "%" + req.getPayNo() + "%"));
                if (!StringUtil.isEmpty(req.getStatus()))
                    bigList.add(cb.equal(root.get("status").as(String.class), req.getStatus()));
                if (!StringUtil.isEmpty(req.getReconStatus()))
                    bigList.add(cb.equal(root.get("reconStatus").as(Integer.class),
                            Integer.valueOf(req.getReconStatus().trim())));
                if (!StringUtil.isEmpty(req.getMemberId()))
                    bigList.add(cb.equal(root.get("memberId").as(String.class), req.getMemberId()));
                if (!StringUtil.isEmpty(req.getAuditStatus())) {
                    bigList.add(cb.equal(root.get("auditStatus").as(String.class), req.getAuditStatus()));
                } else if (!StringUtil.isEmpty(req.getAuditStatusList())) {
                    // 20170815 订单审核记录（产品要求只查询审核通过和驳回）
                    for (String s : req.getAuditStatusList().split(",")) {
                        big2List.add(cb.equal(root.get("auditStatus").as(String.class), s));
                    }
                }
                if (!StringUtil.isEmpty(req.getOperatorName()))
                    bigList.add(cb.equal(root.get("operatorName").as(String.class), "%" + req.getOperatorName() + "%"));
                if (req.getLimitAmount() != null && req.getLimitAmount().compareTo(BigDecimal.ZERO) >= 0)
                    bigList.add(cb.greaterThanOrEqualTo(root.get("amount").as(BigDecimal.class), req.getLimitAmount()));
                if (req.getMaxAmount() != null && req.getMaxAmount().compareTo(BigDecimal.ZERO) >= 0)
                    bigList.add(cb.lessThanOrEqualTo(root.get("amount").as(BigDecimal.class), req.getMaxAmount()));
                if (!StringUtil.isEmpty(req.getBeginTime())) {
                    java.util.Date beginDate = DateUtil.parseDate(req.getBeginTime(), Constant.fomat);
                    bigList.add(cb.greaterThanOrEqualTo(root.get("createTime").as(Timestamp.class),
                            new Timestamp(beginDate.getTime())));
                }
                if (!StringUtil.isEmpty(req.getEndTime())) {
                    java.util.Date beginDate = DateUtil.parseDate(req.getEndTime(), Constant.fomat);
                    bigList.add(cb.lessThanOrEqualTo(root.get("createTime").as(Timestamp.class),
                            new Timestamp(beginDate.getTime())));
                }
                query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
                if (big2List.size() > 0) {
                    query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])), cb.or(big2List.toArray(new Predicate[big2List.size()])));
                }
                if (index == 0) {
//					query.orderBy(cb.desc(root.get("createTime")), cb.desc(root.get("updateTime")));
                    query.orderBy(cb.desc(root.get("createTime")));//2017.01.07优化查询
                }

                // 条件查询
                return query.getRestriction();
            }
        };
        return spec;
    }

    /**
     * 获取订单数
     *
     * @param req
     * @return
     */
    public OrderAccountResponse getCounNum(OrderAccountRequest req) {
        log.info("获取订单数：{}", JSONObject.toJSONString(req));
        java.util.Date beginTime = DateUtil.beginTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
        java.util.Date endTime = DateUtil.endTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
        long result = comOrderDao.countNum(new Timestamp(beginTime.getTime()), new Timestamp(endTime.getTime()));
        OrderAccountResponse res = new OrderAccountResponse();
        res.setCountNum(result);
        return res;
    }

    /**
     * 获取订单对账数据
     *
     * @param req
     * @return
     */
    public List<OrderAccountResponse> getChackCompareData(OrderAccountRequest req) {
        log.info("订单对账数据获取：{}", JSONObject.toJSONString(req));
        java.util.Date beginTime = DateUtil.beginTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
        java.util.Date endTime = DateUtil.endTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
        Object[] results = comOrderDao.orderDetailList(new Timestamp(beginTime.getTime()),
                new Timestamp(endTime.getTime()), req.getCountNum(), Constant.size);
        if (results != null && results.length != 0) {
            List<OrderAccountResponse> listAccounts = new ArrayList<>();
            OrderAccountResponse res = null;
            for (Object result : results) {
                res = new OrderAccountResponse();
                Object[] ob = (Object[]) result;
                res.setInvestorOid(nullToStr(ob[0]));
                res.setOrderCode(nullToStr(ob[1]));
                res.setOrderStatus(nullToStr(ob[2]));
                res.setOrderType(nullToStr(ob[3]).equals(PayEnum.PAYTYPE01.getCode()) ? "invest" : "redeem");
                res.setOrderAmount((BigDecimal) ob[4]);
                res.setBuzzDate(
                        com.guohuai.component.util.DateUtil.format(((Timestamp) ob[5]).getTime(), Constant.fomat));

                if (req.getCountNum() == 0) {
                    res.setCountNum(Long.valueOf(results.length));
                } else {
                    res.setCountNum(Long.valueOf((Long.valueOf(results.length) + Long.valueOf(req.getCountNum()))));
                }

                listAccounts.add(res);
            }
            return listAccounts;
        }
        return null;
    }

    String nullToStr(Object str) {
        if (null == str) {
            return "";
        }
        return str.toString();
    }

    /**
     * 线上登记订单
     *
     * @param req
     * @return
     */
    public BaseResponse checkInOrder(OrderForm req) {
        BaseResponse resp = new BaseResponse();
        //查询用户是否存在，及用户类型
        UserInfoEntity userInfoEntity = null;
        try {
        	userInfoEntity = userInfoService.getAccountUserByPhone(req.getPhone());
		} catch (Exception e) {
			log.error("根据用户账号查询用户信息异常：{}",e);
			resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("该用户信息异常！");
            return resp;
		}
        if (userInfoEntity == null) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("用户账号不存在，请核实！");
            return resp;
        }
        //判断是否已录入该订单
        OrderVo orderEntity = comOrderDao.findByPayNo(req.getPayNo());
        if (orderEntity != null) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("该订单已录入成功，请勿重复录入！");
            return resp;
        }
        //判断是否是服务器当前时间
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if(req.getReceiveTime().after(now)){
        	resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("请选择正确的时间下单时间！");
            return resp;
        }
        final String cardNo = DesPlus.encrypt(req.getCardNo());
        if (StringUtil.isEmpty(cardNo)) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("银行卡号加密失败！");
            return resp;
        }
        //手续费
        BigDecimal fee = BigDecimal.ZERO;
        //提现订单查询是否够
        if("02".equals(req.getType())){
    		BigDecimal amount = req.getAmount();
    		if(req.getFee() != null){
    			fee = req.getFee();
    		}
            amount = amount.add(fee);
            //查询账户余额
            if("T2".equals(userInfoEntity.getUserType())){
            	PublisherAccountInfoRequest publishAccountReq = new PublisherAccountInfoRequest();
            	publishAccountReq.setUserOid(userInfoEntity.getUserOid());
            	PublisherAccountInfoResponse publishAccountInfo = accountInfoService.publisherAccountInfo(publishAccountReq);
            	log.info("提现可用余额{}，提现金额{}",publishAccountInfo.getWithdrawAvailableAmountBalance(),amount);
            	if(publishAccountInfo.getWithdrawAvailableAmountBalance().compareTo(amount)<0){
            		resp.setReturnCode(Constant.FAIL);
                    resp.setErrorMessage("用户账户余额不足！");
                    return resp;
            	}
            }else{
            	AccountBalanceResponse iBalanceRes = accountInfoService.getAccountBalanceByUserOid(userInfoEntity.getUserOid());
            	log.info("提现可用余额{}，提现金额{}",iBalanceRes.getWithdrawAvailableBalance(),amount);
            	if(iBalanceRes.getWithdrawAvailableBalance().compareTo(amount)<0){
            		resp.setReturnCode(Constant.FAIL);
                    resp.setErrorMessage("用户账户余额不足！");
                    return resp;
            	}
            }
    	}

        //新增订单t_bank_order
        OrderVo orderVo = new OrderVo();
        orderVo.setUserOid(userInfoEntity.getUserOid());
        orderVo.setUserType(userInfoEntity.getUserType());
        orderVo.setAmount(req.getAmount());
        orderVo.setAuditStatus(req.getAuditStatus());
        orderVo.setCardNo(cardNo);
        orderVo.setChannel(req.getChannel());
        orderVo.setDescrib(req.getDescribe());
        orderVo.setFee(fee);
        orderVo.setMemberId(req.getMemberId());
        orderVo.setPayNo(req.getPayNo());
        orderVo.setPhone(req.getPhone());
        orderVo.setRealName(userInfoEntity.getName());
        orderVo.setType(req.getType());
        orderVo.setRemark(req.getRemark());
        orderVo.setOperatorId(req.getOperatorId());
        orderVo.setOperatorName(req.getOperatorName());
        orderVo.setSystemSource("settlement");
        orderVo.setReconStatus(0);
        orderVo.setStatus("1");
        orderVo.setAuditStatus("1");
        orderVo.setReceiveTime(req.getReceiveTime());
        orderVo.setCreateTime(now);
        String orderNo = this.seqGenerator.next(CodeConstants.CHECK_IN_NO_PREFIX);
        orderVo.setOrderNo(orderNo);
        String requestNo = this.seqGenerator.next(CodeConstants.CHECK_IN_REQ_PREFIX);
        orderVo.setRequestNo(requestNo);
//		orderVo.setUpdateTime(time);
//		orderVo.setReturnCode(returnCode);
//		orderVo.setSettlementStatus(settlementStatus);
//		orderVo.setBankCode(bankCode);
//		orderVo.setBankReturnSeriNo(bankReturnSeriNo);
//		orderVo.setBusinessStatus(businessStatus);
//		orderVo.setFailDetail(failDetail);
        try {
            comOrderDao.save(orderVo);
        } catch (Exception e) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("系统异常");
            return resp;
        }
        resp.setReturnCode(Constant.SUCCESS);
        resp.setErrorMessage("订单已提交申请");
        return resp;
    }

    //新增订单t_bank_payment,账户记账
    private PaymentVo checkInOrderAddPayment(OrderVo entity) {
        Timestamp time = new Timestamp(System.currentTimeMillis());
        BigDecimal amount = entity.getAmount();
        PaymentVo paymentVo = new PaymentVo();
        paymentVo.setUpdateTime(time);
        paymentVo.setCreateTime(entity.getReceiveTime());//用于外部对账
        paymentVo.setUpTime(entity.getReceiveTime());
        paymentVo.setCardNo(entity.getCardNo());
        paymentVo.setChannelNo(entity.getChannel());
        paymentVo.setCommandStatus(entity.getStatus());
        paymentVo.setMerchantId(entity.getMemberId());
        paymentVo.setPayNo(entity.getPayNo());
		paymentVo.setPhone(entity.getPhone());
		paymentVo.setOrderNo(entity.getOrderNo());
		paymentVo.setRealName(entity.getRealName());
		paymentVo.setReconStatus(0);
		paymentVo.setType(entity.getType());
		paymentVo.setUserOid(entity.getUserOid());
		paymentVo.setUserType(entity.getUserType());
		paymentVo.setAmount(amount);
//      paymentVo.setFailDetail("");
//		paymentVo.setAccountCity(accountCity);
//		paymentVo.setAccountProvince(accountProvince);
//		paymentVo.getAuditOperator();
//		paymentVo.getAuditOperatorReson();
//		paymentVo.getAuditOperatorStatus();
//		paymentVo.getAuditOperatorTime();
//		paymentVo.setBankCode(bankCode);
//		paymentVo.setAuditResetOperator(auditResetOperator);
//		paymentVo.setBankReturnSeriNo(bankReturnSeriNo);
//		paymentVo.setCheckCode(checkCode);
//		paymentVo.setCrossFlag(crossFlag);
//		paymentVo.setCurrency(currency);
//		paymentVo.setDistanceMark(distanceMark);
//		paymentVo.setEmergencyMark(emergencyMark);
//		paymentVo.setHostFlowNo(hostFlowNo);
//		paymentVo.setLaunchplatform(launchplatform);
//		paymentVo.setOperator(operator);
//		paymentVo.setOperatorTime(operatorTime);
//		paymentVo.setPayAddress(payAddress);
//		paymentVo.setPlatformAccount(platformAccount);
//		paymentVo.setPlatformName(platformName);
//		paymentVo.setProductId(productId);
//		paymentVo.setReconciliationMark(reconciliationMark);

		paymentDao.save(paymentVo);
		return paymentVo;
    }

    /**
     * 订单手工录入审核
     */
    public BaseResponse checkInOrderAudit(OrderForm req) {
        BaseResponse resp = new BaseResponse();

        if (StringUtil.isEmpty(req.getOid())) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("oid不能为空！");
            return resp;
        }
        String payNo = req.getPayNo();
        if (StringUtil.isEmpty(payNo)) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("支付流水号不能为空！");
            return resp;
        }
        if (StringUtil.isEmpty(req.getAuditStatus()) || !StringUtil.in(req.getAuditStatus(), "2", "3")) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("审核状态不能为空或状态非法！");
            return resp;
        }
        // 防重复审核提交校验
        log.info("订单录入审核，增加redis缓存，防止重复审核，支付流水号:{}", req.getPayNo());
		Long check = payTwoRedisUtil.setRedisByTime("checkInOrderAudit_redis_tag" + req.getPayNo(), req.getPayNo());
		if (check.intValue() == 0) {
			log.error("订单录入审核处理中，不能重复审核");
			resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("订单录入审核处理中，不能重复审核！");
			return resp;
		}
        // 订单状态
        String status = PayEnum.PAY1.getCode();
        if ("3".equals(req.getAuditStatus())) {
        	//回调业务系统失败
            OrderVo entity = comOrderDao.findByPayNo(payNo);
            status = PayEnum.PAY2.getCode();
            payNo = payNo + "_" + new Date().getTime();
            payNo = payNo.length() > 64 ? payNo.substring(0, 64) : payNo;
            entity.setStatus(status);
            entity.setFailDetail("审核驳回");
            entity.setPayNo(payNo);
            this.callBackByJob(entity);
        }

        //记账
        if("2".equals(req.getAuditStatus())){
        	OrderVo entity = comOrderDao.findByPayNo(payNo);
        	if("02".equals(entity.getType())){
        		BigDecimal amount = entity.getAmount();
                BigDecimal fee = entity.getFee();
                amount = amount.add(fee);
                //查询账户余额
                if("T2".equals(entity.getUserType())){
                	PublisherAccountInfoRequest publishAccountReq = new PublisherAccountInfoRequest();
                	publishAccountReq.setUserOid(entity.getUserOid());
                	PublisherAccountInfoResponse publishAccountInfo = accountInfoService.publisherAccountInfo(publishAccountReq);
                	log.info("提现可用余额{}，提现金额{}",publishAccountInfo.getWithdrawAvailableAmountBalance(),amount);
                	if(publishAccountInfo.getWithdrawAvailableAmountBalance().compareTo(amount)<0){
                		resp.setReturnCode(Constant.FAIL);
                        resp.setErrorMessage("用户账户余额不足！");
        				payTwoRedisUtil.delRedis("checkInOrderAudit_redis_tag" + req.getPayNo());
                        return resp;
                	}
                }else{
                	AccountBalanceResponse iBalanceRes = accountInfoService.getAccountBalanceByUserOid(entity.getUserOid());
                	log.info("提现可用余额{}，提现金额{}",iBalanceRes.getWithdrawAvailableBalance(),amount);
                	if(iBalanceRes.getWithdrawAvailableBalance().compareTo(amount)<0){
                		resp.setReturnCode(Constant.FAIL);
                        resp.setErrorMessage("用户账户余额不足！");
                        payTwoRedisUtil.delRedis("checkInOrderAudit_redis_tag" + req.getPayNo());
                        return resp;
                	}
                }
        	}
        	PaymentVo paymentVo = this.checkInOrderAddPayment(entity);
        	if(paymentVo == null){
        		resp.setReturnCode(Constant.FAIL);
                resp.setErrorMessage("系统异常");
                payTwoRedisUtil.delRedis("checkInOrderAudit_redis_tag" + req.getPayNo());
                return resp;
        	}
        	resp = this.accounting(entity);
        	if(!Constant.SUCCESS.equals(resp.getReturnCode())){
        		payTwoRedisUtil.delRedis("checkInOrderAudit_redis_tag" + req.getPayNo());
        		return resp;
        	}else if("settlement".equals(entity.getSystemSource())){
        		//调用业务系统补单
              	this.callBack(entity);
        	}else if("mimosa".equals(entity.getSystemSource())){
        		//回调业务系统成功
                entity.setFailDetail("审核通过");
                this.callBackByJob(entity);
        	}
        }
        //修改审核状态
        final int i = comOrderDao.updateOrderByOid(req.getAuditStatus(), req.getOid(), req.getAuditRemark(), payNo, status);
        if (i == 0) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("支付流水号：" + req.getPayNo() + " 未找到数据！");
            payTwoRedisUtil.delRedis("checkInOrderAudit_redis_tag" + req.getPayNo());
            return resp;
        }
        resp.setReturnCode(Constant.SUCCESS);
        resp.setErrorMessage("成功");
        payTwoRedisUtil.delRedis("checkInOrderAudit_redis_tag" + req.getPayNo());
        return resp;
    }

    /**
     * 录入订单回调
     * @param entity 订单
     */
    private void callBackByJob(OrderVo entity) {
    	log.info("线下登记订单交易回调，订单{}",JSONObject.toJSONString(entity));
		OrderResponse  orderResponse = new OrderResponse();
		orderResponse.setUserOid(entity.getUserOid());
		orderResponse.setOrderNo(entity.getOrderNo());
		orderResponse.setType(entity.getType());//充值：01 提现：02 赎回：03
		orderResponse.setAmount(entity.getAmount());
		orderResponse.setRemark(entity.getRemark());
		String submitTime = DateUtil.format(entity.getCreateTime().getTime(), Constant.fomat);
		orderResponse.setPayTime(submitTime);
		orderResponse.setStatus(entity.getStatus());
		orderResponse.setPayNo(entity.getPayNo());
		orderResponse.setUserType(entity.getUserType());
		orderResponse.setChannelNo(entity.getChannel());
		if(AccOrderEntity.ORDERSTATUS_SUCCESS.equals(entity.getStatus())){
			orderResponse.setReturnCode(Constant.SUCCESS);
			orderResponse.setErrorMessage(entity.getAuditRemark());
		}else{
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage(entity.getAuditRemark());
		}
		String returnMsg = "";
		Boolean result = false;
		CallBackInfo info = null;
		//通过订单号查回调信息
		info = callbackDao.queryCallBackOneByOrderNo(entity.getOrderNo(), "settlement");
		if(null!=info ){
			log.error(">>>>>>赎回回调信息[订单号orderNo="+entity.getOrderNo()+"]已存在!<<<<<<");
		}else{
			info = CallBackInfo.builder().orderNO(orderResponse.getOrderNo()).tradeType(orderResponse.getType())
					.payNo(orderResponse.getPayNo()).channelNo("").type("settlement").minute(1)
					.totalCount(20).totalMinCount(20).countMin(0).returnCode(orderResponse.getReturnCode())
					.status(CallBackEnum.INIT.getCode()).returnMsg(orderResponse.getErrorMessage()).createTime(new Date())
					.build();
			try {
				log.info("提现交易回调，{}", JSONObject.toJSONString(orderResponse));
				result = settlementSdk.callback(orderResponse);
			} catch (Exception e) {
				returnMsg = "推送交易信息异常";
				log.error(returnMsg + " OrderNO{},{}", orderResponse.getOrderNo(), e);
			}
			log.info("提现交易回调结果，orderNo：{},result：{}", orderResponse.getOrderNo(), result);
			if(true == result){
				info.setStatus(CallBackEnum.SUCCESS.getCode());
				info.setUpdateTime(new Date());
			}else{
				info.setStatus(CallBackEnum.INIT.getCode());
				info.setUpdateTime(new Date());
			}
			callbackDao.saveAndFlush(info);
		}
		
	}

	/**
     * 账户
     * @param entity
     * @return
     */
	private BaseResponse accounting(OrderVo entity) {
		BaseResponse resp = new BaseResponse();
		String orderType = entity.getType();
		BigDecimal balance = BigDecimal.ZERO;
		if("01".equals(orderType)){
			orderType = OrderTypeEnum.RECHARGE.getCode();
			balance = entity.getAmount();
		}else if("02".equals(orderType)){
			orderType = OrderTypeEnum.WITHDRAWALS.getCode();
			balance = entity.getAmount().add(entity.getFee());
		}
		//生成账户订单
		CreateOrderResponse createOrderResponse = new CreateOrderResponse();
		try {
			CreateOrderRequest ordReq = new CreateOrderRequest();
			ordReq.setOrderNo(entity.getOrderNo());
			ordReq.setRequestNo(entity.getRequestNo());
			ordReq.setUserOid(entity.getUserOid());
			ordReq.setOrderType(orderType);
			ordReq.setBalance(balance);
			ordReq.setSystemSource(entity.getSystemSource());
			ordReq.setOrderCreatTime(entity.getReceiveTime().toString());
			ordReq.setFee(entity.getFee());
			ordReq.setUserType(entity.getUserType());
			log.info("生成账户交易定单");
			createOrderResponse = accOrderService.addAccOrder(ordReq);
			if(!Constant.SUCCESS.equals(createOrderResponse.getReturnCode())){
				resp.setErrorMessage("保存账户定单失败");
				resp.setReturnCode(Constant.FAIL);
				return resp;
			}
		} catch (Exception e) {
			log.error("系统繁忙,保存定单失败", e);
			resp.setErrorMessage("系统繁忙,保存定单失败");
			resp.setReturnCode(Constant.FAIL);
			return resp;
		}
		AccOrderEntity accOrderEntity = accOrderService.getOrderOid(createOrderResponse.getOrderOid());
		if(accOrderEntity == null){
			log.error("系统繁忙,保存定单失败");
			resp.setErrorMessage("系统繁忙,保存定单失败");
			resp.setReturnCode(Constant.FAIL);
			return resp;
		}
		AccountInfoEntity basicAccount = null;
		if(OrderTypeEnum.RECHARGE.getCode().equals(orderType)){
			//充值
			basicAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(
					AccountTypeEnum.BASICER.getCode(),accOrderEntity.getUserOid());
			resp = accountRechargeService.addBasicAccountTrans(
					accOrderEntity.getUserType(), accOrderEntity.getUserOid(),
					basicAccount, balance, accOrderEntity);
		}else{
			//提现
			basicAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(
					AccountTypeEnum.BASICER.getCode(),accOrderEntity.getUserOid());
			resp = accountWithdrawalsService.subtractBasicAccountTrans(
					accOrderEntity.getUserType(), accOrderEntity.getUserOid(),
					basicAccount, balance, accOrderEntity);
		}
		if(!Constant.SUCCESS.equals(createOrderResponse.getReturnCode())){
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			orderDao.save(accOrderEntity);
			resp.setErrorMessage("修改用户账户余额失败");
			resp.setReturnCode(Constant.FAIL);
			return resp;
		}else{
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
			orderDao.save(accOrderEntity);
		}
		resp.setReturnCode(Constant.SUCCESS);
        resp.setErrorMessage("成功");
        return resp;
	}

	/**
	 * 通知业务系统登记
	 * @param entity
	 */
	private void callBack(OrderVo entity) {
		BaseResponse result = null;
		if("01".equals(entity.getType())){
			DepositBankOrderSyncReq depositBankOrderSyncReq = new DepositBankOrderSyncReq();
			try {
				depositBankOrderSyncReq.setCompleteTime(entity.getReceiveTime().toString());
				depositBankOrderSyncReq.setFee(BigDecimal.ZERO);
				depositBankOrderSyncReq.setInvestorOid(entity.getUserOid());
				depositBankOrderSyncReq.setOrderAmount(entity.getAmount());
				depositBankOrderSyncReq.setOrderCode(entity.getOrderNo());
				depositBankOrderSyncReq.setOrderStatus("1");
				depositBankOrderSyncReq.setOrderTime(entity.getReceiveTime().toString());
				depositBankOrderSyncReq.setUserType(entity.getUserType());
				depositBankOrderSyncReq.setPayNo(entity.getPayNo());
				depositBankOrderSyncReq.setChannelNo(entity.getChannel());
				result = settlementSdk.depositBankOrder(depositBankOrderSyncReq);
			} catch (Exception e) {
				log.error("登记线下充值记录，通知业务系统登记异常，订单号{}，异常信息{}",entity.getOrderNo(), e);
			}
		}else{
			WithdrawBankOrderSyncReq withdrawBankOrderSyncReq = new WithdrawBankOrderSyncReq();
			try {
				withdrawBankOrderSyncReq.setCompleteTime(entity.getReceiveTime().toString());
				withdrawBankOrderSyncReq.setFee(entity.getFee());
				withdrawBankOrderSyncReq.setInvestorOid(entity.getUserOid());
				withdrawBankOrderSyncReq.setOrderAmount(entity.getAmount());
				withdrawBankOrderSyncReq.setOrderCode(entity.getOrderNo());
				withdrawBankOrderSyncReq.setOrderStatus("1");
				withdrawBankOrderSyncReq.setOrderTime(entity.getReceiveTime().toString());
				withdrawBankOrderSyncReq.setUserType(entity.getUserType());
				withdrawBankOrderSyncReq.setPayNo(entity.getPayNo());
				withdrawBankOrderSyncReq.setChannelNo(entity.getChannel());
				result = settlementSdk.withdrawBankOrder(withdrawBankOrderSyncReq);
			} catch (Exception e) {
				log.error("登记线下提现记录，通知业务系统登记异常，订单号{}，异常信息{}",entity.getOrderNo(), e);
			}
		}
		if(result != null){
			log.info("登记线下提现记录，通知业务系统结果：{}",result.getReturnCode());
		}

	}

	/**
	 * 业务系统订单录入
	 * @param req
	 * @return
	 */
	public BaseResponse checkInAccountOrder(CheckInAccountOrderRequest req) {
		BaseResponse resp = new BaseResponse();
        //校验录入订单信息
        resp = checkParameter(req);
        if(!Constant.SUCCESS.equals(resp.getReturnCode())){
        	return resp;
        }
        //查询用户是否存在，及用户类型
        UserInfoEntity userInfoEntity = null;
        try {
        	userInfoEntity = userInfoService.getAccountUserByUserOid(req.getUserOid());
		} catch (Exception e) {
			log.error("根据用户账号查询用户信息异常：{}",e);
			resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("该用户信息异常！");
            return resp;
		}
        if (userInfoEntity == null) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("用户账号不存在，请核实！");
            return resp;
        }

        final String cardNo = DesPlus.encrypt(req.getCardNo());
        if (StringUtil.isEmpty(cardNo)) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("银行卡号加密失败！");
            return resp;
        }
        if (StringUtil.isEmpty(req.getOperatorId())) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("操作人id不能为空！");
            return resp;
        }
        if (StringUtil.isEmpty(req.getOperatorName())) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("操作人名称不能为空！");
            return resp;
        }
        //手续费
        BigDecimal fee = BigDecimal.ZERO;
        //提现订单查询是否够
        if("02".equals(req.getType())){
    		BigDecimal amount = req.getAmount();
    		if(req.getFee() != null){
    			fee = req.getFee();
    		}
            amount = amount.add(fee);
            //查询账户余额
            if("T2".equals(userInfoEntity.getUserType())){
            	PublisherAccountInfoRequest publishAccountReq = new PublisherAccountInfoRequest();
            	publishAccountReq.setUserOid(userInfoEntity.getUserOid());
            	PublisherAccountInfoResponse publishAccountInfo = accountInfoService.publisherAccountInfo(publishAccountReq);
            	log.info("提现可用余额{}，提现金额{}",publishAccountInfo.getWithdrawAvailableAmountBalance(),amount);
            	if(publishAccountInfo.getWithdrawAvailableAmountBalance().compareTo(amount)<0){
            		resp.setReturnCode(Constant.FAIL);
                    resp.setErrorMessage("用户账户余额不足！");
                    return resp;
            	}
            }else{
            	AccountBalanceResponse iBalanceRes = accountInfoService.getAccountBalanceByUserOid(userInfoEntity.getUserOid());
            	log.info("提现可用余额{}，提现金额{}",iBalanceRes.getWithdrawAvailableBalance(),amount);
            	if(iBalanceRes.getWithdrawAvailableBalance().compareTo(amount)<0){
            		resp.setReturnCode(Constant.FAIL);
                    resp.setErrorMessage("用户账户余额不足！");
                    return resp;
            	}
            }
    	}

        //新增订单t_bank_order
        OrderVo orderVo = new OrderVo();
        orderVo.setUserOid(userInfoEntity.getUserOid());
        orderVo.setUserType(userInfoEntity.getUserType());
        orderVo.setAmount(req.getAmount());
        orderVo.setCardNo(cardNo);
        orderVo.setChannel(req.getChannelNo());
        orderVo.setDescrib(req.getDescribe());
        orderVo.setFee(fee);
        orderVo.setPayNo(req.getPayNo());
        orderVo.setPhone(req.getPhone());
        orderVo.setRealName(userInfoEntity.getName());
        orderVo.setType(req.getType());
        orderVo.setRemark(req.getDescribe());
        orderVo.setDescrib("业务系统录入订单");
        orderVo.setSystemSource(req.getSystemSource());
        orderVo.setReconStatus(0);
        orderVo.setStatus("1");
        orderVo.setAuditStatus("1");
        orderVo.setOrderNo(req.getOrderNo());
        orderVo.setRequestNo(req.getRequestNo());
		orderVo.setOperatorId(req.getOperatorId());
		orderVo.setOperatorName(req.getOperatorName());
		
		Timestamp orderCreateTime = Timestamp.valueOf(req.getOrderCreateTime());//业务订单时间
        Timestamp time = new Timestamp(System.currentTimeMillis());
        Timestamp outOrderTime = Timestamp.valueOf(req.getOutOrderTime());//三方订单时间
        orderVo.setCreateTime(orderCreateTime);
        orderVo.setUpdateTime(time);
        orderVo.setReceiveTime(outOrderTime);
        
		try {
            comOrderDao.save(orderVo);
        } catch (Exception e) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("系统异常");
            return resp;
        }
//        //记账
//    	PaymentVo paymentVo = this.checkInOrderAddPayment(orderVo);
//    	if(paymentVo == null){
//    		resp.setReturnCode(Constant.FAIL);
//            resp.setErrorMessage("系统异常");
//            return resp;
//    	}
//    	resp = this.accounting(orderVo);
//    	if(!Constant.SUCCESS.equals(resp.getReturnCode())){
//    		return resp;
//    	}
    	resp.setReturnCode(Constant.SUCCESS);
        resp.setErrorMessage("收单成功");
		return resp;
	}

	/**
	 * 校验参数
	 * @param req
	 * @return
	 */
	private BaseResponse checkParameter(
			CheckInAccountOrderRequest req) {
		BaseResponse resp = new BaseResponse();
		if(StringUtil.isEmpty(req.getUserOid())){
			resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("用户id不能为空！");
            return resp;
		}
		if(StringUtil.isEmpty(req.getChannelNo())){
			resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("支付通道不能为空！");
            return resp;
		}
		if(StringUtil.isEmpty(req.getOrderNo())){
			resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("订单号不能为空！");
            return resp;
		}
		if(StringUtil.isEmpty(req.getPayNo())){
			resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("支付流水号不能为空！");
            return resp;
		}
		if(StringUtil.isEmpty(req.getRequestNo())){
			resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("请求流水号不能为空！");
            return resp;
		}
		if(StringUtil.isEmpty(req.getType())){
			resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("订单类型不能为空！");
            return resp;
		}
		//判断是否已录入该支付流水号
        OrderVo orderEntity = comOrderDao.findByPayNo(req.getPayNo());
        if (orderEntity != null) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("支付流水号已存在，请勿重复录入！");
            return resp;
        }
        //判断订单号是否已存在
        orderEntity = comOrderDao.findByorderNo(req.getOrderNo());
        if (orderEntity != null) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("订单号已存在，请勿重复录入！");
            return resp;
        }
        //判断渠道id
        ChannelVo channelVo = comChannelDao.findByChannelNo(req.getChannelNo());
        if(channelVo == null){
        	resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("未知支付通道！");
            return resp;
        }
        if(!req.getType().equals(channelVo.getTradeType())){
        	resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("支付通道与订单类型不匹配！");
            return resp;
        }

        resp.setReturnCode(Constant.SUCCESS);
        resp.setErrorMessage("成功");
		return resp;
	}

	public String timeSplitMs(Timestamp ts) {
	    try {
	        if (null != ts) {
	            return com.guohuai.component.util.DateUtil.format(ts.getTime(), Constant.fomat);
	        }
	    } catch (Exception e) {
	        log.warn("TimeStamp转换String格式出错", e);
	    }
	    return ts+"";
	}

}