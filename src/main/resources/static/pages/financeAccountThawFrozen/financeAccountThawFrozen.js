/**
 * 财务账户管理--账户冻结/解冻
 */
define([
	'http',
	'config',
	'accountConfig',
	'util',
	'extension'
], function(http, config, accountConfig, util, $$) {
	return {
		name: 'financeAccountThawFrozen',
		init: function() {
			// js逻辑写在这里
			/**
			 * 数据表格分页、搜索条件配置
			 */
			var pageOptions = {
				number: 1,
				size: 10,
				userOid: '',
				accountType: '',
				phone: '',
				relationProduct: '',
				openTimeBegin: '',
				openTimeEnd: '',
				userType: '',
				frozenStatus: ''
			}
			
			/**
			 * 表格querystring扩展函数，会在表格每次数据加载时触发，用于自定义querystring
			 * @param {Object} val
			 */
			function getQueryParams(val) {
				var form = document.searchForm
				pageOptions.size = val.limit
				pageOptions.number = parseInt(val.offset / val.limit) + 1
				pageOptions.userOid = form.userOid.value.trim()
				pageOptions.accountType = form.accountType.value.trim()
				pageOptions.phone = form.phone.value.trim()
				pageOptions.openTimeBegin = form.openTimeBegin.value
				pageOptions.openTimeEnd = form.openTimeEnd.value
				pageOptions.userType = form.userType.value.trim()
				pageOptions.frozenStatus = form.frozenStatus.value.trim()
				return val
			}

			/**
			 * 数据表格配置
			 */
			var tableConfig = {
				ajax: function(origin) {
					http.post(
						accountConfig.api.financeAccount.thawFrozenList, {
							data: {
								page: pageOptions.number,
								rows: pageOptions.size,
								userOid: pageOptions.userOid,
								accountType: pageOptions.accountType,
								phone: pageOptions.phone,
								openTimeBegin: pageOptions.openTimeBegin,
								openTimeEnd: pageOptions.openTimeEnd,
								userType: pageOptions.userType,
								frozenStatus: pageOptions.frozenStatus
							},
							contentType: 'form'
						},
						function(rlt) {
							origin.success(rlt)
						}
					)
				},
				pageNumber: pageOptions.number,
				pageSize: pageOptions.size,
				pagination: true,
				sidePagination: 'server',
				pageList: [10, 20, 30, 50, 100],
				queryParams: getQueryParams,
				onLoadSuccess: function() {},
				columns: [{
					width: 30,
					align: 'center',
					formatter: function(val, row, index) {
						return(pageOptions.number - 1) * pageOptions.size + index + 1
					}
				}, {
					field: 'accountNo',
					align: 'right'
				}, {
					field: 'accountName'
				}, {
					field: 'accountType',
					formatter: function(val, row, index) {
						switch(val) {
							case '01':
								return '活期户'
							case '02':
								return '活期利息户'
							case '03':
								return '体验金'
							case '04':
								return '在途户'
							case '05':
								return '冻结户'
							case '06':
								return '定期户'	
							case '07':
								return '产品户'
							case '08':
								return '备付金户'
							case '09':
								return '超级户'
							case '10':
								return '基本户'	
							case '11':
								return '运营户'	
							default:
								return '-'
						}
					}
				}, {
					field: 'userOid',
					align: 'right'
				}, {
					field: 'userType',
					formatter: function(val, row, index) {
						switch(val) {
							case 'T1':
								return '投资人'
							case 'T2':
								return '发行人'
							case 'T3':
								return '平台'
							default:
								return '-'
						}
					}
				}, {
					field: 'userName',
				}, {
					field: 'phone',
					align: 'right'
				}, {	
					field: 'openTime',
					align: 'right',
					formatter: function(val) {
						return util.table.formatter.timestampToDate(val, 'YYYY-MM-DD')
					}
				}, {
					field: 'relationProduct',
				}, {
					field: 'frozenStatus',
					formatter: function(val, row, index) {
						switch(val) {
							case 'NORMAL':
								return '正常'
							case 'FROZEN':
								return '冻结'
							case 'FROZENAU':
								return '冻结审批中'
							case 'THAWAU':
								return '解冻审批中'
							default:
								return '-'
						}
					}
				}
//				,
//				{
//					align: 'center',
//					formatter: function(val, row) {
//						var buttons = [{
//							text: '详情',
//							type: 'button',
//							class: 'item-detail',
//							isRender: true
//						}, {
//							text: '冻结',
//							type: 'button',
//							class: 'item-frozen',
//							isRender: row.frozenStatus == 'NORMAL'
//						}, {
//							text: '解冻',
//							type: 'button',
//							class: 'item-thaw',
//							isRender: row.frozenStatus == 'FROZEN'
//						}];
//						return util.table.formatter.generateButton(buttons, 'financeAccountTable');
//					},
//					events: {
//						'click .item-detail': function(e, value, row) {
//							http.post(accountConfig.api.financeAccount.detail, {
//								data: {
//									oid: row.oid
//								},
//								contentType: 'form'
//							}, function(result) {
//								if(result.returnCode == 0) {
//									var data = result;
//									
//									if(data.userType=='T1') {
//										data.userTypeStr = '投资人'
//									} else if(data.userType=='T2') {
//										data.userTypeStr = '发行人'
//									} else if(data.userType=='T3') {
//										data.userTypeStr = '平台'
//									} else {
//										data.userTypeStr = '-'
//									}
//									
//									if(data.accountType=='01') {
//										data.accountTypeStr = '活期户'
//									} else if(data.accountType=='02') {
//										data.accountTypeStr = '活期利息户'
//									} else if(data.accountType=='03') {
//										data.accountTypeStr = '体验金'
//									} else if(data.accountType=='04') {
//										data.accountTypeStr = '在途户'
//									} else if(data.accountType=='05') {
//										data.accountTypeStr = '冻结户'
//									} else if(data.accountType=='06') {
//										data.accountTypeStr = '定期户'
//									} else if(data.accountType=='07') {
//										data.accountTypeStr = '产品户'
//									} else if(data.accountType=='08') {
//										data.accountTypeStr = '备付金户'
//									} else if(data.accountType=='09') {
//										data.accountTypeStr = '超级户'
//									} else if(data.accountType=='10') {
//										data.accountTypeStr = '基本户'
//									} else if(data.accountType=='11') {
//										data.accountTypeStr = '运营户'
//									} else {
//										data.accountTypeStr = '-'
//									}
//
//									$$.detailAutoFix($('#accountDetailModal'), data); // 自动填充详情
//									$('#accountDetailModal').modal('show');
//								} else {
//									alert('查询失败');
//								}
//							})
//						},
//						'click .item-frozen': function(e, value, row) {
//							$("#confirmTitle").html("确定冻结吗？")
//							$$.confirm({
//								container: $('#doConfirm'),
//								trigger: this,
//								accept: function() {
//									http.post(accountConfig.api.financeAccount.frozen, {
//										data: {
//											oid: row.oid
//										},
//										contentType: 'form',
//									}, function(result) {
//										$('#financeAccountTable').bootstrapTable('refresh')
//									})
//								}
//							})
//						},
//						'click .item-thaw': function(e, value, row) {
//							$("#confirmTitle").html("确定解冻吗？")
//							$$.confirm({
//								container: $('#doConfirm'),
//								trigger: this,
//								accept: function() {
//									http.post(accountConfig.api.financeAccount.thaw, {
//										data: {
//											oid: row.oid
//										},
//										contentType: 'form',
//									}, function(result) {
//										$('#financeAccountTable').bootstrapTable('refresh')
//									})
//								}
//							})
//						}
//					}
//				}
				]
			}

			/**
			 * 数据表格初始化
			 */
			$('#financeAccountTable').bootstrapTable(tableConfig)

			/**
			 * 搜索表单初始化
			 */
			$$.searchInit($('#searchForm'), $('#financeAccountTable'))
			

		}
	}
})