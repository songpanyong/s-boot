/**
 * 指令管理
 */
define(
		[ 'http',
		'payConfig', 'settleConfig', 'util', 'extension' ],
		function(http, payConfig, settleConfig, util, $$) {
			return {
				name : 'bathPaymentSettlement',
				init : function() {
					// 分页配置
					var pageOptions = {
						page : 1,
						row : 10
					}
					// 初始化数据表格
					var tableConfig = {

						ajax : function(origin) {
							http.post(settleConfig.api.settlement.settlement_payment_pageBath,
								{
									data : pageOptions,
									contentType : 'form'
								},
								function(rlt) {
								rlt.rows.forEach(function(item) {
								    if (item.commandStatus === '1') {
										item.failDetail = '';
									}
									item.type = item.type === '01' ? '申购':'赎回';
									if (item.commandStatus != null) {
										item.commandStatus = item.commandStatus === '0' ? '未处理'
											: item.commandStatus === '1' ? '交易成功'
											: item.commandStatus === '2' ? '交易失败'
											: item.commandStatus === '3' ? '交易处理中'
											: item.commandStatus === '4' ? '超时'
																										: '撤销';
									}
									if (item.channelNo != null) {
										item.channelNo = loadChannelName(item.channelNo);
									}
									if (item.updateStatus != null) {
										item.updateStatus = item.updateStatus === '0' ? '未处理'
											: item.updateStatus === '1' ? '交易成功'
											: item.updateStatus === '2' ? '交易失败'
											: item.updateStatus === '3' ? '交易处理中'
											: '超时';
									}
									if (item.auditUpdateStatus != null) {
										item.auditUpdateStatus = item.auditUpdateStatus === '0' ? '审核不通过'
											: '审核通过';
									 }
								})
									origin.success(rlt)
							})
						},
						pageNumber : pageOptions.page,
						pageSize : pageOptions.row,
						pagination : true,
						sidePagination : 'server',
						pageList : [ 10, 20, 30, 50, 100 ],
						queryParams : getQueryParams,
						onLoadSuccess : function() {
						},
						columns : [
						       {
						        	 width: 100,
						        	 checkbox: true,
						        	 formatter: function(val,row){
						        	   	return "<input type='text'  class='oid' hidden='hidden' value='"+row.oid+"' >"
						        	  }
						        }, 
								{
									width : 100,
									align : 'center',
									formatter : function(val, row, index) {
										return '<div class="func-area"><a href="javascript:void(0)" class="item-detail_payment"  data-toggle="modal">'
												+ row.orderNo + '</a></div>'
								    }
									
								},
								{
									width : 100,
									align : 'center',
									field : 'payNo'
								},
								{
									width : 100,
									align : 'center',
									field : 'realName'
								},
								{
									width : 100,
									width : 100,
									align : 'center',
									formatter : function(val, row, index) {
										return row.type === '申购' ? (row.amount + '元')
												: (row.amount + '元');
									}
								},
								{
									width : 100,
									align : 'center',
									field : 'type'
								},
								{
									width : 100,
									align : 'center',
									field : 'commandStatus'
								},
								{
									width : 100,
									align : 'center',
									field : 'channelNo'
								},
								{
									width : 100,
									align : 'center',
									field : 'failDetail'
								},
								{
									width : 100,
									align : 'center',
									formatter : function(val, row, index) {
										if (row.updateStatus === null
												&& row.auditUpdateStatus === null) {
											return '-';
										} else if (row.updateStatus != null
												&& row.auditUpdateStatus === null) {
											return row.updateStatus + '|-';
										} else if (row.updateStatus === null
												&& row.auditUpdateStatus != null) {
											return '-|' + row.auditUpdateStatus;
										} else {
											return row.updateStatus + '|'
													+ row.auditUpdateStatus;
										}
									}
								},
								{
									width : 100,
									align : 'center',
									formatter : function(val, row, index) {
										if (row.updateReson === null
												&& row.auditUpdateReson === null) {
											return '-';
										} else if (row.updateReson != null
												&& row.auditUpdateReson === null) {
											return row.updateReson + '|-';
										} else if (row.updateReson === null
												&& row.auditUpdateReson != null) {
											return '-|' + row.auditUpdateReson;
										} else {
											return row.updateReson + '|'
													+ row.auditUpdateReson;
										}
									}
								},
								{
									width : 100,
									align : 'center',
									formatter : function(val, row, index) {
										return row.auditOperatorStatus != null ? row.auditOperatorStatus === '0' ? '审核不通过'
												: '审核通过'
												: '-';
									}
								},
								{
									width : 100,
									align : 'center',
									formatter : function(val, row, index) {
										if (row.operatorReson === null
												&& row.auditOperatorReson === null) {
											return '-';
										} else if (row.operatorReson != null
												&& row.auditOperatorReson === null) {
											return row.operatorReson + '|-';
										} else if (row.operatorReson === null
												&& row.auditOperatorReson != null) {
											return '-|'
													+ row.auditOperatorReson;
										} else {
											return row.operatorReson + '|'
													+ row.auditOperatorReson;
										}
									}
								},
								{
									width : 100,
									align : 'center',
									formatter : function(val, row, index) {
										return row.auditResetOperatorStatus != null ? row.auditResetOperatorStatus === '0' ? '审核不通过'
												: '审核通过'
												: '-';
									}
								},
								{
									width : 100,
									align : 'center',
									formatter : function(val, row, index) {
										if (row.resetOpertatorReson === null
												&& row.auditResetOperatorReson === null) {
											return '-';
										} else if (row.resetOpertatorReson != null
												&& row.auditResetOperatorReson === null) {
											return row.resetOpertatorReson
													+ '|-';
										} else if (row.resetOpertatorReson === null
												&& row.auditResetOperatorReson != null) {
											return '-|'
													+ row.auditResetOperatorReson;
										} else {
											return row.resetOpertatorReson
													+ '|'
													+ row.auditResetOperatorReson;
										}
									}
								},
								{
									width : 100,
									align : 'center',
									field : 'updateTime'
								},
								{
			                    	width: 100, 
			                    	align: 'center',
			                    	 title: '展示可操作',
			                    	formatter: function(val, row, index) {
			                    		var star='';
			                    		   if(row.operatorStatus===null||row.auditOperatorStatus!=null){
			                      				if(row.commandStatus==='未处理'){
			                    					//单笔赎回
			                    					star+='<div class="func-area"><a href="javascript:void(0)" class="item-pay"  id="item-pay" disabled  data-toggle="modal">单笔支付</a></div>'
			                      				}else if(row.commandStatus!='交易成功'){
			                    					//|补单
			                    					star+='<div class="func-area"><a href="javascript:void(0)" class="item-addOrder"  id="item-addOrder" disabled  data-toggle="modal">失败重发</a></div>'
			                      				}
			                    				
			                    			}
			                    			if(row.resetOperatorStatus===null||row.auditResetOperatorStatus!=null){
			                      				if(row.commandStatus!='交易成功'){
			                    					star+='<div class="func-area"><a href="javascript:void(0)" class="item-reset"  id="item-reset" disabled data-toggle="modal">撤回</a></div>'
			                      				}
			                    			}
												
											if(row.updateStatus===null||row.auditUpdateStatus!=null){
												if(row.commandStatus!='交易成功'){
													star=star+'<div class="func-area"><a href="javascript:void(0)" class="item-update"  id="item-update" disabled  data-toggle="modal">修改状态</a></div>'
												}
											}
			                    			return star
			                    	}
									 
			                    }
							]
					}

					// 初始化数据表格
					$('#settlement_page_payment_bath').bootstrapTable(tableConfig);
					// 搜索表单初始化
					$$.searchInit($('#settlement_search_payment_bath'),$('#settlement_page_payment_bath'));
					// 渠道初始化
					loadChannel();
					
					// 组条件查询
					function getQueryParams(val) {
						var form = document.settlement_search_payment_bath
						$.extend(pageOptions, util.form.serializeJson(form)); // 合并对象，修改第一个对象
						pageOptions.rows = val.limit
						pageOptions.page = parseInt(val.offset / val.limit) + 1
						return val
					}
					
				
					$('#payment_bathOperatorTag_bath').on('change',function(e) {
						e.preventDefault()
						var type=$(this).children('option:selected').val();
						if(type==='04'){
							$('#update_bath_show').show();
						}else{
							$('#update_bath_show').hide();
						}
						
					})
		
					//提交一页
					$('#submit_bath').on('click', function(e) {
						e.preventDefault()
						var sform = document.updateModify_bath
						util.form.reset($(sform))
						$('#update_m_bath').prop('disabled', false);
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
						
						
						$('#settlement_oidBath').val(data);
						$('#payment_update_input_bath').modal('show');
					})
									
					
					//提交
					$('#update_m_bath').on('click', function(e) {
						$('#update_m_bath').prop('disabled', true);
						var oidBth =$('#settlement_oidBath').val();
						console.log('data:'+JSON.stringify(oidBth));
						var type=$('#payment_bathOperatorTag_bath').val(),updates='';
						if(type==='04'){
							updates=$('#payment_updateStatus_bath').val();
						}
						http.post(settleConfig.api.settlement.settlement_payment_updateBath,{
							data :{
								bathOid :oidBth,
								updateStatus :updates,
								bathOperatorTag:type,
								bathReson :$('#payment_updateReson_bath').val()
							},
							contentType : 'form'
						},
						function(res) {
							if(res.returnCode!='0000'){
								toastr.error(res.errorMessage, '提示信息', {
									timeOut: 3000
								})
                           	}else{
                           		toastr.success("操作成功！", '提示信息', {
									timeOut: 3000
								})
//                           		toastr.success("操作成功！");
                           	}
							$('#payment_update_input_bath').modal('hide');
							$('#settlement_page_payment_bath').bootstrapTable('refresh', pageOptions);
						 })
						
					})
				
					//提交全部
					$('#submit_bath_all').on('click', function(e) {
						e.preventDefault()
						var sform = document.updateModify_bath_all
						util.form.reset($(sform))
						$('#update_m_bath_all').prop('disabled', false);
						$('#payment_update_input_all_bath').modal('show');
					})

					//全部-提交
					$('#update_m_bath_all').on('click', function(e) {
						e.preventDefault();
						var sform = document.settlement_search_payment_bath;
						var data = util.form.serializeJson(sform);
						
						$('#update_m_bath_all').prop('disabled', true);
						var type=$('#payment_bathOperatorTag_all_bath').val(),updates='';
						if(type==='04'){
							updates=$('#payment_updateStatus_all_bath').val();
						}
						
						data.row = 10
						data.page = 1
						data.updateStatus=updates
						data.bathOperatorTag=type
						data.bathReson=$('#payment_updateReson_bath_all').val()
						data.sumbitTy='0'
						http.post(settleConfig.api.settlement.settlement_payment_updateBath,{
							data :data,
//							{
//								updateStatus :updates,
//								bathOperatorTag:type,
//								bathReson :$('#payment_updateReson_all_bath').val()
//							},
							contentType : 'form'
						},
						function(res) {
							if(res.returnCode!='0000'){
								toastr.error(res.errorMessage, '提示信息', {
									timeOut: 3000
								})
                           	}else{
                           		toastr.success("操作成功！", '提示信息', {
									timeOut: 3000
								})
//                           		toastr.success("操作成功！");
                           	}
							$('#payment_update_input_all_bath').modal('hide');
							$('#settlement_page_payment_bath').bootstrapTable('refresh', pageOptions);
						 })
						
					})
					
					// 搜索
					$('#payment_search_bath').on('click',function(e) {
						e.preventDefault()
						var sform = document.settlement_search_payment_bath
						var data = util.form.serializeJson(sform)
						data.row = 10
						data.page = 1
						pageOptions = data
						$('#settlement_page_payment_bath').bootstrapTable('refresh', pageOptions);
					})

					// 清空
					$('#payment_reset_bath').on('click',function(e) {
						e.preventDefault()
						var sform = document.settlement_search_payment_bath
						util.form.reset($(sform))
						$('#settlement_page_payment_bath').bootstrapTable('refresh', pageOptions);
					})


					$('#payment_updateReson_bath').on('blur keyup', function() {
						var len = $(this).val().length;
						if (len > 49) {

							$(this).val($(this).val().substring(0, 50));
						}
						if (len > 50) {
							$('#update_tishi_bath').text('您还可以输入0个字符');
						} else {
							var num = 50 - len;
							$('#update_tishi_bath').text('您还可以输入' + num + '字符');
						}
					})
					
					
					$('#payment_updateReson_bath_all').on('blur keyup', function() {
						var len = $(this).val().length;
						if (len > 49) {

							$(this).val($(this).val().substring(0, 50));
						}
						if (len > 50) {
							$('#update_tishi_bath_all').text('您还可以输入0个字符');
						} else {
							var num = 50 - len;
							$('#update_tishi_bath_all').text('您还可以输入' + num + '字符');
						}
					})


					// 加载渠道所有渠道
					function loadChannel() {
						http.post(settleConfig.api.settlement.settlement_getChannelPage,
							{
								data : pageOptions,
								contentType : 'form'
							},
							function(data) {
								data.rows.forEach(function(item) {
									$("#ul_Channel_Name").append("<li id=channel_Name"
										+ item.channelNo+ ">"
										+ item.channelName+ "</option>");
									})
							})
					}

					// 加载渠道名称
					function loadChannelName(channelId) {
						return $("#channel_Name" + channelId).html();
					}
				}
			}
		})
