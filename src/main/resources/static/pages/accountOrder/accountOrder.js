/**
 * 账户订单
 */
define([
    'http',
    'config',
    'accountConfig',
    'util',
    'extension'
], function (http, config,accountConfig, util, $$) {
    return {
        name: 'accountOrder',
        init: function () {
            // 分页配置
            var pageOptions = {
                page: 1,
                rows: 10
            }

            // 初始化数据表格
            var tableConfig = {
				ajax: function(origin) {
					http.post(accountConfig.api.accountOrder.account_order_select_list, {
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
					// 手机号
					field: 'phone',
					align: 'right'
					
				},
				{ 
					width: 50,
					// 来源单据号
					field: 'orderNo',
					align: 'right'
					
				},
				{ 
					width: 50,
					// 来源系统类型
					field: 'systemSource'
					
				},
				{
					width: 50,
					// 业务类型
					field: 'orderType',
					formatter: function(val, row, index) {
						switch(val) {
							case '01':
								return '申购'
							case '02':
								return '赎回'
							case '07':
								return '可用金收款'
							case '08':
								return '可用金放款'
							case '57':
								return '发行人放款'
							case '58':
								return '发行人收款'
							case '50':
								return '充值'
							case '51':
								return '提现'
							case '54':
								return '冲正'
							case '55':
								return '冲负'
							case '56':
								return '红包'
							case '59':
								return '返佣冻结'
							case '60':
								return '返佣'
							case '61':
								return '返佣解冻'
							default:
								return '未知'
//							case '03':
//								return '派息'
//							case '04':
//								return '赠送体验金'
//							case '05':
//								return '体验金到期'
//							default:
//								return '增加发行额'
						}
					}
				},
				{
					width: 50,
					align: 'right',
					// 订单金额
					field: 'balance',
					
				},
				{
					width: 50,
					// 订单状态
					field: 'orderStatus',
					formatter: function(val) {
						var className = '';
						if(val == '1'){
								className = 'text-green';
                    			return '<span class="' + className + '">成功</span>';
                    		}else if(val == '2'){
                    			className = 'text-red';
                    			return '<span class="' + className + '">失败</span>';
                    		}else if(val == '0'){
                    			className = 'text-yellow';
                    			return '<span class="' + className + '">未处理</span>';
                    		}else{
                    			className = 'text-blue';
                    			return '<span class="' + className + '">撤单</span>';
                    		}
					}
				},
//				{
//					width: 50,
//					// 关联产品
//					field: 'relationProductName'
//				},
				{
					width: 50,
					// 订单时间
					field: 'submitTime',
					align: 'right'
				},
				{
					width:50,
					//订单描述
					field:'orderDesc'
				}
				]
			}

            // 初始化数据表格
            $('#account_order_Table').bootstrapTable(tableConfig);
            // 搜索表单初始化
            $$.searchInit($('#searchForm'), $('#account_order_Table'));

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
           
           	document.onkeydown=keyDownSearch;  
      
			function keyDownSearch(e) { 
			    // 兼容FF和IE和Opera    
			    var theEvent = e || window.event;    
			    var code = theEvent.keyCode || theEvent.which || theEvent.charCode;    
			    if (code == 13) { 
			    	var sform = document.searchForm
	                var data = util.form.serializeJson(sform)
	                data.row = 10
	                data.page = 1
	                pageOptions = data
	                var beginTime = data.startTime;
	                var endTime = data.endTime;
	                var beginDate = new Date(beginTime.replace(/\-/g, "\/"));  
	 				var endDate = new Date(endTime.replace(/\-/g, "\/")); 
	                if(beginTime != "" && endTime !="" && beginDate>endDate){
	                	toastr.error('下单时间区间不能小于0!', '错误信息', {timeOut: 3000})
	                	return;
	                }
	                var minMoney = data.sBalance.trim();
	                var maxAmount = data.eBalance.trim();
	                if(minMoney != "" && maxAmount !="" && parseInt(minMoney)>parseInt(maxAmount)){
	                	toastr.error('金额范围结束值不能小于开始值!', '错误信息', {timeOut: 3000})
	                	return;
	                }
	                $('#account_order_Table').bootstrapTable('refresh', pageOptions);
			    }
			    return true; 
			}
			
			$('#sBalance,#eBalance').on('keyup', function(e) {
            	e.preventDefault()
            	var sBalance=$('#sBalance').val(),eBalance=$('#eBalance').val();
            	if(((sBalance!=""&&sBalance!=null)||(eBalance!=""&&eBalance!=null))&&(!/^\d{0,8}(\.[0-9]{0,2})?$/.test(this.value))){
            		toastr.error('只能输入数字，小数点后只能保留两位，最大8位整数');
            		this.value='';
            	}
			})
           
            //搜索
            $('#order_search').on('click', function (e) {
                e.preventDefault()
                var sform = document.searchForm
                var data = util.form.serializeJson(sform)
                data.row = 10
                data.page = 1
                pageOptions = data
                var beginTime = data.startTime;
                var endTime = data.endTime;
                var beginDate = new Date(beginTime.replace(/\-/g, "\/"));  
 				var endDate = new Date(endTime.replace(/\-/g, "\/")); 
                if(beginTime != "" && endTime !="" && beginDate>endDate){
                	toastr.error('下单时间区间不能小于0!', '错误信息', {timeOut: 3000})
                	return;
                }
                var minMoney = data.sBalance.trim();
                var maxAmount = data.eBalance.trim();
                if(minMoney != "" && maxAmount !="" && parseInt(minMoney)>parseInt(maxAmount)){
                	toastr.error('金额范围结束值不能小于开始值!', '错误信息', {timeOut: 3000})
                	return;
                }
                $('#account_order_Table').bootstrapTable('refresh', pageOptions);
            })

            //清空
            $('#order_reset').on('click', function (e) {
                e.preventDefault()
                var sform = document.searchForm
                util.form.reset($(sform))
                $('#account_order_Table').bootstrapTable('refresh', pageOptions);
            })

        }
    }
})
