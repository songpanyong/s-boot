/**
 * 结算通道银行信息管理
 */
define([
	'http',
	'settleConfig',
	'util',
	'extension'
], function(http,settleConfig, util, $$) {
	return {
		name: 'bankCallback',
		init: function() {
			// 分页配置
			var pageOptions = {
				page: 1,
				row: 10
			}
			var pageOptionsDetail = {
				page: 1,
				row: 10
			}
			// 初始化数据表格
			var tableConfig = {
				
				ajax: function(origin) {
					http.post(settleConfig.api.settlement.settlement_bankCallback_page, {
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
				onClickCell: function (field, value, row, $element) {
					switch (field) {
						case 'oid':
							qryInfo(value,row)
							break
					}
				},
				columns: [
					{
						width: 30,
						align: 'center',
						formatter: function(val, row, index) {
							return(pageOptions.number - 1) * pageOptions.size + index + 1
						}
					},
					{
						field: 'oid',
						width: 100,
						class:"table_title_detail"						
					},
					{
						width: 100,
						field: 'orderNO',
						align: 'right'
					}, 
					{
						width: 100,
						field: 'channelNo',
						formatter: function (value) {
//	                    		if(value == '1'){
//	                    			return '快付通';
//	                    		}else if(value == '2'){
//	                    			return '平安';
//	                    		}else if(value == '3'){
//	                    			return '先锋支付(代扣)';
//	                    		}else if(value == '4'){
//	                    			return '先锋支付(代付)';
//	                    		}
							return loadChannelName(value);
                   		 }
					},
					{ 	
						width: 100,
						field: 'tradeType',
						formatter: function (value) {
	                    		if(value == '01'){
	                    			return '充值';
	                    		}else if(value == '02'){
	                    			return '提现';
	                    		}else if(value == '03'){
	                    			return '赎回';
	                    		}
                   		 }
					}, {
						width: 100,
						field: 'callbackDate',
						align: 'right'
					}, {
						width: 100,
						field: 'count',
						align: 'right'
					}, {
						width: 100,
						field: 'totalCount',
						align: 'right'
					}, {
						width: 100,
						field: 'status',
						formatter: function (value) {
	                    		if(value == '0'){
	                    			return '未处理';
	                    		}else if(value == '1'){
	                    			return '成功';
	                    		}else if(value == '2'){
	                    			return '失败';
	                    		}else if(value == '3'){
	                    			return '处理中';
	                    		}else if(value == '4'){
	                    			return '超时';
	                    		}
                   		 }
					}, {
						width: 100,
						field: 'returnCode',
						align: 'right'
					}, {
						width: 100,
						field: 'returnMsg'
					}, {
						width: 100,
						field: 'bankReturnSerialId',
						align: 'right'
					}, {
						width: 100,
						field: 'payNo',
						align: 'right'
					}, 
					{
						width: 100,
						field: 'type',
						formatter: function (value) {
	                    		if(value == 'bank'){
	                    			return '支付通道';
	                    		}else if(value == 'settlement'){
	                    			return '业务系统';
	                    		}else {
	                    			return '-';
	                    		}
                   		 }
					}, 
					{
						width: 100,
						field: 'createTime',
						align: 'right'
					},{
						width: 180,
						align: 'center',
						formatter: function(val, row, index) {
//							if(row.totalCount==row.count){
//	                    		var buttons = [{
//									text: '详情',
//									type: 'button',
//									class: 'item-detail'
//								},
//								{
//									text: '手动回调',
//									type: 'button',
//									class: 'item-handCallBack',
//									isRender: true
//								}]
//	                    	}else{
//	                    		var buttons = [{
//									text: '详情',
//									type: 'button',
//									class: 'item-detail'
//								}]
//	                    	}
//	                    	

	                    	
	                    	var buttons = [{
									text: '手动回调',
									type: 'button',
									class: 'item-handCallBack',
									isRender: row.totalCount==row.count
								}]
	                    
							return util.table.formatter.generateButton(buttons, 'formTable')
						},
						events: {
//						'click .item-detail': function(e, val, row) {},
						'click .item-handCallBack': handCallBack
					}
				}]
			}

			// 初始化数据表格
			$('#formTable').bootstrapTable(tableConfig);
			$$.searchInit($('#searchForm'), $('#formTable'));
			loadChannel();
			
			//加载渠道选择
			function loadChannel(){
				$("#search_channelNo").html("");
            	$("#search_channelNo").append("<option value=''>"+'全部'+"</option>");
            	http.post(settleConfig.api.settlement.settlement_getChannelPage,
            		{data:pageOptions,contentType: 'form'},
            		function(data){
   						data.rows.forEach(function(item) {
   							$("#ul_Channel_Name").append("<li id=channel_Name"+item.channelNo+">"+item.channelName+"</option>");
							$("#search_channelNo").append("<option value="+item.channelNo+">"+item.channelName+"</option>");
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
				var data = util.form.serializeJson(form);
				var beginTime = data.startTime;
                var endTime = data.endTime;
                if(beginTime != "" && endTime !="" ){
                	var beginDate = new Date(beginTime.replace(/\-/g, "\/"));  
 					var endDate = new Date(endTime.replace(/\-/g, "\/"));
 					if(beginDate>endDate){
 						toastr.error('不能小于下单开始时间!', '错误信息', {timeOut: 3000})
                		return;
 					}
                }
				return val
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
			
			$('#bankCallbackLogTableClose').on('click', function() {
				$('#bankCallbackLogModal').modal('hide')
			});
			$('#callBack_up').on('click', function() {
			    $('#handCallBackCount').val( parseInt($('#handCallBackCount').val(), 10) + 1);  
			});  
			$('#callBack_down').on('click', function() { 
			    $('#handCallBackCount').val( parseInt($('#handCallBackCount').val(), 10) - 1);  
			});
			
			//加载渠道名称
			function loadChannelName(channelId){
				return $("#channel_Name"+channelId).html();
			}
			
			function handCallBack(e, value, row){
				var confirm = $('#confirmModal');
				confirm.find('.popover-title').html('提示');
				confirm.find('p').html('新增回调');
				$$.confirm({
					container: confirm,
					trigger: this,
					accept: function() {
						var handCallBackCount = parseInt($('#handCallBackCount').val(), 10) + row.totalCount;
						http.get(settleConfig.api.settlement.settlement_bankCallback_handCallBack, {
							data: {
								oid:row.oid,
                    			totalCount:handCallBackCount
                            },
                    		contentType: 'form'
						}, function(res) {
							if(res.result == 'SUCCESS') {
								confirm.modal('hide');
								$('#formTable').bootstrapTable('refresh');
								toastr.info("新增回调次数成功！", '提示信息', {
										timeOut: 3000
									})
							} else {
								toastr.error("新增回调次数失败！", '提示信息', {
										timeOut: 3000
								})
							}
						})
					}
				})
			}
			
			
			function qryInfo(value,row){
				
				$('#bankCallbackLogModal').modal('show');
				pageOptionsDetail.callBackOid =row.oid;
				// 数据表格配置
				var tableConfigLog = {
					ajax: function(origin) {
						http.post(settleConfig.api.settlement.settlement_bankCallbackLog_list, {
							data:pageOptionsDetail,contentType: 'form'
						}, function(rlt) {
							origin.success(rlt)
						})
					},
					pageNumber: pageOptionsDetail.page,
					pageSize: pageOptionsDetail.row,
					pagination: true,
					sidePagination: 'server',
					pageList: [10, 20, 30, 50, 100],
					queryParams: getQueryParamsDetail,
					onLoadSuccess: function() {},
					columns: [
					{ 
						width: 50,
						// 回调主表Id
						field: 'callBackOid',
						align: 'right'
						
					}, { 
						width: 50,
						//支付流水号
						field: 'payNo',
						align: 'right'
						
					},
					{ 
						width: 50,
						// 状态
						field: 'status',
						formatter: function (value) {
							var className = '';
	                    		if(value == '0'){
	                    			className = 'text-red';
	                    			return '<span class="' + className + '">未处理</span>';
	                    		}else if(value == '1'){
	                    			className = 'text-green';
	                    			return '<span class="' + className + '">成功</span>';
	                    		}else if(value == '2'){
	                    			className = 'text-red';
	                    			return '<span class="' + className + '">失败</span>';
	                    		}else if(value == '3'){
	                    			className = 'text-yellow';
	                    			return '<span class="' + className + '">处理中</span>';
	                    		}else if(value == '4'){
	                    			className = 'text-blue';
	                    			return '<span class="' + className + '">超时</span>';
	                    		}
                   		 }
					}, { 
						width: 50,
						//银行返回号
						field: 'returnCode',
						align: 'right'
						
					},{ 
						width: 50,
						//银行返回信息
						field: 'returnMsg'
						
					},{ 
						width: 50,
						//银行返回流水号
						field: 'bankReturnSerialId',
						align: 'right'
						
					},{ 
						width: 100,
						//创建时间
						field: 'createTime',
						align: 'right'
						
					}
					]
				}
				// 初始化数据表格
				$('#bankCallbackLogTable').bootstrapTable('destroy')
				$('#bankCallbackLogTable').bootstrapTable(tableConfigLog);
			}
			
			//组条件查询
			function getQueryParamsDetail(val) {
				pageOptionsDetail.rows = val.limit
				pageOptionsDetail.page = parseInt(val.offset / val.limit) + 1
				pageOptionsDetail.size = val.limit
				pageOptionsDetail.number = parseInt(val.offset / val.limit) + 1
				return val
			}
		}
	}
})