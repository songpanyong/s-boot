/**
 * 银行对账
 */
define([
	'http',
//	'config',
	'settleConfig',
	'util',
	'extension'
], function(http,settleConfig, util, $$) {
	return {
		name: 'bankReconciliation',
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
					http.post(settleConfig.api.settlement.settlement_bankReconciliation_page, {
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
						width: 100,
		                checkbox: true,
		                formatter: function(val,row){
		                	return "<input type='text'  class='oid' hidden='hidden' value='"+row.oid+"' >"
		                }
		         	},
					{
						width: 100,
						field: 'accountNo'
					}, {
						width: 100,
						field: 'currency'
					}, {
						width: 100,
						field: 'tradTime'
					}, {
						width: 100,
						field: 'hostSerial'
					}, {
						width: 100,
						field: 'transactionFlow'
					}, {
						width: 100,
						field: 'paymentPartyNetwork'
					}, {
						width: 100,
						field: 'paymentallianceCode'
					}, {
						width: 100,
						field: 'paymentName'
					},
										{
						width: 100,
						field: 'paymentPartyAccount'
					}, 
										{
						width: 100,
						field: 'paymentAccount'
					}, 
										{
						width: 100,
						field: 'settlementCurrency'
					}, 
										{
						width: 100,
						field: 'tradAmount'
					}, 
										{
						width: 100,
						field: 'receivParty'
					}, 
										{
						width: 100,
						field: 'payeeContact'
					}, 
										{
						width: 100,
						field: 'beneficiaryBankName'
					}, 
										{
						width: 100,
						field: 'beneficiaryAccount'
					}, 
										{
						width: 100,
						field: 'payeeName'
					}, 
										{
						width: 100,
						field: 'lendMark'
					}, 
					{
						width: 100,
						field: 'stract'
					}, 
										{
						width: 100,
						field: 'voucher'
					}, 
										{
						width: 100,
						field: 'fee'
					}, 
										{
						width: 100,
						field: 'postFee'
					}, 
										{
						width: 100,
						field: 'accountBalance'
					}, 
										{
						width: 100,
						field: 'postscript'
					}, 
										{
						width: 100,
						field: 'chineseAbstract'
					}, 
										{
						width: 100,
						field: 'customerCustom'
					}, 
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
						width: 100,
						field: 'reconStatus',
						formatter: function(val) {
							switch (val) {
								case 0:
									return '<span class="text-yellow">未对帐</span>'
								case 1:
									return '<span class="text-green">对账成功</span>'
								case 2:
									return '<span class="text-red">对账失败</span>'
								case 3:
									return '<span class="text-red">已忽略</span>'	
							}
						}						
					}, 
					{
						width: 100,
						field: 'reconciliationMark'
					}, 
				]
			}

			// 初始化数据表格
			$('#settlement_page_bankReconciliation').bootstrapTable(tableConfig);
			// 搜索表单初始化
			$$.searchInit($('#settlement_search_bankReconciliation'), $('#settlement_page_bankReconciliation'));

			//组条件查询
			function getQueryParams(val) {
				var form = document.settlement_search_bankReconciliation
				$.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
				pageOptions.rows = val.limit
				pageOptions.page = parseInt(val.offset / val.limit) + 1
				return val
			}  
            
			$('#balanceQuery').on('click', function(e) {
				e.preventDefault()
				var sform = document.settlement_search_bankReconciliation
				var data = util.form.serializeJson(sform)
				console.log('data'+data);
				data.row = 10
				data.page = 1
				pageOptions = data
				$('#settlement_page_bankReconciliation').bootstrapTable('refresh', pageOptions);
			})
			
			// 获取对账流水按钮点击事件
            $('#btGetKftRecon').on('click', function () {
            	var tradTime = verificationDate($("#tradTime").val());
            	if(tradTime=="noDate"){
            		toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}else if(tradTime=="notDate"){
            		toastr.error("日期格式有误！", '错误信息', {
					  	timeOut: 3000
					})
            		return false;
            	}else{
            		//添加防止重复提交样式
  		  			$('#refreshDiv1').addClass('overlay');
					$('#refreshI1').addClass('fa fa-refresh fa-spin');
					$('#btGetKftRecon').attr("disabled","disabled");
					
	            	http.post(settleConfig.api.settlement.settlement_bankReconciliation_getReconRequest+"?checkDate="+tradTime, {
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
								var checkDate = res.checkDate;
								$("#hideCheckDate").val(checkDate);
            					var tranSn = res.tranSn;
            					$("#tranSn").val(tranSn);
								toastr.info("获取对账流水成功", '提示信息', {
								  	timeOut: 3000
								})
								$('#formTable').bootstrapTable('refresh');
							}
	                    }
	                )
            	}
            })
            
            //查询对账文件并入库
            $('#btRecon').on('click', function () {
            	var checkDate = $("#hideCheckDate").val();
            	var tranSn = $("#tranSn").val();
            	if(tranSn==""){
            		toastr.error("请先发送获取对账文件请求！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}else{
            		//添加防止重复提交样式
  		  			$('#refreshDiv1').addClass('overlay');
					$('#refreshI1').addClass('fa fa-refresh fa-spin');
					$('#btRecon').attr("disabled","disabled");
					
	            	http.post(settleConfig.api.settlement.settlement_bankReconciliation_recon+"?checkDate="+checkDate+"&tranSn="+tranSn, {
	                    }, function (res) {
	                    	//去除重复提交样式
							$('#refreshDiv1').removeClass('overlay');
							$('#refreshI1').removeClass('fa fa-refresh fa-spin');
							$('#btRecon').attr("disabled",false);
							
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
								$("#hideCheckDate").val("");
            					$("#tranSn").val("");
								$('#formTable').bootstrapTable('refresh');
							}
	                    }
	                )
            	}
            })
			
			//对账
			$('#balance').on('click', function(e) {
				var tradTime = $("#tradTime").val();
				var channel = $("#bankType").val();
				if(tradTime.trim().length==0){
					toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
				}
				if(channel.trim().length==0){
					toastr.error("请选择银行渠道！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
				}
				var items = tradTime.split("-");
				var dateString = items.join("");
				confirm.find('.popover-title').html('提示');
				confirm.find('p').html('确定对账？');
				$("#tips_cancle").show();		
				$$.confirm({
					container: confirm,
					trigger: this,
					accept: function() {
					$.ajax({
						type:'post',
						url:settleConfig.api.settlement.settlement_orderReconciliation_check+"?dateString="+dateString+"&channel="+channel,
						beforeSend:	function(){
							//添加防止重复提交样式
		  		  			$('#refreshDiv1').addClass('overlay');
							$('#refreshI1').addClass('fa fa-refresh fa-spin');				
	  						$("#balance").attr("disabled","disabled");
						},
						success:function(data){
							if(data.result==="FAIL"){
								toastr.info(data.resultDetial, '提示信息', {
								timeOut: 3000
								})				
							}
							if(data.result==="SUCCESS"){
								toastr.info("对账成功", '提示信息', {
								timeOut: 3000
								})
							}
							$('#settlement_page_bankReconciliation').bootstrapTable('refresh', pageOptions);
						},
						complete:function(xhr){
							$('#refreshDiv1').removeClass('overlay');
							$('#refreshI1').removeClass('fa fa-refresh fa-spin');				
	                     	$("#balance").removeAttr("disabled");
	               		}
					})
					}
				})			
			})

			//对账忽略
			$('#balanceIgnore').on('click', function(e) {
				var data = [];
				$(".selected").each(function(){
					data.push($(this).children("td:first").children(".oid").val());
				})
				console.log('data:'+JSON.stringify(data));
				if(data.length==0){
						toastr.info("请选择对账列表", '提示信息', {
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
						url:settleConfig.api.settlement.settlement_bankReconciliation_ignore+"?oids="+data,
						beforeSend:	function(){
		  		  			$('#refreshDiv1').addClass('overlay');
							$('#refreshI1').addClass('fa fa-refresh fa-spin');						
	  						$("#balanceIgnore").attr("disabled","disabled");
						},			
						success:function(data){
							$('#settlement_page_bankReconciliation').bootstrapTable('refresh', pageOptions);
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

		}

	}
})