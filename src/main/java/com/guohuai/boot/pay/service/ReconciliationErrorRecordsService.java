package com.guohuai.boot.pay.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.UserInfoService;
import com.guohuai.boot.pay.dao.*;
import com.guohuai.boot.pay.form.ReconciliationErrorRecordsForm;
import com.guohuai.boot.pay.res.ReconciliationErrorRecordsVoRes;
import com.guohuai.boot.pay.vo.ReconciliationErrorRecordsVo;
import com.guohuai.component.util.CheckConstant;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.TradeTypeEnum;
import com.guohuai.settlement.api.response.BaseResponse;
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
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ReconciliationErrorRecordsService {
    @Autowired
    private ReconciliationErrorRecordsDao reconciliationErrorRecordsDao;

    @Autowired
    private ReconciliationPassDao reconciliationPassDao;
    @Autowired
    private ReconciliationRechargeService reconciliationRechargeService;
    @Autowired
    private ReconciliationWithdrawalsService reconciliationWithdrawalsService;
    @Autowired
    private BankLogDao bankLogDao;
    @Autowired
    private PaymentDao paymentDao;
    @Autowired
    private AccOrderDao accOrderDao;
    @Autowired
    private ComOrderDao comOrderDao;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private UserInfoDao userInfoDao;

    /**
     * 异常单据查询
     */
    public ReconciliationErrorRecordsVoRes page(ReconciliationErrorRecordsForm req) {
        log.info("{},对账异常单据查询,{},", req.getUserOid(), JSONObject.toJSONString(req));
        Page<ReconciliationErrorRecordsVo> listPage = reconciliationErrorRecordsDao.findAll(buildSpecification(req),
                new PageRequest(req.getPage() - 1, req.getRows()));
        ReconciliationErrorRecordsVoRes res = new ReconciliationErrorRecordsVoRes();
        if (listPage != null && listPage.getSize() > 0) {
            for (ReconciliationErrorRecordsVo vo : listPage.getContent()) {
                UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(vo.getUserOid());
                if (null != userInfoEntity) {
                    vo.setUserPhone(userInfoEntity.getPhone());
                }
            }
            res.setRows(listPage.getContent());
            res.setTotalPage(listPage.getTotalPages());
            res.setPage(req.getPage());
            res.setRow(req.getRows());
            res.setTotal(listPage.getTotalElements());
            return res;
        }
        return null;
    }

    public Specification<ReconciliationErrorRecordsVo> buildSpecification(final ReconciliationErrorRecordsForm req) {
        Specification<ReconciliationErrorRecordsVo> spec = new Specification<ReconciliationErrorRecordsVo>() {
            @Override
            public Predicate toPredicate(Root<ReconciliationErrorRecordsVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> bigList = new ArrayList<Predicate>();
                if (!StringUtil.isEmpty(req.getChannelNo()))
                    bigList.add(cb.equal(root.get("channelNo").as(String.class), req.getChannelNo()));
                if (!StringUtil.isEmpty(req.getOrderNo()))
                    bigList.add(cb.equal(root.get("orderNo").as(String.class), req.getOrderNo()));
                if (!StringUtil.isEmpty(req.getPayNo()))
                    bigList.add(cb.equal(root.get("payNo").as(String.class), req.getPayNo()));
                if (!StringUtil.isEmpty(req.getOutsideOrderNo()))
                    bigList.add(cb.equal(root.get("outsideOrderNo").as(String.class), req.getOutsideOrderNo()));
                if (!StringUtil.isEmpty(req.getOrderType()))
                    bigList.add(cb.equal(root.get("orderType").as(String.class), req.getOrderType()));
                if (!StringUtil.isEmpty(req.getUserName()))
                    bigList.add(cb.equal(root.get("userName").as(String.class), req.getUserName()));

                if (!StringUtil.isEmpty(req.getUserPhone())) {
                    UserInfoEntity userInfoEntity = userInfoService.getAccountUserByPhone(req.getUserPhone());
                    if (null == userInfoEntity) { //构造一个错误的userOid
                        bigList.add(cb.equal(root.get("userOid").as(String.class), StringUtil.uuid()));
                    } else {
                        bigList.add(cb.equal(root.get("userOid").as(String.class), userInfoEntity.getUserOid()));
                    }
                }

                if (!StringUtil.isEmpty(req.getErrorStatus()))
                    bigList.add(cb.equal(root.get("errorStatus").as(String.class), req.getErrorStatus()));
                if (!StringUtil.isEmpty(req.getErrorType()))
                    bigList.add(cb.equal(root.get("errorType").as(String.class), req.getErrorType()));

                if (!StringUtil.isEmpty(req.getBeginTime())) {
                    java.util.Date beginDate = DateUtil
                            .beginTimeInMillis(DateUtil.parseDate(req.getBeginTime(), Constant.fomat));
                    bigList.add(cb.greaterThanOrEqualTo(root.get("orderTime").as(Timestamp.class),
                            new Timestamp(beginDate.getTime())));
                }
                if (!StringUtil.isEmpty(req.getEndTime())) {
                    java.util.Date beginDate = DateUtil
                            .endTimeInMillis(DateUtil.parseDate(req.getEndTime(), Constant.fomat));
                    bigList.add(cb.lessThanOrEqualTo(root.get("orderTime").as(Timestamp.class),
                            new Timestamp(beginDate.getTime())));
                }

                query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
                log.info("增加按异常定单人工处理排序");
                query.orderBy(cb.asc(root.get("errorSort")),cb.desc(root.get("orderTime")));
                // 条件查询
                return query.getRestriction();
            }
        };
        return spec;
    }

    /**
     * 判断是否可以完成对账
     */
    public boolean completeReconciliation(String channelNo, Timestamp completeDate) {
        boolean completeReconciliation = false;
        String errorStatus = CheckConstant.WAIT_FOR_DEAL;
        int notCompleteCount = reconciliationErrorRecordsDao.getNotcompleteCount(channelNo, completeDate, errorStatus);
        if (notCompleteCount == 0) {
            completeReconciliation = true;
        }
        return completeReconciliation;
    }

    /**
     * 复合完成
     */
    @Transactional
    public BaseResponse artificialComposite(ReconciliationErrorRecordsForm req) {

        String remark = StringUtil.isEmpty(req.getRemark()) ? "" : req.getRemark().trim();
        ReconciliationErrorRecordsVo errorEntity = reconciliationErrorRecordsDao.findOne(req.getOid());
        BaseResponse response = new BaseResponse();
        if (!CheckConstant.WAIT_FOR_DEAL.equals(errorEntity.getErrorStatus())) {
            response.setReturnCode(Constant.FAIL);
            response.setErrorMessage("该数据已处理，请刷新页面后操作！");
            return response;
        }
        // 三方订单多单
        if (CheckConstant.BANK_MORETHAN_ORDER_DESC.equals(errorEntity.getErrorType())) {
            return artificialCompositeUpdate(errorEntity, CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1010, remark);
        }
        // 金额不匹配
        if (CheckConstant.AMOUNT_NOT_CONFORM_DESC.equals(errorEntity.getErrorType())) {
            return artificialCompositeUpdate(errorEntity, CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1015, remark);
        }
        response.setReturnCode(Constant.FAIL);
        response.setErrorMessage("类型不匹配，数据更新失败！");
        return response;
    }

    /**
     * 确认成功
     */
    @Transactional
    public BaseResponse sureSuccess(ReconciliationErrorRecordsForm req) {
        String remark = StringUtil.isEmpty(req.getRemark()) ? "" : req.getRemark().trim();
        ReconciliationErrorRecordsVo errorEntity = reconciliationErrorRecordsDao.findOne(req.getOid());
        BaseResponse response = new BaseResponse();
        if (!CheckConstant.WAIT_FOR_DEAL.equals(errorEntity.getErrorStatus())) {
            response.setReturnCode(Constant.FAIL);
            response.setErrorMessage("该数据已处理，请刷新页面后操作！");
            return response;
        }
        // 平台订单多单
        if (CheckConstant.ORDER_MORETHAN_BANK_DESC.equals(errorEntity.getErrorType())) {
            // 充值
            if (TradeTypeEnum.trade_pay.getCode().equals(errorEntity.getOrderType())) {
                log.info("确认成功->平台订单多单->充值：oid={}，payNo={} ,订单状态={}", errorEntity.getOid(), errorEntity.getPayNo(), errorEntity.getOrderStatus());
                // 订单状态：充值成功
                if (PayEnum.PAY1.getCode().equals(errorEntity.getOrderStatus())) {
                    return sureUpdate(errorEntity, PayEnum.PAY1.getCode(), CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1012, remark);
                } else {
                    // 订单状态：充值失败、超时等
                    final OrderResponse orderResponse = reconciliationRechargeService.rechargeRecon(errorEntity.getOrderNo(), "1", errorEntity.getOrderStatus());
                    if (!Constant.SUCCESS.equals(orderResponse.getReturnCode())) {
                        return orderResponse;
                    }
                    updateOrderStatus(errorEntity, PayEnum.PAY1.getCode(), CheckConstant.STANDARD_RESULT_1017);
                    return sureUpdate(errorEntity, PayEnum.PAY1.getCode(), CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1017, remark);
                }
            }
            // 提现
            if (TradeTypeEnum.trade_payee.getCode().equals(errorEntity.getOrderType())) {
                log.info("确认成功->平台订单多单->提现：oid={}，payNo={} ,订单状态={}", errorEntity.getOid(), errorEntity.getPayNo(), errorEntity.getOrderStatus());
                // 订单状态：提现成功
                if (PayEnum.PAY1.getCode().equals(errorEntity.getOrderStatus())) {
                    return sureUpdate(errorEntity, PayEnum.PAY1.getCode(), CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1012, remark);
                } else {
                    // 订单状态：提现中、超时等
                    final OrderResponse orderResponse = reconciliationWithdrawalsService.withdrawalsRecon(errorEntity.getOrderNo(), "1", errorEntity.getOrderStatus());
                    if (!Constant.SUCCESS.equals(orderResponse.getReturnCode())) {
                        return orderResponse;
                    }
                    updateOrderStatus(errorEntity, PayEnum.PAY1.getCode(), CheckConstant.STANDARD_RESULT_1020);
                    return sureUpdate(errorEntity, PayEnum.PAY1.getCode(), CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1020, remark);
                }
            }
        }
        //状态不匹配
        if (CheckConstant.STATUS_NOT_CONFORM_DESC.equals(errorEntity.getErrorType())) {
            // 充值
            if (TradeTypeEnum.trade_pay.getCode().equals(errorEntity.getOrderType())) {
                log.info("确认成功->状态不匹配->充值：oid={}，payNo={} ,订单状态={}", errorEntity.getOid(), errorEntity.getPayNo(), errorEntity.getOrderStatus());
                // 平台成功	三方失败
                if (PayEnum.PAY1.getCode().equals(errorEntity.getOrderStatus())) {
                    return sureUpdate(errorEntity, PayEnum.PAY1.getCode(), CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1003, remark);
                }
            }
        }
        response.setReturnCode(Constant.FAIL);
        response.setErrorMessage("异常单类型不匹配，数据更新失败！");
        return response;
    }

    /**
     * 确认失败（不尝试扣款）
     */
    @Transactional
    public BaseResponse sureFailed2(ReconciliationErrorRecordsForm req) {
        String remark = StringUtil.isEmpty(req.getRemark()) ? "" : req.getRemark().trim();
        ReconciliationErrorRecordsVo errorEntity = reconciliationErrorRecordsDao.findOne(req.getOid());
        BaseResponse response = new BaseResponse();
        if (!CheckConstant.WAIT_FOR_DEAL.equals(errorEntity.getErrorStatus())) {
            response.setReturnCode(Constant.FAIL);
            response.setErrorMessage("该数据已处理，请刷新页面后操作！");
            return response;
        }
        final String payCode = PayEnum.PAY2.getCode();
        // 平台订单多单
        if (CheckConstant.ORDER_MORETHAN_BANK_DESC.equals(errorEntity.getErrorType())) {
            log.info("确认失败（不尝试扣款）->平台订单多单->充值：oid={}，payNo={} ,订单状态={}", errorEntity.getOid(), errorEntity.getPayNo(), errorEntity.getOrderStatus());
            // 订单状态：充值成功
            if (PayEnum.PAY1.getCode().equals(errorEntity.getOrderStatus())) {
                updateOrderStatus(errorEntity, payCode, errorEntity.getPayNo());
                return sureUpdate(errorEntity, payCode, CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1022, remark);
            }
        }
        //状态不匹配
        if (CheckConstant.STATUS_NOT_CONFORM_DESC.equals(errorEntity.getErrorType())) {
            log.info("确认失败（不尝试扣款）->状态不匹配->充值：oid={}，payNo={} ,订单状态={}", errorEntity.getOid(), errorEntity.getPayNo(), errorEntity.getOrderStatus());
            // 平台成功	三方失败
            if (PayEnum.PAY1.getCode().equals(errorEntity.getOrderStatus())) {
                updateOrderStatus(errorEntity, payCode, errorEntity.getPayNo());
                return sureUpdate(errorEntity, payCode, CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1023, remark);
            }
        }
        response.setReturnCode(Constant.FAIL);
        response.setErrorMessage("异常单类型不匹配，数据更新失败！");
        return response;
    }


    /**
     * 更新订单状态
     */
    private void updateOrderStatus(ReconciliationErrorRecordsVo errorEntity, String payCode, String failDetail) {
        accOrderDao.updateOrderStatus(errorEntity.getOrderNo(), payCode);
        paymentDao.updateByPayNo(payCode, errorEntity.getPayNo());
        bankLogDao.updateByPayNo(payCode, errorEntity.getPayNo());
        comOrderDao.updateStatusAndFailDetailByOrderNo(payCode, errorEntity.getOrderNo(), failDetail);
    }

    /**
     * 确认失败（尝试扣款）
     */
    @Transactional
    public BaseResponse sureFailed1(ReconciliationErrorRecordsForm req) {
        String remark = StringUtil.isEmpty(req.getRemark()) ? "" : req.getRemark().trim();

        ReconciliationErrorRecordsVo errorEntity = reconciliationErrorRecordsDao.findOne(req.getOid());
        BaseResponse response = new BaseResponse();
        if (!CheckConstant.WAIT_FOR_DEAL.equals(errorEntity.getErrorStatus())) {
            response.setReturnCode(Constant.FAIL);
            response.setErrorMessage("该数据已处理，请刷新页面后操作！");
            return response;
        }
        // 平台订单多单
        if (CheckConstant.ORDER_MORETHAN_BANK_DESC.equals(errorEntity.getErrorType())) {
            log.info("确认失败（尝试扣款）->平台订单多单->充值：oid={}，payNo={} ,订单状态={}", errorEntity.getOid(), errorEntity.getPayNo(), errorEntity.getOrderStatus());
            // 充值
            if (TradeTypeEnum.trade_pay.getCode().equals(errorEntity.getOrderType())) {
                // 订单状态：充值成功
                if (PayEnum.PAY1.getCode().equals(errorEntity.getOrderStatus())) {
                    final OrderResponse orderResponse = reconciliationRechargeService.rechargeRecon(errorEntity.getOrderNo(), "2", errorEntity.getOrderStatus());
                    if (!Constant.SUCCESS.equals(orderResponse.getReturnCode())) {
                        return orderResponse;
                    }
                    updateOrderStatus(errorEntity, PayEnum.PAY2.getCode(), CheckConstant.STANDARD_RESULT_1018);
                    return sureUpdate(errorEntity, PayEnum.PAY2.getCode(), CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1018, remark);
                } else {
                    // 订单状态：充值失败、超时等
                    updateOrderStatus(errorEntity, PayEnum.PAY2.getCode(), CheckConstant.STANDARD_RESULT_1013);
                    return sureUpdate(errorEntity, PayEnum.PAY2.getCode(), CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1013, remark);
                }
            }
            // 提现
            if (TradeTypeEnum.trade_payee.getCode().equals(errorEntity.getOrderType())) {
                log.info("确认失败（尝试扣款）->平台订单多单->提现：oid={}，payNo={} ,订单状态={}", errorEntity.getOid(), errorEntity.getPayNo(), errorEntity.getOrderStatus());
                // 订单状态：提现成功、失败、超时等，均需要动帐、修改订单状态
                final OrderResponse orderResponse = reconciliationWithdrawalsService.withdrawalsRecon(errorEntity.getOrderNo(), "2", errorEntity.getOrderStatus());
                if (!Constant.SUCCESS.equals(orderResponse.getReturnCode())) {
                    return orderResponse;
                }
//                updateOrderStatus(errorEntity, PayEnum.PAY2.getCode(), CheckConstant.STANDARD_RESULT_1019);
                return sureUpdate(errorEntity, PayEnum.PAY2.getCode(), CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1019, remark);
            }
        }
        //状态不匹配
        if (CheckConstant.STATUS_NOT_CONFORM_DESC.equals(errorEntity.getErrorType())) {
            log.info("确认失败（尝试扣款）->状态不匹配->充值：oid={}，payNo={} ,订单状态={}", errorEntity.getOid(), errorEntity.getPayNo(), errorEntity.getOrderStatus());
            // 充值
            if (TradeTypeEnum.trade_pay.getCode().equals(errorEntity.getOrderType())) {
                // 平台成功	三方失败
                if (PayEnum.PAY1.getCode().equals(errorEntity.getOrderStatus()) && PayEnum.PAY2.getCode().equals(errorEntity.getOutsideOrderStatus())) {
                    final OrderResponse orderResponse = reconciliationRechargeService.rechargeRecon(errorEntity.getOrderNo(), errorEntity.getOutsideOrderStatus(), errorEntity.getOrderStatus());
                    if (!Constant.SUCCESS.equals(orderResponse.getReturnCode())) {
                        return orderResponse;
                    }
                    updateOrderStatus(errorEntity, PayEnum.PAY2.getCode(), CheckConstant.STANDARD_RESULT_1021);
                    return sureUpdate(errorEntity, PayEnum.PAY2.getCode(), CheckConstant.DEAL_BY_HAND, CheckConstant.STANDARD_RESULT_1021, remark);
                }
            }
        }
        response.setReturnCode(Constant.FAIL);
        response.setErrorMessage("异常单类型不匹配，数据更新失败！");
        return response;
    }


    /**
     * 复合完成 通用更新数据库
     *
     * @param errorEntity         异常数据
     * @param errorStatus 异常处理状态
     * @param errorResult 异常处理结果
     */
    @Transactional
    public BaseResponse artificialCompositeUpdate( ReconciliationErrorRecordsVo errorEntity , String errorStatus, String errorResult, String remark) {
        BaseResponse resp = new BaseResponse();
        resp.setReturnCode(Constant.SUCCESS);
        resp.setErrorMessage("成功");
        int updateRecordsNum = reconciliationErrorRecordsDao.artificialCompositeUpdateData(errorEntity.getOid(), errorStatus, errorResult, remark, CheckConstant.WAIT_FOR_DEAL);
        if (updateRecordsNum == 0) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("数据更新异常！");
            return resp;
        }
        int updatePassNum = reconciliationPassDao.updateRepairStatus(errorEntity.getPayNo());
        log.info("数据更新条数：updateRecordsNum={} updatePassNum={}", updateRecordsNum, updatePassNum);
        return resp;
    }

    /**
     * 确认成功、确认失败 通用更新数据库
     *
     * @param errorEntity       异常单据
     * @param orderStatus 订单状态
     * @param errorStatus 异常处理状态
     * @param errorResult 异常处理结果
     * @param remark      备注
     * @return
     */
    @Transactional
    public BaseResponse sureUpdate(ReconciliationErrorRecordsVo errorEntity, String orderStatus, String errorStatus, String errorResult, String remark) {
        BaseResponse resp = new BaseResponse();
        resp.setReturnCode(Constant.SUCCESS);
        resp.setErrorMessage("成功");
        int updateRecordsNum = reconciliationErrorRecordsDao.sureUpdate(errorEntity.getOid(), orderStatus, errorStatus, errorResult, remark, CheckConstant.WAIT_FOR_DEAL);
        if (updateRecordsNum == 0) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("数据更新异常！");
            return resp;
        }
        int updatePassNum = reconciliationPassDao.updateRepairStatus(errorEntity.getPayNo());
        log.info("数据更新条数：updateRecordsNum={} updatePassNum={}", updateRecordsNum, updatePassNum);
        return resp;
    }

}
