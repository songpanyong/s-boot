/**
 * 订单管理
 */
define([
        'http',
        'config',
        'settleConfig',
        'util',
        'extension'
    ], function (http, config, settleConfig, util, $$) {
        return {
            name: 'receiveOrderSettlement',
            init: function () {
                // 分页配置
                var pageOptions = {
                    page: 1,
                    row: 10
                };

                //渠道初始化
                loadChannel();

                // 初始化数据表格
                var tableConfig = {

                    ajax: function (origin) {
                        http.post(settleConfig.api.settlement.settlement_order_page, {
                            data: pageOptions,
                            contentType: 'form'
                        }, function (rlt) {
                            rlt.rows.forEach(function (item) {
                                if (item.status === '1') {
                                    item.failDetail = '';
                                }
                                item.userType = item.userType === 'T1' ? '投资人' : 
                            		item.userType === 'T2' ? '发行人' :'平台';
                                item.type = item.type === '01' ? '充值' : '提现';
                                item.status = item.status === '0' ? '未处理' :
                                    item.status === '1' ? '交易成功' :
                                        item.status === '2' ? '交易失败' :
                                            item.status === '3' ? '交易处理中' :
                                                item.status === '4' ? '超时' : '撤销';
                                if (item.channel != null) {
                                    item.channel = loadChannelName(item.channel);
//                          	item.channel=item.channel==='1'?'快付通':item.channel==='2'?'平安银行':item.channel==='3'?'先锋代扣':item.channel==='4'?'先锋代付':item.channel==='5'?'南粤代扣':item.channel==='6'?'南粤代付':"";
                                }
                                if (item.auditStatus == 1) {
                                    item.auditStatus = "审核中"
                                } else if (item.auditStatus == 2) {
                                    item.auditStatus = "审核通过"
                                } else if (item.auditStatus == 3) {
                                    item.auditStatus = "审核驳回"
                                }
                                item.amount = item.amount + '元';
                                item.fee = item.fee + '元';
                                if(item.auditStatus == "审核中"){
                                	item.updateTime = "";
                                }
                            });
                            origin.success(rlt)
                        })
                    },
                    pageNumber: pageOptions.page,
                    pageSize: pageOptions.row,
                    pagination: true,
                    sidePagination: 'server',
                    pageList: [10, 20, 30, 50, 100],
                    queryParams: getQueryParams,
                    onLoadSuccess: function (resusl) {
                        $('#totalAccount').text('总人次：' + resusl.totalAccount + '次');
//              	$('#totalAmount').text('总金额：'+resusl.totalAmount+'元');
                    },
                    columns: [
                        {
                            width: 100,
                            align: 'left',
                            formatter: function (val, row, index) {
                                return '<div class="func-area"><a href="javascript:void(0)" class="item-detail"  data-toggle="modal">' + row.orderNo + '</a></div>'
                            },
                            events: {
                                //详情
                                'click .item-detail': function (e, val, row) {
                                    if (row.auditStatus === "0") {
                                        var form = document.detailForm;
                                        util.form.reset($(form));
                                        $$.detailAutoFix($('#detailForm'), row); // 自动填充详情
                                        $('#orderAttEdit').modal('show');
                                    } else {
                                        var form = document.detailForm;
                                        util.form.reset($(form));
                                        $$.detailAutoFix($('#orderDetailForm'), row); // 自动填充详情
                                        $('#orderDetail').modal('show');
                                    }

                                }
                            }
                        },
                        {width: 100, align: 'left', field: 'payNo'},
                        {width: 100, align: 'left', field: 'type'},
                        {width: 100, align: 'left', field: 'status'},
                        {
                            width: 100,
                            align: 'left',
                            formatter: function (val, row, index) {
                                return row.type === '申购' ? (row.amount ) : (row.amount);
                            }
                        },
                        {
                            width: 100,
                            align: 'left',
                            formatter: function (val, row, index) {
                                return row.fee;
                            }
                        },
                        {width: 100, align: 'left', field: 'systemSource'},
                        {width: 100, align: 'left', field: 'realName'},
                        {width: 100, align: 'left', field: 'phone'},
                        {width: 100, align: 'left', field: 'userType'},
                        {width: 100, align: 'left', field: 'channel'},
//                  {width: 100,align: 'left', field: 'bankCode'},
//                  {width: 100,align: 'left', field: 'cardNo'},
//                  {width: 100,align: 'left', field: 'failDetail'},
//                  {width: 100,align: 'left', field: 'returnCode'},
//                  {width: 100,align: 'left', field: 'remark'},
//                  {width: 100,align: 'left', field: 'describe'},
                        {width: 100, align: 'left', field: 'receiveTime'},
//                  {width: 100,align: 'left', field: 'updateTime'}
                    ]
                };
                var userId;
                var userName;
                // 获取登录的用户ID
                http.post(config.api.userInfo, {
                    async: false
                }, function (result) {
                    userId = result.oid;
                    userName = result.name;
                });


                //初始化默认时间
//              $('#beginTime').val(getNowFormatDate(1));
//              $('#endTime').val(getNowFormatDate(0));
                // 初始化数据表格
                $('#settlement_page_order').bootstrapTable(tableConfig);
                // 搜索表单初始化
                $$.searchInit($('#settlement_search_order'), $('#settlement_page_order'));

                document.onkeydown = keyDownSearch;

                function keyDownSearch(e) {
                    // 兼容FF和IE和Opera
                    var theEvent = e || window.event;
                    var code = theEvent.keyCode || theEvent.which || theEvent.charCode;
                    if (code == 13) {
                        var sform = document.settlement_search_order;
                        var data = util.form.serializeJson(sform);
                        data.row = 10;
                        data.page = 1;
                        pageOptions = data;
                        var beginTime = data.beginTime;
                        var endTime = data.endTime;
                        var beginDate = new Date(beginTime.replace(/\-/g, "\/"));
                        var endDate = new Date(endTime.replace(/\-/g, "\/"));
                        if (beginTime != "" && endTime != "" && beginDate > endDate) {
                            toastr.error('下单时间区间不能小于0!', '错误信息', {timeOut: 3000});
                            return;
                        }
                        var minMoney = data.limitAmount.trim();
                        var maxAmount = data.maxAmount.trim();
                        if (minMoney != "" && maxAmount != "" && parseInt(minMoney) > parseInt(maxAmount)) {
                            toastr.error('金额范围结束值不能小于开始值!', '错误信息', {timeOut: 3000});
                            return;
                        }
                        $('#settlement_page_order').bootstrapTable('refresh', pageOptions);
                    }
                    return true;
                }

                //组条件查询
                function getQueryParams(val) {
                    var form = document.settlement_search_order;
                    $.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
                    pageOptions.rows = val.limit;
                    pageOptions.page = parseInt(val.offset / val.limit) + 1;
                    var data = util.form.serializeJson(form);
                    var beginTime = data.beginTime;
                    var endTime = data.endTime;
                    if (beginTime != "" && endTime != "") {
                        var beginDate = new Date(beginTime.replace(/\-/g, "\/"));
                        var endDate = new Date(endTime.replace(/\-/g, "\/"));
                        if (beginDate > endDate) {
                            toastr.error('不能小于下单开始时间!', '错误信息', {timeOut: 3000});
                            return;
                        }
                    }
                    return val
                }

                //  select 改变
                $('#order_type,#order_status,#order_reconStatus').on('change', function (e) {
                    e.preventDefault();
                    $(this).children('option:selected').val();
                    var sform = document.settlement_search_order;
                    var data = util.form.serializeJson(sform);
                    data.row = 10;
                    data.page = 1;
                    pageOptions = data;
                    $('#settlement_page_order').bootstrapTable('refresh', pageOptions);
                });

                // 订单录入，弹出新建页面
                $('#order_entry').on('click', function (e) {
                    var form = document.addOrderForm;
                    util.form.reset($(form));
                    $(form).validator('destroy');
                    util.form.validator.init($(form));
                    $("#new_channel").html("");
                    http.post(settleConfig.api.settlement.settlement_getChannelPage,
                        {data: {page: 1, rows: 100, tradeType: "01"}, contentType: 'form'},
                        function (data) {
                            data.rows.forEach(function (item) {
                                var channel = $("#channel" + item.channelNo).html();
                                if (typeof(channel) === "undefined") {
                                    $("#new_channel").append("<option value=" + item.channelNo + " id=channel" + item.channelNo + ">" + item.channelName + "</option>");
                                }
                            })
                        });
                    $('#addOrderModal').modal("show");
                });

                // 根据订单类型控制手续费和支付渠道的显示
                $('#new_type').on('change', function () {
                    var form = document.addOrderForm;
                    $("#new_channel").html("");
                    if (form.type.value === "02") {
                        http.post(settleConfig.api.settlement.settlement_getChannelPage,
                            {data: {page: 1, rows: 100, tradeType: "02"}, contentType: 'form'},
                            function (data) {
                                data.rows.forEach(function (item) {
                                    var channel = $("#channel" + item.channelNo).html();
                                    if (typeof(channel) === "undefined") {
                                        $("#new_channel").append("<option value=" + item.channelNo + " id=channel" + item.channelNo + ">" + item.channelName + "</option>");
                                    }
                                })
                            });
                        $('#new_fee').removeAttr("disabled");
                        $('#new_fee').attr("required", "required");
                        $('#new_fee_label').append("<span id=\"valierr\" style=\"color:#dd4b39;font-size:16px;position:absolute;\">＊</span>");
                        $('#new_amount').attr("placeholder", "请输入实际到账金额");

                    } else {
                        http.post(settleConfig.api.settlement.settlement_getChannelPage,
                            {data: {page: 1, rows: 100, tradeType: "01"}, contentType: 'form'},
                            function (data) {
                                data.rows.forEach(function (item) {
                                    var channel = $("#channel" + item.channelNo).html();
                                    if (typeof(channel) === "undefined") {
                                        $("#new_channel").append("<option value=" + item.channelNo + " id=channel" + item.channelNo + ">" + item.channelName + "</option>");
                                    }
                                })
                            });
                        $('#new_fee').attr("disabled", "disabled");
                        $('#new_fee').removeAttr("required");
                        $('#new_fee').val('');
                        $('#new_fee_label').html("手续费（元）");
                        $('#new_amount').attr("placeholder", "请输入交易金额");
                    }
                });

                // 组装需要send的数据
                function addData() {
                    var form = document.addOrderForm;
                    var data = {};
                    data.page = 1;
                    data.rows = 100;
                    data.payNo = form.payNo.value;
                    data.type = form.type.value;
                    data.amount = form.amount.value;
                    data.receiveTime = form.receiveTime.value;
                    data.channel = form.channel.value;
                    data.memberId = form.memberId.value;
                    data.operatorName = userName;
                    data.operatorId = userId;
                    data.remark = form.remark.value;
                    // 充值
                    if (form.type.value === "01") {
                        data.fee = 0;
                        // 当充值时，收款人是：phone，付款人是：cardNo
                        data.phone = form.payeeAccount.value;
                        data.cardNo = form.payerAccount.value;
                    } else {
                        data.fee = (form.fee.value.length === 0 ? 0 : form.fee.value);
                        //  当提现时，收款人是：cardNo，付款人是：phone
                        data.cardNo = form.payeeAccount.value;
                        data.phone = form.payerAccount.value;
                    }
                    // console.log(data);
                    return data;
                }

                // 新建表单数据校验
                function checkAddOrder() {
                    var form = document.addOrderForm;

                    var isAmount = /^\d+(\.\d{1,2})?$/;
                    if (!isAmount.test(form.amount.value) || form.amount.value <= 0) {
                        toastr.error('订单金额格式错误。金额大于0，最多2位小数！', '错误信息', {
                            timeOut: 5000
                        });
                        return true;
                    }
                    // 充值
                    if (form.type.value === "01") {
                        // 当充值时，收款人是：phone，付款人是：cardNo
                        var phone = form.payeeAccount.value;
                        if (phone.length != 11 || (phone.substring(0, 1) != "1" && phone.substring(0, 1) != "2")) {
                            toastr.error('请输入正确的用户账号！', '错误信息', {
                                timeOut: 5000
                            });
                            return true;
                        }
                    } else {
                        //  当提现时，收款人是：cardNo，付款人是：phone
                        var phone = form.payerAccount.value;
                        if (phone.length != 11 || (phone.substring(0, 1) != "1" && phone.substring(0, 1) != "2")) {
                            toastr.error('请输入正确的用户账号！', '错误信息', {
                                timeOut: 5000
                            });
                            return true;
                        }
                        if (!isAmount.test(form.fee.value) || form.fee.value < 0) {
                            toastr.error('手续费格式错误。金额大于等于0，最多2位小数！', '错误信息', {
                                timeOut: 5000
                            });
                            return true;
                        }
                    }
                    return false;
                }

                // 点击 新建 保存按钮
                $('#addOrderSubmit').on('click', function () {
                    $('#addOrderSubmit').attr('disabled', true);
                    var form = document.addOrderForm;
                    util.form.validator.init($(form));
                    if (!$(form).validator('doSubmitCheck')) {
                        $('#addOrderSubmit').attr('disabled', false);
                        return;
                    }
                    if (checkAddOrder()) {
                        $('#addOrderSubmit').attr('disabled', false);
                        return;
                    }
                    http.post(settleConfig.api.settlement.settlement_order_checkInOrder,
                        {data: addData(), contentType: 'form'},
                        function (rlt) {
                            if (rlt.returnCode == "0000") {
                                $('#settlement_page_order').bootstrapTable('refresh');
                                toastr.success('操作成功', '提示', {
                                    timeOut: 3000
                                });
                                $("#addOrderModal").modal('hide');
                            } else {
                                toastr.error(rlt.errorMessage, '错误信息', {
                                    timeOut: 3000
                                })
                            }
                            setTimeout(function () {
                                $('#addOrderSubmit').attr('disabled', false);
                            }, 500);
                        });
                    $(form).validator('destroy');
                });

                //搜索
                $('#order_search').on('click', function (e) {
                    e.preventDefault();
                    var sform = document.settlement_search_order;
                    var data = util.form.serializeJson(sform);
                    data.row = 10;
                    data.page = 1;
                    pageOptions = data;
                    var beginTime = data.beginTime;
                    var endTime = data.endTime;
                    var beginDate = new Date(beginTime.replace(/\-/g, "\/"));
                    var endDate = new Date(endTime.replace(/\-/g, "\/"));
                    if (beginTime != "" && endTime != "" && beginDate > endDate) {
                        toastr.error('下单时间区间不能小于0!', '错误信息', {timeOut: 3000});
                        return;
                    }
                    var minMoney = data.limitAmount.trim();
                    var maxAmount = data.maxAmount.trim();
                    if (minMoney != "" && maxAmount != "" && parseInt(minMoney) > parseInt(maxAmount)) {
                        toastr.error('金额范围结束值不能小于开始值!', '错误信息', {timeOut: 3000});
                        return;
                    }
                    $('#settlement_page_order').bootstrapTable('refresh', pageOptions);
                });

                //清空
                $('#order_reset').on('click', function (e) {
                    e.preventDefault();
                    var sform = document.settlement_search_order;
                    util.form.reset($(sform));
                    $('#settlement_page_order').bootstrapTable('refresh', pageOptions);
                });


                $('#settlement_order_down').on('click', function (e) {
                    e.preventDefault();
                    var sform = document.settlement_search_order;
                    var data = util.form.serializeJson(sform);
                    data = $.param(data);
                    var Url = settleConfig.api.settlement.settlement_order_down + '?' + data;
                    var div = document.getElementById("ifile");
                    var iframe = document.createElement("iframe");
                    iframe.src = Url;
                    div.appendChild(iframe);
                    $("#dialog1").dialog('open');
//              window.location.href=settleConfig.api.settlement.settlement_order_down+'?realName='+pageOptions.realName+'&type='+pageOptions.type+'&status='+pageOptions.status+'&beginTime='+pageOptions.beginTime+'&endTime='+pageOptions.endTime+'&limitAmount='+pageOptions.limitAmount+'&maxAmount='+pageOptions.maxAmount+'&orderNo='+pageOptions.orderNo+'&row=10&page=1';
//              window.open(window.location.href=settleConfig.api.settlement.settlement_order_down+'?realName='+pageOptions.realName+'&type='+pageOptions.type+'&status='+pageOptions.status+'&beginTime='+pageOptions.beginTime+'&endTime='+pageOptions.endTime+'&limitAmount='+pageOptions.limitAmount+'&maxAmount='+pageOptions.maxAmount+'&orderNo='+pageOptions.orderNo+'&row=10&page=1');
                });

                $('#limitAmount,#maxAmount').on('keyup', function (e) {
                    e.preventDefault();
                    var limitAmount = $('#limitAmount').val(), maxAmount = $('#maxAmount').val();
                    if (((limitAmount != "" && limitAmount != null) || (maxAmount != "" && maxAmount != null)) && (!/^\d{0,8}(\.[0-9]{0,2})?$/.test(this.value))) {
                        toastr.error('只能输入数字，小数点后只能保留两位，最大8位整数');
                        this.value = '';
                    }
                });

                //加载渠道所有渠道
                function loadChannel() {
                    http.post(settleConfig.api.settlement.settlement_getChannelPage,
                        {data: pageOptions, contentType: 'form'},
                        function (data) {
                            data.rows.forEach(function (item) {
                                $("#ul_Channel_Name").append("<li id=channel_Name" + item.channelNo + ">" + item.channelName + "</option>");
                            })
                        })
                }

                //加载渠道名称
                function loadChannelName(channelId) {
                    return $("#channel_Name" + channelId).html();
                }

                function getFormatDate(day) {
                    var date = new Date();
                    var seperator2 = ":";
                    var currentdate = day + " " + date.getHours() + seperator2 +
                        date.getMinutes() + seperator2 + date.getSeconds();
                    return currentdate;
                }
            }
        }
    }
)
