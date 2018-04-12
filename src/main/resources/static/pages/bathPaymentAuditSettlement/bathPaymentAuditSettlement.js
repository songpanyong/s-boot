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
        name: 'bathPaymentAuditSettlement',
        init: function () {
            // 分页配置
            var pageOptions = {
                page: 1,
                row: 10,
                autorType:1
            }

            // 初始化数据表格
            var tableConfig = {

                ajax: function (origin) {
                    http.post(settleConfig.api.settlement.settlement_payment_pageBath,{
                        data: pageOptions,
                        contentType: 'form'
                    }, function (rlt) {
                        rlt.rows.forEach(function (item) {
                        	if(item.commandStatus==='1'){
                        		item.failDetail='';
                        	}
                            item.type = item.type==='01'?'申购' :'赎回';
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
                    			 item.operatorStatus = item.operatorStatus==='01'?'单笔支付' :item.operatorStatus==='02'?'失败重发':'修改支付状态';
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
                },
                columns: [
                   {
                       width: 100,
                       checkbox: true,
                       formatter: function(val,row){
                          return "<input type='text'  class='oid' hidden='hidden' value='"+row.oid+"' >"
                       }
                    }, 
                    {width: 100,align: 'center', field: 'orderNo'},
                    {width: 100,align: 'center', field: 'payNo'},
                    {width: 100,align: 'center', field: 'type'},
                    {width: 100,align: 'center', field: 'commandStatus'},
                    {width: 100,align: 'center', field: 'cardNo'},
                    {width: 100,align: 'center', field: 'realName'},
                    {
                    	width: 100,
                    	align: 'center',
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
                    	align: 'center',
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
                    	align: 'center',
                    	formatter: function(val, row, index) {
                    		return row.auditOperatorStatus!=null?row.auditOperatorStatus==='0'?'审核不通过':'审核通过':'-';
						}
                    },
                    {
                    	width: 100,
                    	align: 'center',
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
                    	align: 'center',
                    	formatter: function(val, row, index) {
                    		return row.auditResetOperatorStatus!=null?row.auditResetOperatorStatus==='0'?'审核不通过':'审核通过':'-';
						}
                    },
                    {
                    	width: 100,
                    	align: 'center',
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
                    {width: 100,align: 'center', field: 'updateTime'},
                    {
                    	width: 100, 
                    	align: 'center',
                    	 title: '展示可操作',
                    	formatter: function(val, row, index) {
                    		var star='';
                    		if(row.auditUpdateStatus===null&&row.updateStatus!=null){
                    			star+='<div class="func-area"><a href="javascript:void(0)" class="item-pass" disabled   data-toggle="modal">修改支付状态</a></div>';
                            }
                            if(row.auditOperatorStatus===null&&row.operatorStatus!=null){
                            	if(row.operatorStatus==='单笔支付'){
                            		star+='<div class="func-area"><a href="javascript:void(0)" class="item-pass" disabled   data-toggle="modal">单笔支付</a></div>';
                            	}else if(row.operatorStatus==='失败重发'){
                            		star+='<div class="func-area"><a href="javascript:void(0)" class="item-pass" disabled   data-toggle="modal">失败重发</a></div>';
                            	}
                            }
                            if(row.auditResetOperatorStatus===null&&row.resetOperatorStatus!=null){
                            	if(row.resetOperatorStatus==='撤销'){
                            		star+='<div class="func-area"><a href="javascript:void(0)" class="item-pass" disabled   data-toggle="modal">撤销</a></div>';
                            	}
                            }
							return star;
                    	}
                      }
                    ]
            }

            // 初始化数据表格
            $('#settlement_page_payment_audit_bath').bootstrapTable(tableConfig);
            // 搜索表单初始化
            $$.searchInit($('#settlement_search_payment_audit_bath'), $('#settlement_page_payment_audit_bath'));
            //组条件查询
            function getQueryParams(val) {
                var form = document.settlement_search_payment_audit_bath
                $.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
                pageOptions.rows = val.limit
                pageOptions.page = parseInt(val.offset / val.limit) + 1
                return val
            }


            //搜索
            $('#payment_search_audit_bath').on('click', function (e) {
                e.preventDefault()
                var sform = document.settlement_search_payment_audit_bath
                var data = util.form.serializeJson(sform)
                data.row = 10
                data.page = 1
                data.autorType=1
                pageOptions = data
                $('#settlement_page_payment_audit_bath').bootstrapTable('refresh', pageOptions);
            })

            //清空
            $('#payment_reset_audit_bath').on('click', function (e) {
                e.preventDefault()
                var sform = document.settlement_search_payment_audit_bath
                util.form.reset($(sform))
                $('#settlement_page_payment_audit_bath').bootstrapTable('refresh', pageOptions);
            })
            
            
            //提交一页
            $('#submit_audit_bath').on('click', function(e) {
            	e.preventDefault()
            	var sform = document.audit_settlement_modify_bath
            	util.form.reset($(sform))
            	$('#auditStatus_1_bath').prop('disabled', false);
            	
            	var data = [];
            	$(".selected").each(function(){
            		data.push($(this).children("td:first").children(".oid").val());
            	})
            	if(data.length==0){
            		toastr.info("请选择需要提交审核的列表", '提示信息', {
            			timeOut: 3000
            		})
            		return false;
            	}


            	$('#settlement_oidBath_audit').val(data);
            	$('#audit_settlement_attEdit_bath').modal('show');
            })
            
            //提交全部
            $('#submit_audit_bath_all').on('click', function(e) {
            	e.preventDefault()
            	var sform = document.audit_settlement_modify_all
            	util.form.reset($(sform))
            	$('#auditStatus_1_bath_all').prop('disabled', false);
            	$('#audit_settlement_attEdit_bath_all').modal('show');
            })
			
            
            //审核全部
            $('#auditStatus_1_bath_all').on('click', function (e) {
            	$('#auditStatus_1_bath_all').prop('disabled', true);
                e.preventDefault()
				var type=$("#audit_type_settlement_bath_all").val();
				
                var sform = document.settlement_search_payment_audit_bath
                var data = util.form.serializeJson(sform)
                data.row = 10
                data.page = 1
                data.autorType=1
                data.bathReson=$("#reson_bath_all").val()
                data.auditStatus='1'
                data.sumbitTy='1'
                data.bathOperatorTag=type
				if(type==='01'||type==='02'){
					Ewin.confirm({ message: "您确定要审核通过？！审核通过后将划款到用户账户,请谨慎操作,不要反复点击确认！" }).on(function (e) {
   						if (e) {
    						http.post(settleConfig.api.settlement.settlement_payment_auditBath, {
                    			data:data,
                   				contentType: 'form'
                			}, function (res) {
                				if(res.returnCode!='0000'){
                              	   toastr.error(res.errorMessage);
                              	}else{
                              		toastr.success("操作成功！");
                              	}
                 				$('#auditStatus_1_bath_all').prop('disabled', false);
                 				$('#audit_validTime_show_input_bath').modal('hide');
                    			$('#audit_settlement_attEdit_bath_all').modal('hide');
                				$('#settlement_page_payment_audit_bath').bootstrapTable('refresh', pageOptions);
                			})
   						}else{
   							return
   						}
   					})
				}else{
					http.post(settleConfig.api.settlement.settlement_payment_auditBath, {
                    			data:data,
                   				contentType: 'form'
                			}, function (res) {
                				if(res.returnCode!='0000'){
                              	   toastr.error(res.errorMessage);
                              	}else{
                              		toastr.success("操作成功！");
                              	}
                 				$('#auditStatus_1_bath_all').prop('disabled', false);
                 				$('#audit_validTime_show_input_bath').modal('hide');
                    			$('#audit_settlement_attEdit_bath_all').modal('hide');
                				$('#settlement_page_payment_audit_bath').bootstrapTable('refresh', pageOptions);
                		})
				}
               
            })
            
            
            //审核不通过
            $('#auditStatus_0_bath_all').on('click', function (e) {
                e.preventDefault()
                 $('#auditStatus_0_bath_all').prop('disabled', true);
                var type=$("#audit_type_settlement_bath_all").val();
				
                var sform = document.settlement_search_payment_audit_bath
                var data = util.form.serializeJson(sform)
                data.row = 10
                data.page = 1
                data.autorType=1
                data.bathReson=$("#reson_bath_all").val()
                data.auditStatus='1'
                data.sumbitTy='1'
                data.bathOperatorTag=type
				 http.post(settleConfig.api.settlement.settlement_payment_auditBath, {
                         data:data,
                        contentType: 'form'
                 	}, function (res) {
                 		if(res.returnCode!='0000'){
                      	   toastr.error(res.errorMessage);
                      	}else{
                      		toastr.success("操作成功！");
                      	}
                 		$('#auditStatus_0_bath_all').prop('disabled', false);
                        $('#settlement_page_payment_audit_bath').modal('hide');
               	        $('#settlement_page_payment_audit_bath').bootstrapTable('refresh', pageOptions);
                 	})
             })
					
            //审核通过
            $('#auditStatus_1_bath').on('click', function (e) {
            	$('#auditStatus_1_bath').prop('disabled', true);
                e.preventDefault()
				var type=$("#audit_type_settlement_bath").val();
				
				if(type==='01'||type==='02'){
					Ewin.confirm({ message: "您确定要审核通过？！审核通过后将划款到用户账户,请谨慎操作,不要反复点击确认！" }).on(function (e) {
   						if (e) {
    						http.post(settleConfig.api.settlement.settlement_payment_auditBath, {
                    			data:{
                    				bathOid:$("#settlement_oidBath_audit").val(),
                    				bathReson:$("#reson_bath").val(),
                       				auditStatus:'1',
                       				bathOperatorTag:type
                    			},
                   				contentType: 'form'
                			}, function (res) {
                				if(res.returnCode!='0000'){
                              	   toastr.error(res.errorMessage);
                              	}else{
                              		toastr.success("操作成功！");
                              	}
                 				$('#auditStatus_1_bath').prop('disabled', false);
                 				$('#audit_validTime_show_input_bath').modal('hide');
                    			$('#audit_settlement_attEdit_bath').modal('hide');
                				$('#settlement_page_payment_audit_bath').bootstrapTable('refresh', pageOptions);
                			})
   						}else{
   							return
   						}
   					})
				}else{
					http.post(settleConfig.api.settlement.settlement_payment_auditBath, {
                    			data:{
                    				bathOid:$("#settlement_oidBath_audit").val(),
                    				bathReson:$("#reson_bath").val(),
                       				auditStatus:'1',
                       				bathOperatorTag:type
                    			},
                   				contentType: 'form'
                			}, function (res) {
                				if(res.returnCode!='0000'){
                              	   toastr.error(res.errorMessage);
                              	}else{
                              		toastr.success("操作成功！");
                              	}
                 				$('#auditStatus_1_bath').prop('disabled', false);
                 				$('#audit_validTime_show_input_bath').modal('hide');
                    			$('#audit_settlement_attEdit_bath').modal('hide');
                				$('#settlement_page_payment_audit_bath').bootstrapTable('refresh', pageOptions);
                		})
				}
               
            })
            
            

			//审核不通过
            $('#auditStatus_0_bath').on('click', function (e) {
                e.preventDefault()
                 $('#auditStatus_0_bath').prop('disabled', true);
				 http.post(settleConfig.api.settlement.settlement_payment_auditBath, {
                         data:{
                        	 bathOid:$("#settlement_oidBath_audit").val(),
             				 bathReson:$("#reson_bath").val(),
                			 auditStatus:'0',
                			 bathOperatorTag:$("#audit_type_settlement_bath").val()
                        },
                        contentType: 'form'
                 	}, function (res) {
                 		if(res.returnCode!='0000'){
                      	   toastr.error(res.errorMessage);
                      	}else{
                      		toastr.success("操作成功！");
                      	}
                 		$('#auditStatus_0_bath').prop('disabled', false);
                        $('#audit_settlement_attEdit_bath').modal('hide');
               	        $('#settlement_page_payment_audit_bath').bootstrapTable('refresh', pageOptions);
                 	})
             })
            
            $('#reson_bath').on('blur keyup',function(){
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
        }
    }
})
