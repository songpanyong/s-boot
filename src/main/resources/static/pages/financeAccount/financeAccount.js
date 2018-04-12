/**
 * 财务账户管理--账户列表
 */
define([
	'http',
	'config',
	'accountConfig',
	'util',
	'extension'
], function(http, config, accountConfig, util, $$) {
	return {
		name: 'financeAccount',
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
				userName:'',
				accountNo: '',
				relationProductName: '',
//				openTimeBegin: '',
//				openTimeEnd: '',
				userType: '',
				status: ''
			}
			
			var tableConfig = {
				ajax: function(origin) {
					http.post(
						accountConfig.api.financeAccount.list, {
						data: pageOptions,
						contentType: 'form'
					}, function(rlt) {
						rlt.rows.forEach(function(item) {
							
						})
						origin.success(rlt)
					})
				},
				pageNumber: pageOptions.page,
				pageSize: pageOptions.row,
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
				},{
					field: 'userName',
				}, {
					field: 'phone',
				},{
					formatter: function(val, row, index) {
                		return row.userType==='T1'?'投资人':row.userType==='T2'?'发行人':'平台';
					}
				},{
					field: 'accountNo',
				}, {
					formatter: function(val, row, index){
						if(row.accountType == '01') {
							return '活期户';
						} else if(row.accountType == '02') {
							return '活期利息户';
						} else if(row.accountType == '03') {
							return  '体验金';
						} else if(row.accountType == '04') {
							return '在途户';
						} else if(row.accountType == '05') {
							return '提现冻结户';
						} else if(row.accountType == '06') {
							return '定期户';
						} else if(row.accountType == '07') {
							return '产品户';
						} else if(row.accountType == '08') {
							return '备付金户';
						} else if(row.accountType == '09') {
							return '超级户';
						} else if(row.accountType == '10') {
							return '基本户';
						} else if(row.accountType == '11') {
							return '运营户';
						} else if(row.accountType == '12') {
							return '归集清算户';
						} else if(row.accountType == '13') {
							return '可用金户';
						} else if(row.accountType == '14') {
							return '充值冻结户';
						} else if(row.accountType == '15') {
							return '冻结资金户';
						} else if(row.accountType == '17') {
							return '续投冻结户';
						}else {
							return  '-';
						}
					}
				}, {
					field: 'balance',
				},{
					formatter: function(val, row) {
						if(row.lineOfCredit>row.balance){
							return row.balance;
						}else{
							return row.lineOfCredit;
						}
					}
				},{
					field: 'lineOfCredit',
				},
				{
					align: 'center',
					formatter: function(val, row) {
						var buttons = [
							{
								text: '设置',
								type: 'button',
								class: 'item-set',
								isRender: row.accountType==='07'||row.accountType==='08'
							},{
								text: '详情',
								type: 'button',
								class: 'item-detail',
								isRender: true
							}];
						return util.table.formatter.generateButton(buttons, 'financeAccountTable');
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
										data.accountTypeStr = '提现冻结户'
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
									} else if(data.accountType == '12') {
										data.accountTypeStr = '归集清算户'
									} else if(data.accountType == '13') {
										data.accountTypeStr = '可用金户'
									} else if(data.accountType == '14') {
										data.accountTypeStr = '充值冻结户'
									} else if(data.accountType == '15') {
										data.accountTypeStr = '冻结资金户'
									} else if(data.accountType == '17') {
										data.accountTypeStr = '续投冻结户'
									}else {
										data.accountTypeStr = '-'
									}
									
									$$.detailAutoFix($('#accountDetailModal'), data); // 自动填充详情
									$('#accountDetailModal').modal('show');
								} else {
									alert('查询失败');
								}
							})
						},
						'click .item-set': function(e, value, row) {
							if(row.userType == 'T1') {
								row.userTypeStr = '投资人'
							} else if(row.userType == 'T2') {
								row.userTypeStr = '发行人'
							} else if(row.userType == 'T3') {
								row.userTypeStr = '平台'
							} else {
								row.userTypeStr = '-'
							}
							if(row.balance.toString().indexOf('元') == -1){
								row.balance = row.balance +'元';
							}
							$$.detailAutoFix($('#accountSetModal'), row); // 自动填充详情
							$('#oldBalance').val(row.lineOfCredit);
							$('#newBalance').val(row.lineOfCredit);
							$('#accountSetModal').modal('show');
						}
					}
				}]
			}

			var userId;
            var userName;
            // 获取登录的用户ID
            http.post(config.api.userInfo, {
                async: false
            }, function (result) {
                userId = result.oid;
                userName = result.name;
            });
			/**
			 * 表格querystring扩展函数，会在表格每次数据加载时触发，用于自定义querystring
			 * @param {Object} val
			 */
//			function getQueryParams(val) {
//				var form = document.searchForm
//				pageOptions.size = val.limit
//				pageOptions.number = parseInt(val.offset / val.limit) + 1
//				pageOptions.userOid = form.userOid.value.trim()
////				pageOptions.accountType = form.accountType.value.trim()
//				pageOptions.phone = form.phone.value.trim()
////				pageOptions.openTimeBegin = form.openTimeBegin.value
////				pageOptions.openTimeEnd = form.openTimeEnd.value
//				pageOptions.userType = form.userType.value.trim()
////				pageOptions.status = form.status.value.trim()
//				pageOptions.userName=form.userName.value.trim()
//				return val
//			}

			/**
			 * 数据表格配置
			 */
//			var tableConfig = {
//				ajax: function(origin) {
//					http.post(
//						accountConfig.api.financeAccount.list, {
//							data: {
//								page: pageOptions.number,
//								rows: pageOptions.size,
//								userName: pageOptions.userName,
//								accountType: pageOptions.accountType,
//								phone: pageOptions.phone,
//								openTimeBegin: pageOptions.openTimeBegin,
//								openTimeEnd: pageOptions.openTimeEnd,
//								userType: pageOptions.userType,
//								status: pageOptions.status
//							},
//							contentType: 'form'
//						},
//						function(rlt) {
//							origin.success(rlt)
//						}
//					)
//				},
//				pageNumber: pageOptions.number,
//				pageSize: pageOptions.size,
//				pagination: true,
//				sidePagination: 'server',
//				pageList: [10, 20, 30, 50, 100],
//				queryParams: getQueryParams,
//				onLoadSuccess: function() {},
//				columns: [{
//					width: 30,
//					align: 'center',
//					formatter: function(val, row, index) {
//						return(pageOptions.number - 1) * pageOptions.size + index + 1
//					}
//				},{
//					field: 'userName',
//				}, {
//					field: 'phone',
//				},  {
//					field: 'accountNo',
//					align: 'right'
//				}, {
//					field: 'accountName'
//				},
////				{
////					field: 'accountType',
////					formatter: function(val, row, index) {
////						switch(val) {
////							case '01':
////								return '活期户'
////							case '02':
////								return '活期利息户'
////							case '03':
////								return '体验金'
////							case '04':
////								return '在途户'
////							case '05':
////								return '提现冻结户'
////							case '06':
////								return '定期户'
////							case '07':
////								return '产品户'
////							case '08':
////								return '备付金户'
////							case '09':
////								return '超级户'
////							case '10':
////								return '基本户'
////							case '11':
////								return '运营户'
////							case '12':
////								return '清算户'
////							case '13':
////								return '可用金户'
////							case '14':
////								return '充值冻结户'
////							case '15':
////								return '冻结资金户'
////							default:
////								return '-'
////						}
////					}
////				},
////				{
////					field: 'status',
////					formatter: function(val, row, index) {
////						switch(val) {
////							case 'SAVE':
////								return '保存'
////							case 'SUBMIT':
////								return '提交'
////							case 'VALID':
////								return '生效'
////							case 'SEALING':
////								return '封存中'
////							case 'SEALED':
////								return '封存'
////							default:
////								return '-'
////						}
////					}
////				}, 
////			{
////					field: 'userType',
////					formatter: function(val, row, index) {
////						switch(val) {
////							case 'T1':
////								return '投资人'
////							case 'T2':
////								return '发行人'
////							case 'T3':
////								return '平台'
////							default:
////								return '-'
////						}
////				}
////				}, 
//				{
//					field: 'balance',
//				},
////				{
////					field: 'relationProductName',
////				}, 
//				
////				{
////					field: 'openTime',
////					align: 'right'
////					,
////					formatter: function(val) {
////						return util.table.formatter.timestampToDate(val, 'YYYY-MM-DD')
////					}
////				}, 
////				{
////					field: 'frozenStatus',
////					formatter: function(val, row, index) {
////						switch(val) {
////							case 'NORMAL':
////								return '正常'
////							case 'FROZEN':
////								return '冻结'
////							case 'FROZENAU':
////								return '冻结审批中'
////							case 'THAWAU':
////								return '解冻审批中'
////							default:
////								return '-'
////						}
////					}
////				},
//				{
//					align: 'center',
//					formatter: function(val, row) {
//						var buttons = [{
//							text: '详情',
//							type: 'button',
//							class: 'item-detail',
//							isRender: true
//						}
////						, {
////							text: '修改',
////							type: 'button',
////							class: 'item-update',
////							isRender: row.status == 'SAVE' && (row.userType == 'T2' || row.userType == 'T3')
////						}, {
////							text: '修改',
////							type: 'button',
////							class: 'item-edit',
////							isRender: row.userType == 'T1' && row.auditStatus != 'SUBMIT'
////						}, {
////							text: '封存',
////							type: 'button',
////							class: 'item-seal',
////							isRender: row.status == 'VALID'
////						}, {
////							text: '删除',
////							type: 'button',
////							class: 'item-invalid',
////							isRender: row.status == 'SAVE'
////						}
//						];
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
//									if(data.userType == 'T1') {
//										data.userTypeStr = '投资人'
//									} else if(data.userType == 'T2') {
//										data.userTypeStr = '发行人'
//									} else if(data.userType == 'T3') {
//										data.userTypeStr = '平台'
//									} else {
//										data.userTypeStr = '-'
//									}
//
//									if(data.accountType == '01') {
//										data.accountTypeStr = '活期户'
//									} else if(data.accountType == '02') {
//										data.accountTypeStr = '活期利息户'
//									} else if(data.accountType == '03') {
//										data.accountTypeStr = '体验金'
//									} else if(data.accountType == '04') {
//										data.accountTypeStr = '在途户'
//									} else if(data.accountType == '05') {
//										data.accountTypeStr = '提现冻结户'
//									} else if(data.accountType == '06') {
//										data.accountTypeStr = '定期户'
//									} else if(data.accountType == '07') {
//										data.accountTypeStr = '产品户'
//									} else if(data.accountType == '08') {
//										data.accountTypeStr = '备付金户'
//									} else if(data.accountType == '09') {
//										data.accountTypeStr = '超级户'
//									} else if(data.accountType == '10') {
//										data.accountTypeStr = '基本户'
//									} else if(data.accountType == '11') {
//										data.accountTypeStr = '运营户'
//									} else if(data.accountType == '12') {
//										data.accountTypeStr = '清算户'
//									} else if(data.accountType == '13') {
//										data.accountTypeStr = '可用金户'
//									} else if(data.accountType == '14') {
//										data.accountTypeStr = '充值冻结户'
//									} else if(data.accountType == '15') {
//										data.accountTypeStr = '冻结资金户'
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
////						'click .item-update': function(e, value, row) {
////							$('#updateProductForm').validator('destroy')
////
////							http.post(accountConfig.api.financeAccount.detail, {
////								data: {
////									oid: row.oid
////								},
////								contentType: 'form'
////							}, function(result) {
////								if(result.returnCode == 0) {
////									util.form.reset($('#updateAccountForm'))
////									var data = result;
////
////									$$.formAutoFix($('#updateAccountForm'), data); // 自动填充表单
////
////									util.form.validator.init($('#updateAccountForm'))
////									$('#updateAccountModal').modal('show');
////
////								} else {
////									alert('查询失败');
////								}
////							})
////						},
////						'click .item-edit': function(e, value, row) {
////							$('#editAccountForm').validator('destroy')
////
////							http.post(accountConfig.api.financeAccount.detail, {
////								data: {
////									oid: row.oid
////								},
////								contentType: 'form'
////							}, function(result) {
////								if(result.returnCode == 0) {
////									var data = result;
////
////									$$.formAutoFix($('#editAccountForm'), data); // 自动填充表单
////
////									util.form.validator.init($('#editAccountForm'))
////									$('#editAccountModal').modal('show');
////
////								} else {
////									alert('查询失败');
////								}
////							})
////						},
////						'click .item-workbench': function(e, val, row) {
////							util.nav.dispatch('accountTrans', 'id=' + row.oid)
////						},
////						'click .item-seal': function(e, value, row) {
////							$("#confirmTitle").html("确定封存吗？")
////							$$.confirm({
////								container: $('#doConfirm'),
////								trigger: this,
////								accept: function() {
////									http.post(accountConfig.api.financeAccount.seal, {
////										data: {
////											oid: row.oid
////										},
////										contentType: 'form',
////									}, function(result) {
////										$('#financeAccountTable').bootstrapTable('refresh')
////									})
////								}
////							})
////						},
////						'click .item-invalid': function(e, value, row) {
////							$("#confirmTitle").html("确定删除吗？")
////							$$.confirm({
////								container: $('#doConfirm'),
////								trigger: this,
////								accept: function() {
////									http.post(accountConfig.api.financeAccount.invalid, {
////										data: {
////											oid: row.oid
////										},
////										contentType: 'form',
////									}, function(result) {
////										$('#financeAccountTable').bootstrapTable('refresh')
////									})
////								}
////							})
////						}
//					}
//				}]
//			}

			/**
			 * 数据表格初始化
			 */
			$('#financeAccountTable').bootstrapTable(tableConfig)

			/**
			 * 搜索表单初始化
			 */
			$$.searchInit($('#searchForm'), $('#financeAccountTable'))

			$('#financeAccountTableDown').on('click', function() {
				util.tableToExcel("financeAccountTable", "账户列表")
			})

			/**
			 * 更新平台发行人用户“保存”按钮点击事件
			 */
			$('#updateAccountSubmit').on('click', function() {
				if(!$('#updateAccountForm').validator('doSubmitCheck')) return

				var oid = document.updateAccountForm.oid.value
				var status = document.updateAccountForm.submitType.value
				var accountType = document.updateAccountForm.accountType.value
				var relationProductName = document.updateAccountForm.relationProductName.value

				http.post(accountConfig.api.financeAccount.update, {
					data: {
						oid: oid,
						status: status,
						accountType: accountType,
						relationProductName: relationProductName
					},
					contentType: 'form'
				}, function(result) {
					if(result.errorCode == 0) {
						$('#updateAccountModal').modal('hide')
						$('#financeAccountTable').bootstrapTable('refresh')
					} else {
						alert('失败');
					}
				})

			})
			
			/**
			 * 更新投资人用户“保存”按钮点击事件
			 */
			$('#editAccountSubmit').on('click', function() {
				if(!$('#editAccountForm').validator('doSubmitCheck')) return

				var oid = document.editAccountForm.oid.value
				var phone = document.editAccountForm.phone.value
				var relationProductName = document.editAccountForm.relationProductName.value
				var remark = document.editAccountForm.remark.value

				http.post(accountConfig.api.financeAccount.edit, {
					data: {
						oid: oid,
						phone: phone,
						remark: remark,
						relationProductName: relationProductName
					},
					contentType: 'form'
				}, function(result) {
					if(result.errorCode == 0) {
						$('#editAccountModal').modal('hide')
						$('#financeAccountTable').bootstrapTable('refresh')
					} else {
						alert('失败');
					}
				})

			})
			
			//搜索
           function getQueryParams(val) {
				var form = document.searchForm
				$.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
				pageOptions.rows = val.limit
				pageOptions.page = parseInt(val.offset / val.limit) + 1
				var data = util.form.serializeJson(form);
				return val
			}
           
           //select事件
           $('.userType').change(function(){
           	var userType = this.value;
           	if('T1' === userType){
           		$(".accountType option[value='12']").hide();
           		$(".accountType option[value='13']").hide();
           		$(".accountType option[value='15']").hide();
           	}else{
           		$(".accountType option[value='12']").show();
           		$(".accountType option[value='13']").show();
           		$(".accountType option[value='15']").show();
           	}
           })
			//变更申请
			$('#applyAudit').on('click', function() {
				$('#accountSetModal').modal('hide')
				var accountNo = $('#accountNo').html();
				var accountName = $('#accountName').html();
				var userOid = $('#userOid').html();
				var oldBalance = $('#oldBalance').val();
				var newBalance = $('#newBalance').val();
				var change = new Object();
				change.changeType="07";
				change.accountNo=accountNo;
				change.oldName = accountName;
				change.newBalance=newBalance;
				change.oldBalance=oldBalance;
				if(newBalance==oldBalance){
					toastr.error("授信额度无调整，无需申请", '错误信息', {
                            timeOut: 3000
                        })
                	return;
				}
				var changeRecordsList = [];
				changeRecordsList.push(change);
				http.post(accountConfig.api.platform.audit_apply, {
					data: {
						userOid:userOid,
						applyType:"2",
//						applicantId:userId,
//						applicantName:userName,
						applicantId:'111111',
						applicantName:'aaa',
						changeRecordsList:JSON.stringify(changeRecordsList)
					},
					contentType: 'form'
				}, function(res) {
					if(res.returnCode == '0000') {
						toastr.success("提交成功");
						$('#financeAccountTable').bootstrapTable('refresh')
					} else {
						toastr.error(res.errorMessage);
					}
				})
			})
		}
	}
})