/**
 * 银行账户管理
 */
define([
	'http',
//	'config',
	'settleConfig',
	'util',
	'extension'
], function(http,settleConfig, util, $$) {
	return {
		name: 'informationApproval',
		init: function() {
			// 分页配置
			var pageOptions = {
				page: 1,
				row: 10
			}
			
			var confirm = $('#confirmModal');

			// 初始化数据表格
			var tableConfig = {

				ajax: function(origin) {
					http.post(settleConfig.api.settlement.settlement_informationApproval_approval, {
						data: pageOptions,
						contentType: 'form'
					}, function(rlt) {
						rlt.rows.forEach(function(item) {
							item.accountType = item.accountType === '01' ? '基本户' :
								item.accountType === '02' ? '一般户' :
								item.accountType === '03' ? '收入户' : '支出户';
							item.accountStatus = item.accountStatus === '0' ? '可用' :
								item.accountStatus === '1' ? '禁用' :
								item.accountStatus === '2' ? '待审核' : 
								item.accountStatus === '3' ? '待提交' :'已拒绝';  
						})
						origin.success(rlt)
					})
				},
				pageNumber: pageOptions.page,
				pageSize: pageOptions.row,
				pagination: true,
				sidePagination: 'server',
				pageList: [10, 20, 30, 50, 100],
//				queryParams: getQueryParams,
				onLoadSuccess: function() {},
				columns: [
					{
						width: 100,
						field: 'bankAccount'
					}, {
						width: 100,
						field: 'bankAccountName'
					}, {
						width: 100,
						field: 'bankAccontClass'
					}, {
						width: 100,
						field: 'accountFullName'
					}, {
						width: 100,
						field: 'bankAddress'
					}, {
						width: 100,
						field: 'openAccountProvince'
					}, {
						width: 100,
						field: 'openAccountCity'
					}, {
						width: 100,
						field: 'accountType'
					}, {
						width: 100,
						field: 'accountStatus'
					},
					{
						width: 100,
						field: 'createTime'
					}, {
						width: 100,
						align: 'center',
						filed: 'update',
						title: '操作',
						formatter: function(value, row) {
							return '<div class="func-area">' +
								'<a href="javascript:void(0)" class="style-approve"  data-toggle="modal">同意</a>' +
								'<a href="javascript:void(0)" class="style-refuse"  data-toggle="modal">拒绝</a>' +
								'<a href="javascript:void(0)" class="style-detail"  data-toggle="modal">详情</a>'+
								'</div>'
						},
						events: {
						//详情
						  'click .style-detail': function(e, val, row) {
									console.log('oid' + row.oid);
									var bankAccount = row.bankAccount;
									var bankAccountName = row.bankAccountName;
									var bankAccontClass = row.bankAccontClass;
									var accountFullName = row.accountFullName;
									var bankAddress = row.bankAddress;
									var openAccountProvince = row.openAccountProvince;
									var openAccountCity = row.openAccountCity;
									var accountType = row.accountType;
									var accountStatus = row.accountStatus;
									$("#bankAccount").html(bankAccount);
									$("#bankAccountName").html(bankAccountName);
									$("#bankAccontClass").html(bankAccontClass);
									$("#accountFullName").html(accountFullName);
									$("#bankAddress").html(bankAddress);
									$("#openAccountProvince").html(openAccountProvince);
									$("#openAccountCity").html(openAccountCity);
									$("#accountType").html(accountType);
									$("#accountStatus").html(accountStatus);									
									$('#informationDetailModal').modal('show');
							},							
						//同意
						  'click .style-approve': function(e, val, row) {
									console.log('oid' + row.oid);
									var oid = row.oid;
									var resource = settleConfig.api.settlement.settlement_information_updateStatus;
									console.log('resource' + resource);
									confirm.find('.popover-title').html('提示');
									confirm.find('p').html('确定审批？');
									$("#tips_cancle").show();		
									$$.confirm({
										container: confirm,
										trigger: this,
										accept: function() {
											http.get(resource, {
												data: {
													oid: oid,
													accountStatus:0
												}
											}, function(res) {
												if(res.errorCode == 0) {
													confirm.modal('hide');
													$('#settlement_approval_information').bootstrapTable('refresh',pageOptions);
												} else {
													errorHandle(res);
												}
											})
										}
									})
							},
						//拒绝 
						  'click .style-refuse': function(e, val, row) {
									console.log('oid' + row.oid);
									var oid = row.oid;
									var resource = settleConfig.api.settlement.settlement_information_updateStatus;
									console.log('resource' + resource);
									confirm.find('.popover-title').html('提示');
									confirm.find('p').html('拒绝审批？');									
									$("#tips_cancle").show();		
									$$.confirm({
										container: confirm,
										trigger: this,
										accept: function() {
											http.get(resource, {
												data: {
													oid: oid,
													accountStatus:3
												}
											}, function(res) {
												if(res.errorCode == 0) {
													confirm.modal('hide');
													$('#settlement_approval_information').bootstrapTable('refresh',pageOptions);
												} else {
													errorHandle(res);
												}
											})
										}
									})	
							},					

						}
					},
				]
			}

			// 初始化数据表格
			$('#settlement_approval_information').bootstrapTable(tableConfig);
		}
	}
})