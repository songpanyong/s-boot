/**
 * 快付通对账
 */
define([
	'http',
//	'config',
	'settleConfig',
	'util',
	'extension'
],
function (http, settleConfig, util, $$) {
	return {
		name: 'exceptionOrderCheck',
		init: function(){
			// 分页配置
            var pageOptions = {
                page: 1,
                row: 10
            }
            //确认提示框
            var confirm = $('#confirmModal');
            // 初始化数据表格
            var tableConfig = {
                pageNumber: pageOptions.page,
                pageSize: pageOptions.row,
                pagination: true,
                pageList: [10, 20, 30, 50, 100],
                sidePagination: 'server',
                queryParams: getQueryParams,
                onLoadSuccess: function () {
                },
                ajax: function (origin) {
                	var channelNo = $("#search_channelNo").val();
                	var startDate = verificationDate($("#startDate").val());
                	var endDate = verificationDate($("#endDate").val());
                	var reconciliationMark= $("#reconciliationMark").val();
                    http.post(settleConfig.api.settlement.settlement_exceptionOrderCheck_pageEX
                    	+"?channelNo="+channelNo+"&startDate="+startDate+"&endDate="+endDate+"&reconciliationMark="+reconciliationMark
                    	+"&page="+pageOptions.page+"&row="+pageOptions.row,{
                    }, function (rlt) {
                        origin.success(rlt)
                    })
                },
                columns: [
//              	{
//						width: 60,
//		                checkbox: true,
//		                formatter: function(val,row){
//		                	return "<input type='text'  class='oid' hidden='hidden' value='"+row.oid+"' >"
//		                }
//		         	},
                    {width: 100,field: 'orderNo',align: 'right'},
                    {field: 'payNo'},
                    {
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		return row.channelNo;
						}
                    },
                    {
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		return row.type==='01'?'代扣':'02'?'代付':'未知';
						}
                    },
                    {
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		return loadChannelName(row.channelNo);
						}
                    },
                    {
                    	align: 'right',
                    	formatter: function(val, row, index) {
                    		return row.amount+'元';
						}
                    },
                    {field: 'platformAccount',align: 'right'},
                    {field: 'platformName'},
                    {field: 'cardNo',align: 'right'},
                    {field: 'realName'},
                    {field: 'transactionTime',align: 'right'},
                    {
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		return '<span class="text-red">'+row.reconciliationMark+'</span>';
						}
                    }
//                  ,
//              	{
//                  width: 120,
//                  align: 'center',
//                  formatter: function(val, row, index) {
////                  	if(row.type==='01'&&row.reconciliationMark==="状态不匹配"){
////                  		var buttons = [{
////								text: '置为忽略',
////								type: 'button',
////								class: 'item-update',
////								isRender: true
////							},
////							{
////								text: '修改状态',
////								type: 'button',
////								class: 'item-changeStatus',
////								isRender: true
////							}]
////                  	}else{
//                  		var buttons = [{
//								text: '置为忽略',
//								type: 'button',
//								class: 'item-update',
//								isRender: true
//							}]
////                  	}
//                  		
//                  		return util.table.formatter.generateButton(buttons, 'proAccTable');
//						},
//						events: {
//							'click .item-update': ignoreRecon,
//							'click .item-changeStatus': changeStatus
//						}
//					}
                	]
            }
            
            // 初始化数据表格
            $('#formTable').bootstrapTable(tableConfig);
            //初始化渠道
            loadChannel();
            
            //置为忽略
			function ignoreRecon(e, value, row) {
//              var form = document.formModify;
//              util.form.reset($(form));
//              $$.formAutoFix($(form),row);
//              var data = util.form.serializeJson(row);
				var data = [];
				data.push(row.oid);
                confirm.find('.popover-title').html('提示');
					confirm.find('p').html('确定置为忽略状态？');
					$$.confirm({
						container: confirm,
						trigger: this,
						accept: function() {
							http.post(settleConfig.api.settlement.settlement_exceptionOrderCheck_ignore+"?oids="+data, {
							}, function(res) {
							})
							$('#formTable').bootstrapTable('refresh');
						}
					})
			}
			
			 //修改状态
			function changeStatus(e, value, row) {
                confirm.find('.popover-title').html('提示');
				confirm.find('p').html('确定修改订单状态？');
				$$.confirm({
					container: confirm,
					trigger: this,
					accept: function() {
						http.post(settleConfig.api.settlement.settlement_exceptionOrderCheck_changeStatus, {
							data: {
								oid:row.oid,
                    			orderNo:row.orderNo,
                    			payNo:row.payNo,
                    			commandStatus:row.commandStatus
                            },
                    		contentType: 'form'
						}, function(res) {
							if(res.result == 'SUCCESS') {
								confirm.modal('hide');
								$('#formTable').bootstrapTable('refresh');
								toastr.info("修改订单状态成功！", '提示信息', {
										timeOut: 3000
									})
							} else {
								toastr.error("修改订单状态失败！", '提示信息', {
										timeOut: 3000
								})
							}
						})
					}
				})
			}
            
            // 查询按钮点击事件
            $('#btSearch').on('click', function () {
            	var startDate = verificationDate($("#startDate").val());
                var endDate = verificationDate($("#endDate").val());
            	if(startDate=="noDate"||endDate=="noDate"){
            		toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}
//          	else if(startDate=="notDate"||endDate=="notDate"){
//          		toastr.error("日期格式有误！", '错误信息', {
//					  	timeOut: 3000
//					})
//          		return false;
//          	}
            	else{
            		var beginTime = new Date($("#startDate").val().replace(/\-/g, "\/"));
 					var endTime = new Date($("#endDate").val().replace(/\-/g, "\/"));
                	if(startDate != "" && endDate !="" && beginTime>endTime){
                		toastr.error('下单时间区间不能小于0!', '错误信息', {timeOut: 3000})
                	return;
                	}
            		$('#formTable').bootstrapTable('refresh');
            	}
          })
          
          	//校验日期并转换
          	function verificationDate(checkDate){
	          	if(checkDate){
	          		var items=checkDate.split("-");
	          		var newStr=items.join("");
	          		if(newStr.length!=8){
	          			return "notDate";
	          		}
	          		return newStr;
	          	}else{
	          		return "noDate";
	          	}
	         }
          
          	function getQueryParams(val) {
				var form = document.exQueryForm;
				// 分页数据赋值
				pageOptions.row = val.limit;
				pageOptions.page = parseInt(val.offset / val.limit) + 1;
		        var data = util.form.serializeJson(form);
				var beginTime = data.startDate;
                var endTime = data.endDate;
                if(beginTime != "" && endTime !="" ){
                	var beginDate = new Date(beginTime.replace(/\-/g, "\/"));  
 					var endDate = new Date(endTime.replace(/\-/g, "\/"));
 					if(beginDate>endDate){
 						toastr.error('不能小于下单开始时间!', '错误信息', {timeOut: 3000})
                		return;
 					}
                } 		
				return val;
			}
          	
          	//对账忽略
			$('#balanceIgnore').on('click', function(e) {
				var data = [];
				$(".selected").each(function(){
					data.push($(this).children("td:first").children(".oid").val());
				})
				console.log('data:'+JSON.stringify(data));
				if(data.length==0){
						toastr.info("请选择对账忽略订单列表", '提示信息', {
						timeOut: 3000
					})
					return false;
				}
				
				confirm.find('.popover-title').html('提示');
				confirm.find('p').html('确定忽略该订单对账？');
				$("#tips_cancle").show();		
				$$.confirm({
					container: confirm,
					trigger: this,
					accept: function() {
					$.ajax({
						type:'post',
						url:settleConfig.api.settlement.settlement_exceptionOrderCheck_ignore+"?oids="+data,
						beforeSend:	function(){
		  		  			$('#refreshDiv1').addClass('overlay');
							$('#refreshI1').addClass('fa fa-refresh fa-spin');						
	  						$("#balanceIgnore").attr("disabled","disabled");
						},			
						success:function(data){
							$('#formTable').bootstrapTable('refresh', pageOptions);
						},
						complete:function(xhr){
							$('#refreshDiv1').removeClass('overlay');
							$('#refreshI1').removeClass('fa fa-refresh fa-spin');	
	                     	$("#balanceIgnore").removeAttr("disabled");
	               		}			
					})	
					}
				})		
			})
			
			//加载渠道选择
			function loadChannel(){
				$("#search_channelNo").html("");
            	$("#search_channelNo").append("<option value=''>"+'全部'+"</option>");
            	http.post(settleConfig.api.settlement.settlement_getChannelPage,
            		{data:{page: 1,rows: 100},contentType: 'form'},
            		function(data){
   						data.rows.forEach(function(item) {
							$("#search_channelNo").append("<option value="+item.channelNo+">"+item.channelName+"</option>");
							$("#ul_Channel_Name").append("<li id=channel_Name"+item.channelNo+">"+item.channelName+"</option>");
						})
					})
			}
			
			//加载渠道名称
			function loadChannelName(channelId){
				return $("#channel_Name"+channelId).html();
			}
	   }
	}
})
