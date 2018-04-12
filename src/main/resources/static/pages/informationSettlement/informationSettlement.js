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
		name: 'informationSettlement',
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
					http.post(settleConfig.api.settlement.settlement_information_page, {
						data: pageOptions,
						contentType: 'form'
					}, function(rlt) {
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
				},
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
						field: 'accountType',
						formatter: function(val) {
							if(val === "01"){
								return '<span>基本户</span>'
							}
							if(val ==='02'){
								return '<span>一般户</span>'
							}
							if(val ==='03'){
								return '<span>收入户</span>'
							}
							if(val ==='04'){
								return '<span>支出户</span>'
							}
						}
					}, {
						width: 100,
						field: 'accountStatus',
						formatter: function(val) {
							var val = parseInt(val);
							switch (val) {
								case 0:
									return '<span class="text-green">可用</span>'
								case 1:
									return '<span class="text-red">已禁用</span>'
								case 2:
									return '<span class="text-yellow">待审核</span>'
								case 3:
									return '<span class="text-yellow">待提交</span>'	
							}
						}						
					},
					{
						width: 100,
						field: 'createTime'
					}, 
					{
						width: 100,
						align: 'center',
						filed: 'update',
						title: '操作',
						formatter: function(value, row) {
							var str1 = "";
							var str2 = "";
							var str3 = "";
							if(row.accountStatus === '3') {
								str1 = '<a href="javascript:void(0)" class="style-submit"  data-toggle="modal">提交审批</a>'
							}
							if(row.accountStatus === '0') {
								str2 = '<a href="javascript:void(0)" class="style-enable"  data-toggle="modal">封存</a>'
							}
							if(row.accountStatus === '2'){
								return ""
							}
							if(row.accountStatus !== '1'){
								str3 = '<a href="javascript:void(0)" class="style-update"  data-toggle="modal">修改</a>'
							}
							return '<div class="func-area">' + str3 +str1 + str2 +'</div>'						
						},
						events: {
						//修改填充
						  'click .style-update': function(e, val, row) {
                              		console.log('oid'+row.oid);
                              		var oid = row.oid;
									confirm.find('.popover-title').html('提示');
									confirm.find('p').html('修改将对正在进行的交易造成影响,确定修改？');
									$("#tips_cancle").show();		
									$$.confirm({
										container: confirm,
										trigger: this,
										accept: function() {
										var form = document.upateInformationForm;
										util.form.reset($(form))
										$$.formAutoFix($(form), row);
										$('#updateInformationModal').modal('show');											
										}
									})
							},
						//提交审批    
                          'click .style-submit': function (e, val, row) {
                             		console.log('oid'+row.oid);
                            		var oid = row.oid;
									confirm.find('.popover-title').html('提示');
									confirm.find('p').html('确定提交审批？');
									$("#tips_cancle").show();		
									$$.confirm({
										container: confirm,
										trigger: this,
										accept: function() {
											http.get(settleConfig.api.settlement.settlement_information_updateStatus, {
												data: {
													oid: oid,
													accountStatus:2
												}
											}, function(res) {
												if(res.errorCode == 0) {
													confirm.modal('hide');
													$('#settlement_page_information').bootstrapTable('refresh',pageOptions);
												} else {
													errorHandle(res);
												}
											})
										}
									})                               

                              },
                        //封存
                          'click .style-enable': function (e, val, row) {
                              		console.log('oid'+row.oid);
                              		var oid = row.oid;
									confirm.find('.popover-title').html('提示');
									confirm.find('p').html('封存将对正在进行的交易造成影响,确定封存？');
									$("#tips_cancle").show();		
									$$.confirm({
										container: confirm,
										trigger: this,
										accept: function() {
											http.get(settleConfig.api.settlement.settlement_information_updateStatus, {
												data: {
													oid: oid,
													accountStatus:1
												}
											}, function(res) {
												if(res.errorCode == 0) {
													confirm.modal('hide');
													$('#settlement_page_information').bootstrapTable('refresh',pageOptions);
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
			$('#settlement_page_information').bootstrapTable(tableConfig);
			// 搜索表单初始化
			$$.searchInit($('#settlement_search_information'), $('#settlement_page_information'));

			//组条件查询
			function getQueryParams(val) {
				var form = document.settlement_search_information
				$.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
				pageOptions.rows = val.limit
				pageOptions.page = parseInt(val.offset / val.limit) + 1
				pageOptions.size = val.limit
				pageOptions.number = parseInt(val.offset / val.limit) + 1
				return val
			}

			//select 改变
			$('#information_accountType,#information_accountStatus').on('change', function(e) {
				e.preventDefault()
				$(this).children('option:selected').val();
				var sform = document.settlement_search_accountStatus
				var data = util.form.serializeJson(sform)
				data.row = 10
				data.page = 1
				pageOptions = data
				$('#settlement_page_information').bootstrapTable('refresh', pageOptions);
			})

			//搜索
			$('#information_search').on('click', function(e) {
				e.preventDefault()
				var sform = document.settlement_search_information
				var data = util.form.serializeJson(sform)
				data.row = 10
				data.page = 1
				pageOptions = data
				$('#settlement_page_information').bootstrapTable('refresh', pageOptions);
			})

			//清空
			$('#information_reset').on('click', function(e) {
				e.preventDefault()
				var sform = document.settlement_search_information
				util.form.reset($(sform))
				$('#settlement_page_information').bootstrapTable('refresh', pageOptions);
			})

			// 点击新增事件
			$('#information_add').on('click', function() {
				$('#addInformationModal').modal('show');
			})


			//新建 - 确定按钮点击事件
			$('#doAddInformation').on('click', function() {
				var bankAccount = $("#add_bankAccount").val().trim();
				var bankAccountName = $("#add_bankAccountName").val().trim();
				var bankAccountClass = $("#add_bankAccountClass").val().trim();
				var accountFullName = $("#add_accountFullName").val().trim();
				var bankAddress = $("#add_bankAddress").val().trim();
				var openAccountProvince = $("#add_openAccountProvince").val().trim();
				var openAccountCity = $("#add_openAccountCity").val().trim();
				
				if (bankAccount == "") {
                    $("#add_bankAccount_err").text("银行帐号不能为空");
                    return;
                }
				if (bankAccountName == "") {
                    $("#add_bankAccountName_err").text("银行户名不能为空");
                    return;
                }				
				if (bankAccountClass == "") {
                    $("#add_bankAccountClass_err").text("开户行类别不能为空");
                    return;
                }				
				if (accountFullName == "") {
                    $("#add_accountFullName_err").text("开户行全名不能为空");
                    return;
                }				
				if (bankAddress == "") {
                    $("#add_bankAddress_err").text("开户行地址不能为空");
                    return;
                }
				if (openAccountProvince == "") {
                    $("#add_openAccountProvince_err").text("开户行省份不能为空");
                    return;
                }
				if (openAccountCity == "") {
                    $("#add_openAccountCity_err").text("开户行城市不能为空");
                    return;
                }
				var form = document.addInformationForm;
				$(form).ajaxSubmit({
					type: 'post',
					url: settleConfig.api.settlement.settlement_information_save,
					success: function() {
						util.form.reset($(form))
						$('#addInformationModal').modal('hide');
						$('#settlement_page_information').bootstrapTable('refresh', pageOptions);
					}
				})

			})

			//修改 - 确定按钮点击事件
			$('#doUpdateInformation').on('click', function() {
				var bankAccount = $("#update_bankAccount").val().trim();
				var bankAccountName = $("#update_bankAccountName").val().trim();
				var bankAccontClass = $("#update_bankAccontClass").val().trim();
				var accountFullName = $("#update_accountFullName").val().trim();
				var bankAddress = $("#update_bankAddress").val().trim();
				var openAccountProvince = $("#update_openAccountProvince").val().trim();
				var openAccountCity = $("#update_openAccountCity").val().trim();
				
				if (bankAccount == "") {
                    $("#update_bankAccount_err").text("银行帐号不能为空");
                    return;
                }
				
				if (bankAccountName == "") {
                    $("#update_bankAccountName_err").text("银行户名不能为空");
                    return;
                }				
				if (bankAccontClass == "") {
                    $("#update_bankAccontClass_err").text("开户行类别不能为空");
                    return;
                }				
				if (accountFullName == "") {
                    $("#update_accountFullName_err").text("开户行全名不能为空");
                    return;
                }				
				if (bankAddress == "") {
                    $("#update_bankAddress_err").text("开户行地址不能为空");
                    return;
                }
				if (openAccountProvince == "") {
                    $("#update_openAccountProvince_err").text("开户行省份不能为空");
                    return;
                }
				if (openAccountCity == "") {
                    $("#update_openAccountCity_err").text("开户行城市不能为空");
                    return;
                }
				
				var form = document.upateInformationForm;
				$(form).ajaxSubmit({
					type: 'post',
					url: settleConfig.api.settlement.settlement_information_update,
					success: function() {
						util.form.reset($(form))
						$('#updateInformationModal').modal('hide');
						$('#settlement_page_information').bootstrapTable('refresh', pageOptions);
					}
				})

			})

		}
	}
})