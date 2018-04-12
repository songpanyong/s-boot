
/**
 * 配置项，提供全局使用各项配置 amd模块，使用requirejs载入
 */
define(['config'], function(config) {
  this.host = '';
  return {
    host: this.host,
    /**
	 * api 接口提供与服务器异步交互地址
	 * 
	 */
    api: {
    	financeUser: {//用户
        	save: this.host + '/account/user/save',
        	update: this.host + '/account/user/update',
        	detail: this.host + '/account/user/detail',
        	list: this.host + '/account/user/list',
        	getUsers: this.host + '/account/user/getUsers'
        },
    	financeAccount: {//账号
        	add: this.host + '/account/account/submit',
        	save: this.host + '/account/account/save',
        	invalid: this.host + '/account/account/delete',
        	seal: this.host + '/account/account/seal',//封存账户
        	frozen: this.host + '/account/account/frozen',//冻结账户
        	thaw: this.host + '/account/account/thaw',//解冻账户
        	update: this.host + '/account/account/update',
        	edit: this.host + '/account/account/edit',
        	detail: this.host + '/account/account/detail',
        	list: this.host + '/account/account/list',
        	thawFrozenList: this.host + '/account/account/thawFrozen/list',//查询账户冻结解冻列表
        	auditList: this.host + '/account/account/audit/list',
        	updateList: this.host + '/account/account/update/list',//查询修改审核列表
        	addReject: this.host + "/account/account/add/reject", //新增账户审核不通过
			addApprove: this.host + "/account/account/add/approve", //新增账户审核通过
			sealReject: this.host + "/account/account/seal/reject", //封存账户审核不通过
			sealApprove: this.host + "/account/account/seal/approve", //封存账户审核通过
			updateReject: this.host + "/account/account/update/reject", //修改账户审核不通过
			updateApprove: this.host + "/account/account/update/approve", //修改账户审核通过
			thawReject: this.host + "/account/account/thaw/reject", //解冻账户审核不通过
			thawApprove: this.host + "/account/account/thaw/approve", //解冻账户审核通过
			frozenReject: this.host + "/account/account/frozen/reject", //冻结账户审核不通过
			frozenApprove: this.host + "/account/account/frozen/approve", //冻结账户审核通过
        	getRelationProducts: this.host + '/account/account/getRelationProducts'
        },
     	accountTrans:{
        	    account_trans_select_list:'/account/trans/detaillist'
        },
    	 accountOrder:{
     	  account_order_select_list:'/account/order/list',
     	 account_order_page:'/account/order/orderPage',
    	  account_event_trans_list:'/account/event/getEventTransList',
    	  account_order_detail:'/account/order/orderDetail'
    	 },
     	accountSign:{
    	  account_sign_select_list:'/account/card/cardlist',
    	  bind_card_query:'/account/card/getBindCardByUserOid'//根据用户id获取已绑卡信息（平台首页展示平台已绑卡信息）
    	},
    	platform:{
    	  audit_apply: this.host+'/account/platformAudit/addAudit',//审核申请（授信额度调整、平台信息修改、登账事件变更）
    	  audit_page: this.host+'/account/platformAudit/page',//审核查询（平台信息审核页面查询）
    	  audit: this.host+'/account/platformAudit/audit',//审核查询（审核授信额度调整、平台信息修改、登账事件变更）
    	  audit_changeRecords: this.host+'/account/platformAudit/changeRecords', //审核查询（审核页面展示修改详情）
    	  audit_revoke: this.host+'/account/platformAudit/revoke',//登账事件撤销
    	  platfrom_info_list: this.host+'/account/platform/platfromInfoList', //获取平台信息下拉
    	  platform_detial: this.host+'/account/platform/getPlatfromInfoByUserOid', //根据用户id获取平台信息
    	  platfrom_info: this.host+'/account/platform/platformInfo', //根据用户id获取平台信息及账户信息
    	  platfrom_page: this.host+'/account/platform/page', // 获取所有平台信息
    	  platfrom_info_change: this.host+'/account/platformAudit/paltformChangeRecords',// 平台信息首页展示变更记录
    	  platfrom_account_info: this.host+'/account/platform/platfromAccountInfoList',// 备付金下拉及备付金信息
    	  bind_card_audit_info: this.host+'/account/bindCardAudit/auditInfo', // 获取单个平台绑卡审核信息
    	  bind_card_audit_page: this.host+'/account/bindCardAudit/page', // 获取平台绑卡审核信息
    	  bind_card_audit_apply: this.host+'/account/bindCardAudit/apply', // 平台绑卡申请
    	  bind_card_unlock: this.host+'/account/bindCardAudit/unlock', // 平台绑卡手动尝试解绑
    	  bind_card_bindApply: this.host+'/account/bindCardAudit/bindApply', // 个人卡平台绑卡申请获取验证码
    	  bind_card_bindConfrim: this.host+'/account/bindCardAudit/bindConfrim', // 个人卡平台绑卡确认
    	  bind_card_audit: this.host+'/account/bindCardAudit/audit', // 平台绑卡审核
    	  event_page: this.host+'/account/event/page', //登账事件查询页面
    	  event_effect_info: this.host+'/account/event/getEffectInfo' //登账事件时间设置详情
    	}
    }

  }
})