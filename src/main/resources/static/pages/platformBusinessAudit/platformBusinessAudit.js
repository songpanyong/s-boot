define([
    'http',
    'config',
    'accountConfig',
    'util',
    'extension'
], function (http, config, accountConfig, util, $$) {
    return {
        name: 'platformBusinessAudit',
        init: function () {
        	var userId;
            var userName;
            // 获取登录的用户ID
            http.post(config.api.userInfo, {
                async: false
            }, function (result) {
                userId = result.oid;
                userName = result.name;
            });


            var auditDataOptions = {
                number: 1,
				size: 10,
				offset: 0,
                userName: '',
                userType: '',
                beginTime: '',
                endTime: '',
                auditStatus: '',
                auditStatusList: "1",
                phone: ''
            };

            var auditTableConfig = {
                ajax: function (origin) {
                    http.post(accountConfig.api.platform.audit_page, {
                        data: {
                        	page: auditDataOptions.number,
                        	rows: auditDataOptions.size,
                        	userName: auditDataOptions.userName,
                        	userType: auditDataOptions.userType,
                        	beginTime: auditDataOptions.beginTime,
                        	endTime: auditDataOptions.endTime,
                        	auditStatus: auditDataOptions.auditStatus,
                        	auditStatusList: auditDataOptions.auditStatusList,
                        	phone:auditDataOptions.phone
                        },
                        contentType: 'form'
                    }, function (rlt) {
                        origin.success(rlt)
                    })
                },
                pageNumber: auditDataOptions.number,
				pageSize: auditDataOptions.size,
				pagination: true,
				sidePagination: 'server',
				pageList: [10, 20, 30, 50, 100],
				queryParams: auditGetQueryParams,
                onClickCell: function (field, value, row, $element) {
                    switch (field) {
                        case 'applyTypeName':
                            queryInfo(value, row);
                            break
                    }
                },
                columns: [
                	{
						align: 'center',
						width: 30,
						formatter: function (val, row, index) {
							return index + 1 + auditDataOptions.offset
						}
                	},
                    {
                    	field: 'userName'
                    },
                    {
                    	field: 'userType',
                    	formatter: function (val, row, index) {
                    		return val === 'T1' ? '投资人' : val === 'T2' ? '发行人' : '平台'
                    	}
                    },
                    {
                    	field: 'phone'
                    },
                    {
                    	field: 'userStatus',
                    	formatter: function (val, row, index) {
                    		return val === '0' ? '停用' : '启用'
                    	}
                    },
                    {
                    	field: 'applyTypeName',
                        formatter: function (val, row, index) {
                            return "<span style='color:#169BE2;cursor:pointer'>"+val+"</span>";
                        }
                    },
                    {
                    	field: 'createTime'
                    },
                    {
                    	field: 'auditReason'
                    },
                    {
                    	field: 'auditStatus',
                    	formatter: function (val, row, index) {
                    		return val === '0' ? '待审核' : val === '1' ? '审核通过' : val === '2' ? '驳回' : '已撤销'
                    	}
                    },
                    {
                    	align: 'center',
						formatter: function (val, row, index) {
							var buttons = [{
								text: '审核',
								type: 'button',
								class: 'item-audit',
								isRender: row.auditStatus === "0"
							}]
							return util.table.formatter.generateButton(buttons, 'auditTable');
						},
						events: {
							'click .item-audit': function (e, value, row) {
								var form = document.auditForm;
								$(form).clearForm();
								$(form).validator('destroy')
								util.form.validator.init($(form));
								form.oid.value = row.oid;
								form.operatorId.value = userId;
								form.operatorName.value = userName;
								$('#auditTitle').html('平台信息审核');
								$("#mark").attr("name", "auditReason")
								$('#auditModal').modal('show')
							}
						}
                    }
                ]
            };

            var bindCardAuditOptions = {
				number: 1,
				size: 10,
				offset: 0,
				platformName: '',
				realName: '',
				beginTime: '',
				endTime: '',
				accountBankType: '',
				auditStatus: ''
			}

            var bindCardAuditTableConfig = {
				ajax: function (origin) {
					http.post(accountConfig.api.platform.bind_card_audit_page, {
						data: {
							page: bindCardAuditOptions.number,
							rows: bindCardAuditOptions.size,
							platformName: bindCardAuditOptions.platformName,
							realName: bindCardAuditOptions.realName,
							beginTime: bindCardAuditOptions.beginTime,
							endTime: bindCardAuditOptions.endTime,
							accountBankType: bindCardAuditOptions.accountBankType,
							auditStatus: bindCardAuditOptions.auditStatus
						},
						contentType: 'form'
					}, function (rlt) {
						origin.success(rlt)
					})
				},
				pageNumber: bindCardAuditOptions.number,
				pageSize: bindCardAuditOptions.size,
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
							return index + 1 + bindCardAuditOptions.offset
						}
					},
					{
						field: 'platformName'
					},
					{
						field: 'realName'
					},
					{
						field: 'accountBankType',
						formatter: function (val, row, index) {
							switch (val) {
								case "01" : return "企业";break
								case "02" : return "个人";break
								default : return "--";break
							}
						}
					},
					{
						field: 'cardNo'
					},
					{
						field: 'bankName'
					},
					{
						field: 'bankBranch'
					},
					{
						field: 'bankAddress'
					},
					{
						field: 'certificateNo'
					},
					{
						field: 'phone'
					},
					{
						field: 'createTime'
					},
					{
						field: 'auditMark'
					},
					{
						field: 'auditStatus',
						formatter: function (val, row, index) {
							switch (val) {
								case "0" : return "待审核";break
								case "1" : return "通过";break
								case "2" : return "驳回";break
								default : return "--";break
							}
						}
					},
					{
						align: 'center',
						formatter: function (val, row, index) {
							var buttons = [{
								text: '审核',
								type: 'button',
								class: 'item-audit',
								isRender: row.auditStatus === "0"
							}]
							return util.table.formatter.generateButton(buttons, 'bindCardAuditTable');
						},
						events: {
							'click .item-audit': function (e, value, row) {
								var form = document.auditForm;
								$(form).clearForm();
								$(form).validator('destroy')
								util.form.validator.init($(form));
								form.oid.value = row.oid;
								form.operatorId.value = userId;
								form.operatorName.value = userName;
								$('#auditTitle').html('绑卡信息审核');
								$("#mark").attr("name", "auditMark")
								$('#auditModal').modal('show')
							}
						}
					}
				]
			}

            /**
             * 订单审核表格初始化
             */
            $('#auditTable').bootstrapTable(auditTableConfig);
            $('#bindCardAuditTable').bootstrapTable(bindCardAuditTableConfig);

            $$.searchInit($('#searchAuditForm'), $('#auditTable'))
            $$.searchInit($('#bindCardAuditForm'), $('#bindCardAuditTable'))

            //组条件查询
            function auditGetQueryParams(val) {
                var form = document.searchAuditForm;
                auditDataOptions.size = val.limit
				auditDataOptions.number = parseInt(val.offset / val.limit) + 1
				auditDataOptions.offset = val.offset
				auditDataOptions.userName = form.userName.value,
				auditDataOptions.userType = form.userType.value,
				auditDataOptions.beginTime = form.beginTime.value,
				auditDataOptions.endTime = form.endTime.value,
				auditDataOptions.auditStatus = form.auditStatus.value,
				auditDataOptions.phone = form.phone.value
                return val;
            }

            function getQueryParams (val) {
				var form = document.bindCardAuditForm
				bindCardAuditOptions.size = val.limit
				bindCardAuditOptions.number = parseInt(val.offset / val.limit) + 1
				bindCardAuditOptions.offset = val.offset
				bindCardAuditOptions.platformName = form.platformName.value,
				bindCardAuditOptions.realName = form.realName.value,
				bindCardAuditOptions.beginTime = form.beginTime.value,
				bindCardAuditOptions.endTime = form.endTime.value,
				bindCardAuditOptions.accountBankType = form.accountBankType.value,
				bindCardAuditOptions.auditStatus = form.auditStatus.value
				return val
			}

            //审核通过
            $('#passBut').on('click', function(){
            	if (!$('#auditForm').validator('doSubmitCheck')) return
            	document.auditForm.auditStatus.value = '1'
            	var url = ''
            	if($("#mark").attr("name") == "auditMark"){
            		url = accountConfig.api.platform.bind_card_audit
            	}else{
            		url = accountConfig.api.platform.audit
            	}
            	$('#auditForm').ajaxSubmit({
            		url: url,
            		success: function (result) {
            			if('0000'==result.returnCode){
            				$('#auditTable').bootstrapTable('refresh')
            				$('#bindCardAuditTable').bootstrapTable('refresh')
            				$('#auditModal').modal('hide')
            			}else{
            				errorHandle(result)
            				$('#auditTable').bootstrapTable('refresh')
            				$('#bindCardAuditTable').bootstrapTable('refresh')
            				$('#auditModal').modal('hide')
            			}
            		}
            	})
            })
            //审核驳回
            $('#refusedBut').on('click', function(){
            	if (!$('#auditForm').validator('doSubmitCheck')) return
            	document.auditForm.auditStatus.value = '2'
            	var url = ''
            	if($("#mark").attr("name") == "auditMark"){
            		url = accountConfig.api.platform.bind_card_audit
            	}else{
            		url = accountConfig.api.platform.audit
            	}
            	$('#auditForm').ajaxSubmit({
            		url: url,
            		success: function (result) {
            			if('0000'==result.returnCode){
            				$('#auditTable').bootstrapTable('refresh')
            				$('#bindCardAuditTable').bootstrapTable('refresh')
            				$('#auditModal').modal('hide')
            			}else{
            				errorHandle(result)
            			}
            		}
            	})
            })

            //查看详情
            function queryInfo(value, row) {
            	http.post(accountConfig.api.platform.audit_changeRecords, {
                    data: {
                        oid: row.oid
                    },
                    contentType: 'form'
                }, function (result) {
                	if(value == '平台登账设置更新' && result[0].changeType == '08'){
                		$("#orderTitle").html(result[0].eventName+'-登账设置更新')
                		var detailObj = {}
                		detailObj.oldOutputAccountName = result[0].oldOutputAccountName
                		detailObj.newOutputAccountName = result[0].newOutputAccountName
                		detailObj.effectiveTimeType = function(){
                			switch (result[0].effectiveTimeType) {
                				case "01" : return "即时生效"; break
								case "02" : return "次日生效"; break
								case "03" : return "次月生效"; break
								default : return "--"; break
                			}
                		}
                		$$.detailAutoFix($('#orderDetail'), detailObj)
                		$(".registrationAccount").show()
                		$(".baseInfo").hide()
                		$(".accountQuota").hide()
                		$("#orderDetail").modal('show');
                	}else if(value == '平台基本信息变更'){
                		var leftHtml = '', rightHtml = ''
                		result.forEach(function(e){
                			switch (e.changeType) {
                				case '01' :
                					$("#orderTitle").html('平台名称更改')
                					leftHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>平台名称:</dt>'+
                											'<dd>'+e.oldName+'</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					rightHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>平台名称:</dt>'+
                											'<dd>'+e.newName+'</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					break
                				case '02' :
                					$("#orderTitle").html('平台停用')
                					leftHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>平台状态:</dt>'+
                											'<dd>启用</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					rightHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>平台状态:</dt>'+
                											'<dd>停用</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					break
                				case '03' :
                					$("#orderTitle").html('平台启用')
                					leftHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>平台状态:</dt>'+
                											'<dd>停用</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					rightHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>平台状态:</dt>'+
                											'<dd>启用</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					break
                				case '04' :
                					$("#orderTitle").html('新建备付金账户')
                					leftHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>&nbsp;</dt>'+
                											'<dd>&nbsp;</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					rightHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>'+e.newName+':</dt>'+
                											'<dd>新增</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					break
                				case '05' :
                					$("#orderTitle").html('启用备付金账户')
                					leftHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>'+e.oldName+':</dt>'+
                											'<dd>停用</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					rightHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>'+e.oldName+':</dt>'+
                											'<dd>启用</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					break
                				case '06' :
                					$("#orderTitle").html('停用备付金账户')
                					leftHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>'+e.oldName+':</dt>'+
                											'<dd>启用</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					rightHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>'+e.oldName+':</dt>'+
                											'<dd>停用</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					break
                				case '09' :
                					$("#orderTitle").html('账户名称更改')
                					leftHtml += '<div class="row">'+
                									'<div class="col-sm-12">'+
                										'<dl class="dl-horizontal">'+
                											'<dt>账户名称:</dt>'+
                											'<dd>'+e.oldName+'</dd>'+
                										'</dl>'+
                									'</div>'+
                								'</div>'
                					rightHtml += '<div class="row">'+
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
                		$("#baseInfoLeft").html(leftHtml)
                		$("#baseInfoRight").html(rightHtml)
                		$(".registrationAccount").hide()
                		$(".baseInfo").show()
                		$(".accountQuota").hide()
                		$("#orderDetail").modal('show');
                	}else if(value == '平台账户额度调整' && result[0].changeType == '07'){
                		$("#orderTitle").html('平台账户额度调整')
                		var detailObj = {}
                		detailObj.oldName = result[0].oldName+"授信额度:"
                		detailObj.oldBalance = result[0].oldBalance+"元"
                		detailObj.newBalance = result[0].newBalance+"元"
                		$$.detailAutoFix($('#orderDetail'), detailObj)
                		$(".registrationAccount").hide()
                		$(".baseInfo").hide()
                		$(".accountQuota").show()
                		$("#orderDetail").modal('show');
                	}
                })
            }
        }
    }
});