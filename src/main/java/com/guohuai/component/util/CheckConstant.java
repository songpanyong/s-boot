package com.guohuai.component.util;

/**
 * 对账常量
 *
 * @author chendonghui
 */
public final class CheckConstant {

    public static final String SUCCESS = "0000";
    public static final String FAIL = "9999";

    public static final int CHECK_SUCCESS = 1;//对账成功
    public static final int CHECK_FAIL = 2;//对账失败
    /**
     * 等待人工处理
     */
    public static final  String ERRORSORT_WAIT_FOR_DEAL="0";
    /**
     * 已处理完成
     */
    public  static  final  String ERRORSORT_FINISH="1";

    //异常状态
    public static final String BANK_MORETHAN_ORDER_DESC = "三方订单多单";
    public static final String ORDER_MORETHAN_BANK_DESC = "平台订单多单";
    public static final String AMOUNT_NOT_CONFORM_DESC = "金额不匹配";
    public static final String STATUS_NOT_CONFORM_DESC = "状态不匹配";

    //异常处理状态
    public static final String WAIT_FOR_DEAL = "等待人工处理";
    public static final String DEAL_BY_HAND = "人工处理完成";
    public static final String DEAL_BY_SYSTEM = "系统自动处理成功";
    public static final String DEAL_FAIL = "系统自动处理成功";

    //处理结果标准化输出
    //充值：平台成功，三方失败
    public static final String STANDARD_RESULT_1001 = "系统自动扣减用户账户余额成功并变更订单状态为“交易失败”";
    public static final String STANDARD_RESULT_1002 = "系统自动扣减用户账户余额失败，需人工确认";
    //充值：平台成功，确认成功，只标记终态；
    public static final String STANDARD_RESULT_1003 = "系统自动扣减用户账户余额失败，手动确认订单状态为“确认成功”";
    //充值：平台成功，确认失败，尝试扣减用户余额，若成功则修改订单状态为失败，若失败，取消无操作，确认失败，更改订单状态为失败
    public static final String STANDARD_RESULT_1004 = "系统自动扣减用户账户余额失败，手动确认订单状态为“确认失败”";//手动在此尝试
    public static final String STANDARD_RESULT_1016 = "系统自动扣减用户账户余额成功，变更订单状态为“交易失败”";//手动在此尝试
    //充值：平台失败，三方成功；充值：平台处理中、超时，三方成功
    public static final String STANDARD_RESULT_1005 = "系统变更订单状态为“交易成功”，并增加账户余额成功";
    //充值：平台处理中、超时，三方失败
    public static final String STANDARD_RESULT_1006 = "系统变更订单状态为“交易失败”";
    //提现：平台成功，三方失败；提现：平台处理中、超时，三方失败
    public static final String STANDARD_RESULT_1007 = "系统变更订单状态为“交易失败”，并增加账户余额成功";
    //提现：平台失败，三方成功；提现：平台处理中、超时，三方成功
    public static final String STANDARD_RESULT_1008 = "系统变更订单状态为“交易成功”，并扣减账户余额成功";//提现失败的订单，业务不能实时解冻，以对账结果为准

    //三方多单
    public static final String STANDARD_RESULT_1009 = "该笔订单平台系统查询不到，需人工复核";
    public static final String STANDARD_RESULT_1010 = "该笔订单平台系统查询不到，人工复核完成";

    //平台多单
    public static final String STANDARD_RESULT_1011 = "该笔订单三方对账文件查询不到，需人工确认";
    //充值：平台成功，确认成功，只标记终态；充值：平台失败、处理中、超时，确认成功，修改订单状态为成功，增加账户余额
    //提现：平台成功，确认成功，只标记终态；提现：平台失败、处理中、超时，确认成功，修改订单状态为成功，扣减账户余额（业务不能解冻）
    public static final String STANDARD_RESULT_1012 = "该笔订单三方对账文件查询不到，手动确认订单状态为“确认成功”";
    //充值：平台成功，确认失败，尝试扣减用户余额，若成功则修改订单状态为失败，若失败，取消无操作，确认失败，更改订单状态为失败
    //充值：平台失败、处理中、超时，确认失败，修改订单状态为失败
    //提现：平台成功，确认失败，增加账户余额，修改订单状态为失败
    //提现：平台失败，确认失败，标记为终态；提现：平台处理中、超时，确认失败，修改订单状态为失败
    public static final String STANDARD_RESULT_1013 = "该笔订单三方对账文件查询不到，手动确认订单状态为“确认失败”";

    //金额不匹配
    public static final String STANDARD_RESULT_1014 = "该笔订单金额不匹配，需人工复核";
    public static final String STANDARD_RESULT_1015 = "该笔订单金额不匹配，人工复核完成";

    public static final String STANDARD_RESULT_1017 = "该笔订单三方对账文件查询不到，手动确认订单状态为“确认成功”，并增加用户账户余额成功";
    public static final String STANDARD_RESULT_1018 = "该笔订单三方对账文件查询不到，手动确认订单状态为“确认失败”，并扣减用户账户余额成功";
    public static final String STANDARD_RESULT_1019 = "该笔订单三方对账文件查询不到，手动确认订单状态为“确认失败”，并增加用户账户余额成功";
    public static final String STANDARD_RESULT_1020 = "该笔订单三方对账文件查询不到，手动确认订单状态为“确认成功”，并扣减用户账户余额成功";
    public static final String STANDARD_RESULT_1021 = "系统自动扣减用户账户余额失败，手动确认订单状态为“确认失败”，并扣减用户账户余额成功";
    public static final String STANDARD_RESULT_1022 = "该笔订单三方对账文件查询不到，手动确认订单状态为“确认失败”，并扣减用户账户余额失败";
    public static final String STANDARD_RESULT_1023 = "系统自动扣减用户账户余额失败，手动确认订单状态为“确认失败”，并扣减用户账户余额失败";


}
