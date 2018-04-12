/**
 * 宝付对账
 */
define([
	'http',
//	'config',
	'settleConfig',
	'util',
	'extension'
],
function (http, settleConfig, util, $$) {
	return {
		name: 'ucfReconciliation',
		init: function(){
			// 分页配置
            var pageOptions = {
                page: 1,
                row: 10
            }
            //提示框
            var confirm = $('#confirmModal');
            // 初始化数据表格
            var tableConfig = {
                pageNumber: pageOptions.page,
                pageSize: pageOptions.row,
                pagination: true,
                pageList: [10, 20, 30, 50, 100],
                sidePagination: 'server',
                queryParams: getQueryParams,
                onLoadSuccess: function () {
                },
                ajax: function (origin) {
                	var checkDate = verificationDate($("#checkDate").val());
                	var reconStatus = $("#reconStatus").val();
                	var channelId = $("#channelId").val();
                	if(checkDate=="noDate"){
                		checkDate = "20160101";//无数据
                	}
                	statisticsDetail();//统计
                    http.post(settleConfig.api.settlement.settlement_reconciliation_page
                    	+"?channelId="+channelId+"&checkDate="+checkDate+"&reconStatus="+reconStatus
                    	+"&page="+pageOptions.page+"&row="+pageOptions.row,{
                    }, function (rlt) {
                        origin.success(rlt)
                    })
                },
                columns: [
//              	{
//						width: 100,
//		                checkbox: true,
//		                formatter: function(val,row){
//		                	return "<input type='text'  class='oid' hidden='hidden' value='"+row.oid+"' >"
//		                }
//		         	},
                    {field: 'orderId'},
                    {field: 'outsideOrderNo'},
                    {field: 'memberId'},
                    {
                    	align: 'center',
                    	formatter: function(val, row, index) {
                    		return row.channelId==='10'?'宝付（认证支付）':row.channelId==='11'?'宝付（代付）':
                    		row.channelId==='16'?'先锋（认证支付）':row.channelId==='17'?'先锋（代扣）':
                    		row.channelId==='18'?'先锋（认证支付）':'未知';
						}
                    },
//                  {field: 'productId'},
//                  {field: 'transactionCurrency'},
//                  {
//                  	align: 'center',
//                  	formatter: function(val, row, index) {
//                  		return row.transactionCurrency==='156'?'人民币':'CNY'?'人民币':'其他';
//						}
//                  },
                    {
                    	align: 'center',
                    	formatter: function(val, row, index) {
                    		return row.transactionAmount+'元';
						}
                    },
                    {
                    	align: 'center',
                    	formatter: function(val, row, index) {
                    		return row.fee+'元';
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
							switch (row.tradStatus) {
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
                    {field: 'failDetail'},
                    {field: 'errorCode'},
                    {field: 'transactionTime'},
                    {
                    	align: 'center',
						formatter: function(val, row, index) {
							switch (row.reconStatus) {
								case 0:
									return '<span class="text-yellow">未对帐</span>';
								case 1:
									return '<span class="text-green">对账成功</span>';
								case 2:
									return '<span class="text-red">对账失败</span>';
							}
						}
					},
                    {field: 'checkMark'},
                    {
                    	align: 'center',
						formatter: function(val, row, index) {
							switch (row.repairStatus) {
								case 'Y':
									return '<span class="text-green">已处理</span>';
								case 'N':
									return '<span class="text-yellow">未处理</span>';
								default :
									return '-';
							}
						}
					}
                    ]
                
            }
            
            // 初始化时间
			$('#checkDate').val(getNowFormatDate(1));
            // 初始化数据表格
            $('#formTable').bootstrapTable(tableConfig);
			
//			statisticsDetail()//统计
			function statisticsDetail(){
				var statisticsData = {
                	outsideDate : $("#checkDate").val(),
            		channelNo : $("#channelId").val()
            	}
				http.get(settleConfig.api.settlement.settlement_reconciliation_statistics,{
					 data: statisticsData,
                     contentType: 'form'
				}, function(result){
					
					if(result.result =="FAIL"){
						var data = new Date();
						data.setHours(0)
						data.setMinutes(0)
						data.setSeconds(0)
						data = util.table.formatter.timestampToDate(data,"YYYY-MM-DD HH:mm:ss")
						result.res={
							reconciliationDate:'',
							channelName:$("#channelId :selected").text(),
							outsideDate:'',
							outsideCount:0,
							systemCount:0,
							outsideAmount:0,
							systemAmount:0,
							errorCount:0,
							errorAmount:0,
							reconciliationStatus:'未确认完成对账'
						}
					}else if(result.result==="SUCCESS"){
						var reconciliationStatus = result.res.reconciliationStatus
						if(reconciliationStatus == '1'){
							reconciliationStatus = '确认完成对账'
						}else{
							reconciliationStatus = '未确认完成对账'
						}
						var outsideDate =  result.res.outsideDate
						outsideDate = outsideDate.substring(0,10)
						result.res.outsideDate = outsideDate;
						result.res.reconciliationStatus = reconciliationStatus
					}
					
					$$.detailAutoFix($("#statisticsDetail"),result.res);
				})
			}
			
			
            
            // 获取对账流水按钮点击事件
            $('#bfGetKftRecon').on('click', function () {
            	var checkDate = verificationDate($("#checkDate").val());
            	var channelId = $("#channelId").val();
            	if(checkDate=="noDate"){
            		toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}else if(checkDate=="notDate"){
            		toastr.error("日期格式有误！", '错误信息', {
					  	timeOut: 3000
					})
            		return false;
            	}else if(null == channelId){
            		toastr.error("请选择渠道！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}
            	else{
            		//添加防止重复提交样式
  		  			$('#refreshDiv1').addClass('overlay');
					$('#refreshI1').addClass('fa fa-refresh fa-spin');
					$('#bfGetKftRecon').attr("disabled","disabled");
					
	            	http.post(settleConfig.api.settlement.settlement_baofooReconciliation_getRecon+"?checkDate="+checkDate+"&channelId="+channelId, {
	                    }, function (res) {
	                    	//去除重复提交样式
							$('#refreshDiv1').removeClass('overlay');
							$('#refreshI1').removeClass('fa fa-refresh fa-spin');
							$('#bfGetKftRecon').attr("disabled",false);
							
	                    	if(res.result==="FAIL"){
								//alert(res.resultDetial);
								toastr.error(res.resultDetial, '错误信息', {
								  	timeOut: 3000
								})
							}
							if(res.result==="SUCCESS"){
								//alert("对账成功");
								toastr.info("获取对账流水成功", '提示信息', {
								  	timeOut: 3000
								})
								$('#formTable').bootstrapTable('refresh');
								statisticsDetail()//统计
							}
	                    }
	                )
            	}
            })
            
            // 对账按钮点击事件
            $('#bfRecon').on('click', function () {
            	var checkDate = verificationDate($("#checkDate").val());
            	if(checkDate=="noDate"){
            		toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}else if(checkDate=="notDate"){
            		toastr.error("日期格式有误！", '错误信息', {
					  	timeOut: 3000
					})
            		return false;
            	}else{
            		//添加防止重复提交样式
  		  			$('#refreshDiv1').addClass('overlay');
					$('#refreshI1').addClass('fa fa-refresh fa-spin');
					$('#bfRecon').attr("disabled","disabled");
					
            		var channelId = $("#channelId").val();
	            	http.post(settleConfig.api.settlement.settlement_reconciliation_doRecon+"?checkDate="+checkDate+"&channelId="+channelId, {
	                    }, function (res) {
	                    	//去除重复提交样式
							$('#refreshDiv1').removeClass('overlay');
							$('#refreshI1').removeClass('fa fa-refresh fa-spin');
							$('#bfRecon').attr("disabled",false);
							
	                    	if(res.result==="FAIL"){
								toastr.error(res.resultDetial, '错误信息', {
								  	timeOut: 3000
								})
							}
							if(res.result==="SUCCESS"){
								toastr.info("对账成功", '提示信息', {
								  	timeOut: 3000
								})
								$('#formTable').bootstrapTable('refresh');
								statisticsDetail()//统计
							}
	                    }
	                )
            	}
            })
            
            // 查询按钮点击事件
            $('#bfQuery').on('click', function () {
            	var checkDate = verificationDate($("#checkDate").val());
            	if(checkDate=="noDate"){
            		toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}else if(checkDate=="notDate"){
            		toastr.error("日期格式有误！", '错误信息', {
					  	timeOut: 3000
					})
            		return false;
            	}else{
            		$('#formTable').bootstrapTable('refresh');
            		statisticsDetail()//统计
            	}
          	})
            
            // 确认完成对账按钮点击事件
            $('#bfSureRecon').on('click', function () {
            	var checkDate = verificationDate($("#checkDate").val());
            	var channelNo = $("#channelId").val();
            	if(checkDate=="noDate"){
            		toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}else if(checkDate=="notDate"){
            		toastr.error("日期格式有误！", '错误信息', {
					  	timeOut: 3000
					})
            		return false;
            	}else if(null == channelNo){
            		toastr.error("请选择渠道！", '错误信息', {
					  	timeOut: 3000
					})
					return false;
            	}
            	else{
            		//添加防止重复提交样式
  		  			$('#refreshDiv1').addClass('overlay');
					$('#refreshI1').addClass('fa fa-refresh fa-spin');
					$('#bfGetKftRecon').attr("disabled","disabled");
					var outsideDate = $("#checkDate").val();
	            	http.get(settleConfig.api.settlement.settlement_reconciliation_confirmComplete,{
					 	data:{
					 		outsideDate,
					 		channelNo
					 	},
                     	contentType: 'form'
					} ,function (res) {
	                    	//去除重复提交样式
							$('#refreshDiv1').removeClass('overlay');
							$('#refreshI1').removeClass('fa fa-refresh fa-spin');
							$('#bfGetKftRecon').attr("disabled",false);
	                    	if(res.result==="FAIL"){
								toastr.error(res.resultDetial, '错误信息', {
								  	timeOut: 3000
								})
							}
							if(res.result==="SUCCESS"){
								toastr.info("确认完成对账成功", '提示信息', {
								  	timeOut: 3000
								})
								$('#formTable').bootstrapTable('refresh');
								statisticsDetail()//统计
							}
	                    }
	                )
            	}
            })
          
          	//校验日期并转换
          	function verificationDate(checkDate){
	          	if(checkDate){
	          		var items=checkDate.split("-");
	          		var newStr=items.join("");
	          		if(newStr.length!=8){
	          			return "notDate";
	          		}
	          		return newStr;
	          	}else{
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
				$(".selected").each(function(){
					data.push($(this).children("td:first").children(".oid").val());
				})
				console.log('data:'+JSON.stringify(data));
				if(data.length==0){
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
						type:'post',
						url:settleConfig.api.settlement.settlement_reconciliation_recon_ignore+"?oids="+data,
						beforeSend:	function(){
		  		  			$('#refreshDiv1').addClass('overlay');
							$('#refreshI1').addClass('fa fa-refresh fa-spin');						
	  						$("#balanceIgnore").attr("disabled","disabled");
						},			
						success:function(data){
							$('#formTable').bootstrapTable('refresh', pageOptions);
						},
						complete:function(xhr){
							$('#refreshDiv1').removeClass('overlay');
							$('#refreshI1').removeClass('fa fa-refresh fa-spin');	
	                     	$("#balanceIgnore").removeAttr("disabled");
	               		}			
					})	
					}
				})			
			})
			
			function getNowFormatDate(day) {
			    var date = new Date();
			    var seperator1 = "-";
			    var seperator2 = ":";
			    var month = date.getMonth() + 1;
			    var strDate = date.getDate();
			    if (month >= 1 && month <= 9) {
			        month = "0" + month;
			    }
			    if (strDate >= 0 && strDate <= 9) {
			        strDate = "0" + strDate;
			    }
			    var currentdate = date.getFullYear() + seperator1 + month + seperator1 + strDate
			            + " " + date.getHours() + seperator2 + date.getMinutes()
			            + seperator2 + date.getSeconds();
			    if(day===1){
					currentdate = date.getFullYear() + seperator1 + month + seperator1 + strDate;
				}
			    return currentdate;
			}
			
			// 选择文件事件
			var input = document.getElementById("uploadFile");

			//文件域选择文件时, 执行readFile函数
			input.addEventListener('change',readFile,false);

			//获取文件名及格式，填充菜单及校验格式
			function readFile(){
				$("#add_channelName_err").text("");
				$("#add_tradTime_err").text("");
				$("#add_uploadFile_err").text("");
			    var file = this.files[0];
			    console.log("文件:"+file);
			    console.log("文件名:"+file.name);
			    console.log("文件类型:"+file.type);
				console.log("文件大小:"+file.size);
			    var myList=new Array();
			    myList = file.name.split("-");
			    console.log("文件名称解析:"+myList);
			    var fileDate = myList[0];
			    var fileChannel = myList[1];
			    var fileType = file.type;
			    if(fileChannel=="Daifa"){
			    	$("#channelId_add").val("18");
			    	$("#channelName_add").val("先锋（代付）");
			    }else if(fileChannel=="Pay"){
			    	$("#channelId_add").val("16");
			    	$("#channelName_add").val("先锋（认证支付）");
			    }else if(fileChannel=="Fee"||fileChannel=="Refund"){
			    	$("#channelId_add").val("未知");
			    	$("#channelName_add").val("未知");
			    	$("#add_channelName_err").text("请上传代发或支付对账文件");
			    }else{
			    	$("#channelId_add").val("未知");
			    	$("#channelName_add").val("未知");
			    	$("#add_channelName_err").text("文件名格式不符,例“20161013-Daifa-M200000220.txt”");
			    }
			    //判断文件名日期格式是否正确
			    $("#tradTime_add").val(fileDate);
			    if (fileDate.length != 8) {  
					$("#add_tradTime_err").text("文件名格式不符,例“20161013-Daifa-M200000220.txt”");
				}  
				var nYear = parseInt( fileDate.substring( 0, 4 ), 10 ) ;  
				var nMonth = parseInt( fileDate.substring( 4, 6 ), 10 ) ;  
				var nDay = parseInt( fileDate.substring( 6, 8 ), 10 ) ;  
				if( isNaN( nYear ) == true || isNaN( nMonth ) == true || isNaN( nDay ) == true ){  
					$("#add_tradTime_err").text("文件名格式不符,例“20161013-Daifa-M200000220.txt”");
				}
				if(fileType!="text/plain"){
					$("#add_uploadFile_err").text("文件格式不符,请上传txt格式对账文件");
				}
			}
			
			// 导入对账文件按钮点击事件
			$('#btGetucfRecon').on('click', function () {
			 	$('#addFormModal').modal('show');
			 })
			// 上传按钮点击事件
            $('#uploadRecon').on('click', function () {
            	var add_uploadFile_err = $("#add_uploadFile_err").text();
            	var add_tradTime_err = $("#add_tradTime_err").text();
            	var add_channelName_err = $("#add_channelName_err").text();
            	var add_uploadFile_err = $("#add_uploadFile_err").text();
            	var upFile = $("#uploadFile").val();
            	var checkDate = $("#tradTime_add").val();
            	var channelId= $("#channelId_add").val();
            	var file = input.files[0];
            	if(add_uploadFile_err!=""||add_tradTime_err!=""||add_channelName_err!=""||add_uploadFile_err!=""){
            		toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
            		return false;
            	}else if(checkDate==""||channelId==""||upFile==""){
            		toastr.error("请选择日期！", '错误信息', {
					  	timeOut: 3000
					})
            		return false;
            	}else{
            		//添加防止重复提交样式
  		  			$('#refreshDiv1').addClass('overlay');
					$('#refreshI1').addClass('fa fa-refresh fa-spin');
					$('#uploadRecon').attr("disabled","disabled");
					
					$('#addFormForm').ajaxSubmit({
					url: settleConfig.api.settlement.settlement_reconciliation_uploadUcfRecon,
					success: function (res) {
							//去除重复提交样式
							$('#refreshDiv1').removeClass('overlay');
							$('#refreshI1').removeClass('fa fa-refresh fa-spin');
							$('#uploadRecon').attr("disabled",false);
							$('#addFormModal').modal('hide');
							if(res.result==="FAIL"){
								toastr.error(res.resultDetial, '错误信息', {
								  	timeOut: 3000
								})
							}
							if(res.result==="SUCCESS"){
								toastr.info("导入对账文件成功", '提示信息', {
								  	timeOut: 3000
								})
								$('#formTable').bootstrapTable('refresh');
							}
						}
					})
            	}
            })
	   }
	}
})
