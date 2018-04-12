// 载入所需模块
define([
'http',
'config',
'accountConfig',
'util',
'extension'
], function (http, config, accountConfig, util, $$) {
	return {
		name: 'platFormInfo',
		init: function() {
			//银行列表信息
			var bankCodeList = []
			var bankNameList = []
			
			//编辑平台信息保存原平台信息
			var platformObj = {}
			
			//用户信息
			var applicantId = ''
			var applicantName = ''
			
			//获取登录用户信息
			http.post(config.api.userInfo, function(result){
				applicantId = result.oid
				applicantName = result.name
			})
			
			//初始化倒计时
			var countStr = "; " + document.cookie + "; ", countIndex = countStr.indexOf("; countTime=");
			if(countIndex != -1){
				var tempStr = countStr.substring(countIndex + "countTime".length + 3, countStr.length), countTime = tempStr.substring(0, tempStr.indexOf("; "));
				if(decodeURIComponent(countTime)){
					var time = decodeURIComponent(countTime) - new Date().getTime()
					time = (time / 1000).toString()
					time.indexOf('.') > -1 ? time = parseInt(time.split('.')[0]) : time = parseInt(time)
					console.log(time)
					if(time && time > 0){
						var btntime = setInterval(function() {
							if(time >= 0) {
								$("#sendMessage").addClass("btn_loading")
								$("#sendMessage").html('重新获取(' + time + ')');
								time--;
							} else {
								$("#sendMessage").removeClass("btn_loading");
								$("#sendMessage").html("点击发送验证码");
								clearInterval(btntime);
							}
						}, 1000)
					}
				}
			}
			
			var platformInfo = {
				userOid: '',
				bindCardStatus: '',
				platformName: '',
				platformStatus: ''
			}
            // 分页配置
			var platformPageOptions = {
				page: 1,
				rows: 10,
//				offset: 0,
				platformName: ''
			}
			var platformTableConfig = {
                ajax: function (origin) {
                    http.post(accountConfig.api.platform.platfrom_page, {
                        data: platformPageOptions,
                        contentType: 'form'
                    }, function (rlt) {
                        origin.success(rlt)
                    })
                },
                pageNumber: platformPageOptions.page,
                pageSize: platformPageOptions.row,
                pagination: true,
                sidePagination: 'server',
                pageList: [10, 20, 30, 50, 100],
                queryParams: function(val) {
					platformPageOptions.size = val.limit
					platformPageOptions.number = parseInt(val.offset / val.limit) + 1
					platformPageOptions.offset = val.offset
					platformPageOptions.platformName = $('#searchplatformName').val()
					return val
				},
                onLoadSuccess: function () {
                },
				columns: [{
						width: 30,
						align: 'center',
						formatter: function(val, row, index) {
							return platformPageOptions.offset + index + 1
						}
					},{
						field: 'platformName'
					},{
                    	width: 100,
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		return row.platformStatus==='1'?'启用':'停用';
						}
                    },{
                    	width: 100,
                    	align: 'left',
                    	formatter: function(val, row, index) {
                    		return row.bindCardStatus==='0'?'未绑卡':
                    		row.bindCardStatus==='1'?'已绑卡':
                    		row.bindCardStatus==='2'?'绑卡申请中':
                    		row.bindCardStatus==='3'?'待绑卡确认':
                    		row.bindCardStatus==='4'?'换绑卡申请中':'待换绑卡确认';
						}
                    }, {
						align: 'center',
						width: 80,
						formatter: function(val, row, index) {
							var buttons = [{
								text: '切换',
								type: 'button',
								class: 'item-choose'
							}]
							return util.table.formatter.generateButton(buttons, 'platformTable');
						},
						events: {
							'click .item-choose': function(e, value, row) {
								platformInfo.userOid = row.userOid;
								platformInit();
								$('#platformsModal').modal('hide');
							}
						}
					}
				]
			}
			 // 初始化数据表格
            $('#platformTable').bootstrapTable(platformTableConfig);

			//切换平台
			$("#changePlatform").on("click", function() {
				$('#platformsModal').modal('show');
				$('#platformTable').bootstrapTable('refresh');
			});

			//修改平台
			$("#editPlatform").on("click", function() {
				platformEdit();
			});
			
			//查看变更
			$("#changeDetial").on('click', function(){
				showPlatformChange();
			})
			
			//绑定银行卡
			$("#bindCard,#changeCard").on("click", function() {
				$('#enterpriseForm').clearForm()
				$("#enterpriseForm").validator('destroy')
				util.form.validator.init($("#enterpriseForm"));
				
				$('#personalForm').clearForm()
				$("#personalForm").validator('destroy')
				util.form.validator.init($("#personalForm"));
				
				initBankList()
				
				document.enterpriseForm.applicantId.value = applicantId
				document.enterpriseForm.applicantName.value = applicantName
				document.enterpriseForm.userOid.value = platformInfo.userOid
				document.enterpriseForm.platformName.value = platformInfo.platformName
				document.enterpriseForm.accountBankType.value = '01'
				document.enterpriseForm.bindCardStatus.value = platformInfo.bindCardStatus
				
				document.personalForm.applicantId.value = applicantId
				document.personalForm.applicantName.value = applicantName
				document.personalForm.userOid.value = platformInfo.userOid
				document.personalForm.platformName.value = platformInfo.platformName
				document.personalForm.accountBankType.value = '02'
				document.personalForm.bindCardStatus.value = platformInfo.bindCardStatus
				
				var provinceOptions = ''
				config.province.forEach(function(e){
					provinceOptions += '<option value="'+e+'">'+e+'</option>'
				})
				$('#province').html(provinceOptions)
				
				var cityOptions = ''
				config.city[0].forEach(function(e){
					cityOptions += '<option value="'+e+'">'+e+'</option>'
				})
				$('#city').html(cityOptions)
				if(this == document.getElementById("bindCard")){
					$(".cardType").html("新增")
				}else{
					$(".cardType").html("更换")
				}
				$('#bindCardModal').modal('show');
			});
			//企业个人绑定切换
			$("#enterprise").on("click", function() {
				$('.enterprise').show();
				$('.personal').hide();
				$('#enterpriseDiv').show();
				$('#personalDiv').hide();
			});
			$("#personal").on("click", function() {
				$('.personal').show();
				$('.enterprise').hide();
				$('#enterpriseDiv').hide();
				$('#personalDiv').show();
			});
			//企业绑卡省份切换
			$(document.enterpriseForm.province).on('change', function(){
				var cityOptions = ''
				config.city[config.province.indexOf($(this).val())].forEach(function(e){
					cityOptions += '<option value="'+e+'">'+e+'</option>'
				})
				$('#city').html(cityOptions)
			})
			//输入卡号匹配银行
			$(document.personalForm.cardNo).on('input', function(){
				if($(this).val().length > 15){
					findBankByCard();
				}
			})
			
			$(document.personalForm.cardNo).on('blur', function(){
				if($(this).val().length > 15){
					findBankByCard();
				}
			})
			
			$(document.personalForm.bankName).on('change', function(){
				if($(document.personalForm.cardNo).val().length > 15){
					findBankByCard();
				}
			})
			//企业绑卡
			$("#enterpriseSubmit").on("click", function() {
				if (!$('#enterpriseForm').validator('doSubmitCheck')) return
				document.enterpriseForm.bankAddress.value = $("#province").val()+$("#city").val()
				if(platformInfo.bindCardStatus == "3" || platformInfo.bindCardStatus == "5" || platformInfo.bindCardStatus == "6"){
					$("#bindCardConfirmModal").modal('show')
				}else{
					bindCard($('#enterpriseForm'))
				}
			});
			
			//个人绑卡
			$("#personalSubmit").on("click", function() {
				if (!$('#personalForm').validator('doSubmitCheck')) return
				findBankByCard(function(){
					if(platformInfo.bindCardStatus == "3" || platformInfo.bindCardStatus == "5" || platformInfo.bindCardStatus == "6"){
						$("#bindCardConfirmModal").modal('show')
					}else{
						bindCard($('#personalForm'))
					}
				})
			});
			
			//确认更换银行卡
			$("#bindCardConfirmSubmit").on('click', function(){
				bindCard($("#enterprise").is(':checked') ? $('#enterpriseForm') : $('#personalForm'))
			})
			
			//发送验证码
			$("#sendMessage").on('click', function(){
				if($(this).hasClass("btn_loading")){
					return
				}
				http.post(accountConfig.api.platform.bind_card_bindApply, {
					data: {
						userOid: platformInfo.userOid,
						realName: $("#realName").html(),
						cardNo: $("#cardNo").html(),
						certificateNo: $("#certificateNo").html(),
						phone: $("#phone").html(),
						bankName: $("#bankName").html()
					},
					contentType: 'form'
				}, function(result){
					if('0000'==result.returnCode){
						//倒计时120s，cookie写入返回数据保存120s
						var timeLimit = new Date()
						timeLimit.setTime(new Date().getTime()+120000)
						document.cookie = "cardOrderId="+ escape (result.cardOrderId) + ";expires=" + timeLimit.toGMTString()
						document.cookie = "countTime="+ escape(timeLimit.getTime()) + ";expires=" + timeLimit.toGMTString()
						
						var t = 120;
						var btntime = setInterval(function() {
							if(t >= 0) {
								$("#sendMessage").addClass("btn_loading")
								$("#sendMessage").html('重新获取(' + t + ')');
								t--;
							} else {
								$("#sendMessage").removeClass("btn_loading");
								$("#sendMessage").html("点击发送验证码");
								clearInterval(btntime);
								t = 120;
							}
						}, 1000)
					}else{
						errorHandle(result)
					}
				})
			})
			
			//个人绑卡确认
			$("#bindConfirm").on('click', function(){
				var str = "; " + document.cookie + "; ", index = str.indexOf("; cardOrderId=");
				if($("#smsCode").val().length != 6){
					console.log('click')
					toastr.error("请输入6位验证码！", '错误信息', {
						timeOut: 2000
					})
				}else if(index == -1){
					toastr.error("验证码已失效，请重新获取验证码！", '错误信息', {
						timeOut: 2000
					})
				}else{
					var tempStr = str.substring(index + "cardOrderId".length + 3, str.length), cardOrderId = tempStr.substring(0, tempStr.indexOf("; "));
					console.log(decodeURIComponent(cardOrderId))
					http.post(accountConfig.api.platform.bind_card_bindConfrim, {
						data: {
							userOid: platformInfo.userOid,
							phone: $("#phone").html(),
							smsCode: $("#smsCode").val(),
							cardOrderId: decodeURIComponent(cardOrderId)
						},
						contentType: 'form'
					}, function(result){
						if('0000'==result.returnCode){
							var timeLimit = new Date();
							timeLimit.setTime(timeLimit.getTime() - 1);
							document.cookie= "cardOrderId="+cardOrderId+";expires="+timeLimit.toGMTString();
							document.cookie= "countTime="+escape(timeLimit.getTime())+";expires="+timeLimit.toGMTString();
							platformInit()
						}else{
							errorHandle(result)
						}
					})
				}
			})
			
			//解绑卡
			$("#unbind").on('click', function(){
				http.post(accountConfig.api.platform.bind_card_unlock, {
					data: {
						userOid: platformInfo.userOid
					},
					contentType: 'form'
				}, function(result){
					if('0000'==result.returnCode){
						platformInit()
					}else{
						errorHandle(result)
					}
				})
			})
			
			$("#addNewReserveSubmit").on('click', function(){
				if($("#reserveAccountDiv").children('div').length >= 5) return
				var form = document.getElementById('addNewReserveForm')
				if (!$(form).validator('doSubmitCheck')) return
				var html = ''
				html = '<div class="form-group">'+
							'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<label>'+$('#addNewReserveForm input[name=newReserve]').val()+'</label>'+
						'</div>'
				$("#reserveAccountDiv").append(html)
				$('#addNewReserveForm input[name=newReserve]').val('')
				$(form).validator('destroy');
				util.form.validator.init($(form));
				if($("#reserveAccountDiv").children('div').length >= 5){
					$("#addNewReserveDiv").hide()
				}
			})
			
			$("#channelSubmit").on('click', function(){
				var form = document.channelForm
				if (!$(form).validator('doSubmitCheck')) return
				var data = {}
				data.userOid = platformInfo.userOid
				data.applyType = 1
				data.applicantId = applicantId
				data.applicantName = applicantName
				var changeRecordsList = []
				if($("#platformName").val() != platformObj.platformName){
					changeRecordsList.push({
						changeType: "01",
						oldName: platformObj.platformName,
						newName: $("#platformName").val()
					})
				}
				if($("input[name=platformStatus]:checked").val() != platformObj.platformStatus){
					if(platformObj.platformStatus == "1"){
						changeRecordsList.push({
							changeType: "02"
						})
					}else{
						changeRecordsList.push({
							changeType: "03"
						})
					}
				}
				if($("#reserveAccountDiv").children('div').length != platformObj.account.length){
					for (var i = platformObj.account.length;i < $("#reserveAccountDiv").children('div').length;i++) {
						changeRecordsList.push({
							changeType: "04",
							newName: $("#reserveAccountDiv").children('div').eq(i).find('label').html()
						})
					}
				}
				for (var i = 0;i < platformObj.account.length;i++) {
					if(platformObj.account[i].settleStatus == "1" && platformObj.account[i].accountStatus != $("#reserveAccountDiv").children('div').eq(i).find('input:checked').val()){
						if(platformObj.account[i].accountStatus == "0"){
							changeRecordsList.push({
								changeType: "05",
								accountNo: platformObj.account[i].accountNo,
								oldName: platformObj.account[i].accountName
							})
						}else{
							changeRecordsList.push({
								changeType: "06",
								accountNo: platformObj.account[i].accountNo,
								oldName: platformObj.account[i].accountName
							})
						}
					}
					if($("#reserveAccountDiv").children('div').eq(i).find('label').is(':has(input)') && $("#reserveAccountDiv").children('div').eq(i).find('label').find('input').val() != platformObj.account[i].accountName){
						changeRecordsList.push({
							changeType: "09",
							oldName: platformObj.account[i].accountName,
							newName: $("#reserveAccountDiv").children('div').eq(i).find('label').find('input').val(),
							accountNo: platformObj.account[i].accountNo
						})
					}
				}
				if(changeRecordsList.length > 0){
					data.changeRecordsList = JSON.stringify(changeRecordsList)
					http.post(accountConfig.api.platform.audit_apply, {
						data: data,
						contentType: 'form'
					}, function(result){
						if('0000'==result.returnCode){
							platformInit();
							$("#editplatformsModal").modal('hide')
						}else{
							errorHandle(result)
						}
					})
				}
			})
			
			platformInit();
			//初始化页面数据
			function platformInit() {
				http.post(accountConfig.api.platform.platfrom_info,{
		            data:{
		            	userOid : platformInfo.userOid
		            },
		            contentType: 'form'
	            }, function (result) {//0停用1启用2修改中
	                platformInfo.userOid = result.userOid;
	                platformInfo.bindCardStatus = result.bindCardStatus;
	                platformInfo.platformName = result.platformName;
	                platformInfo.platformStatus = result.platformStatus
	                if (result.settleStatus == '0') {
						$('#editPlatform').show();
						$('#changeDetial').hide();
					} else {
						$('#editPlatform').hide();
						$('#changeDetial').show();
					}
					result.platformStatus = result.platformStatus ==='0'?'停用':'启用';
					//绑卡状态：0未绑卡，1已绑卡，2未绑卡绑卡申请中，3绑卡审核通过（个人卡），4已绑卡换绑申请中，5已绑卡换绑审核通过，6换绑审核通过
					if(result.bindCardStatus == '0'){
						$("#bindCardTitle").show()
						$("#changeCardTitle").hide()
						$('#bindCard').show();
						$('#changeCard').hide();
						$('#bindCardDiv').hide();
						$('#changeCardDiv').hide();
					}else if(result.bindCardStatus == '1'){
						$("#bindCardTitle").show()
						$("#changeCardTitle").hide()
						$('#bindCard').hide();
						$('#changeCard').show();
						$('#bindCardDiv').show();
						$('#changeCardDiv').hide();
						$("#unbindDiv").hide()
					}else if(result.bindCardStatus == '2'){
						$("#bindCardTitle").hide()
						$("#changeCardTitle").hide()
						$('#bindCard').hide();
						$('#changeCard').hide();
						$('#bindCardDiv').hide();
						$('#changeCardDiv').hide();
					}else if(result.bindCardStatus == '3'){
						$("#bindCardTitle").hide()
						$("#changeCardTitle").show()
						$('#bindCard').hide();
						$('#changeCard').show();
						$('#bindCardDiv').hide();
						$('#changeCardDiv').show();
						$("#smsCodeDiv").show()
					}else if(result.bindCardStatus == '4'){
						$("#bindCardTitle").show()
						$("#changeCardTitle").hide()
						$('#bindCard').hide();
						$('#changeCard').hide();
						$('#bindCardDiv').show();
						$('#changeCardDiv').hide();
						$("#unbindDiv").hide()
					}else if(result.bindCardStatus == '5'){
						$("#bindCardTitle").show()
						$("#changeCardTitle").show()
						$('#bindCard').hide();
						$('#changeCard').show();
						$('#bindCardDiv').show();
						$('#changeCardDiv').show();
						$("#smsCodeDiv").show()
						$("#unbindDiv").hide()
					}else if(result.bindCardStatus == '6'){
						$("#bindCardTitle").show()
						$("#changeCardTitle").show()
						$('#bindCard').hide();
						$('#changeCard').show();
						$('#bindCardDiv').show();
						$('#changeCardDiv').show();
						$("#smsCodeDiv").hide()
						$("#unbindDiv").show()
					}
					$$.detailAutoFix($('#detailForm'), result); // 自动填充详情
					var openList = '';
					var closeList = '';
					for(var i=0;i<result.openList.length;i++){
						if(i==3||i==7||i==11||i==15||i==19){
							openList = openList + result.openList[i]+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br></br>';
						}else{
							openList = openList + result.openList[i]+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
						}
					}
					for(var i=0;i<result.closeList.length;i++){
						if(i==3||i==7||i==11||i==15||i==19){
							closeList = closeList + result.closeList[i]+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br></br>';
						}else{
							closeList = closeList + result.closeList[i]+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
						}
					}
					$("#openList").html('');
					$("#openList").append(openList);
					$("#closeList").html('');
					$("#closeList").append(closeList);
					
					//已绑卡，或者已绑卡并在换绑中
					if(result.bindCardStatus==1 || result.bindCardStatus==4 || result.bindCardStatus==5 || result.bindCardStatus==6){
						//获取已绑卡信息
						http.post(accountConfig.api.accountSign.bind_card_query,{
				            data:{
				            	userOid : platformInfo.userOid
				            },
				            contentType: 'form'
				        }, function (result) {
			                platformInfo.userOid = result.userOid;
			                result.cardType = result.cardType == "个人" ? "个人储蓄银行卡" : "企业对公账户"
							if(result.cardType == "企业对公账户"){
								result.bankAddress = result.province + result.city
								$("#accountBankDetail").show()
								$("#personBankDetail").hide()
							}else{
								$("#accountBankDetail").hide()
								$("#personBankDetail").show()
							}
							$$.detailAutoFix($('#bindCardDiv'), result); // 自动填充详情
				       	})
					}
					//未绑卡绑卡审核通过或者已绑卡换卡审核通过
					if(result.bindCardStatus==3 || result.bindCardStatus==5 || result.bindCardStatus==6){
						//获取绑卡审核信息
						http.post(accountConfig.api.platform.bind_card_audit_info,{
				            data:{
				            	userOid : platformInfo.userOid
				            },
				            contentType: 'form'
				        }, function (result) {
			                platformInfo.userOid = result.userOid;
							$$.detailAutoFix($('#changeCardDiv'), result); // 自动填充详情
				       	})
					}
	        	})
			}
			
			// 编辑平台信息
			function platformEdit () {
				http.post(accountConfig.api.platform.platfrom_account_info, {
					data: {
						userOid: platformInfo.userOid,
						accountType: "08"
					},
					contentType: 'form'
				}, function(result){
					$$.formAutoFix($("#channelForm"), platformInfo)
					if(result.length >= 5){
						$("#addNewReserveDiv").hide()
					}else{
						$("#addNewReserveDiv").show()
					}
					$(document.addNewReserveForm).clearForm();
					$(document.addNewReserveForm).validator('destroy');
					util.form.validator.init($(document.addNewReserveForm));
					$(document.channelForm).validator('destroy');
					util.form.validator.init($(document.channelForm));
					
					$("#reserveAccountDiv").html('<h5>备付金账户</h5>')
					var html = ''
					platformObj.account = []
					result.forEach(function(e){
						if(e.settleStatus == "1"){
							html += '<div class="form-group">'+
										'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<label data-settleStatus="1">'+e.accountName+'<span id="valierr" style="color:#dd4b39;font-size:16px;position:absolute;">＊</span></label>'+
										'<div class="row">'+
											'<div class="col-sm-2"></div>'+
											'<div class="col-sm-2">'+
												'<input name="'+e.accountNo+'" type="radio" value="1" required '+(e.accountStatus == "1" ? "checked" : "")+'> 启用'+
											'</div>'+
											'<div class="col-sm-2">'+
												'<input name="'+e.accountNo+'" type="radio" value="0" required '+(e.accountStatus == "0" ? "checked" : "")+'> 停用'+
											'</div>'+
										'</div>'+
									'</div>'
						}else{
							html += '<div class="form-group">'+
										'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<label data-settleStatus="0">'+e.accountName+'</label>'+
									'</div>'
						}
						platformObj.account.push({
							accountNo: e.accountNo,
							accountName: e.accountName,
							accountStatus: e.accountStatus,
							settleStatus: e.settleStatus
						})
					})
					$("#reserveAccountDiv").append(html)
					platformObj.platformName = platformInfo.platformName
					platformObj.platformStatus = platformInfo.platformStatus
					
					$("#reserveAccountDiv label").off('click').on('click', function(){
						if($(this).attr("data-settleStatus") == "1" && !$(this).is(':has(input)')){
							$(this).html('<input data-name="'+$(this).html().substr(0, $(this).html().indexOf('<span'))+'" /><span id="valierr" style="color:#dd4b39;font-size:16px;position:absolute;">＊</span>')
						}
						$("#reserveAccountDiv label input").off('blur').on('blur', function(){
							if(!$(this).val() || $(this).val() == $(this).attr("data-name")){
								$(this).parent('label').html($(this).attr("data-name")+'<span id="valierr" style="color:#dd4b39;font-size:16px;position:absolute;">＊</span>')
							}
						})
					})
					
					$('#editplatformsModal').modal('show');
				})
			}
			
			//查看变更详情
			function showPlatformChange () {
				http.post(accountConfig.api.platform.platfrom_info_change, {
					data: {
						userOid: platformInfo.userOid
					},
					contentType: 'form'
				}, function(result){
					var topHtml = '', bottomHtml = ''
					result.forEach(function(e){
            			switch (e.changeType) {
            				case '01' : 
            					topHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>平台名称:</dt>'+
            											'<dd>'+e.oldName+'</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					bottomHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>平台名称:</dt>'+
            											'<dd>'+e.newName+'</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					break
            				case '02' : 
            					topHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>平台状态:</dt>'+
            											'<dd>已启用</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					bottomHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>平台状态:</dt>'+
            											'<dd>已停用</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					break
            				case '03' : 
            					topHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>平台状态:</dt>'+
            											'<dd>已停用</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					bottomHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>平台状态:</dt>'+
            											'<dd>已启用</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					break
            				case '04' : 
            					bottomHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>新增平台备付金户:</dt>'+
            											'<dd>'+e.newName+'</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					break
            				case '05' : 
            					topHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>'+e.oldName+'状态:</dt>'+
            											'<dd>已停用</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					bottomHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>'+e.oldName+'状态:</dt>'+
            											'<dd>已启用</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					break
            				case '06' : 
            					topHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>'+e.oldName+'状态:</dt>'+
            											'<dd>已启用</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					bottomHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>'+e.oldName+'状态:</dt>'+
            											'<dd>已停用</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					break
            				case '09' : 
            					topHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>账户名称:</dt>'+
            											'<dd>'+e.oldName+'</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					bottomHtml += '<div class="row">'+
            									'<div class="col-sm-12">'+
            										'<dl class="dl-horizontal">'+
            											'<dt>账户名称:</dt>'+
            											'<dd>'+e.newName+'</dd>'+
            										'</dl>'+
            									'</div>'+
            								'</div>'
            					break
            				default : break
            			}
            		})
					$("#oldContent").html(topHtml)
					$("#newContent").html(bottomHtml)
					$("#platformChangeModal").modal('show')
				})
			}
			
			//获取银行列表
			function initBankList (formDom) {
				bankCodeList = []
				bankNameList = []
				http.post(config.api.caccountBank, {
					contentType: 'application/json'
				}, function(result){
					if (result.datas && result.datas.length > 0) {
						var html = ""
						result.datas.forEach(function(e, i){
							html += '<option value="' + e.bankName + '">' + e.bankName + '</option>'
							bankCodeList.push(e.peopleBankCode)
							bankNameList.push(e.bankName)
						})
						document.enterpriseForm.bankName.innerHTML = html
						document.personalForm.bankName.innerHTML = html
					}else{
						toastr.error("暂无可选择的银行", '错误信息', {
							timeOut: 2000
						})
					}
				})
			}
			
			//匹配银行信息
			function findBankByCard (callback) {
				http.post(config.api.findBankByCard+"?bankCardNumber="+document.personalForm.cardNo.value, {
					contentType: 'application/json'
				}, function(result){
					if(result.bankCode){
						if(result.bankCode == bankCodeList[bankNameList.indexOf($(document.personalForm.bankName).val())]){
							$("#cardNoDiv").removeClass("has-error")
							$("#cardNoError").html('')
							if(callback && typeof(callback) == 'function'){
								callback()
							}
						}else{
							if(bankCodeList.indexOf(result.bankCode) > -1){
								toastr.error("银行卡号与银行不匹配！已为您自动匹配银行", '错误信息', {
									timeOut: 2000
								})
								$("#cardNoDiv").removeClass("has-error")
								$("#cardNoError").html('')
								$(document.personalForm.bankName).val(bankNameList[bankCodeList.indexOf(result.bankCode)])
							}else{
								$("#cardNoDiv").addClass("has-error")
								$("#cardNoError").html('不支持该银行卡！')
							}
						}
					}else{
						$("#cardNoDiv").addClass("has-error")
						$("#cardNoError").html('银行卡号错误或不支持该银行卡！')
					}
				})
			}
			
			//绑卡
			function bindCard (formObj) {
				formObj.ajaxSubmit({
					url: accountConfig.api.platform.bind_card_audit_apply,
					success: function(res) {
						if ('0000' == res.returnCode) {
							platformInit()
							$('#bindCardModal').modal('hide')
							$("#bindCardConfirmModal").modal('hide')
						} else {
							errorHandle(res);
						}
					}
				})
			}
		}
	}
})