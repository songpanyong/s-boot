/**
 * 配置项，提供全局使用各项配置
 * amd模块，使用requirejs载入
 */
define(function () {
this.host = '';
	return {
		host: this.host,    // 系统地址
		api:{
			settlement:{
				settlement_addChannel: this.host + '/settlement/channel/save', //结算通道配置
				settlement_updateChannel: this.host + '/settlement/channel/update', //结算通道修改
				settlement_getChannelPage: this.host + '/settlement/channel/page', //结算通道查询
				settlement_changeChannelStatus: this.host + '/settlement/channel/changeStatus', //结算通道状态修改
				settlement_order_page:this.host+'/settlement/order/page',//订单分页查询
				settlement_order_down:this.host+'/settlement/order/down',//订单导出
	            settlement_order_one:this.host+'/settlement/order/findOne',//查询详情
				settlement_order_checkInOrder:this.host+'/settlement/order/checkInOrder', // 手动录入订单
				settlement_order_checkInOrderAudit:this.host+'/settlement/order/checkInOrderAudit',// 手动录入订单审核
	            settlement_payment_page:this.host+'/settlement/payment/page',//指令查询
	            settlement_payment_page_batch:this.host+'/settlement/payment/page2',//提现审核查询
	            settlement_payment_update:this.host+'/settlement/payment/update',//单笔支付(失败重发)|撤回|修改
	            settlement_payment_audit:this.host+'/settlement/payment/audit',//审核
	            
	            settlement_payment_pageBath:this.host+'/settlement/payment/pageBath',//指令查询
	            settlement_payment_updateBath:this.host+'/settlement/payment/updateBath',//单笔支付(失败重发)|撤回|修改
	            settlement_payment_auditBath:this.host+'/settlement/payment/auditBath',//审核
	            settlement_payment_pageLargeAmount:this.host+'/settlement/payment/pageLargeAmount',//大额指令查询
	            settlement_payment_batchUpdate:this.host+'/settlement/payment/batchUpdate',//批量支付(失败重发)|撤回|修改
	            settlement_payment_audit_batch:this.host+'/settlement/payment/batchAudit',//批量审核
	            settlement_payment_getAuditBalance:this.host+'/settlement/payment/getAuditBalance',//提现审核总金额
	            
	            settlement_banklog_page:this.host+'/settlement/bankLog/page',//交互日志查询
	            settlement_banklog_findNo:this.host+'/settlement/bankLog/findByOrderNo',//交互日志查询
	            settlement_banklog_findReconc:this.host+'/settlement/payment/findReconc',//交互日志查询
	            
	            settlement_payment_operate:this.host+'/operate_cloud/admin/info',//获取当前用户信息
	            settlement_information_page:this.host+'/settlement/information/page',//银行账户查询
                settlement_information_save:this.host+'/settlement/information/save',//银行账户新增
                settlement_information_update:this.host+'/settlement/information/update',//银行账户信息修改
                settlement_information_updateStatus:this.host+'/settlement/information/updateStatus',//银行账户状态修改
                settlement_informationApproval_approval:this.host+'/settlement/information/approval',//银行账户审批查询
                settlement_bankReconciliation_page:this.host+'/settlement/bankHistory/page',//银行查询对账
                settlement_bankReconciliation_ignore:this.host+'/settlement/bankHistory/ignore',//银行忽略对账
                settlement_bankReconciliation_recon:this.host+'/settlement/bankHistory/getRecon',//银行获取对账文件
                settlement_bankReconciliation_getReconRequest:this.host+'/settlement/bankHistory/getReconRequest',//银行获取对账文件
                settlement_orderReconciliation_check:this.host+'/settlement/orderReconciliation/check',//执行对账
				settlement_kftReconciliation_getRecon:this.host+'/settlement/pass/getRecon',//快付通获取对账文件并入库
				settlement_reconciliation_page:this.host+'/settlement/pass/page',//快付通、先锋支付、金运通、宝付查询对账结果
				settlement_reconciliation_doRecon:this.host+'/settlement/pass/ucfRecon',//执行对账
				settlement_reconciliation_uploadUcfRecon:this.host+'/settlement/pass/uploadUcfRecon',//先锋支付导入对账文件并入库
				settlement_reconciliation_recon_ignore:this.host+'/settlement/pass/ignoreRecon',//先锋支付、快付通忽略对账
				settlement_reconciliation_uploadNanyueRecon:this.host+'/settlement/pass/uploadNanyueRecon',//南粤银行导入对账文件并入库
				settlement_reconciliation_uploadJytRecon:this.host+'/settlement/pass/uploadJytRecon',//金运通导入对账文件并入库
				settlement_exceptionOrderCheck_pageEX:this.host+'/settlement/payment/pageEX', //异常订单查询
				settlement_exceptionOrderCheck_ignore:this.host+'/settlement/payment/ignoreRecon', //忽略异常订单
				settlement_exceptionOrderCheck_changeStatus:this.host+'/settlement/payment/changeStatus', //修改订单状态
				settlement_comChannelBank_page:this.host+'/settlement/channelBank/page', //结算通道银行信息查询
				settlement_comChannelBank_save:this.host+'/settlement/channelBank/save', //结算通道银行信息增加
				settlement_comChannelBank_update:this.host+'/settlement/channelBank/update', //结算通道银行信息修改
				settlement_comChannelBank_delete:this.host+'/settlement/channelBank/delete', //结算通道银行信息删除
//				settlement_comChannelBank_findBank:this.host+'/settlement/channelBank/findBank', //校验银行名称信息
				settlement_comChannelBank_findAllBank:this.host+'/settlement/channelBank/findAllBank', //查询所有已开通的渠道支持的银行信息
				settlement_comChannelBank_findBankByCard:this.host+'/settlement/channelBank/findBankByCard', //通过卡号查询银行名称
				settlement_comChannelBank_findAllBankName:this.host+'/settlement/channelBank/findAllBankName', //查询所有银行名称
				settlement_bankCallback_page:this.host+'/settlement/bankCallback/page', //查询银行回调信息
				settlement_bankCallback_handCallBack:this.host+'/settlement/bankCallback/addCallBackCount', //新增回调次数
				settlement_bankCallbackLog_list:this.host+'/settlement/bankCallbackLog/listbycboid',//查询银行回调日志信息
				settlement_baofooReconciliation_getRecon:this.host+'/settlement/pass/uploadBaofooRecon',//宝付获取对账文件并入库
				settlement_order_memBalance:this.host+'/settlement/order/getMemBalance',//商户基本户余额
				settlement_reconciliation_statistics:this.host+'/settlement/statistics/getData',//对账统计信息
				settlement_reconciliation_confirmComplete:this.host+'/settlement/statistics/confirmComplete',//确认完成对账
				settlement_reconciliation_success:this.host+'/settlement/reconciliationErrorRecords/success',//确认成功
				settlement_reconciliation_composite:this.host+'/settlement/reconciliationErrorRecords/composite',//复合完成
				settlement_reconciliation_failedWithTryToCharge:this.host+'/settlement/reconciliationErrorRecords/failedWithTryToCharge',//确认失败（尝试扣款）
				settlement_reconciliation_failedWithNoCharge:this.host+'/settlement/reconciliationErrorRecords/failedWithNoCharge',//确认失败（不尝试扣款）
				settlement_reconciliation_records_page:this.host+'/settlement/reconciliationErrorRecords/page',//分页数据
				settlement_reconciliation_records_mimosa:this.host+'/mimosa/boot/tradeorder/mng',//申购数据

			}
		}
	}
})
