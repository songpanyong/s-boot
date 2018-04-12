/**
 * 账户交易流水
 */
define([
    'http',
    'config',
    'accountConfig',
    'util',
    'extension'
], function (http, config,accountConfig, util, $$) {
    return {
        name: 'accountTrans',
        init: function () {
            // 分页配置
            var pageOptions = {
                page: 1,
                rows: 10
            }

            // 初始化数据表格
            var tableConfig = {
				ajax: function(origin) {
					http.post(accountConfig.api.accountTrans.account_trans_select_list, {
						data: pageOptions,
						contentType: 'form'
					}, function(rlt) {
						origin.success(rlt)
					})
				},
				pageNumber: pageOptions.page,
				pageSize: pageOptions.rows,
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
						return index + 1
					}
				},
				{ 
					width: 50,
					// 用户编码
					field: 'phone'
					
				},
				{
					width: 50,
					//用户类型
					field:'userType',
					formatter:function (value) {
						if(value == 'T1'){
                    			return '投资人';
                    	}else if(value == 'T2'){
                    			return '发行人';
                    	}else if(value == 'T3'){
                    		return '平台';
                    	}
					}
				},
				{ 
					width: 50,
					// 账户编码
					field: 'accountOid'
					
				},
				{
					width: 50,
					// 账户编码
					field: 'accountType',
					formatter:function (value) {
						if(value == '01'){
                    			return '活期户';
                    	}else if(value == '02'){
                    			return '活期利息户';
                    	}else if(value == '03'){
                    			return '体验金户';
                    	}else if(value == '04'){
                    			return '在途户';
                    	}else if(value == '05'){
                    			return '提现冻结户';
                    	}else if(value == '06'){
                    			return '定期户';
                    	}else if(value == '07'){
                    			return '产品户';
                    	}else if(value == '08'){
                    			return '备付金户';
                    	}else if(value == '09'){
                    			return '超级户';
                    	}else if(value == '10'){
                    			return '基本户';
                    	}else if(value == '11'){
                    			return '运营户';
                    	}else if(value == '12'){
                    			return '归集清算户';
                    	}else if(value == '13'){
                    			return '可用金户';
                    	}else if(value == '14'){
                    			return '充值冻结户';
                    	}else if(value == '15'){
                    			return '冻结资金户';
                    	}else if(value == '16'){
                    			return '定期利息户';
                    	}else if(value == '17'){
                    			return '续投冻结户';
                    	}
					}
				},
				{ 
					width: 50,
					// 账户编码
					field: 'orderNo'
					
				},
				{
					width:50,
					//交易类型
					field:'orderType'
//					formatter:function (value) {
//                  		if(value == '01'){
//                  			return '申购';
//                  		}else if(value == '02'){
//                  			return '赎回';
//                  		}else if(value == '50'){
//                  			return '充值';
//                  		}else if(value == '51'){
//                  			return '提现';
//                  		}else if(value == '03'){
//                  			return '派息';
//                  		}else if(value == '04'){
//                  			return '赠送体验金';
//                  		}else if(value == '05'){
//                  			return '体验金到期';
//                  		}else if(value == '06'){
//                  			return '增加发行额';
//                  		}else if(value == '07'){
//                  			return '可用金收款';
//                  		}else if(value == '08'){
//                  			return '可用金放款';
//                  		}else if(value == '54'){
//                  			return '冲正';
//                  		}else if(value == '55'){
//                  			return '冲负';
//                  		}else if(value == '56'){
//                  			return '红包';
//                  		}else if(value == '57'){
//                  			return '发行人放款';
//                  		}else if(value == '58'){
//                  			return '发行人收款';
//                  		}else if(value == '59'){
//                  			return '返佣冻结';
//                  		}else if(value == '60'){
//                  			return '返佣';
//                  		}else if(value == '61'){
//                  			return '返佣解冻';
//                  		}else if(value == '62'){
//                  			return '调额';
//                  		}
//					}
				},
				{
					width: 50,
					// 交易金额
					//field: 'orderBalance',
					align: 'right',
					formatter: function(val, row, index) {
						if(row.direction=='01'){
							return '+'+row.orderBalance;
						}
                    	if(row.direction=='02'){
                    		return '-'+row.orderBalance;
                    	}	
					}
				},
				{
					width: 50,
					// 交易后余额
					field: 'balance',
					align: 'right'
				},
				
				{
					width: 50,
					// 交易时间
					field: 'updateTime',
					align: 'right'
				}
				]
			}

            // 初始化数据表格
            $('#account_trans_Table').bootstrapTable(tableConfig);
            // 搜索表单初始化
            $$.searchInit($('#searchForm'), $('#account_trans_Table'));

            //搜索
           function getQueryParams(val) {
				var form = document.searchForm
				$.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
				pageOptions.rows = val.limit
				pageOptions.page = parseInt(val.offset / val.limit) + 1
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

            //清空
            $('#clear').on('click', function (e) {
                e.preventDefault()
                var sform = document.searchForm
                util.form.reset($(sform))
                $('#account_trans_Table').bootstrapTable('refresh', pageOptions);
            })

        }
    }
})
