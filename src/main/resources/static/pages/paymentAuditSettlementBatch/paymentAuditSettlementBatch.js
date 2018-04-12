/**
 * 指令管理
 */
define([
    'http',
//	'config',
	'payConfig',
	'settleConfig',
    'util',
    'extension'
], function (http,payConfig,settleConfig,util, $$) {
    return {
        name: 'paymentAuditSettlementBatch',
        init: function () {
            // 分页配置
            var pageOptions = {
                page: 1,
                row: 10,
                autorType:1
            }
            
 			//渠道初始化
            loadChannel();
            
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
            // 初始化数据表格
            var tableConfig = {

                ajax: function (origin) {
                    http.post(settleConfig.api.settlement.settlement_payment_page_batch,{
                        data: pageOptions,
                        contentType: 'form'
                    }, function (rlt) {
                        rlt.rows.forEach(function (item) {
                        	if(item.commandStatus==='1'){
                        		item.failDetail='';
                        	}
                            item.type = item.type==='01'?'充值' :'提现';
                            item.commandStatus = item.commandStatus==='0'?'未处理' :
                            item.commandStatus==='1'?'交易成功':
                            item.commandStatus==='2'?'交易失败':
                            item.commandStatus==='3'?'交易处理中':
                            item.commandStatus==='4'?'超时':'撤销';
                            if(item.updateStatus!=null){
                    			 item.updateStatus = item.updateStatus==='0'?'未处理' :
                            	 item.updateStatus==='1'?'交易成功':
                           	 	 item.updateStatus==='2'?'交易失败':
                            	 item.updateStatus==='3'?'交易处理中':'超时';
                            	 
                    		}
                    		if(item.operatorStatus!=null){
                    			 item.operatorStatus = item.operatorStatus==='01'?'单笔支付' :'失败重发';
                    		}
                    		if(item.channelNo!=null){
                            	item.channelNo = loadChannelName(item.channelNo);
                            }
                    		if(item.resetOperatorStatus!=null){
                   			 	item.resetOperatorStatus ='撤销';
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
                	 http.post(settleConfig.api.settlement.settlement_payment_getAuditBalance,{},
                	 	function(auditAmount){
                	 		$("#auditAmount").html(auditAmount);
                	 		http.post(settleConfig.api.settlement.settlement_order_memBalance,{},
                	 			function(balance){
                	 				if("0000" != balance.returnCode){
                	 					$("#balance_err").html("("+balance.errorMessage+")");
                	 				}else{
                	 					$("#balance").html(balance);
                	 				}
                	 				console.log(balance);
                	 			})
                	 	})
                },
                columns: [
					{
						width: 100,
		                checkbox: true,
		                formatter: function(val,row){
		                	var type='';
		                	var operatorType='';
		                	var crossFlag = row.crossFlag;
		                	if(crossFlag===null){
		                		crossFlag = '正常';
		                	}
		                	if(row.auditUpdateStatus===null&&row.updateStatus!=null){
                                    type='04';//审核修改支付状态
                            }
                            if(row.auditOperatorStatus===null&&row.operatorStatus!=null){
                            	if(row.operatorStatus==='单笔支付'){
                            		type='01';//审核单笔支付
                            	}else if(row.operatorStatus==='失败重发'){
                            		type='02';//审核失败重发
                            	}
                            }
                            
                            if(row.auditResetOperatorStatus===null&&row.resetOperatorStatus!=null){
                            	if(row.resetOperatorStatus==='撤销'){
                            		type='03';//审核撤销
                            	}
                            }
                            if(type==='04'){
								operatorType='02';
							}else if(type==='01'||type==='02'){
								operatorType='01';
							}else {
								operatorType=type;
							}
		                	return "<input type='text'  class='oid' hidden='hidden' value='"+row.oid+"' >"
		                		+"<input type='text'  class='orderNo' hidden='hidden' value='"+row.orderNo+"' >"
		                		+"<input type='text'  class='updateStatus' hidden='hidden' value='"+row.updateStatus+"' >"
		                		+"<input type='text'  class='type' hidden='hidden' value='"+type+"' >"
		                		+"<input type='text'  class='operatorType' hidden='hidden' value='"+operatorType+"' >"
		                		+"<input type='text'  class='crossFlag' hidden='hidden' value='"+crossFlag+"' >"
		                }
		         	},
		         	  {width: 100,align: 'left', field: 'orderNo'},
              		{width: 100, align: 'left', field: 'realName'},
                    {width: 100,align: 'left', field: 'phone'},
                     {width: 100,align: 'left', field: 'channelNo'},
                    {width: 100,align: 'right', field: 'payNo'},
                    {width: 100,align: 'left', field: 'type'},
                    {width: 100,align: 'left', field: 'commandStatus'},
                    {width: 100,align: 'right', field: 'cardNo'},
                     {width: 100,align: 'right', field: 'amount'},
//                  {width: 100,align: 'right', field: 'distanceMark'},
//                  {width: 100,align: 'right', field: 'emergencyMark'},
//                  {width: 100,align: 'left', field: 'crossFlag'},
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
                    	align: 'center',
                    	 title: '操作',
                    	formatter: function(val, row, index) {
                    		var buttons = [{
								text: '审核操作',
								type: 'button',
								class: 'item-pass_audit',
								isRender:(row.auditOperatorStatus===null&&row.operatorStatus!=null)||(row.auditUpdateStatus===null&&row.updateStatus!=null)||(row.auditResetOperatorStatus===null&&row.resetOperatorStatus!=null)
							}]
                    		return util.table.formatter.generateButton(buttons, 'settlement_page_payment_audit');
//                  		if((row.auditOperatorStatus===null&&row.operatorStatus!=null)||(row.auditUpdateStatus===null&&row.updateStatus!=null)||(row.auditResetOperatorStatus===null&&row.resetOperatorStatus!=null)){
//                  			return '<div class="func-area"><a href="javascript:void(0)" class="item-pass"  id="item-pass_audit"   data-toggle="modal">审核操作</a></div>'
//                  		}
							
                    	},
						 events: {
                            //单笔提现
                            'click .item-pass_audit': function (e, val, row) {
                            	$('#audit_tishi').text('');
                            	$('#audit_resule').val('')
                                $("#reson").val('')
                            	if(row.updateStatus!=null){
                    			 	row.updateStatus =row.updateStatus==='未处理'?'0' :
                            	 	row.updateStatus==='交易成功'?'1':
                           	 	 	row.updateStatus==='交易失败'?'2':
                            	 	row.updateStatus==='交易处理中'?'3':
                            	 	row.updateStatus==='超时'?'4':'5';
                    			}
                                var form = document.audit_settlement_modify
                                util.form.reset($(form))
                                $("#audit_settlement_oid").val("");
                				$("#audit_settlement_orderNo").val("");
                				$("#audit_settlement_updateStatus").val("");
                				$("#audit_crossFlag_settlement").val("");
                				$("#audit_type_settlement").empty();
                				$("#audit_settlement_oid").val(row.oid);
                				$("#audit_settlement_orderNo").val(row.orderNo);
                				$("#audit_settlement_updateStatus").val(row.updateStatus);
                				
                                if(row.auditUpdateStatus===null&&row.updateStatus!=null){
                                    $("#audit_type_settlement").append($('<option>').val('04').text('审核修改支付状态'));
                                }
                                if(row.auditOperatorStatus===null&&row.operatorStatus!=null){
                                	if(row.operatorStatus==='单笔支付'){
                                		$("#audit_type_settlement").append($('<option>').val('01').text('审核单笔支付'));
                                	}else if(row.operatorStatus==='失败重发'){
                                		$("#audit_type_settlement").append($('<option>').val('02').text('审核失败重发'));
                                	}
                                }
                                if(row.auditResetOperatorStatus===null&&row.resetOperatorStatus!=null){
                                	if(row.resetOperatorStatus==='撤销'){
                                		$("#audit_type_settlement").append($('<option>').val('03').text('审核撤销'));
                                	}
                                }
                                $("#audit_crossFlag_settlement").val(row.crossFlag);
                                $('#audit_settlement_attEdit').modal('show');
                            }
                            
                         }
                    }
                    ]
            }

            // 初始化数据表格
            $('#settlement_page_payment_audit').bootstrapTable(tableConfig);
            // 搜索表单初始化
            $$.searchInit($('#settlement_search_payment_audit'), $('#settlement_page_payment_audit'));
            //组条件查询
            function getQueryParams(val) {
                var form = document.settlement_search_payment_audit
                $.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
                pageOptions.rows = val.limit
                pageOptions.page = parseInt(val.offset / val.limit) + 1
                return val
            }


//             select 改变
            $('#payment_type_audit,#payment_commandStatus_audit,#payment_reconStatus_audit').on('change', function (e) {
            	e.preventDefault()
            	$(this).children('option:selected').val(); 
                var sform = document.settlement_search_payment_audit
                var data = util.form.serializeJson(sform)
                data.row = 10
                data.page = 1
                data.autorType=1
                pageOptions = data
                $('#settlement_page_payment_audit').bootstrapTable('refresh', pageOptions);
            })


            //搜索
            $('#payment_search_audit').on('click', function (e) {
                e.preventDefault()
                var sform = document.settlement_search_payment_audit
                var data = util.form.serializeJson(sform)
                data.row = 10
                data.page = 1
                data.autorType=1
                pageOptions = data
                $('#settlement_page_payment_audit').bootstrapTable('refresh', pageOptions);
            })

            //清空
            $('#payment_reset_audit').on('click', function (e) {
                e.preventDefault()
                var sform = document.settlement_search_payment_audit
                util.form.reset($(sform))
                $('#settlement_page_payment_audit').bootstrapTable('refresh', pageOptions);
            })
            
            //审核通过
            $('#auditStatus_1').on('click', function (e) {
                e.preventDefault()
				var type=$("#audit_type_settlement").val(),operatorType='',updateStatus=$("#audit_settlement_updateStatus").val();
				var crossFlag = $("#audit_crossFlag_settlement").val();
				if(type==='04'){
					operatorType='02';
				}else if(type==='01'||type==='02'){
					operatorType='01';
				}else {
					operatorType=type;
				}
//				if(crossFlag==='正常'){
					if(operatorType==='01'){
						Ewin.confirm({ message: "您确定要审核通过？！审核通过后将划款到用户账户,请谨慎操作,不要反复点击确认！" }).on(function (e) {
	   						if (e) {
	   							$('#audit_settlement_attEdit').modal('hide');
	    						http.post(settleConfig.api.settlement.settlement_payment_audit, {
	                    			data:{
	                       				oid:$("#audit_settlement_oid").val(),
	                       				orderNo:$("#audit_settlement_orderNo").val(),
	                       				reson:$("#reson").val(),
	                       				auditStatus:'1',
	                       				operatorType:operatorType,
	                       				operatorStatus:type,
	                       				updateStatus:updateStatus
	                    			},
	                   				contentType: 'form'
	                			}, function (res) {
	                				if(res.returnCode!='0000'){
	                              	   toastr.error(res.errorMessage);
	                              	}else{
	                              		toastr.success("操作成功！");
	                              	}
	                              	$('#auditStatus_1').prop('disabled', false);
	                 				$('#audit_validTime_show_input').modal('hide');
	                				$('#settlement_page_payment_audit').bootstrapTable('refresh', pageOptions);
	                			})
	   						}else{
	   							return
	   						}
	   					})
					}else{
						$('#audit_settlement_attEdit').modal('hide');
						http.post(settleConfig.api.settlement.settlement_payment_audit, {
	            			data:{
	               				oid:$("#audit_settlement_oid").val(),
	               				orderNo:$("#audit_settlement_orderNo").val(),
	               				reson:$("#reson").val(),
	               				auditStatus:'1',
	               				operatorType:operatorType,
	               				operatorStatus:type,
	               				updateStatus:updateStatus
	            			},
	           				contentType: 'form'
	        			}, function (res) {
	        				if(res.returnCode!='0000'){
	                      	   toastr.error(res.errorMessage);
	                      	}else{
	                      		toastr.success("操作成功！");
	                      	}
	         				$('#auditStatus_1').prop('disabled', false);
	         				$('#audit_validTime_show_input').modal('hide');
	        				$('#settlement_page_payment_audit').bootstrapTable('refresh', pageOptions);
	        			})
					}
//				}else{
//					toastr.error("该用户已被冻结，审核无法通过！");
//					$('#settlement_page_payment_audit').bootstrapTable('refresh', pageOptions);
//				}
            })
            
            

			//审核不通过
            $('#auditStatus_0').on('click', function (e) {
                e.preventDefault()
                 $('#auditStatus_0').prop('disabled', true);
                var type=$("#audit_type_settlement").val(),operatorType='';
				if(type==='04'){
					operatorType='02';
				}else if(type==='01'||type==='02'){
					operatorType='01';
				}else {
					operatorType=type;
				}
				 http.post(settleConfig.api.settlement.settlement_payment_audit, {
                         data:{
                        	oid:$("#audit_settlement_oid").val(),
                        	orderNo:$("#audit_settlement_orderNo").val(),
                        	auditStatus:'0',
                        	reson:$("#reson").val(),
                        	operatorType:operatorType,
                        	operatorStatus:type
                        },
                        contentType: 'form'
                 	}, function (res) {
                 		if(res.returnCode!='0000'){
                      	   toastr.error(res.errorMessage);
                      	}else{
                      		toastr.success("操作成功！");
                      	}
                 		$('#auditStatus_0').prop('disabled', false);
                        $('#audit_settlement_attEdit').modal('hide');
               	        $('#settlement_page_payment_audit').bootstrapTable('refresh', pageOptions);
                 	})
             })
            
            $('#reson').on('blur keyup',function(){
   				var len = $(this).val().length;
   				if(len > 49){
   					
    				$(this).val($(this).val().substring(0,50));
   				}
   				if(len>50){
   					$('#audit_tishi').text('您还可以输入0个字符');
   				}else{
   					var num = 50 - len;
   					$('#audit_tishi').text('您还可以输入'+num+'字符');
   				}
   				
  			})
            
            //批量审核
			$('#batchAudit').on('click', function(e) {
				var oids = [];
				var orderNos = [];
				var operatorTypes = [];
				var operatorTypes = [];
				var types = [];
				var updateStatus = [];
				$(".selected").each(function(){
//					if($(this).children("td:first").children(".crossFlag").val()==='正常'){
						oids.push($(this).children("td:first").children(".oid").val());
						orderNos.push($(this).children("td:first").children(".orderNo").val());
						operatorTypes.push($(this).children("td:first").children(".operatorType").val());
						types.push($(this).children("td:first").children(".type").val());
						updateStatus.push($(this).children("td:first").children(".updateStatus").val());
//					}
				})
//				console.log('oids:'+JSON.stringify(oids));
//				console.log('orderNos:'+JSON.stringify(orderNos));
//				console.log('operatorTypes:'+JSON.stringify(operatorTypes));
//				console.log('types:'+JSON.stringify(types));
//				console.log('updateStatus:'+JSON.stringify(updateStatus));
				if(oids.length==0){
						toastr.info("请选择批量支付订单列表", '提示信息', {
						timeOut: 3000
					})
					return false;
				}else{
					$('#batch_audit_settlement_attEdit').modal('show');
				}
			})
			
			//批量审核不通过
            $('#batch_auditStatus_0').on('click', function (e) {
                e.preventDefault()
                $('#batch_auditStatus_0').prop('disabled', true);
                $('#batch_auditStatus_1').prop('disabled', true);
                var oids = [];
				var orderNos = [];
				var operatorTypes = [];
				var operatorTypes = [];
				var types = [];
				var updateStatus = [];
				var auditStatus = 0;
				var reson = $("#batch_reson").val();
				$(".selected").each(function(){
					oids.push($(this).children("td:first").children(".oid").val());
					orderNos.push($(this).children("td:first").children(".orderNo").val());
					operatorTypes.push($(this).children("td:first").children(".operatorType").val());
					types.push($(this).children("td:first").children(".type").val());
					updateStatus.push($(this).children("td:first").children(".updateStatus").val());
				})
				 http.post(settleConfig.api.settlement.settlement_payment_audit_batch+"?oids="+oids+"&orderNos="+orderNos
						+"&reson="+reson+"&auditStatus="+auditStatus+"&operatorTypes="+operatorTypes
						+"&operatorStatus="+types+"&updateStatus="+updateStatus, {
            			}, function (res) {
                 		if(res.result==="SUCCESS"){
							toastr.success("批量操作数据共"+res.totle+"条，成功"+res.succCount+"条！");
						}else{
							toastr.error("批量操作失败！请重试。。。");
						}
                 		$('#batch_auditStatus_0').prop('disabled', false);
                 		$('#batch_auditStatus_1').prop('disabled', false);
                        $('#batch_audit_settlement_attEdit').modal('hide');
               	        $('#settlement_page_payment_audit').bootstrapTable('refresh', pageOptions);
                 	})
             })
            
             //批量审核通过
            $('#batch_auditStatus_1').on('click', function (e) {
                e.preventDefault()
                $('#batch_auditStatus_0').prop('disabled', true);
                $('#batch_auditStatus_1').prop('disabled', true);
                var oids = [];
				var orderNos = [];
				var operatorTypes = [];
				var types = [];
				var updateStatus = [];
				var auditStatus = 1;
				var reson = $("#batch_reson").val();
				$(".selected").each(function(){
					oids.push($(this).children("td:first").children(".oid").val());
					orderNos.push($(this).children("td:first").children(".orderNo").val());
					operatorTypes.push($(this).children("td:first").children(".operatorType").val());
					types.push($(this).children("td:first").children(".type").val());
					updateStatus.push($(this).children("td:first").children(".updateStatus").val());
				})
				Ewin.confirm({ message: "您确定要审核通过？！若审核单笔支付，审核通过后将划款到用户账户,请谨慎操作,不要反复点击确认！" }).on(function (e) {
					if (e) {
						http.post(settleConfig.api.settlement.settlement_payment_audit_batch+"?oids="+oids+"&orderNos="+orderNos
						+"&reson="+reson+"&auditStatus="+auditStatus+"&operatorTypes="+operatorTypes
						+"&operatorStatus="+types+"&updateStatus="+updateStatus, {
            			}, function (res) {
            				if(res.result==="SUCCESS"){
								toastr.success("批量操作数据共"+res.totle+"条，成功"+res.succCount+"条！");
							}else{
								toastr.error("批量操作失败！请重试。。。");
							}
	                 		$('#batch_auditStatus_0').prop('disabled', false);
	                 		$('#batch_auditStatus_1').prop('disabled', false);
	                        $('#batch_audit_settlement_attEdit').modal('hide');
            				$('#settlement_page_payment_audit').bootstrapTable('refresh', pageOptions);
            			})
					}else{
						$('#batch_auditStatus_0').prop('disabled', false);
                 		$('#batch_auditStatus_1').prop('disabled', false);
						return;
					}
				})
               
            })
            
            //批量审核原因
            $('#batch_reson').on('blur keyup',function(){
   				var len = $(this).val().length;
   				if(len > 49){
   					
    				$(this).val($(this).val().substring(0,50));
   				}
   				if(len>50){
   					$('#batch_audit_tishi').text('您还可以输入0个字符');
   				}else{
   					var num = 50 - len;
   					$('#batch_audit_tishi').text('您还可以输入'+num+'字符');
   				}
   				
  			})
            
            
            
        }
    }
})
