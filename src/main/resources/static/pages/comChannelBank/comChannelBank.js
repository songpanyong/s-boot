/**
 * 结算通道银行信息管理
 */
define([
	'http',
//	'config',
	'settleConfig',
	'util',
	'extension'
], function(http,settleConfig, util, $$) {
	return {
		name: 'comChannelBank',
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
					http.post(settleConfig.api.settlement.settlement_comChannelBank_page, {
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
				columns: [
					{
					width: 30,
					align: 'center',
					formatter: function(val, row, index) {
						return(pageOptions.number - 1) * pageOptions.size + index + 1
					}
					},
					{
						width: 100,
						field: 'channelNo',
						align: 'right'
					}, 
					{
						width: 100,
						field: 'channelName',
					},
					{ 	
						width: 100,
						field: 'channelbankCode'
					}, {
						width: 100,
						field: 'channelbankName'
					}, {
						width: 100,
						field: 'singleQuota',
						align: 'right'
					}, {
						width: 100,
						field: 'dailyLimit',
						align: 'right'
					}, {
						width: 100,
						field: 'monthlyLimit',
						align: 'right'
					},{
                    width: 100,
                    align: 'center',
                    formatter: function(val, row, index) {
//							var buttons = [{
//								text: '修改',
//								type: 'button',
//								class: 'item-update',
//								isRender: true
//							},{
//								text: '删除',
//								type: 'button',
//								class: 'item-delete',
//								isRender: true
//							}]
//							return util.table.formatter.generateButton(buttons, 'proAccTable');
							var format = ''
							format += '<span style=" margin:auto 0px auto 10px;cursor: pointer;" class="fa fa-pencil-square-o item-update"></span>'
	            				+'<span style=" margin:auto 0px auto 10px;cursor: pointer;" class="fa fa-trash-o item-delete"></span>';
							return format;
							
							
							
						},
						events: {
							'click .item-update': updateChannelBank,
							'click .item-delete': deleteChannelBank
						}
					}
				]
			}

			//初始化渠道
			loadChannel();
			// 初始化数据表格
			$('#formTable').bootstrapTable(tableConfig);
			
			//修改详情
			function updateChannelBank(e, value, row) {
					confirm.find('.popover-title').html('提示');
					confirm.find('p').html('确定修改?');
					$("#tips_cancle").show();
					$$.confirm({
						container: confirm,
						trigger: this,
						accept: function() {
							var bankCode = row.standardCode;
							updateBankInfo(bankCode);
			                var form = document.formModify;
			                util.form.reset($(form));
			                $$.formAutoFix($(form),row);
			            	$("#update_channelbankCode_err").text("");
			            	$("#update_channelbankName_err").text("");
			            	$("#update_singleQuota_err").text("");
			            	$("#update_dailyLimit_err").text("");
			            	$("#update_monthlyLimit_err").text("");
			                $('#attModify').modal('show');
						}
					})
			}		
			
			//删除
			function deleteChannelBank(e, value, row) {
					confirm.find('.popover-title').html('提示');
					confirm.find('p').html('确定删除?');
					$("#tips_cancle").show();
					$$.confirm({
						container: confirm,
						trigger: this,
						accept: function() {
						http.get(settleConfig.api.settlement.settlement_comChannelBank_delete, {
							data: {
								oid: row.oid,
							}
						}, function(data) {
							if(data.result==="SUCCESS"){
								toastr.info("删除成功", '提示信息', {
								timeOut: 3000
								})				
							}else{
								toastr.info("删除失败", '提示信息', {
								timeOut: 3000
								})
							}
							$('#formTable').bootstrapTable('refresh', pageOptions);					
						})							
							
						}
					})
			}		
			//加载通道银行选择
			getBankInfo();
			//加载渠道选择
			function loadChannel(){
				$("#search_channelNo").html("");
            	$("#search_channelNo").append("<option value=''>"+'全部'+"</option>");
            	http.post(settleConfig.api.settlement.settlement_getChannelPage,
            		{data:{page: 1,rows: 100},contentType:'form'},
            		function(data){
   						data.rows.forEach(function(item) {
   							var channel = $("#channel"+item.channelNo).html();
							if(typeof(channel)==="undefined"){
								$("#search_channelNo").append("<option value="+item.channelNo+" id=channel"+item.channelNo+">"+item.channelName+"</option>");
							}
						})
					})
			}
			
			//组条件查询
			function getQueryParams(val) {
				var form = document.searchForm
				$.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
				pageOptions.rows = val.limit
				pageOptions.page = parseInt(val.offset / val.limit) + 1
				pageOptions.size = val.limit
				pageOptions.number = parseInt(val.offset / val.limit) + 1
				return val
			}
			
			//新增时加载银行信息
			function getBankInfo(){
				$("#add_channelbankName").empty();
				http.post(settleConfig.api.settlement.settlement_comChannelBank_findAllBankName,
            		{contentType: 'form'},
            		function(data){
            			var bankList = data.bankList;
   						bankList.forEach(function(item) {
							$("#add_channelbankName").append("<option value="+item.bankCode+">"+item.bankName+"</option>");
							$("#search_channelbankCode").append("<option value="+item.bankCode+">"+item.bankName+"</option>");
						})
					}
            		)
				}
			
			//修改时加载银行信息
			function updateBankInfo(bankCode){
				$("#update_channelbankName").empty();
				http.post(settleConfig.api.settlement.settlement_comChannelBank_findAllBankName,
            		{contentType: 'form'},
            		function(data){
            			var bankList = data.bankList;
   						bankList.forEach(function(item) {
   							if(item.bankCode === bankCode){
   								$("#update_channelbankName").append("<option value="+item.bankCode+" selected=selected>"+item.bankName+"</option>");
   							}else{
   								$("#update_channelbankName").append("<option value="+item.bankCode+">"+item.bankName+"</option>");
   							}
							
						})
					}
            		)
				}
			
			//点击查询
			$('#btn_search').on('click', function(e) {
				e.preventDefault()
				var sform = document.searchForm
				var data = util.form.serializeJson(sform)
				data.row = 10
				data.page = 1
				pageOptions = data
				$('#formTable').bootstrapTable('refresh', pageOptions);
			})

            // 结算通道配置按钮点击事件
            $('#addFormConfig').on('click', function () {
            	getBankInfo();
            	$("#add_channelNo").html("");
            	http.post(settleConfig.api.settlement.settlement_getChannelPage,
            		{data:{page: 1,rows: 100},contentType:'form'},
            		function(data){
   						data.rows.forEach(function(item) {
							$("#add_channelNo").append("<option value="+item.channelNo+">"+item.channelName+"</option>");
						})
					})
          	
			
            	$("#add_channelNo_err").text("");
            	$("#add_channelbankCode_err").text("");
            	$("#add_channelbankName_err").text("");
            	$("#add_singleQuota_err").text("");
            	$("#add_dailyLimit_err").text("");
            	$("#add_monthlyLimit_err").text("");
            	$("#add_standardCode_err").text("");
                $('#addFormModal').modal('show');
            })
            
            // 新增提交
            $('#doAddForm').on('click', function () {
            	var errormsg = true;
            	var channelNo = $("#add_channelNo").val().trim();
            	var channelName = $("#add_channelNo option:selected").text();
            	console.log("channelName"+channelName);
            	$("#add_channelName").val(channelName);
            	
            	var channelbankCode = $("#add_channelbankCode").val().trim();
            	var channelbankName = $("#add_channelbankName").val().trim();
            	var singleQuota = $("#add_singleQuota").val().trim();
            	var dailyLimit = $("#add_dailyLimit").val().trim();
            	var monthlyLimit = $("#add_monthlyLimit").val().trim();
                
                if (channelNo == "") {
                    $("#add_channelNo_err").text("支付通道编号不能为空!");
                    return;
                }
                if (channelbankCode == "") {
                    $("#add_channelbankCode_err").text("支付通道银行代码不能为空!");
                    return;
                }
                if (dailyLimit == "") {
                    $("#add_dailyLimit_err").text("日限额不能为空!");
                    return;
                }
                if (monthlyLimit == "") {
                    $("#add_monthlyLimit_err").text("月限额不能为空!");
                    return;
                }                
                if (singleQuota == "") {
                    $("#add_singleQuota_err").text("单笔限额不能为空!");
                    return;
                }

                var form = document.addFormForm;
                if(errormsg){
	                $(form).ajaxSubmit({
	                    type: 'post',
	                    url: settleConfig.api.settlement.settlement_comChannelBank_save,
	                    success: function (data) {
	                        util.form.reset($(form));
	                        $('#formTable').bootstrapTable('refresh');
	                        $('#addFormModal').modal('hide');
	                    }
	                })
                }
            })
            
            // 修改提交
            $('#btUpdate').on('click', function (e) {
            	var errormsg = true;
            	var channelNo = $("#update_channelNo").val().trim();
            	var channelbankCode = $("#update_channelbankCode").val().trim();
            	var channelbankName = $("#update_channelbankName").val().trim();
            	var singleQuota = $("#update_singleQuota").val().trim();
            	var dailyLimit = $("#update_dailyLimit").val().trim();
            	var monthlyLimit = $("#update_monthlyLimit").val().trim();
                if (channelbankCode == "") {
                    $("#update_channelbankCode_err").text("支付通道银行代码不能为空!");
                    return;
                }
                if (dailyLimit == "") {
                    $("#update_dailyLimit_err").text("日限额不能为空!");
                    return;
                }
                if (monthlyLimit == "") {
                    $("#update_monthlyLimit_err").text("月限额不能为空!");
                    return;
                }
                if (singleQuota == "") {
                    $("#update_singleQuota_err").text("单笔限额不能为空!");
                    return;
                }
        
                var form = document.formModify
                if(errormsg){
                $(form).ajaxSubmit({
                    type: 'post',
                    url: settleConfig.api.settlement.settlement_comChannelBank_update,
                    success: function (data) {
                        util.form.reset($(form));
                        $('#formTable').bootstrapTable('refresh');
                        $('#attModify').modal('hide');
                    }
                })
                }
            }) 

		}
	}
})