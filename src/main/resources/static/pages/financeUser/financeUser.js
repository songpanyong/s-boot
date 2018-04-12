/**
 * 财务用户管理--系统用户查询
 */
define([
	'http',
	'config',
	'accountConfig',
	'util',
	'extension'
], function(http, config, accountConfig, util, $$) {
	return {
		name: 'financeUser',
		init: function() {

			// js逻辑写在这里
			/**
			 * 数据表格分页、搜索条件配置
			 */
			var pageOptions = {
				number: 1,
				size: 10,
				userOid: '',
				name: '',
				phone: '',
				userType: ''
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
				pageOptions.name = form.name.value.trim()
				pageOptions.phone = form.phone.value.trim()
				pageOptions.userType = form.userType.value.trim()
				return val
			}

			/**
			 * 数据表格配置
			 */
			var tableConfig = {
				ajax: function(origin) {
					http.post(
						accountConfig.api.financeUser.list, {
							data: {
								page: pageOptions.number,
								rows: pageOptions.size,
								userOid: pageOptions.userOid,
								name: pageOptions.name,
								phone: pageOptions.phone,
								userType: pageOptions.userType
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
					field: 'phone',
				}, {
					field: 'name'
				},{
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
				}, {
					field: 'systemSource',
				}]
			}

			/**
			 * 数据表格初始化
			 */
			$('#financeUserTable').bootstrapTable(tableConfig)

			/**
			 * 搜索表单初始化
			 */
			$$.searchInit($('#searchForm'), $('#financeUserTable'))
			util.form.validator.init($('#addAccountForm'))

			/**
			 * 新建用户按钮点击事件
			 */
			$('#userAdd').on('click', function() {
				var form = document.addUserForm
				$(form).validator('destroy')

				util.form.validator.init($(form));
				$('#addUserModal').modal('show')
			})

			/**
			 * 新建用户“保存”按钮点击事件
			 */
			$('#addUserSubmit').on('click', function() {
				if(!$('#addUserForm').validator('doSubmitCheck')) return

				$('#addUserModal').modal('hide')

				$('#addUserForm').ajaxSubmit({
					url: accountConfig.api.financeUser.save,
					success: function(addResult) {
						if(addResult.returnCode == 0) {
							util.form.reset($('#addUserForm'))
							$('#financeUserTable').bootstrapTable('refresh')
						} else {
							alert(addResult.errorMessage)
						}
					}
				})

			})

			/**
			 * 编辑用户“保存”按钮点击事件
			 */
			$('#updateUserSubmit').on('click', function() {
				if(!$('#updateUserForm').validator('doSubmitCheck')) return

				var oid = document.updateUserForm.oid.value
				var userType = document.updateUserForm.userType.value
				var name = document.updateUserForm.name.value
				var phone = document.updateUserForm.phone.value
				var remark = document.updateUserForm.remark.value

				http.post(accountConfig.api.financeUser.update, {
					data: {
						oid: oid,
						userType: userType,
						name: name,
						phone: phone,
						remark: remark
					},
					contentType: 'form'
				}, function(result) {
					if(result.returnCode == 0) {
						$('#updateUserModal').modal('hide')
						$('#financeUserTable').bootstrapTable('refresh')

					} else {
						alert('失败');
					}
				})

			})

			/**
			 * 新建用户“保存”按钮点击事件
			 */
			$('#addAccountSubmit').on('click', function() {
				if(!$('#addAccountForm').validator('doSubmitCheck')) return

				var submitType = document.addAccountForm.submitType.value

				var userOid = document.addAccountForm.userOid.value
				var userType = document.addAccountForm.userType.value
				var accountType = document.addAccountForm.accountType.value
				var relationProduct = document.addAccountForm.relationProduct.value

				$('#addAccountModal').modal('hide')
				if(submitType == "YES") {
					http.post(accountConfig.api.financeAccount.add, {
						data: {
							userOid: userOid,
							userType: userType,
							accountType: accountType,
							relationProduct: relationProduct
						},
						contentType: 'form'
					}, function(result) {
						if(result.returnCode == 0) {

						} else {
							alert('失败');
						}
					})

				} else {

					http.post(accountConfig.api.financeAccount.save, {
						data: {
							userOid: userOid,
							userType: userType,
							accountType: accountType,
							relationProduct: relationProduct
						},
						contentType: 'form'
					}, function(result) {
						if(result.returnCode == 0) {

						} else {
							alert('失败');
						}
					})

				}

			})

		}
	}
})