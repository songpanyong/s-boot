package com.guohuai.component.util;

/**
 * @author xueyunlong
 * @ClassName: Constant
 * @Description: 通用常量
 * @date 2016年11月9日 上午10:16:07
 */
public final class Constant {
	public static final String SUCCESS = "0000";
	public static final String FAIL = "9999";
	public static final String INPROCESS = "0001";
	public final static String fomat = "yyyy-MM-dd HH:mm:ss";
	public final static String fomatNo = "yyyy-MM-dd";
	public final static int size = 2000;

	/**
	 * 用户的冻结账户
	 */
	public final static String CHANGESTATUS = "CHANGESTATUS";//修改订单状态
	public final static String KILLORDER = "KILLORDER";//撤单
	public final static String FIRSTIME = "1001";//第一次调用
	public final static String SECONDTIME = "1002";//第二次调用
	/**
	 * 赎回已已成功
	 */
	public final static String REDEEM_SUCCESSED="1101";
	
	/**
	 * 已提现成功
	 */
	public final static String WITHHOLD_SUCCESSED="1102";

	/**
	 * 原account
	 */
	public static final String SYSTEM_SOURCE = "SYSTEM";
	//用户类型不存在
	public static final String USERTYPENOTEXISTS = "9001";
	//交易类型不存在
	public static final String ORDERTYPENOTEXISTS = "9002";
	//用户不存在
	public static final String USERNOTEXISTS = "9003";
	//用户已存在
	public static final String USEREXISTS = "9004";
	//账户类型不存在
	public static final String ACCOUNTTYPENOTEXISTS = "9005";
	//账户不存在
	public static final String ACCOUNTNOTEXISTS = "9006";
	//账户余额不足
	public static final String BALANCELESS = "9007";
	//订单已经存在
	public static final String ORDEREXISTS = "9008";
	//关系产品不能为空
	public static final String RelationProductNotNULL = "9009";
	//金额不能为负数
	public static final String BALANCEERROR = "9010";

	//账户状态错误
	public static final String ACCOUNTSTATUSEERROR = "9011";

	//订单不存在
	public static final String ORDERNOEXISTS = "9012";
	//订单状态错误
	public static final String ORDERSTATUSERROR = "9013";
	//产品类别不能为空
	public static final String PRODUCTTYPEISNULL = "9014";
	//请求流水为空
	public static final String REQUEST_NUMBER_IS_NULL = "9015";
	//发行人id不能为空
	public static final String REQUEST_USEROID_IS_NULL = "9016";
	//轧差日期不能为空
	public static final String REQUEST_NETTING_TIME_IS_NULL = "9017";
	//轧差金额不能为空
	public static final String REQUEST_NETTING_BALANCE_IS_NULL = "9018";
	//轧差金额不相同
	public static final String REQUEST_NETTING_BALANCE_NOT_SAME = "9019";
	//轧差金额不足
	public static final String REQUEST_PUBLISHER_BALANCE_NOT_ENOUGH = "9020";
	//请求流水号不能为空
	public static final String REQUESTNO_IS_NULL = "9021";
	//请求流水号已经存在
	public static final String REQUESTNOEXISTS = "9022";
	//批量赎回订单不存在
	public static final String BATCH_REDEEM_ORDER_IS_NULL = "9023";
	//赎回订单总金额不能小于轧差额
	public static final String NETTING_BALANCE_LESS_THAN_REDEEM_BALANCE = "9024";
	//订单号不能为空
	public static final String ACCOUNT_ORDER_IS_NULL = "9025";
	//订单号已存在
	public static final String ACCOUNT_ORDER_EXISTS = "9026";
	//交易类型不能为空
	public static final String ORDERTYPENOT_IS_NULL = "9027";
	
	//订单对应的用户不匹配
	public static final String ORDER_USER_MISMATCH = "9028";
	//订单对应的发行人不匹配
	public static final String ORDER_PUBLISHER_MISMATCH = "9029";
	//订单对应的续投金额不匹配
	public static final String ORDER_CONTINU_BALANCE_MISMATCH = "9030";
	//续投金额不能为空
	public static final String CONTINU_BALANCE_IS_NULL = "9031";
	//续投金额不能为负
	public static final String CONTINU_BALANCE_IS_MINUS = "9032";
	//绑卡失败
	public static final String BIND_CARD_FAIL = "9033";
	//解绑卡失败
	public static final String UNBIND_CARD_FAIL = "9034";

	/**
	 * 未记账
	 */
	public static final String NOACCTING = "0";
	/**
	 * 提现冻结户已记账
	 */
	public static final String FROZENACCTING = "1";
	/**
	 * 基本户已记账
	 */
	public static final String BASICERACCTING = "2";
	
	//审核通过
	public static final String PASS = "1";
	//驳回
	public static final String REJECT = "2";
	//待审核
	public static final String AUDIT = "0";
	//撤销
	public static final String REVOKE = "3";
}
