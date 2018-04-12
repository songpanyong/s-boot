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
		name: 'kftReconciliation',
		init: function(){
			// 分页配置
            var pageOptions = {
                page: 1,
                row: 10
            }
            //提示框
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
                	var checkDate = verificationDate($("#checkDate").val());
                	var reconStatus= $("#reconStatus").val();
                	if(checkDate=="noDate"){
                		checkDate = "20160101";//无数据
                	}
                	var channelId = "1";
                    http.post(settleConfig.api.settlement.settlement_kftReconciliation_page
                    	+"?channelId="+channelId+"&checkDate="+checkDate+"&reconStatus="+reconStatus+"&page="+pageOptions.page+"&row="+pageOptions.row,{
                    }, function (rlt) {
                        origin.success(rlt)
                    })
                },
                columns: [
                	{
						width: 100,
		                checkbox: true,
		                formatter: function(val,row){
		                	return "<input type='text'  class='oid' hidden='hidden' value='"+row.oid+"' >"
		                }
		         	},
                    {field: 'orderId'},
                    {
                    	align: 'center',
                    	formatter: function(val, row, index) {
                    		return row.channelId==='1'?'快付通':'其他';
						}
                    },
                    {field: 'productId'},
                    {field: 'transactionCurrency'},
                    {
                    	align: 'center',
                    	formatter: function(val, row, index) {
                    		return row.transactionAmount+'元';
						}
                    },
                    {field: 'paymentBankNo'},
                    {
                    	align: 'center',
                    	formatter: function(val, row, index) {
                    		return row.beneficiaryBankNo===''?'-':'row.beneficiaryBankNo';
						}
                    },
//                  {field: 'beneficiaryBankNo'},
                    {
                    	align: 'center',
						formatter: function(val, row, index) {
							switch (row.tradStatus) {
								case '0':
									return '<span class="text-yellow">未处理</span>';
								case '1':
									return '<span class="text-green">交易成功</span>';
								case '2':
									return '<span class="text-red">交易失败</span>';
								case '3':
									return '<span class="text-yellow">交易处理中</span>';
								case '4':
									return '<span class="text-yellow">超时</span>';
							}
						}
					},
					{
                    	align: 'center',
                    	formatter: function(val, row, index) {
                    		return row.failDetail===''?'-':'row.failDetail';
						}
                    },
                    {
                    	align: 'center',
                    	formatter: function(val, row, index) {
                    		return row.errorCode===''?'-':'row.errorCode';
						}
                    },
//                  {field: 'failDetail'},
//                  {field: 'errorCode'},
                    {field: 'transactionTime'},
                    {
                    	align: 'center',
						formatter: function(val, row, index) {
							switch (row.reconStatus) {
								case 0:
									return '<span class="text-yellow">未对帐</span>';
								case 1:
									return '<span class="text-green">对账成功</span>';
								case 2:
									return '<span class="text-red">对账失败</span>';
							}
						}
					},
                    {field: 'checkMark'}]
            }
            
            // 初始化数据表格
            $('#formTable').bootstrapTable(tableConfig);
            
			// 获取对账流水按钮点击事件
            $('#btGetKftRecon').on('click', function () {
            	var checkDate = verificationDate($("#checkDate").val());
            	if(checkDate=="noDate"){
            		toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}else if(checkDate=="notDate"){
            		toastr.error("日期格式有误！", '错误信息', {
					  	timeOut: 3000
					})
            		return false;
            	}else{
            		//添加防止重复提交样式
  		  			$('#refreshDiv1').addClass('overlay');
					$('#refreshI1').addClass('fa fa-refresh fa-spin');
					$('#btGetKftRecon').attr("disabled","disabled");
					
	            	http.post(settleConfig.api.settlement.settlement_kftReconciliation_getRecon+"?checkDate="+checkDate, {
	                    }, function (res) {
	                    	//去除重复提交样式
							$('#refreshDiv1').removeClass('overlay');
							$('#refreshI1').removeClass('fa fa-refresh fa-spin');
							$('#btGetKftRecon').attr("disabled",false);
							
	                    	if(res.result==="FAIL"){
								//alert(res.resultDetial);
								toastr.error(res.resultDetial, '错误信息', {
								  	timeOut: 3000
								})
							}
							if(res.result==="SUCCESS"){
								//alert("对账成功");
								toastr.info("获取对账流水成功", '提示信息', {
								  	timeOut: 3000
								})
								$('#formTable').bootstrapTable('refresh');
							}
	                    }
	                )
            	}
            })
            
            // 对账按钮点击事件
            $('#btKftRecon').on('click', function () {
            	var checkDate = verificationDate($("#checkDate").val());
            	if(checkDate=="noDate"){
            		toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}else if(checkDate=="notDate"){
            		toastr.error("日期格式有误！", '错误信息', {
					  	timeOut: 3000
					})
            		return false;
            	}else{
            		//添加防止重复提交样式
  		  			$('#refreshDiv1').addClass('overlay');
					$('#refreshI1').addClass('fa fa-refresh fa-spin');
					$('#btKftRecon').attr("disabled","disabled");
					
            		var channel = '1';
	            	http.post(settleConfig.api.settlement.settlement_orderReconciliation_check+"?dateString="+checkDate+"&channel="+channel, {
	                    }, function (res) {
	                    	//去除重复提交样式
							$('#refreshDiv1').removeClass('overlay');
							$('#refreshI1').removeClass('fa fa-refresh fa-spin');
							$('#btKftRecon').attr("disabled",false);
							
	                    	if(res.result==="FAIL"){
								toastr.error(res.resultDetial, '错误信息', {
								  	timeOut: 3000
								})
							}
							if(res.result==="SUCCESS"){
								//alert("对账成功");
								toastr.info("对账成功", '提示信息', {
								  	timeOut: 3000
								})
								$('#formTable').bootstrapTable('refresh');
							}
	                    }
	                )
            	}
            })
            
            // 查询按钮点击事件
            $('#btQuery').on('click', function () {
            	var checkDate = verificationDate($("#checkDate").val());
            	if(checkDate=="noDate"){
            		toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}else if(checkDate=="notDate"){
            		toastr.error("日期格式有误！", '错误信息', {
					  	timeOut: 3000
					})
            		return false;
            	}else{
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
				//var form = document.waitSearchForm
				// 分页数据赋值
				pageOptions.row = val.limit;
				pageOptions.page = parseInt(val.offset / val.limit) + 1;
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
				confirm.find('p').html('确定忽略对账？');
				$("#tips_cancle").show();		
				$$.confirm({
					container: confirm,
					trigger: this,
					accept: function() {
					$.ajax({
						type:'post',
						url:settleConfig.api.settlement.settlement_reconciliation_recon_ignore+"?oids="+data,
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
	   }
	}
})
