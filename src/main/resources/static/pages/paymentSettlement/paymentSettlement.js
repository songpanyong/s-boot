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
], function (http,payConfig,settleConfig, util, $$) {
    return {
        name: 'paymentSettlement',
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
                                item.commandStatus==='4'?'超时':
                                item.commandStatus==='5'?'撤销':'未知';
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
                    	// class:'table_title_detail',
                    	formatter: function(val, row, index) {
                    	    return row.orderNo;
                    		// return '<div class="func-area"><a href="javascript:void(0)" style="color:#333;" class="item-detail_payment"  data-toggle="modal">'+row.orderNo+'</a></div>'
						},
						/* events: {
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
                                           
                                            title: '指令ID'
                                        },
                                        {
                                            field: 'orderNo',
                                            align: 'right',
                                            title: '订单号'
                                        },
                                        {
                    						width: 100, 
                    						
                    						title: '订单金额',
                    						formatter: function(val, row, index) {
                    							return row.type==='充值'?(row.amount+'元'):(row.amount+'元');
											}
                    					},
                                        {
                                            field: 'tradStatus',
                                            
                                            title: '订单状态'
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
                         }*/
                    },
                    {width: 100,align: 'right', field: 'payNo'},
                    {width: 100, align: 'left', field: 'type'},
                    {width: 100,align: 'left', field: 'commandStatus'},
                    {
                    	width: 100, 

                    	formatter: function(val, row, index) {
                    		return row.type==='充值'?(row.amount+'元'):(row.amount+'元');
						}
                    },
                     {width: 100, align: 'left', field: 'realName'},
                     {width: 100,align: 'left', field: 'phone'},
                     {
                    	width: 100,
                    	align: 'right',
                    	formatter: function(val, row, index) {
                    		return row.userType==='T1'?'投资者':row.userType==='T2'?'发行人':row.userType==='T3'?'平台':'-';
						}
                    },
                     {width: 100,align: 'right', field: 'cardNo'},
                     {width: 100,align: 'left', field: 'channelNo'},
                    {
                    	width: 100,
                    	align: 'left',
                    	formatter: function(val, row, index) {
                            var str ="-";
                            if(row.auditStatus === null){
                                return str;
                            } else if(row.auditStatus =="1"){
                                str ="审核通过";
                            }else if(row.auditStatus =="0"){
                                str ="审核不通过";
                            }
                            return str;
						}
                    },
                    {
                    	width: 100,
                    	align: 'left',
                    	formatter: function(val, row, index) {
							if(row.updateReson===null){
								return '-';
							}else{
								return  row.updateReson;
							}
						}
                    },
                    {
                    	field: 'failDetail',
                            formatter: function (value, row, index) {
                                if (undefined == value || null == value) {
                                    value = "-";
                                }
                                var _val = value;
                                if (('' + value).length > 10) {
                                    _val = value.substr(0, 10) + "……"
                                }
                                return "<span title='" + value + "'>" + _val + "</span>"
                            }
                    },
//                  {
//                  	width: 100,
//                  	align: 'left',
//                  	formatter: function(val, row, index) {
//                  		return row.auditOperatorStatus!=null?row.auditOperatorStatus==='0'?'审核不通过':'审核通过':'-';
//						}
//                  },
//                  {
//                  	width: 100,
//                  	align: 'left',
//                  	formatter: function(val, row, index) {
//                  		if(row.operatorReson===null&&row.auditOperatorReson===null){
//                  			return '-';
//                  		}else if(row.operatorReson!=null&&row.auditOperatorReson===null){
//                  			return row.operatorReson+'|-';
//                  		}else if(row.operatorReson===null&&row.auditOperatorReson!=null){
//                  			return '-|'+row.auditOperatorReson;
//                  		}else{
//                  			return row.operatorReson+'|'+row.auditOperatorReson;
//                  		}
//						}
//                  },
//                  {
//                  	width: 100,
//                  	align: 'left',
//                  	formatter: function(val, row, index) {
//                  		return row.auditResetOperatorStatus!=null?row.auditResetOperatorStatus==='0'?'审核不通过':'审核通过':'-';
//						}
//                  },
//                  {
//                  	width: 100,
//                  	align: 'left',
//                  	formatter: function(val, row, index) {
//                  		if(row.resetOpertatorReson===null&&row.auditResetOperatorReson===null){
//                  			return '-';
//                  		}else if(row.resetOpertatorReson!=null&&row.auditResetOperatorReson===null){
//                  			return row.resetOpertatorReson+'|-';
//                  		}else if(row.resetOpertatorReson===null&&row.auditResetOperatorReson!=null){
//                  			return '-|'+row.auditResetOperatorReson;
//                  		}else{
//                  			return row.resetOpertatorReson+'|'+row.auditResetOperatorReson;
//                  		}
//						}
//                  },
                    {width: 100,align: 'right', field: 'updateTime'},
                    {	
						align: 'center',
						formatter: function(val, row, index) {
							var star = '';
							if(row.operatorStatus!=null&&row.auditOperatorStatus===null){
								if(row.commandStatus==='未处理'){
                    				star+='<div class="func-area"><a href="javascript:void(0)" data-toggle="modal" class="text-yellow">支付审核中</a></div>'
                    				return star;
                    			}else if(row.commandStatus!='交易成功'){
                    				star+='<div class="func-area"><a href="javascript:void(0)" data-toggle="modal">重发审核中</a></div>'
                    				return star;
                    			}
							}else if(row.resetOperatorStatus!=null&&row.auditResetOperatorStatus===null){
								if(row.commandStatus!='交易成功'){
                    				star+='<div class="func-area"><a href="javascript:void(0)" data-toggle="modal">撤回审核中</a></div>'
                    				return star;
								}
							}else if(row.updateStatus!=null&&row.auditUpdateStatus===null){
								if(row.commandStatus!='交易成功'){
									star=star+'<div class="func-area"><a href="javascript:void(0)" data-toggle="modal">修改审核中</a></div>'
									return star;
								}
							}
							var show = true;
							if(row.commandStatus==='撤销'||row.commandStatus==='交易成功'){
								show = false;
							}
							var buttons = [{
								text: '操作',
								type: 'buttonGroup',
								isRender: show,
								isCloseBottom: index > $('#settlement_page_payment').bootstrapTable('getData').length - 1,
								sub: [
//								{
//									text: '单笔提现',
//									class: 'item-pay',
//									isRender: row.type==='提现'&&row.commandStatus!='撤销'&&(row.operatorStatus===null||row.auditOperatorStatus!=null)&&row.commandStatus==='未处理'
//								},
//								{
//									text: '失败重发',
//									class: 'item-pay',
//									isRender: row.type==='提现'&&row.commandStatus!='撤销'&&(row.operatorStatus===null||row.auditOperatorStatus!=null)&&row.commandStatus!='交易成功'&&row.commandStatus!='未处理'
//								},
//								{
//									text: '撤回',
//									class: 'item-reset',
//									isRender: row.type==='提现'&&row.commandStatus!='撤销'&&(row.resetOperatorStatus===null||row.auditResetOperatorStatus!=null)&&row.commandStatus!='交易成功'
//								},
//								{
//									text: '修改状态',
//									class: 'item-update',
////									isRender: row.type==='提现'&&row.commandStatus!='撤销'&&(row.updateStatus===null||row.auditUpdateStatus!=null)&&row.commandStatus!='交易成功'
//									isRender: row.type==='提现'&&row.commandStatus!='撤销'&&(row.updateStatus===null||row.auditUpdateStatus!=null)
//								}
//								,
								{
									text: '修改状态',
									class: 'item-update',
									isRender: row.type==='充值'&&row.commandStatus!='撤销'&&(row.updateStatus===null||row.auditUpdateStatus!=null)&&row.commandStatus!='交易成功'&&row.reconStatus===0
								}
								]
							}]
							return util.table.formatter.generateButton(buttons, 'settlement_page_payment');
						},
//						events: {
//							'click .item-confirm': toConfirm
//						}
//
//                  	width: 500, 
//                  	align: 'center',
//                  	 title: '操作',
//                  	formatter: function(val, row, index) {
//                  		var star='';
//                  		var buttons = [];
//                  		if(row.type==='提现'&&row.commandStatus!='撤销'){
//                  			if(row.operatorStatus===null||row.auditOperatorStatus!=null){
//                    				if(row.commandStatus==='未处理'){
//                  					//单笔提现
////                  					star+='<div class="func-area"><a href="javascript:void(0)" class="item-pay"  id="item-pay"   data-toggle="modal">单笔支付</a></div>'
//                    					buttons.push({
//											text: '单笔支付',
//											type: 'button',
//											class: 'item-pay',
//											isRender: true
//										});
//                    				}else if(row.commandStatus!='交易成功'){
//                  					//|补单
//                  					//star+='<div class="func-area"><a href="javascript:void(0)" class="item-addOrder"  id="item-addOrder" data-toggle="modal">失败重发</a></div>'
//                    					buttons.push({
//											text: '失败重发',
//											type: 'button',
//											class: 'item-addOrder',
//											isRender: true
//										});
//                    				}
//                  				
//                  			}else {
//                  				if(row.commandStatus==='未处理'){
//                  					//star+='<div class="func-area"><a href="javascript:void(0)" class="item-pay"  data-toggle="modal">支付审核中</a></div>'
//                  					buttons.push({
//											text: '支付审核中',
//											type: 'button',
//											class: 'item-pay',
//											isRender: true
//										});
//                  				}else if(row.commandStatus!='交易成功'){
//                  					//star+='<div class="func-area"><a href="javascript:void(0)" class="item-addOrder"  data-toggle="modal">重发审核中</a></div>'
//                  					buttons.push({
//											text: '重发审核中',
//											type: 'button',
//											class: 'item-addOrder',
//											isRender: true
//										});
//                  				}
//                  				
//                  			}
//                  			if(row.resetOperatorStatus===null||row.auditResetOperatorStatus!=null){
//                    				if(row.commandStatus!='交易成功'){
//                  					//star+='<div class="func-area"><a href="javascript:void(0)" class="item-reset"  id="item-reset" data-toggle="modal">撤回</a></div>'
//                  					buttons.push({
//											text: '撤回',
//											type: 'button',
//											class: 'item-reset',
//											isRender: true
//										});
//                    				}
//                  			}else{
//                  				if(row.commandStatus!='交易成功'){
//                  					//star+='<div class="func-area"><a href="javascript:void(0)" class="item-reset"  data-toggle="modal">撤回审核中</a></div>'
//                  					buttons.push({
//											text: '撤回审核中',
//											type: 'button',
//											class: 'item-reset',
//											isRender: true
//										});
//                  				}
//                  			}
//									
//								if(row.updateStatus===null||row.auditUpdateStatus!=null){
//									if(row.commandStatus!='交易成功'){
//										//star=star+'<div class="func-area"><a href="javascript:void(0)" class="item-update"  id="item-update"   data-toggle="modal">修改状态</a></div>'
//										buttons.push({
//											text: '修改状态',
//											type: 'button',
//											class: 'item-update',
//											isRender: true
//										});
//									}
//								}else {
//									if(row.commandStatus!='交易成功'){
//										//star=star+'<div class="func-area"><a href="javascript:void(0)" class="item-update" data-toggle="modal">修改审核中</a></div>'
//										buttons.push({
//											text: '修改审核中',
//											type: 'button',
//											class: 'item-update',
//											isRender: true
//										});
//									}
//								}
//                  			return util.table.formatter.generateButton(buttons, 'settlement_page_payment');
//                  		}else if(row.type==='充值'&&row.commandStatus!='撤销'){
//                  			if(row.updateStatus===null||row.auditUpdateStatus!=null){
//									if(row.commandStatus!='交易成功'){
////										star=star+'<div class="func-area"><a href="javascript:void(0)" class="item-update"  id="item-update"   data-toggle="modal">修改状态</a></div>'
//										var buttons = [{
//											text: '修改状态',
//											type: 'button',
//											class: 'item-update',
//											isRender: true
//										}]
//									}
//								}else {
//									if(row.commandStatus!='交易成功'){
////										star=star+'<div class="func-area"><a href="javascript:void(0)" class="item-update" data-toggle="modal">修改审核中</a></div>'
//										var buttons = [{
//											text: '修改审核中',
//											type: 'button',
//											class: 'item-update',
//											isRender: true
//										}]
//									}
//								}
//                  			return util.table.formatter.generateButton(buttons, 'proAccTable');
//                  		}
//							
//                  	},
						 events: {
                            //单笔提现
                            'click .item-pay': function (e, val, row) {
                            	$('#pay_operatorReson').val('')
                            	$('#pay_tishi').text('');
                            	$$.confirm({
                                    container: $('#payment_pay_input'),
                                    trigger: this,
                                    accept: function () {
                                        http.post(settleConfig.api.settlement.settlement_payment_update, {
                                            data: {
                                            	oid:row.oid,
												operatorStatus:'01',
												operatorReson:$('#pay_operatorReson').val()
                                            },
                                            contentType: 'form'
                                        }, function (res) {
                                        	if(res.returnCode!='0000'){
                                          	   toastr.error(res.errorMessage);
                                          	}else{
                                          		toastr.success("操作成功！");
                                          	}
                                            $('#settlement_page_payment').bootstrapTable('refresh', pageOptions);
                                        })
                                    }
                                })
                            	$('#pay_m').removeAttr('disabled','disabled');
                            },
                            
                            //失败重发
                            'click .item-addOrder': function (e, val, row) {
                            	$('#fial_operatorReson').val('')
                            	$('#fial_tishi').text('');
                            	$$.confirm({
                                    container: $('#payment_fial_input'),
                                    trigger: this,
                                    accept: function () {
                                        http.post(settleConfig.api.settlement.settlement_payment_update, {
                                            data: {
												oid:row.oid,
                                    			operatorStatus:'02',
                                    			operatorReson:$('#fial_operatorReson').val()
                                            },
                                            contentType: 'form'
                                        }, function (res) {
                                        	if(res.returnCode!='0000'){
                                          	   toastr.error(res.errorMessage);
                                          	}else{
                                          		toastr.success("操作成功！");
                                          	}
                                            $('#settlement_page_payment').bootstrapTable('refresh', pageOptions);
                                        })
                                    }
                                })
                            	$('#fail_m').removeAttr('disabled','disabled');
                            },
                             
                             //撤回
                            'click .item-reset': function (e, val, row) {
                            	$('#reset_resetOpertatorReson').val('')
                            	$('#reset_tishi').text('');
                            	$$.confirm({
                                    container: $('#payment_reset_input'),
                                    trigger: this,
                                    accept: function () {
                                        http.post(settleConfig.api.settlement.settlement_payment_update, {
                                            data: {
												oid:row.oid,
												resetOperatorStatus:'03',
												resetOpertatorReson:$('#reset_resetOpertatorReson').val()
                                            },
                                            contentType: 'form'
                                        }, function (res) {
                                        	if(res.returnCode!='0000'){
                                          	   toastr.error(res.errorMessage);
                                          	}else{
                                          		toastr.success("操作成功！");
                                          	}
                                            $('#settlement_page_payment').bootstrapTable('refresh', pageOptions);
                                        })
                                    }
                                })
                            	$('#reset_m').removeAttr('disabled','disabled');
                            },
                            'click .item-update': function (e, val, row) {
                            	$('#payment_updateReson').val('')
                            	$('#update_tishi').text('');
                            	document.getElementById("payment_updateStatus").options.length=0;
                            	var selectObj=document.getElementById("payment_updateStatus");
                            	if(row.type==='充值'){
                            		if(row.commandStatus==="交易失败"){
                            			selectObj.options[selectObj.length] = new Option("交易成功",1);
                            		}else{
                            			selectObj.options[selectObj.length] = new Option("交易成功",1);
                            			selectObj.options[selectObj.length] = new Option("交易失败",2);
                            		}
                            	}else{
   									selectObj.options[selectObj.length] = new Option("交易成功",1);
                            	}
                             	$$.confirm({
                                    container: $('#payment_update_input'),
                                    trigger: this,
                                    accept: function () {
                                        http.post(settleConfig.api.settlement.settlement_payment_update, {
                                            data: {
												oid:row.oid,
                                    			updateReson:$('#payment_updateReson').val(),
                                    			updateStatus:$('#payment_updateStatus').val()
                                            },
                                            contentType: 'form'
                                        }, function (res) {
                                        	if(res.returnCode!='0000'){
                                         	   toastr.error(res.errorMessage);
                                         	}else{
                                         		toastr.success("操作成功！");
                                         	}
                                            $('#settlement_page_payment').bootstrapTable('refresh', pageOptions);
                                        })
                                    }
                                })
                             	$('#update_m').removeAttr('disabled','disabled');
                            }
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
