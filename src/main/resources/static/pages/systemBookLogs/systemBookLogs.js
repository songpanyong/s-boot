/**
 * 系统登账日志
 */
define([
'http',
'config',
'accountConfig',
'util',
'extension'
], function (http, config, accountConfig, util, $$) {
	return {
		name: 'systemBookLogs',
		init: function() {
			var pageOptions1 = {
				number: 1,
				size: 10,
				offset: 0,
				orderNo: '',
				orderType: '',
				startTime: '',
				endTime: ''
			}
			
			var pageOptions2 = {
				offset: 0,
				orderNo: ''
			}
			
			var tableConfig1 = {
				ajax: function (origin) {
					http.post(accountConfig.api.accountOrder.account_order_page, {
						data: {
							page: pageOptions1.number,
							rows: pageOptions1.size,
							orderNo: pageOptions1.orderNo,
							orderType: pageOptions1.orderType,
							startTime: pageOptions1.startTime,
							endTime: pageOptions1.endTime
						},
						contentType: 'form'
					}, function (rlt) {
						origin.success(rlt)
					})
				},
				pageNumber: pageOptions1.number,
				pageSize: pageOptions1.size,
				pagination: true,
				sidePagination: 'server',
				pageList: [10, 20, 30, 50, 100],
				queryParams: getQueryParams1,
				onClickCell: function (field, value, row, $element) {
					switch (field) {
						case 'orderNo':
							queryInfo(value, row)
							break
					}
				},
				columns: [
					{
						align: 'center',
						width: 30,
						formatter: function (val, row, index) {
							return index + 1 + pageOptions1.offset
						}
					},
					{
						field: 'orderNo',
						class: 'table_title_detail'
					},
					{
						field: 'orderType',
						formatter: function (val, row, index) {
							switch (val) {
								case 'investT0' : 
									return '快申';break
								case 'investT1' : 
									return '申购';break
								case 'redeemT0' : 
									return '快赎';break
								case 'redeemT1' : 
									return '赎回';break
								case 'conversionRedeem' : 
									return '转换-赎回';break
								case 'conversionInvest' : 
									return '转换-申购';break
								case 'repayCapitalWithInterest' : 
									return '还本付息';break
								case 'windUpRedeem' : 
									return '清盘赎回';break
								case 'dividend' : 
									return '现金分红';break
								case 'recharge' : 
									return '充值';break
								case 'withdraw' : 
									return '提现';break
								case 'useRedPacket' : 
									return '红包';break
								case 'rebate' : 
									return '返佣';break
								case 'reFund' : 
									return '退款';break
								case 'RaiseFailureReFund' : 
									return '募集失败退款';break
								case 'transfer' : 
									return '转账';break
								case 'netting' : 
									return '轧差';break
								default : return val;break
							}
						}
					},
					{
						field: 'balance',
						class: 'currency',
						align: 'right'
					},
					{
						field: 'orderStatus',
						formatter: function (val, row, index) {
							switch (val) {
								case '0' : 
									return '创建';break
								case '1' : 
									return '成功';break
								case '2' : 
									return '失败';break
								default : return val;break
							}
						}
					},
					{
						field: 'phone',
						align: 'right'
					},
					{
						field: 'systemSource'
					},
					{
						field: 'submitTime',
						align: 'right'
					},
					{
						field: 'remark'
					}
				]
			}
			
			var tableConfig2 = {
				ajax: function (origin) {
					http.post(accountConfig.api.accountOrder.account_event_trans_list, {
						data: {
							orderNo: pageOptions2.orderNo
						},
						contentType: 'form'
					}, function (rlt) {
						rlt.rows = rlt
						origin.success(rlt)
					})
				},
				pageNumber: 1,
				pageSize: 10,
				pagination: false,
				sidePagination: 'server',
				queryParams: getQueryParams2,
				columns: [
					{
						align: 'center',
						width: 30,
						formatter: function (val, row, index) {
							return index + 1
						}
					},
					{
						field: 'requestNo'
					},
					{
						field: 'transNo'
					},
					{
						field: 'eventChildOid'
					},
					{
						field: 'childEventName'
					},
					{
						field: 'balance',
						class: 'currency',
						align: 'right'
					},
					{
						field: 'status',
						formatter: function (val, row, index) {
							switch (val) {
								case '0' : 
									return '创建';break
								case '1' : 
									return '成功';break
								case '2' : 
									return '失败';break
								default : return val;break
							}
						}
					},
					{
						field: 'outputAccountNo'
					},
					{
						field: 'inputAccountNo'
					},
					{
						field: 'systemSource',
						formatter: function () {
							return 'mimosa'
						}
					},
					{
						field: 'createTime',
						align: 'right'
					},
					{
						field: 'remark'
					}
				]
			}
			
			$("#dataTable1").bootstrapTable(tableConfig1)
			
			$$.searchInit($('#searchForm1'), $('#dataTable1'))
			
			function getQueryParams1 (val) {
				var form = document.searchForm1
				pageOptions1.size = val.limit
				pageOptions1.number = parseInt(val.offset / val.limit) + 1
				pageOptions1.offset = val.offset
				pageOptions1.orderNo = form.orderNo.value
				pageOptions1.orderType = form.orderType.value
				pageOptions1.startTime = form.startTime.value
				pageOptions1.endTime = form.endTime.value
				return val
			}
			
			function getQueryParams2 (val) {
				pageOptions2.offset = val.offset
				return val
			}
			
			function queryInfo (value, row) {
				pageOptions2.orderNo = value
				$("#dataTable2").bootstrapTable('destroy')
				$("#dataTable2").bootstrapTable(tableConfig2)
				$('.detailDivs').hide()
				http.post(accountConfig.api.accountOrder.account_order_detail, {
					data: {
						orderNo: value
					},
					contentType: 'form'
				}, function(result){
					result.orderTypeDisp = function(){
						switch (result.orderType) {
							case 'investT0' : 
								return '快申';break
							case 'investT1' : 
								return '申购';break
							case 'redeemT0' : 
								return '快赎';break
							case 'redeemT1' : 
								return '赎回';break
							case 'conversionRedeem' : 
								return '转换-赎回';break
							case 'conversionInvest' : 
								return '转换-申购';break
							case 'repayCapitalWithInterest' : 
								return '还本付息';break
							case 'windUpRedeem' : 
								return '清盘赎回';break
							case 'dividend' : 
								return '现金分红';break
							case 'recharge' : 
								return '充值';break
							case 'withdraw' : 
								return '提现';break
							case 'useRedPacket' : 
								return '红包';break
							case 'rebate' : 
								return '返佣';break
							case 'reFund' : 
								return '退款';break
							case 'RaiseFailureReFund' : 
								return '募集失败退款';break
							case 'transfer' : 
								return '转账';break
							case 'netting' : 
								return '轧差';break
							default : return result.orderType;break
						}
					}
					result.orderStatusDisp = function(){
						switch (result.orderStatus) {
							case '0' : 
								return '创建';break
							case '1' : 
								return '成功';break
							case '2' : 
								return '失败';break
							default : return result.orderStatus;break
						}
					}
					result.userTypeDisp = function(){
						switch (result.userType) {
							case 'T1' : 
								return '投资人';break
							case 'T2' : 
								return '发行人';break
							case 'T3' : 
								return '平台';break
							default : return result.userType;break
						}
					}
					if(row.orderType == 'investT1'){
						result.balance2 = result.balance + result.voucher
						$('#balanceDiv1').hide()
						$('#balanceDiv2').show()
						$('#detailDiv1').show()
					}else if(row.orderType == 'investT0'){
						result.balance2 = result.balance + result.voucher
						$('#balanceDiv1').hide()
						$('#balanceDiv2').show()
						$('#detailDiv2').show()
					}else if(row.orderType == 'redeemT0' || row.orderType == 'redeemT1' || row.orderType == 'repayCapitalWithInterest' || row.orderType == 'windUpRedeem'){
						result.receivables = result.balance + result.rateBalance
						$('#balanceDiv1').show()
						$('#balanceDiv2').hide()
						$('#detailDiv3').show()
					}else if(row.orderType == 'conversionRedeem'){
						result.receivables = result.balance + result.rateBalance
						$('#balanceDiv1').show()
						$('#balanceDiv2').hide()
						$('#detailDiv4').show()
					}else if(row.orderType == 'conversionInvest'){
						result.balance2 = result.balance + result.voucher
						$('#balanceDiv1').hide()
						$('#balanceDiv2').show()
						$('#detailDiv5').show()
					}else if(row.orderType == 'RaiseFailureReFund'){
						result.receivables = result.balance
						$('#balanceDiv1').show()
						$('#balanceDiv2').hide()
						$('#detailDiv6').show()
					}else if(row.orderType == 'dividend'){
						$('#balanceDiv1').show()
						$('#balanceDiv2').hide()
						$('#detailDiv7').show()
					}else if(row.orderType == 'useRedPacket' || row.orderType == 'rebate' || row.orderType == 'recharge' || row.orderType == 'withdraw' || row.orderType == 'transfer' || row.orderType == 'netting'){
						$('#balanceDiv1').show()
						$('#balanceDiv2').hide()
						$('#detailDiv8').show()
					}else if(row.orderType == 'reFund'){
						result.receivables = result.balance
						$('#balanceDiv1').show()
						$('#balanceDiv2').hide()
						$('#detailDiv9').show()
					}
					$$.detailAutoFix($('#detailModal'), result)
					$('#detailModal').modal('show')
				})
			}
		}
	}
})