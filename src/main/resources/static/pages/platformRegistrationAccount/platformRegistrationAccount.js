/**
 * 平台登账设置
 */
define([
	'http',
	'config',
	'accountConfig',
	'util',
	'extension'
], function (http, config, accountConfig, util, $$) {
	return {
		name: 'platformRegistrationAccount',
		init:function(){
			//用户信息
			var applicantId = ''
			var applicantName = ''
			
			//存储设置选中数据
			var selectRow = {}
			
			//获取登录用户信息
			http.post(config.api.userInfo, function(result){
				applicantId = result.oid
				applicantName = result.name
			})
			
			var pageOptions = {
				number: 1,
				size: 10,
				offset: 0,
				userOid: '',
				eventName: '',
				setUpStatus: ''
			}
			//平台登账账户下拉列表
			http.post(accountConfig.api.platform.platfrom_info_list, {
				contentType: 'form'
			}, function (enums) {
				var platformRegistrationAccountOptions = ''
				var userOid = ''
				var select = document.getElementById("platformRegistrationAccount")
				enums.forEach(function(item) {
					platformRegistrationAccountOptions += '<option value="' + item.userOid + '">' + item.platformName + '</option>'
					if(userOid==='') {
						userOid = item.userOid
					}
				})
				$(select).html(platformRegistrationAccountOptions)
				
				if(pageOptions.userOid!=='') {
					select.value = pageOptions.userOid
				} else {
					pageOptions.userOid = userOid
				}
				
				$('#dataTable').bootstrapTable(tableConfig)
			})
			
			$('#platformRegistrationAccount').on('change', function(){
				pageOptions.userOid = this.value
				$('#dataTable').bootstrapTable('refresh')
			})
			
			var tableConfig = {
				ajax: function (origin) {
					http.post(accountConfig.api.platform.event_page, {
						data: {
							page: pageOptions.number,
							rows: pageOptions.size,
							userOid: pageOptions.userOid,
							eventName: pageOptions.eventName,
							setUpStatus: pageOptions.setUpStatus,
							eventType: pageOptions.eventType
						},
						contentType: 'form'
					}, function (rlt) {
						origin.success(rlt)
					})
				},
				pageNumber: pageOptions.number,
				pageSize: pageOptions.size,
				pagination: true,
				sidePagination: 'server',
				pageList: [10, 20, 30, 50, 100],
				queryParams: getQueryParams,
				columns: [
					{
						halign: 'center',
						align: 'center',
						width: 30,
						formatter: function (val, row, index) {
							return index + 1 + pageOptions.offset
						}
					},
					{
						field: 'eventName'
					},
					{
						field: 'childEvents',
						formatter: function (val, row, index) {
							if(val.length > 1){
								if(val[0]['inputAccountName'] === val[1]['inputAccountName']){
									return val[0]['inputAccountName']
								}else{
									return val[0]['childEventName'] + '：' + val[0]['inputAccountName'] + '</br>' + val[1]['childEventName'] + '：' + val[1]['inputAccountName']
								}
							}else{
								return val[0]['inputAccountName']
							}
						}
					},
					{
						field: 'childEvents',
						formatter: function (val, row, index) {
							if(val.length > 1){
								if(val[0]['outputAccountName'] === val[1]['outputAccountName']){
									return val[0]['outputAccountName']
								}else{
									return val[0]['childEventName'] + '：' + val[0]['outputAccountName'] + '</br>' + val[1]['childEventName'] + '：' + val[1]['outputAccountName']
								}
							}else{
								return val[0]['outputAccountName']
							}
						}
					},
					{
						field: 'setUpStatus',
						align: 'center',
						formatter: function (val, row, index) {
							switch (val) {
								case "0" : return "已生效"; break
								case "1" : return "生效中"; break
								case "2" : return "待审核"; break
								default : return "--"; break
							}
						}
					},
					{
						align: 'center',
						formatter: function (val, row, index) {
							var buttons = [
								{
									text: '设置',
									type: 'button',
									class: 'item-cancel',
									isRender: row.canBeSetUp === 'Y' && (row.setUpStatus === '1' || row.setUpStatus === '2')
								},{
									text: '设置',
									type: 'button',
									class: 'item-submit',
									isRender: row.canBeSetUp === 'Y' && row.setUpStatus === '0'
								},{
									text: '详情',
									type: 'button',
									class: 'item-detail',
									isRender: row.canBeSetUp === 'N'
								}
							]
							return util.table.formatter.generateButton(buttons, 'dataTable');
						},
						events: {
							'click .item-cancel': function (e, value, row) {
								selectRow = row
								http.post(accountConfig.api.platform.event_effect_info, {
									data: {
										oid: row.oid
									},
									contentType: 'form'
								}, function(result){
									var detailObj = {}
									detailObj.eventName = row.eventName
									detailObj.eventType = function(){
										switch (row.eventType) {
											case "01" : return "充值"; break
											case "02" : return "提现"; break
											case "03" : return "转账"; break
											default : return "--"; break
										}
									}
									if(row.childEvents.length > 1){
										if(row.childEvents[0]['inputAccountName'] === row.childEvents[1]['inputAccountName']){
											$("#cancelInputAccountNameDiv1").show()
											$("#cancelInputAccountNameDiv2").hide()
											detailObj.inputAccountName = row.childEvents[0]['inputAccountName']
										}else{
											$("#cancelInputAccountNameDiv1").hide()
											$("#cancelInputAccountNameDiv2").show()
											if(row.childEvents[0]['childEventName'] === 'T+0'){
												detailObj.T0InputAccountName = row.childEvents[0]['inputAccountName']
												detailObj.T1InputAccountName = row.childEvents[1]['inputAccountName']
											}else{
												detailObj.T0InputAccountName = row.childEvents[1]['inputAccountName']
												detailObj.T1InputAccountName = row.childEvents[0]['inputAccountName']
											}
										}
									}else{
										$("#cancelInputAccountNameDiv1").show()
										$("#cancelInputAccountNameDiv2").hide()
										detailObj.inputAccountName = row.childEvents[0]['inputAccountName']
									}
									detailObj.outputAccountName = row.childEvents[0]['outputAccountName']
									detailObj.effectiveTimeType = function(){
										switch (result.effectiveTimeType) {
											case "01" : return "即时生效"; break
											case "02" : return "次日生效"; break
											case "03" : return "次月生效"; break
											default : return "--"; break
										}
									}
									detailObj.setUpStatus = function(){
										switch (result.setUpStatus) {
											case "0" : return "已生效"; break
											case "1" : return "生效中"; break
											case "2" : return "待审核"; break
											default : return "--"; break
										}
									}
									detailObj.setUpTime = result.setUpTime
									$$.detailAutoFix($('#cancelModal'), detailObj)
									
									$("#cancelModal").modal("show")
								})
							},
							'click .item-submit': function (e, value, row) {
								selectRow = row
								http.post(accountConfig.api.platform.platfrom_account_info, {
									data: {
										userOid: pageOptions.userOid,
										accountType: "08",
										accountStatus: "1"
									},
									contentType: 'form'
								}, function(result){
									var outputAccountOptions = ''
									result.forEach(function(item){
										if(item.accountName == row.childEvents[0].outputAccountName){
											outputAccountOptions += '<option value="' + item.accountNo + '" selected>' + item.accountName + '</option>'
										}else{
											outputAccountOptions += '<option value="' + item.accountNo + '">' + item.accountName + '</option>'
										}
									})
									$("#outputAccount").html(outputAccountOptions)
									
									var detailObj = {}
									detailObj.eventName = row.eventName
									detailObj.eventType = function(){
										switch (row.eventType) {
											case "01" : return "充值"; break
											case "02" : return "提现"; break
											case "03" : return "转账"; break
											default : return "--"; break
										}
									}
									if(row.childEvents.length > 1){
										if(row.childEvents[0]['inputAccountName'] === row.childEvents[1]['inputAccountName']){
											$("#submitInputAccountNameDiv1").show()
											$("#submitInputAccountNameDiv2").hide()
											detailObj.inputAccountName = row.childEvents[0]['inputAccountName']
										}else{
											$("#submitInputAccountNameDiv1").hide()
											$("#submitInputAccountNameDiv2").show()
											if(row.childEvents[0]['childEventName'] === 'T+0'){
												detailObj.T0InputAccountName = row.childEvents[0]['inputAccountName']
												detailObj.T1InputAccountName = row.childEvents[1]['inputAccountName']
											}else{
												detailObj.T0InputAccountName = row.childEvents[1]['inputAccountName']
												detailObj.T1InputAccountName = row.childEvents[0]['inputAccountName']
											}
										}
									}else{
										$("#submitInputAccountNameDiv1").show()
										$("#submitInputAccountNameDiv2").hide()
										detailObj.inputAccountName = row.childEvents[0]['inputAccountName']
									}
									$$.detailAutoFix($('#submitModal'), detailObj)
									$("#effectiveTimeType").attr("disabled","disabled")
									$("#submitSet").attr("disabled","disabled")
									$("#submitModal").modal("show")
								})
							},
							'click .item-detail': function (e, value, row) {
								var detailObj = {}
								detailObj.eventName = row.eventName
								detailObj.eventType = function(){
									switch (row.eventType) {
										case "01" : return "充值"; break
										case "02" : return "提现"; break
										case "03" : return "转账"; break
										default : return "--"; break
									}
								}
								if(row.childEvents.length > 1){
									if(row.childEvents[0]['inputAccountName'] === row.childEvents[1]['inputAccountName']){
										$("#inputAccountNameDiv1").show()
										$("#inputAccountNameDiv2").hide()
										detailObj.inputAccountName = row.childEvents[0]['inputAccountName']
									}else{
										$("#inputAccountNameDiv1").hide()
										$("#inputAccountNameDiv2").show()
										if(row.childEvents[0]['childEventName'] === 'T+0'){
											detailObj.T0InputAccountName = row.childEvents[0]['inputAccountName']
											detailObj.T1InputAccountName = row.childEvents[1]['inputAccountName']
										}else{
											detailObj.T0InputAccountName = row.childEvents[1]['inputAccountName']
											detailObj.T1InputAccountName = row.childEvents[0]['inputAccountName']
										}
									}
									if(row.childEvents[0]['outputAccountName'] === row.childEvents[1]['outputAccountName']){
										$("#outputAccountNameDiv1").show()
										$("#outputAccountNameDiv2").hide()
										detailObj.outputAccountName = row.childEvents[0]['outputAccountName']
									}else{
										$("#outputAccountNameDiv1").hide()
										$("#outputAccountNameDiv2").show()
										if(row.childEvents[0]['childEventName'] === 'T+0'){
											detailObj.T0OutputAccountName = row.childEvents[0]['outputAccountName']
											detailObj.T1OutputAccountName = row.childEvents[1]['outputAccountName']
										}else{
											detailObj.T0OutputAccountName = row.childEvents[1]['outputAccountName']
											detailObj.T1OutputAccountName = row.childEvents[0]['outputAccountName']
										}
									}
								}else{
									$("#inputAccountNameDiv1").show()
									$("#inputAccountNameDiv2").hide()
									$("#outputAccountNameDiv1").show()
									$("#outputAccountNameDiv2").hide()
									detailObj.inputAccountName = row.childEvents[0]['inputAccountName']
									detailObj.outputAccountName = row.childEvents[0]['outputAccountName']
								}
								$$.detailAutoFix($('#detailModal'), detailObj)
								$("#detailModal").modal("show")
							}
						}
					}
				]
			}
			
			$$.searchInit($('#searchForm'), $('#dataTable'))
			
			//登账设置出款账户变更
			$("#outputAccount").on("change", function(){
				if(selectRow.childEvents[0].outputAccountName == $(this).find("option:selected").text()){
					$("#effectiveTimeType").attr("disabled","disabled")
					$("#submitSet").attr("disabled","disabled")
				}else{
					$("#effectiveTimeType").removeAttr("disabled")
					$("#submitSet").removeAttr("disabled")
				}
			})
			
			$("#submitSet").on('click', function(){
				http.post(accountConfig.api.platform.audit_apply, {
					data: {
						userOid: pageOptions.userOid,
						applyType: 3,
						applicantId: applicantId,
						applicantName: applicantName,
						changeRecordsList: JSON.stringify([{
							changeType: '08',
							newOutputAccountNo: $("#outputAccount").val(),
							newOutputAccountName: $("#outputAccount option:selected").text(),
							oldOutputAccountNo: selectRow.childEvents[0].outputAccountNo,
							oldOutputAccountName: selectRow.childEvents[0].outputAccountName,
							effectiveTimeType: $("#effectiveTimeType").val(),
							eventOid: selectRow.oid,
							eventName: selectRow.eventName,
							eventChildOid: selectRow.childEvents[0].oid
						}])
					},
					contentType: 'form'
				}, function(result){
					if('0000'==result.returnCode){
						$('#dataTable').bootstrapTable('refresh')
						$("#submitModal").modal('hide')
					}else{
						errorHandle(result)
					}
				})
			})
			
			$("#cancelSet").on('click', function(){
				http.post(accountConfig.api.platform.audit_revoke, {
					data: {
						oid: selectRow.oid
					},
					contentType: 'form'
				}, function(result){
					if('0000'==result.returnCode){
						$('#dataTable').bootstrapTable('refresh')
						$("#cancelModal").modal('hide')
					}else{
						errorHandle(result)
					}
				})
			})
			
			function getQueryParams (val) {
				var form = document.searchForm
				pageOptions.size = val.limit
				pageOptions.number = parseInt(val.offset / val.limit) + 1
				pageOptions.offset = val.offset
				pageOptions.eventName = form.eventName.value
				pageOptions.setUpStatus = form.setUpStatus.value
				return val
			}
		}
	}
})