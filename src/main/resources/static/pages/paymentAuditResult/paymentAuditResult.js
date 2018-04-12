/**
 * 提现审核结果查询
 */
define([
    'http',
//	'config',
	'payConfig',
	'settleConfig',
    'util',
    'extension'
], function (http,payConfig,settleConfig, util, $$) {
    return {
        name: 'paymentAuditResult',
        init: function () {
            // 分页配置
            var pageOptions = {
                page: 1,
                row: 10
            }

            // 初始化数据表格
            var tableConfig = {

                ajax: function (origin) {
                    http.post(settleConfig.api.settlement.settlement_payment_page,{
                        data: pageOptions,
                        contentType: 'form'
                    }, function (rlt) {
                        rlt.rows.forEach(function (item) {
                        	if(item.commandStatus==='1'){
                        		item.failDetail='';
                        	}
                            item.type = item.type==='01'?'充值' :'提现';
                            if(item.commandStatus!=null){
                            	item.commandStatus = item.commandStatus==='0'?'未处理' :
                                item.commandStatus==='1'?'交易成功':
                                item.commandStatus==='2'?'交易失败':
                                item.commandStatus==='3'?'交易处理中':
                                item.commandStatus==='4'?'超时':'撤销';
                            }
                            if(item.channelNo!=null){
                            	item.channelNo = loadChannelName(item.channelNo);
//                          	item.channelNo=item.channelNo==='1'?'快付通':
//                          	item.channelNo==='2'?'平安银行':
//                          	item.channelNo==='3'?'先锋代扣':"先锋代付";
                            }
                            if(item.updateStatus!=null){
                            	item.updateStatus = item.updateStatus==='0'?'未处理' :
                            	item.updateStatus==='1'?'交易成功':
                            	item.updateStatus==='2'?'交易失败':
                            	item.updateStatus==='3'?'交易处理中':'超时';
                            }
                            if(item.auditUpdateStatus!=null){
                            	item.auditUpdateStatus = item.auditUpdateStatus==='0'?'审核不通过' :'审核通过';
                            }
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
                onLoadSuccess: function () {
                },
                columns: [
 					{
                    	width: 100, 
                    	align: 'right',
                    	formatter: function(val, row, index) {
                    		return '<div class="func-area"><a href="javascript:void(0)" class="item-detail_payment"  data-toggle="modal">'+row.orderNo+'</a></div>'
						},
						 events: {
                            //交互日志
                            'click .item-detail_payment': function (e, val, row) {
                                var form = document.orderModify
                                util.form.reset($(form))
                                var tableConfigT = {
                                    ajax: function (origin) {
                                        http.get(settleConfig.api.settlement.settlement_banklog_findNo, {
                                            data: {orderNo: row.orderNo},
                                            contentType: 'form'
                                        }, function (rlt) {
											rlt.rows.forEach(function (item) {
												if(item.tradStatus==='1'){
					                        		item.failDetail='';
					                        	}
                            					item.type = item.type==='01'?'充值' :'提现';
                            					item.tradStatus = item.tradStatus==='0'?'未处理' :
                            					item.tradStatus==='1'?'交易成功':
                            					item.tradStatus==='2'?'交易失败':
                            					item.tradStatus==='3'?'交易处理中':'超时';
                        					})
                                            origin.success(rlt)
                                        })
                                    },
                                    pageNumber: pageOptions.page,
                                    pageSize: 100,
                                    pagination: false,
                                    sidePagination: 'server',
                                    queryParams: getQueryParams,
                                    onLoadSuccess: function () {
                                    },
                                    columns: [
                                        {
                                            field: 'sheetId',
                                            align: 'left',
                                            title: '指令ID'
                                        },
                                        {
                                            field: 'orderNo',
                                            align: 'right',
                                            title: '订单号'
                                        },
                                        {
                    						width: 100, 
                    						width: 100,
                    						align: 'right',
                    						title: '交易金额',
                    						formatter: function(val, row, index) {
                    							return row.type==='充值'?(row.amount+'元'):(row.amount+'元');
											}
                    					},
                                        {
                                            field: 'tradStatus',
                                            align: 'left',
                                            title: '交易状态'
                                        },
                                        {
                                            field: 'createTime',
                                            align: 'right',
                                            title: '创建时间'
                                        }
                                    ]
                                }

                                $('#settlement_table').bootstrapTable('refreshOptions', tableConfigT)
                                $('#settlement_table').bootstrapTable(tableConfigT);
                                $('#settlement_log').modal('show')
                            }
                         }
                    },
                    {width: 100, align: 'left', field: 'realName'},
                    {width: 100,align: 'left', field: 'phone'},
                     {width: 100,align: 'left', field: 'channelNo'},
                    {width: 100,align: 'right', field: 'payNo'},
                    {
                    	width: 100, 
                    	width: 100,
                    	align: 'right',
                    	formatter: function(val, row, index) {
                    		return row.type==='充值'?(row.amount+'元'):(row.amount+'元');
						}
                    },
                    {width: 100, align: 'left', field: 'type'},
                    {width: 100,align: 'left', field: 'commandStatus'},
                    {width: 100,align: 'left', field: 'failDetail'},
                    {
                    	width: 100,
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		if(row.updateStatus===null&&row.auditUpdateStatus===null){
                    			return '-';
                    		}else if(row.updateStatus!=null&&row.auditUpdateStatus===null){
                    			return row.updateStatus+'|-';
                    		}else if(row.updateStatus===null&&row.auditUpdateStatus!=null){
                    			return '-|'+row.auditUpdateStatus;
                    		}else{
                    			return row.updateStatus+'|'+row.auditUpdateStatus;
                    		}
						}
                    },
                    {
                    	width: 100,
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		if(row.updateReson===null&&row.auditUpdateReson===null){
                    			return '-';
                    		}else if(row.updateReson!=null&&row.auditUpdateReson===null){
                    			return row.updateReson+'|-';
                    		}else if(row.updateReson===null&&row.auditUpdateReson!=null){
                    			return '-|'+row.auditUpdateReson;
                    		}else{
                    			return row.updateReson+'|'+row.auditUpdateReson;
                    		}
						}
                    },
                    {
                    	width: 100,
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		return row.auditOperatorStatus!=null?row.auditOperatorStatus==='0'?'审核不通过':'审核通过':'-';
						}
                    },
                    {
                    	width: 100,
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		if(row.operatorReson===null&&row.auditOperatorReson===null){
                    			return '-';
                    		}else if(row.operatorReson!=null&&row.auditOperatorReson===null){
                    			return row.operatorReson+'|-';
                    		}else if(row.operatorReson===null&&row.auditOperatorReson!=null){
                    			return '-|'+row.auditOperatorReson;
                    		}else{
                    			return row.operatorReson+'|'+row.auditOperatorReson;
                    		}
						}
                    },
                    {
                    	width: 100,
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		return row.auditResetOperatorStatus!=null?row.auditResetOperatorStatus==='0'?'审核不通过':'审核通过':'-';
						}
                    },
                    {
                    	width: 100,
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		if(row.resetOpertatorReson===null&&row.auditResetOperatorReson===null){
                    			return '-';
                    		}else if(row.resetOpertatorReson!=null&&row.auditResetOperatorReson===null){
                    			return row.resetOpertatorReson+'|-';
                    		}else if(row.resetOpertatorReson===null&&row.auditResetOperatorReson!=null){
                    			return '-|'+row.auditResetOperatorReson;
                    		}else{
                    			return row.resetOpertatorReson+'|'+row.auditResetOperatorReson;
                    		}
						}
                    },
                    {width: 100,align: 'right', field: 'updateTime'},
                    {
                    	width: 100, 
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		return row.auditStatus=row.auditStatus==='0'?'审核不通过':row.auditStatus==='1'?'审核通过':'审核中';
                    	}
                    }
                    ]
            }

            // 初始化数据表格
            $('#settlement_page_payment').bootstrapTable(tableConfig);
            // 搜索表单初始化
            $$.searchInit($('#settlement_search_payment'), $('#settlement_page_payment'));
            //组条件查询
            function getQueryParams(val) {
                var form = document.settlement_search_payment
                $.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
                pageOptions.rows = val.limit
                pageOptions.page = parseInt(val.offset / val.limit) + 1
                return val
            }
            //渠道初始化
            loadChannel();


//             select 改变
            $('#payment_type,#payment_commandStatus,#payment_reconStatus').on('change', function (e) {
            	e.preventDefault()
            	$(this).children('option:selected').val(); 
                var sform = document.settlement_search_payment
                var data = util.form.serializeJson(sform)
                data.row = 10
                data.page = 1
                pageOptions = data
                $('#settlement_page_payment').bootstrapTable('refresh', pageOptions);
            })


            //搜索
            $('#payment_search').on('click', function (e) {
                e.preventDefault()
                var sform = document.settlement_search_payment
                var data = util.form.serializeJson(sform)
                data.row = 10
                data.page = 1
                pageOptions = data
                $('#settlement_page_payment').bootstrapTable('refresh', pageOptions);
            })

            //清空
            $('#payment_reset').on('click', function (e) {
                e.preventDefault()
                var sform = document.settlement_search_payment
                util.form.reset($(sform))
                $('#settlement_page_payment').bootstrapTable('refresh', pageOptions);
            })
            
            $('#pay_operatorReson').on('blur keyup',function(){
   				var len = $(this).val().length;
   				if(len > 49){
   					
    				$(this).val($(this).val().substring(0,50));
   				}
   				if(len>50){
   					$('#pay_tishi').text('您还可以输入0个字符');
   				}else{
   					var num = 50 - len;
   					$('#pay_tishi').text('您还可以输入'+num+'字符');
   				}
  			})
            
            $('#fial_operatorReson').on('blur keyup',function(){
   				var len = $(this).val().length;
   				if(len > 49){
   					
    				$(this).val($(this).val().substring(0,50));
   				}
   				if(len>50){
   					$('#fial_tishi').text('您还可以输入0个字符');
   				}else{
   					var num = 50 - len;
   					$('#fial_tishi').text('您还可以输入'+num+'字符');
   				}
  			})
            
            $('#reset_resetOpertatorReson').on('blur keyup',function(){
   				var len = $(this).val().length;
   				if(len > 49){
   					
    				$(this).val($(this).val().substring(0,50));
   				}
   				if(len>50){
   					$('#reset_tishi').text('您还可以输入0个字符');
   				}else{
   					var num = 50 - len;
   					$('#reset_tishi').text('您还可以输入'+num+'字符');
   				}
  			})
            
            $('#payment_updateReson').on('blur keyup',function(){
   				var len = $(this).val().length;
   				if(len > 49){
   					
    				$(this).val($(this).val().substring(0,50));
   				}
   				if(len>50){
   					$('#update_tishi').text('您还可以输入0个字符');
   				}else{
   					var num = 50 - len;
   					$('#update_tishi').text('您还可以输入'+num+'字符');
   				}
   				
  			})
            
            
            // 初始化数据表格
            $('#settlement_page_payment').bootstrapTable(tableConfig);
            // 搜索表单初始化
            $$.searchInit($('#settlement_search_payment'), $('#settlement_page_payment'));
            //组条件查询
            function getQueryParams(val) {
                var form = document.settlement_search_payment
                $.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
                pageOptions.rows = val.limit
                pageOptions.page = parseInt(val.offset / val.limit) + 1
                return val
            }


//             select 改变
            $('#payment_type,#payment_commandStatus,#payment_reconStatus').on('change', function (e) {
            	e.preventDefault()
            	$(this).children('option:selected').val(); 
                var sform = document.settlement_search_payment
                var data = util.form.serializeJson(sform)
                data.row = 10
                data.page = 1
                pageOptions = data
                $('#settlement_page_payment').bootstrapTable('refresh', pageOptions);
            })


            //搜索
            $('#payment_search').on('click', function (e) {
                e.preventDefault()
                var sform = document.settlement_search_payment
                var data = util.form.serializeJson(sform)
                data.row = 10
                data.page = 1
                pageOptions = data
                $('#settlement_page_payment').bootstrapTable('refresh', pageOptions);
            })

            //清空
            $('#payment_reset').on('click', function (e) {
                e.preventDefault()
                var sform = document.settlement_search_payment
                util.form.reset($(sform))
                $('#settlement_page_payment').bootstrapTable('refresh', pageOptions);
            })
            
            $('#pay_operatorReson').on('blur keyup',function(){
   				var len = $(this).val().length;
   				if(len > 49){
   					
    				$(this).val($(this).val().substring(0,50));
   				}
   				if(len>50){
   					$('#pay_tishi').text('您还可以输入0个字符');
   				}else{
   					var num = 50 - len;
   					$('#pay_tishi').text('您还可以输入'+num+'字符');
   				}
  			})
            
            $('#fial_operatorReson').on('blur keyup',function(){
   				var len = $(this).val().length;
   				if(len > 49){
   					
    				$(this).val($(this).val().substring(0,50));
   				}
   				if(len>50){
   					$('#fial_tishi').text('您还可以输入0个字符');
   				}else{
   					var num = 50 - len;
   					$('#fial_tishi').text('您还可以输入'+num+'字符');
   				}
  			})
            
            $('#reset_resetOpertatorReson').on('blur keyup',function(){
   				var len = $(this).val().length;
   				if(len > 49){
   					
    				$(this).val($(this).val().substring(0,50));
   				}
   				if(len>50){
   					$('#reset_tishi').text('您还可以输入0个字符');
   				}else{
   					var num = 50 - len;
   					$('#reset_tishi').text('您还可以输入'+num+'字符');
   				}
  			})
            
            $('#payment_updateReson').on('blur keyup',function(){
   				var len = $(this).val().length;
   				if(len > 49){
   					
    				$(this).val($(this).val().substring(0,50));
   				}
   				if(len>50){
   					$('#update_tishi').text('您还可以输入0个字符');
   				}else{
   					var num = 50 - len;
   					$('#update_tishi').text('您还可以输入'+num+'字符');
   				}
   				
  			})
  			
  			$('#pay_m,#fail_m,#reset_m,#update_m').on('click',function(){
  			    $(this).attr('disabled','disabled');
  			})
  			
  			//加载渠道所有渠道
			function loadChannel(){
            	http.post(settleConfig.api.settlement.settlement_getChannelPage,
            		{data:pageOptions,contentType: 'form'},
            		function(data){
   						data.rows.forEach(function(item) {
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
