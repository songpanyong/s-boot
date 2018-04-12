/**
 * 金运通对账
 */
define([
		'http',
		//	'config',
		'settleConfig',
		'util',
		'extension'
	],
	function(http, settleConfig, util, $$) {
		return {
			name: 'jytReconciliation',
			init: function() {
				// 分页配置
				var pageOptions = {
						page: 1,
						row: 10
					}
					//提示框
				var confirm = $('#confirmModal1');
				// 初始化数据表格
				var tableConfig = {
					pageNumber: pageOptions.page,
					pageSize: pageOptions.row,
					pagination: true,
					pageList: [10, 20, 30, 50, 100],
					sidePagination: 'server',
					queryParams: getQueryParams,
					onLoadSuccess: function() {},
					ajax: function(origin) {
						var checkDate = verificationDate($("#checkDate").val());
						var reconStatus = $("#reconStatus").val();
						var channelId = $("#channelId").val();
						if(checkDate == "noDate") {
							checkDate = "20160101"; //无数据
						}
						http.post(settleConfig.api.settlement.settlement_reconciliation_page +
							"?channelId=" + channelId + "&checkDate=" + checkDate + "&reconStatus=" + reconStatus +
							"&page=" + pageOptions.page + "&row=" + pageOptions.row, {},
							function(rlt) {
								origin.success(rlt)
							})
					},
					columns: [{
							width: 100,
							checkbox: true,
							formatter: function(val, row) {
								return "<input type='text'  class='oid' hidden='hidden' value='" + row.oid + "' >"
							}
						}, {
							field: 'orderId',
							align: 'right'
						}, {
							align: 'center',
							formatter: function(val, row, index) {
								return row.channelId === '8' ? '实名支付' :
									row.channelId === '7' ? '代付' : '网银';
							}
						}, {
							align: 'center',
							formatter: function(val, row, index) {
								return row.transactionCurrency === 'CNY' ? '人民币' : '其他';
							}
						}, {
							align: 'center',
							formatter: function(val, row, index) {
								return row.transactionAmount + '元';
							}
						},
						//                  {field: 'paymentBankNo'},
						//                  {
						//                  	align: 'center',
						//                  	formatter: function(val, row, index) {
						//                  		return row.beneficiaryBankNo===''?'-':row.beneficiaryBankNo;
						//						}
						//                  },
						//                  {field: 'beneficiaryBankNo'},
						{
							align: 'center',
							formatter: function(val, row, index) {
								switch(row.tradStatus) {
									case '0':
										return '<span class="text-yellow">未处理</span>';
									case '1':
										return '<span class="text-green">交易成功</span>';
									case '2':
										return '<span class="text-red">交易失败</span>';
									case '3':
										return '<span class="text-yellow">交易处理中</span>';
									case '4':
										return '<span class="text-yellow">超时</span>';
								}
							}
						},
						//					{
						//                  	align: 'center',
						//                  	formatter: function(val, row, index) {
						//                  		return row.failDetail===''?'-':row.failDetail;
						//						}
						//                  },
						//                  {field: 'beneficiaryBankNo'},
						//                  {
						//                  	align: 'center',
						//                  	formatter: function(val, row, index) {
						//                  		return row.errorCode===''?'-':row.errorCode;
						//						}
						//                  },
						{
							field: 'failDetail'
						}, {
							field: 'errorCode'
						}, {
							field: 'transactionTime'
						}, {
							align: 'center',
							formatter: function(val, row, index) {
								switch(row.reconStatus) {
									case 0:
										return '<span class="text-yellow">未对帐</span>';
									case 1:
										return '<span class="text-green">对账成功</span>';
									case 2:
										return '<span class="text-red">对账失败</span>';
								}
							}
						}, {
							field: 'checkMark'
						}
					]
				}

				// 初始化数据表格
				$('#formTable').bootstrapTable(tableConfig);

				// 选择文件事件
				var input = document.getElementById("uploadFile");

				//文件域选择文件时, 执行readFile函数
				input.addEventListener('change', readFile1, false);

				//获取文件名及格式，填充菜单及校验格式
				function readFile1() {
					$("#add_channelName_err").text("");
					$("#add_tradTime_err").text("");
					$("#add_uploadFile_err").text("");
					var file = this.files[0];
					console.log("文件:" + file);
					console.log("文件名:" + file.name);
					console.log("文件类型:" + file.type);
					console.log("文件大小:" + file.size);
					var myList = new Array();
					myList = file.name.split("-");
					var fileType = file.type;
					if(fileType != "application/vnd.ms-excel") {
						$("#add_uploadFile_err").text("文件格式不符,请上传xls格式对账文件,例“2016-10-13290060100010.xls");
					}
					var year = myList[0];
					var month = myList[1];
					var day = myList[2].substring(0, 2);
					var fileDate = year + month + day;
					console.log("对账日期:" + fileDate);
					//判断文件名日期格式是否正确
					$("#tradTime_add").val(fileDate);
					if(fileDate.length != 8) {
						$("#add_tradTime_err").text("文件名格式不符,例“2016-10-13290060100010.xls”");
					}
					var nYear = parseInt(year);
					var nMonth = parseInt(month);
					var nDay = parseInt(day);
					if(isNaN(nYear) == true || isNaN(nMonth) == true || isNaN(nDay) == true) {
						$("#add_tradTime_err").text("文件名格式不符,例“2016-10-13290060100010.xls”");
					}
				}

				// 导入对账文件按钮点击事件
				$('#btGetKftRecon').on('click', function() {
					$('#addFormModal1').modal('show');
				})

				// 1上传按钮点击事件1
				$('#uploadRecon').on('click', function() {
					var add_uploadFile_err = $("#add_uploadFile_err").text();
					var add_tradTime_err = $("#add_tradTime_err").text();
					var add_channelName_err = $("#add_channelName_err").text();
					var upFile = $("#uploadFile").val();
					var checkDate = $("#tradTime_add").val();
					var channelId = $("#uploadChannel").val();
					var file = input.files[0];
					if(add_uploadFile_err != "" || add_tradTime_err != "" || add_channelName_err != "") {
						toastr.error("请选择日期！", '错误信息', {
							timeOut: 3000
						})
						return false;
					} else if(checkDate == "" || channelId == "" || upFile == "") {
						toastr.error("请选择日期！", '错误信息', {
							timeOut: 3000
						})
						return false;
					} else {
						//添加防止重复提交样式
						$('#refreshDiv1').addClass('overlay');
						$('#refreshI2').addClass('fa fa-refresh fa-spin');
						$('#uploadRecon').attr("disabled", "disabled");

						$('#addFormForm1').ajaxSubmit({
							url: settleConfig.api.settlement.settlement_reconciliation_uploadJytRecon,
							success: function(res) {
								//去除重复提交样式
								$('#refreshDiv1').removeClass('overlay');
								$('#refreshI2').removeClass('fa fa-refresh fa-spin');
								$('#uploadRecon').attr("disabled", false);
								$('#addFormModal1').modal('hide');
								if(res.result === "FAIL") {
									toastr.error(res.resultDetial, '错误信息', {
										timeOut: 3000
									})
								}
								if(res.result === "SUCCESS") {
									toastr.info("导入对账文件成功", '提示信息', {
										timeOut: 3000
									})
									$('#formTable').bootstrapTable('refresh');
								}
							}
						})
					}
				})

				//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
				// 选择文件事件
				var input2 = document.getElementById("uploadFile2");

				//文件域选择文件时, 执行readFile函数
				input2.addEventListener('change', readFile2, false);

				//网关支付获取文件名及格式，填充菜单及校验格式
				//				function readFile2() {
				//					$("#add_channelName_err2").text("");
				//					$("#add_tradTime_err2").text("");
				//					$("#add_uploadFile_err2").text("");
				//					var file = this.files[0];
				//					console.log("文件:" + file);
				//					console.log("文件名:" + file.name);
				//					console.log("文件类型:" + file.type);
				//					console.log("文件大小:" + file.size);
				//					var myList = new Array();
				//					myList = file.name.split("_");
				//					console.log("文件名称解析:" + myList);
				//					var fileDate = myList[2].substring(0, 8);
				//					var fileChannel = myList[0];
				//					var fileType = file.type;
				//					if(fileType != "application/vnd.ms-excel") {
				//						$("#add_uploadFile_err2").text("文件格式不符,例“NETPAY_10120000000010004002_20160708TN1001.xls”");
				//					}
				//					if(fileChannel == "NETPAY") {
				//						$("#channelId_add2").val("9");
				//						$("#channelName_add2").val("网银");
				//					} else {
				//						$("#channelId_add2").val("未知");
				//						$("#channelName_add2").val("未知");
				//						$("#add_channelName_err2").text("文件名格式不符,例“NETPAY_10120000000010004002_20160708TN1001.xls”");
				//					}
				//					//判断文件名日期格式是否正确
				//					$("#tradTime_add2").val(fileDate);
				//					if(fileDate.length != 8) {
				//						$("#add_tradTime_err2").text("文件名格式不符,例“NETPAY_10120000000010004002_20160708TN1001.xls”");
				//					}
				//					var nYear = parseInt(fileDate.substring(0, 4), 10);
				//					var nMonth = parseInt(fileDate.substring(4, 6), 10);
				//					var nDay = parseInt(fileDate.substring(6, 8), 10);
				//					if(isNaN(nYear) == true || isNaN(nMonth) == true || isNaN(nDay) == true) {
				//						$("#add_tradTime_err2").text("文件名格式不符,例“NETPAY_10120000000010004002_20160708TN1001.xls”");
				//					}
				//
				//				}

				function readFile2() {
					$("#add_channelName_err2").text("");
					$("#add_tradTime_err2").text("");
					$("#add_uploadFile_err2").text("");
					var file = this.files[0];
					console.log("文件:" + file);
					console.log("文件名:" + file.name);
					console.log("文件类型:" + file.type);
					console.log("文件大小:" + file.size);
					var myList = new Array();
					
					myList = file.name.split("-");
					var fileType = file.type;
					if(fileType != "application/vnd.ms-excel") {
						$("#add_uploadFile_err2").text("文件格式不符,请上传xls格式对账文件,例“2016-10-13290060100010.xls");
					}
					var year = myList[0];
					var month = myList[1];
					var day = myList[2].substring(0, 2);
					var fileDate = year + month + day;
					console.log("对账日期:" + fileDate);
					
					$("#channelId_add2").val("9");
					$("#channelName_add2").val("网银");
					
					//判断文件名日期格式是否正确
					$("#tradTime_add2").val(fileDate);
					if(fileDate.length != 8) {
						$("#add_tradTime_err2").text("文件名格式不符,例“2016-10-13290060100010.xls”");
					}
					var nYear = parseInt(year);
					var nMonth = parseInt(month);
					var nDay = parseInt(day);
					if(isNaN(nYear) == true || isNaN(nMonth) == true || isNaN(nDay) == true) {
						$("#add_tradTime_err2").text("文件名格式不符,例“2016-10-13290060100010.xls”");
					}
					
					//NET开头文件名
//					myList = file.name.split("_");
//					console.log("文件名称解析:" + myList);
//					var fileDate = myList[2].substring(0, 8);
//					var fileChannel = myList[0];
//					var fileType = file.type;
//					if(fileType != "application/vnd.ms-excel") {
//						$("#add_uploadFile_err2").text("文件格式不符,例“NETPAY_10120000000010004002_20160708TN1001.xls”");
//					}
//					if(fileChannel == "NETPAY") {
//						$("#channelId_add2").val("9");
//						$("#channelName_add2").val("网银");
//					} else {
//						$("#channelId_add2").val("未知");
//						$("#channelName_add2").val("未知");
//						$("#add_channelName_err2").text("文件名格式不符,例“NETPAY_10120000000010004002_20160708TN1001.xls”");
//					}
//					//判断文件名日期格式是否正确
//					$("#tradTime_add2").val(fileDate);
//					if(fileDate.length != 8) {
//						$("#add_tradTime_err2").text("文件名格式不符,例“NETPAY_10120000000010004002_20160708TN1001.xls”");
//					}
//					var nYear = parseInt(fileDate.substring(0, 4), 10);
//					var nMonth = parseInt(fileDate.substring(4, 6), 10);
//					var nDay = parseInt(fileDate.substring(6, 8), 10);
//					if(isNaN(nYear) == true || isNaN(nMonth) == true || isNaN(nDay) == true) {
//						$("#add_tradTime_err2").text("文件名格式不符,例“NETPAY_10120000000010004002_20160708TN1001.xls”");
//					}

				}

				// 导入对账文件按钮点击事件
				$('#btGetKftRecon2').on('click', function() {
					$('#addFormModal2').modal('show');
				})

				// 2上传按钮点击事件2
				$('#uploadRecon2').on('click', function() {
						var add_uploadFile_err = $("#add_uploadFile_err2").text();
						var add_tradTime_err = $("#add_tradTime_err2").text();
						var add_channelName_err = $("#add_channelName_err2").text();
						var upFile = $("#uploadFile2").val();
						var checkDate = $("#tradTime_add2").val();
						var channelId = $("#channelId_add2").val();
						var file = input.files[0];
						if(add_uploadFile_err != "" || add_tradTime_err != "" || add_channelName_err != "") {
							toastr.error("请选择日期！", '错误信息', {
								timeOut: 3000
							})
							return false;
						} else if(checkDate == "" || channelId == "" || upFile == "") {
							toastr.error("请选择日期！", '错误信息', {
								timeOut: 3000
							})
							return false;
						} else {
							//添加防止重复提交样式
							$('#refreshDiv1').addClass('overlay');
							$('#refreshI2').addClass('fa fa-refresh fa-spin');
							$('#uploadRecon2').attr("disabled", "disabled");

							$('#addFormForm2').ajaxSubmit({
								url: settleConfig.api.settlement.settlement_reconciliation_uploadJytRecon,
								success: function(res) {
									//去除重复提交样式
									$('#refreshDiv1').removeClass('overlay');
									$('#refreshI2').removeClass('fa fa-refresh fa-spin');
									$('#uploadRecon2').attr("disabled", false);
									$('#addFormModal2').modal('hide');
									if(res.result === "FAIL") {
										toastr.error(res.resultDetial, '错误信息', {
											timeOut: 3000
										})
									}
									if(res.result === "SUCCESS") {
										toastr.info("导入对账文件成功", '提示信息', {
											timeOut: 3000
										})
										$('#formTable').bootstrapTable('refresh');
									}
								}
							})
						}
					})
					//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

				// 对账按钮点击事件
				$('#btKftRecon').on('click', function() {
					var checkDate = verificationDate($("#checkDate").val());
					if(checkDate == "noDate") {
						toastr.error("请选择日期！", '错误信息', {
							timeOut: 3000
						})
						return false;
					} else if(checkDate == "notDate") {
						toastr.error("日期格式有误！", '错误信息', {
							timeOut: 3000
						})
						return false;
					} else {
						//添加防止重复提交样式
						$('#refreshDiv1').addClass('overlay');
						$('#refreshI2').addClass('fa fa-refresh fa-spin');
						$('#btKftRecon').attr("disabled", "disabled");

						var channelId = $("#channelId").val();
						http.post(settleConfig.api.settlement.settlement_reconciliation_doRecon + "?checkDate=" + checkDate + "&channelId=" + channelId, {}, function(res) {
							//去除重复提交样式
							$('#refreshDiv1').removeClass('overlay');
							$('#refreshI2').removeClass('fa fa-refresh fa-spin');
							$('#btKftRecon').attr("disabled", false);

							if(res.result === "FAIL") {
								toastr.error(res.resultDetial, '错误信息', {
									timeOut: 3000
								})
							}
							if(res.result === "SUCCESS") {
								//alert("对账成功");
								toastr.info("对账成功", '提示信息', {
									timeOut: 3000
								})
								$('#formTable').bootstrapTable('refresh');
							}
						})
					}
				})

				// 查询按钮点击事件
				$('#btQuery').on('click', function() {
					var checkDate = verificationDate($("#checkDate").val());
					if(checkDate == "noDate") {
						toastr.error("请选择日期！", '错误信息', {
							timeOut: 3000
						})
						return false;
					} else if(checkDate == "notDate") {
						toastr.error("日期格式有误！", '错误信息', {
							timeOut: 3000
						})
						return false;
					} else {
						$('#formTable').bootstrapTable('refresh');
					}
				})

				//校验日期并转换
				function verificationDate(checkDate) {
					if(checkDate) {
						var items = checkDate.split("-");
						var newStr = items.join("");
						if(newStr.length != 8) {
							return "notDate";
						}
						return newStr;
					} else {
						return "noDate";
					}
				}

				function getQueryParams(val) {
					//var form = document.waitSearchForm
					// 分页数据赋值
					pageOptions.row = val.limit;
					pageOptions.page = parseInt(val.offset / val.limit) + 1;
					return val;
				}

				//对账忽略
				$('#balanceIgnore').on('click', function(e) {
					var data = [];
					$(".selected").each(function() {
						data.push($(this).children("td:first").children(".oid").val());
					})
					console.log('data:' + JSON.stringify(data));
					if(data.length == 0) {
						toastr.info("请选择对账忽略订单列表", '提示信息', {
							timeOut: 3000
						})
						return false;
					}

					confirm.find('.popover-title').html('提示');
					confirm.find('p').html('确定忽略对账？');
					$("#tips_cancle").show();
					$$.confirm({
						container: confirm,
						trigger: this,
						accept: function() {
							$.ajax({
								type: 'post',
								url: settleConfig.api.settlement.settlement_reconciliation_recon_ignore + "?oids=" + data,
								beforeSend: function() {
									$('#refreshDiv1').addClass('overlay');
									$('#refreshI2').addClass('fa fa-refresh fa-spin');
									$("#balanceIgnore").attr("disabled", "disabled");
								},
								success: function(data) {
									$('#formTable').bootstrapTable('refresh', pageOptions);
								},
								complete: function(xhr) {
									$('#refreshDiv1').removeClass('overlay');
									$('#refreshI2').removeClass('fa fa-refresh fa-spin');
									$("#balanceIgnore").removeAttr("disabled");
								}
							})
						}
					})
				})
			}
		}
	})