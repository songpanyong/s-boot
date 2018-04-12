/**
 * 财务系统审核
 */
define([
	'http',
	'config',
	'accountConfig',
	'util',
	'extension'
], function(http, config, accountConfig, util, $$) {
	return {
		name: 'financeAccountAudit',
		init: function() {
			
			var currentAccount = null

			/**
			 * 账户审核数据表格分页、搜索条件配置
			 */
			var accountAuditPageOptions = {
				number: 1,
				size: 10,
				userType: '',
				status: ''
			}

			/**
			 * 账户表格querystring扩展函数，会在表格每次数据加载时触发，用于自定义querystring
			 * @param {Object} val
			 */
			function getAUQueryParams(val) {
				var form = document.accountSearchForm
				accountAuditPageOptions.size = val.limit
				accountAuditPageOptions.number = parseInt(val.offset / val.limit) + 1
				accountAuditPageOptions.userType = form.userType.value.trim()
				accountAuditPageOptions.status = form.status.value.trim()
				return val
			}

			/**
			 * 账户审核数据表格配置
			 */
			var accountAuditTableConfig = {
				ajax: function(origin) {
					http.post(
						accountConfig.api.financeAccount.auditList, {
							data: {
								page: accountAuditPageOptions.number,
								rows: accountAuditPageOptions.size,
								userType: accountAuditPageOptions.userType,
								status: accountAuditPageOptions.status
							},
							contentType: 'form'
						},
						function(rlt) {
							origin.success(rlt)
						}
					)
				},
				pageNumber: accountAuditPageOptions.number,
				pageSize: accountAuditPageOptions.size,
				pagination: true,
				sidePagination: 'server',
				pageList: [10, 20, 30, 50, 100],
				queryParams: getAUQueryParams,
				onLoadSuccess: function() {},
				columns: [{
					width: 30,
					align: 'center',
					formatter: function(val, row, index) {
						return(accountAuditPageOptions.number - 1) * accountAuditPageOptions.size + index + 1
					}
				}, {
					field: 'accountNo',
					align: 'right'
				}, {
					field: 'accountName'
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
//				}, {
//					field: 'userName',
				}, {
					field: 'relationProductName',
				},	
					{
					field: 'status',
					formatter: function(val, row, index) {
						switch(val) {
							case 'SUBMIT':
								return '提交'
							case 'SEALING':
								return '封存中'
							default:
								return '-'
						}
					}
				}, {
					field: 'updateTime',
					align: 'right'
				}, {
					align: 'center',
					formatter: function(val, row) {
						var buttons = [{
							text: '详情',
							type: 'button',
							class: 'item-detail',
							isRender: true
						}, {
							text: '同意',
							type: 'button',
							class: 'item-approve',
							isRender: true
						}, {
							text: '拒绝',
							type: 'button',
							class: 'item-reject',
							isRender: true
						}];
						return util.table.formatter.generateButton(buttons, 'accountAuditTable');
					},
					events: {
						'click .item-detail': function(e, value, row) {
							http.post(accountConfig.api.financeAccount.detail, {
								data: {
									oid: row.oid
								},
								contentType: 'form'
							}, function(result) {
								if(result.returnCode == 0) {
									var data = result;

									if(data.userType == 'T1') {
										data.userTypeStr = '投资人'
									} else if(data.userType == 'T2') {
										data.userTypeStr = '发行人'
									} else if(data.userType == 'T3') {
										data.userTypeStr = '平台'
									} else {
										data.userTypeStr = '-'
									}

									if(data.accountType == '01') {
										data.accountTypeStr = '活期户'
									} else if(data.accountType == '02') {
										data.accountTypeStr = '活期利息户'
									} else if(data.accountType == '03') {
										data.accountTypeStr = '体验金'
									} else if(data.accountType == '04') {
										data.accountTypeStr = '在途户'
									} else if(data.accountType == '05') {
										data.accountTypeStr = '冻结户'
									} else if(data.accountType == '06') {
										data.accountTypeStr = '定期户'
									} else if(data.accountType == '07') {
										data.accountTypeStr = '产品户'
									} else if(data.accountType == '08') {
										data.accountTypeStr = '备付金户'
									} else if(data.accountType == '09') {
										data.accountTypeStr = '超级户'
									} else if(data.accountType == '10') {
										data.accountTypeStr = '基本户'
									} else if(data.accountType == '11') {
										data.accountTypeStr = '运营户'
									} else {
										data.accountTypeStr = '-'
									}

									$$.detailAutoFix($('#accountDetailModal'), data); // 自动填充详情
									$('#accountDetailModal').modal('show');
								} else {
									alert('查询失败');
								}
							})
						},
						'click .item-approve': function(e, value, row) {
							$('#auditForm').validator('destroy')

							$("#auditOid").val(row.oid)
							if(row.status == 'SUBMIT') {
								$('#auditTitle').html('账户新增审批-->同意')
								$("#auditType").val('SUBMIT')
							} else if(row.status == 'SEALING') {
								$('#auditTitle').html('账户封存审批-->同意')
								$("#auditType").val('SEALING')
							}
							$("#auditStatus").val('pass')
							$("#auditComment").val("")
							document.auditForm.auditComment.value = ""
							util.form.validator.init($('#auditForm'))
							$('#auditModal').modal('show')
						},
						'click .item-reject': function(e, value, row) {
							$('#auditForm').validator('destroy')

							$("#auditOid").val(row.oid)
							if(row.status == 'SUBMIT') {
								$('#auditTitle').html('账户新增审批-->拒绝')
								$("#auditType").val('SUBMIT')
							} else if(row.status == 'SEALING') {
								$('#auditTitle').html('账户封存审批-->拒绝')
								$("#auditType").val('SEALING')
							}
							$("#auditStatus").val('fail')
							$("#auditComment").val("")

							document.auditForm.auditComment.value = ""
							util.form.validator.init($('#auditForm'))
							$('#auditModal').modal('show')
						}

					}
				}]
			}

			$('#accountAuditTable').bootstrapTable(accountAuditTableConfig)
			$$.searchInit($('#accountSearchForm'), $('#accountAuditTable'))

			/**
			 * 投资人账户修改审核数据表格分页、搜索条件配置
			 */
			var thawFrozenPageOptions = {
				number: 1,
				size: 10
			}

			/**
			 * 投资人账户修改审核表格querystring扩展函数，会在表格每次数据加载时触发，用于自定义querystring
			 * @param {Object} val
			 */
			function getTFQueryParams(val) {
				thawFrozenPageOptions.size = val.limit
				thawFrozenPageOptions.number = parseInt(val.offset / val.limit) + 1
				return val
			}

			/**
			 * 投资人账户修改审核数据表格配置
			 */
			var investorAccountTableConfig = {
				ajax: function(origin) {
					http.post(
						accountConfig.api.financeAccount.updateList, {
							data: {
								page: thawFrozenPageOptions.number,
								rows: thawFrozenPageOptions.size
							},
							contentType: 'form'
						},
						function(rlt) {
							origin.success(rlt)
						}
					)
				},
				pageNumber: thawFrozenPageOptions.number,
				pageSize: thawFrozenPageOptions.size,
				pagination: true,
				sidePagination: 'server',
				pageList: [10, 20, 30, 50, 100],
				queryParams: getTFQueryParams,
				onLoadSuccess: function() {},
				columns: [{
					width: 30,
					align: 'center',
					formatter: function(val, row, index) {
						return(thawFrozenPageOptions.number - 1) * thawFrozenPageOptions.size + index + 1
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
//				}, {
//					field: 'userName',
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
				},{
					field: 'relationProductName',
				},	
				   {
					field: 'phone',
					align: 'right'
				}, {
					field: 'remark'
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
				}, {
					field: 'updateTime',
					align: 'right'
				}, {
					field: 'openTime',
					align: 'right',
					formatter: function(val) {
						return util.table.formatter.timestampToDate(val, 'YYYY-MM-DD')
					}
				}, {
					align: 'center',
					formatter: function(val, row) {
						var buttons = [{
							text: '审批',
							type: 'button',
							class: 'item-audit',
							isRender: true
						}];
						return util.table.formatter.generateButton(buttons, 'investorAccountTable');
					},
					events: {
						'click .item-audit': function(e, value, row) {
							currentAccount = row
							http.post(accountConfig.api.financeAccount.detail, {
								data: {
									oid: row.oid
								},
								contentType: 'form'
							}, function(result) {
								if(result.returnCode == 0) {
									var data = result;

									if(data.userType == 'T1') {
										data.userTypeStr = '投资人'
									} else if(data.userType == 'T2') {
										data.userTypeStr = '发行人'
									} else if(data.userType == 'T3') {
										data.userTypeStr = '平台'
									} else {
										data.userTypeStr = '-'
									}

									if(data.accountType == '01') {
										data.accountTypeStr = '活期户'
									} else if(data.accountType == '02') {
										data.accountTypeStr = '活期利息户'
									} else if(data.accountType == '03') {
										data.accountTypeStr = '体验金'
									} else if(data.accountType == '04') {
										data.accountTypeStr = '在途户'
									} else if(data.accountType == '05') {
										data.accountTypeStr = '冻结户'
									} else if(data.accountType == '06') {
										data.accountTypeStr = '定期户'
									} else if(data.accountType == '07') {
										data.accountTypeStr = '产品户'
									} else if(data.accountType == '08') {
										data.accountTypeStr = '备付金户'
									} else if(data.accountType == '09') {
										data.accountTypeStr = '超级户'
									} else if(data.accountType == '10') {
										data.accountTypeStr = '基本户'
									} else if(data.accountType == '11') {
										data.accountTypeStr = '运营户'
									} else {
										data.accountTypeStr = '-'
									}

									$$.detailAutoFix($('#updateAuditModal'), data); // 自动填充详情
									$('#updateAuditModal').modal('show');
								} else {
									alert('查询失败');
								}
							})
						}
					}
				}]
			}

			/**
			 * 投资人账户修改审核数据表格初始化
			 */
			$('#investorAccountTable').bootstrapTable(investorAccountTableConfig)

			/**
			 * 账户冻结解冻审核数据表格分页、搜索条件配置
			 */
			var thawFrozenPageOptions = {
				number: 1,
				size: 10,
				userOid: '',
				accountType: '',
				phone: '',
				relationProductName: '',
				openTimeBegin: '',
				openTimeEnd: '',
				userType: '',
				frozenStatus: ''
			}

			/**
			 * 账户冻结解冻审核表格querystring扩展函数，会在表格每次数据加载时触发，用于自定义querystring
			 * @param {Object} val
			 */
			function getThawFrozenQueryParams(val) {
				var form = document.searchTFForm
				thawFrozenPageOptions.size = val.limit
				thawFrozenPageOptions.number = parseInt(val.offset / val.limit) + 1
				thawFrozenPageOptions.userOid = form.userOid.value.trim()
				thawFrozenPageOptions.accountType = form.accountType.value.trim()
				thawFrozenPageOptions.phone = form.phone.value.trim()
				thawFrozenPageOptions.openTimeBegin = form.openTimeBegin.value
				thawFrozenPageOptions.openTimeEnd = form.openTimeEnd.value
				thawFrozenPageOptions.userType = form.userType.value.trim()
				thawFrozenPageOptions.frozenStatus = form.frozenStatus.value.trim()
				return val
			}

			/**
			 * 账户冻结解冻审核数据表格配置
			 */
			var financeThawFrozenAccountTableConfig = {
				ajax: function(origin) {
					http.post(
						accountConfig.api.financeAccount.thawFrozenList, {
							data: {
								page: thawFrozenPageOptions.number,
								rows: thawFrozenPageOptions.size,
								userOid: thawFrozenPageOptions.userOid,
								accountType: thawFrozenPageOptions.accountType,
								phone: thawFrozenPageOptions.phone,
								relationProductName: thawFrozenPageOptions.relationProductName,
								openTimeBegin: thawFrozenPageOptions.openTimeBegin,
								openTimeEnd: thawFrozenPageOptions.openTimeEnd,
								userType: thawFrozenPageOptions.userType,
								frozenStatus: thawFrozenPageOptions.frozenStatus
							},
							contentType: 'form'
						},
						function(rlt) {
							origin.success(rlt)
						}
					)
				},
				pageNumber: thawFrozenPageOptions.number,
				pageSize: thawFrozenPageOptions.size,
				pagination: true,
				sidePagination: 'server',
				pageList: [10, 20, 30, 50, 100],
				queryParams: getThawFrozenQueryParams,
				onLoadSuccess: function() {},
				columns: [{
					width: 30,
					align: 'center',
					formatter: function(val, row, index) {
						return(thawFrozenPageOptions.number - 1) * thawFrozenPageOptions.size + index + 1
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
//				}, {
//					field: 'userName',
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
					field: 'relationProductName',
				},	
				   {
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
				}, {
					align: 'center',
					formatter: function(val, row) {
						var buttons = [{
							text: '详情',
							type: 'button',
							class: 'item-detail',
							isRender: true
						}, {
							text: '同意',
							type: 'button',
							class: 'item-approve',
							isRender: true
						}, {
							text: '拒绝',
							type: 'button',
							class: 'item-reject',
							isRender: true
						}];
						return util.table.formatter.generateButton(buttons, 'accountAuditTable');
					},
					events: {
						'click .item-detail': function(e, value, row) {
							http.post(accountConfig.api.financeAccount.detail, {
								data: {
									oid: row.oid
								},
								contentType: 'form'
							}, function(result) {
								if(result.returnCode == 0) {
									var data = result;

									if(data.userType == 'T1') {
										data.userTypeStr = '投资人'
									} else if(data.userType == 'T2') {
										data.userTypeStr = '发行人'
									} else if(data.userType == 'T3') {
										data.userTypeStr = '平台'
									} else {
										data.userTypeStr = '-'
									}

									if(data.accountType == '01') {
										data.accountTypeStr = '活期户'
									} else if(data.accountType == '02') {
										data.accountTypeStr = '活期利息户'
									} else if(data.accountType == '03') {
										data.accountTypeStr = '体验金'
									} else if(data.accountType == '04') {
										data.accountTypeStr = '在途户'
									} else if(data.accountType == '05') {
										data.accountTypeStr = '冻结户'
									} else if(data.accountType == '06') {
										data.accountTypeStr = '定期户'
									} else if(data.accountType == '07') {
										data.accountTypeStr = '产品户'
									} else if(data.accountType == '08') {
										data.accountTypeStr = '备付金户'
									} else if(data.accountType == '09') {
										data.accountTypeStr = '超级户'
									} else if(data.accountType == '10') {
										data.accountTypeStr = '基本户'
									} else if(data.accountType == '11') {
										data.accountTypeStr = '运营户'
									} else {
										data.accountTypeStr = '-'
									}

									$$.detailAutoFix($('#accountDetailModal'), data); // 自动填充详情
									$('#accountDetailModal').modal('show');
								} else {
									alert('查询失败');
								}
							})
						},
						'click .item-approve': function(e, value, row) {
							$('#auditForm').validator('destroy')

							$("#auditOid").val(row.oid)
							if(row.frozenStatus == 'FROZENAU') {
								$('#auditTitle').html('冻结审批-->同意')
								$("#auditType").val('FROZENAU')
							} else if(row.frozenStatus == 'THAWAU') {
								$('#auditTitle').html('解冻审批-->同意')
								$("#auditType").val('THAWAU')
							}
							$("#auditStatus").val('pass')
							$("#auditComment").val("")
							document.auditForm.auditComment.value = ""
							util.form.validator.init($('#auditForm'))
							$('#auditModal').modal('show')
						},
						'click .item-reject': function(e, value, row) {
							$('#auditForm').validator('destroy')

							$("#auditOid").val(row.oid)

							if(row.frozenStatus == 'FROZENAU') {
								$('#auditTitle').html('冻结审批-->拒绝')
								$("#auditType").val('FROZENAU')
							} else if(row.frozenStatus == 'THAWAU') {
								$('#auditTitle').html('解冻审批-->拒绝')
								$("#auditType").val('THAWAU')
							}
							$("#auditStatus").val('fail')
							$("#auditComment").val("")

							document.auditForm.auditComment.value = ""
							util.form.validator.init($('#auditForm'))
							$('#auditModal').modal('show')
						}

					}
				}]
			}

			/**
			 * 账户冻结解冻审核数据表格初始化
			 */
			$('#financeThawFrozenAccountTable').bootstrapTable(financeThawFrozenAccountTableConfig)

			/**
			 * 账户冻结解冻审核搜索表单初始化
			 */
			$$.searchInit($('#searchTFForm'), $('#financeThawFrozenAccountTable'))

			/**
			 * 审核
			 */
			$('#auditSubmit').on('click', function() {
				if(!$('#auditForm').validator('doSubmitCheck')) return
				
				var auditType = document.auditForm.auditType.value;
				var auditStatus = document.auditForm.auditStatus.value;

				var auditOid = document.auditForm.auditOid.value;
				var auditComment = document.auditForm.auditComment.value;
					
				if(auditType == "SUBMIT") { //账户新增审核
					if(auditStatus == 'pass') {
						console.log("auditOid:"+auditOid,"auditComment:"+auditComment);
						http.post(accountConfig.api.financeAccount.addApprove, {
							data: {
								oid: auditOid,
								auditComment: auditComment
							},
							contentType: 'form'
						}, function(result) {
							if(result.errorCode == 0) {
								$('#auditModal').modal('hide')
								$('#accountAuditTable').bootstrapTable('refresh')
							} else {
								alert('失败');
							}
						})

					} else if(auditStatus == 'fail') {
						http.post(accountConfig.api.financeAccount.addReject, {
							data: {
								oid: auditOid,
								auditComment: auditComment
							},
							contentType: 'form'
						}, function(result) {
							if(result.errorCode == 0) {
								$('#auditModal').modal('hide')
								$('#accountAuditTable').bootstrapTable('refresh')
							} else {
								alert('失败');
							}
						})
					}
				} else if(auditType == "SEALING") { //账户封存审核
					if(auditStatus == 'pass') {
						http.post(accountConfig.api.financeAccount.sealApprove, {
							data: {
								oid: auditOid,
								auditComment: auditComment
							},
							contentType: 'form'
						}, function(result) {
							if(result.errorCode == 0) {
								$('#auditModal').modal('hide')
								$('#accountAuditTable').bootstrapTable('refresh')
							} else {
								alert('失败');
							}
						})
					} else if(auditStatus == 'fail') {
						http.post(accountConfig.api.financeAccount.sealReject, {
							data: {
								oid: auditOid,
								auditComment: auditComment
							},
							contentType: 'form'
						}, function(result) {
							if(result.errorCode == 0) {
								$('#auditModal').modal('hide')
								$('#accountAuditTable').bootstrapTable('refresh')
							} else {
								alert('失败');
							}
						})
					}	
				} else if(auditType == "FROZENAU") { //账户冻结审批
					if(auditStatus == 'pass') {
						http.post(accountConfig.api.financeAccount.frozenApprove, {
							data: {
								oid: auditOid,
								auditComment: auditComment
							},
							contentType: 'form'
						}, function(result) {
							if(result.errorCode == 0) {
								$('#auditModal').modal('hide')
								$('#financeThawFrozenAccountTable').bootstrapTable('refresh')
							} else {
								alert('失败');
							}
						})
					} else if(auditStatus == 'fail') {
						http.post(accountConfig.api.financeAccount.frozenReject, {
							data: {
								oid: auditOid,
								auditComment: auditComment
							},
							contentType: 'form'
						}, function(result) {
							if(result.errorCode == 0) {
								$('#auditModal').modal('hide')
								$('#financeThawFrozenAccountTable').bootstrapTable('refresh')
							} else {
								alert('失败');
							}
						})
					}	
				} else if(auditType == "THAWAU") { //账户解冻审批
					if(auditStatus == 'pass') {
						http.post(accountConfig.api.financeAccount.thawApprove, {
							data: {
								oid: auditOid,
								auditComment: auditComment
							},
							contentType: 'form'
						}, function(result) {
							if(result.errorCode == 0) {
								$('#auditModal').modal('hide')
								$('#financeThawFrozenAccountTable').bootstrapTable('refresh')
							} else {
								alert('失败');
							}
						})
					} else if(auditStatus == 'fail') {
						http.post(accountConfig.api.financeAccount.thawReject, {
							data: {
								oid: auditOid,
								auditComment: auditComment
							},
							contentType: 'form'
						}, function(result) {
							if(result.errorCode == 0) {
								$('#auditModal').modal('hide')
								$('#financeThawFrozenAccountTable').bootstrapTable('refresh')
							} else {
								alert('失败');
							}
						})
					}
				}
			})
			
			// 投资人账户修改审批 - 拒绝按钮点击事件
			$('#updateAduitPass').on('click', function() {
				http.post(accountConfig.api.financeAccount.updateReject, {
					data: {
						oid: currentAccount.oid,
						auditComment: ''
					},
					contentType: 'form'
				}, function(result) {
					if(result.errorCode == 0) {
						$('#updateAuditModal').modal('hide')
						$('#investorAccountTable').bootstrapTable('refresh')
					} else {
						alert('失败');
					}
				})
			})
			
			// 投资人账户修改审批 - 同意按钮点击事件
			$('#updateAduitFail').on('click', function() {
				http.post(accountConfig.api.financeAccount.updateApprove, {
					data: {
						oid: currentAccount.oid,
						auditComment: ''
					},
					contentType: 'form'
				}, function(result) {
					if(result.errorCode == 0) {
						$('#updateAuditModal').modal('hide')
						$('#investorAccountTable').bootstrapTable('refresh')
					} else {
						alert('失败');
					}
				})
			})
			
			
		}
	}	
})