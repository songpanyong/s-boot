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
        name: 'accountSign',
        init: function () {
            // 分页配置
            var pageOptions = {
                page: 1,
                rows: 10
            }

            // 初始化数据表格
            var tableConfig = {
				ajax: function(origin) {
					http.post(accountConfig.api.accountSign.account_sign_select_list, {
						data: pageOptions,
						contentType: 'form'
					}, function(rlt) {
						rlt.rows.forEach(function (item) {
                            if(item.status!=null){
                            	item.status=item.status==='1'?'已绑卡':'已解绑';
                            }
                        })
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
					width: 100,
					// 手机号
					field: 'phone'
					
				}, 
				{ 
					width: 50,
					// 真实姓名
					field: 'accountName'
					
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
					field:'bankName'
				},
				{
					width: 50,
					field:'certificates',
					
					formatter: function(val, row, index) {
						if(row.identityNo.length == 0){
							return '-';
						}else{
							if(row.certificates == '1'){
								return '身份证';
							}else{
								return'其他';
							}
						}
					}
				},
				{ 
					width: 50,
					// 身份证号
					field: 'identityNo',
					align: 'right'
					
				},
				{
					width:50,
					field: 'realName'
				},
				{
					width:50,
					field: 'reservedCellPhone'
				},
				{
					width: 50,
					// 银行卡号
					field: 'bankCard',
					align: 'right'
				},
				{
					width:50,
					field: 'cardType'
				},
				{
					width: 50,
					// 绑卡状态
					field: 'status'
				},
				
				
				{
					width: 50,
					// 创建时间
					field: 'createTime',
					align: 'right'
				}
				]
			}

            // 初始化数据表格
            $('#account_sign_Table').bootstrapTable(tableConfig);
            // 搜索表单初始化
            $$.searchInit($('#searchForm'), $('#account_sign_Table'));

            //搜索
           function getQueryParams(val) {
				var form = document.searchForm
				$.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
			
				pageOptions.rows = val.limit
				pageOptions.page = parseInt(val.offset / val.limit) + 1
				
				return val
			}

            //清空
            $('#clear').on('click', function (e) {
                e.preventDefault()
                var sform = document.searchForm
                util.form.reset($(sform))
                $('#account_sign_Table').bootstrapTable('refresh', pageOptions);
            })
            
            
            
            $('#account_elem_status').on('change', function (e) {
            	e.preventDefault()
            	$(this).children('option:selected').val(); 
                var sform = document.searchForm
                var data = util.form.serializeJson(sform)
                data.row = 10
                data.page = 1
                pageOptions = data
                $('#account_sign_Table').bootstrapTable('refresh', pageOptions);
            })

        }
    }
})
