package com.guohuai.boot.pay.form;

import java.math.BigDecimal;
import java.sql.Timestamp;

import lombok.Data;

@Data
public class OrderForm {
    private String oid;
    private String userOid;//用户ID
    private String sourceSystemType;//来源系统类型
    private String orderNo;//订单号
    private String status;//单据状态
    private Timestamp receiveTime;
    private String type;//扣:01 付:02
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal fee = BigDecimal.ZERO;
    private String bankCode;
    private String cardNo;
    private String describe;
    private String remark;
    private String realName;
    private String channel;
    private String failDetail;
    private String settlementStatus;//成功，失败，进行中（冗余）
    private String businessStatus;//跟业务系统对账状态
    private String reconStatus;//跟银行流水对账状态
    private Timestamp createTime;
    private Timestamp updateTime;
    private String requestNo;
    private int page = 1;
    private int rows = 10;
    private String beginTime;
    private String endTime;
    private String phone; // 手机号
    private BigDecimal limitAmount = BigDecimal.ZERO;//小金额
    private BigDecimal maxAmount = BigDecimal.ZERO;//大金额
    private String payNo;
    private String memberId;//商户号
    private String auditStatus;//审核状态，0无需审核、1待审核、2审核通过、3驳回
    private String auditStatusList;//审核状态，多个状态用逗号分隔
    private String operatorName;//操作员名称
    private String operatorId;//操作员ID
    private String auditRemark;//审核备注

}
