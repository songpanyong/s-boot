/**
 * 用户管理
 */
define([
        'http',
        'config',
        'settleConfig',
        'util',
        'extension'
    ],
    function (http, config, settleConfig, util, $$) {
        return {
            name: 'exceptionBillDeal',
            init: function () {

                // 三方支付-异常单据处理
                var pageOptions = {
                    page: 1,
                    rows: 10,
                    channelNo: '',//渠道编码
                    orderNo: '',//订单号
                    payNo: '',//支付流水号
                    outsideOrderNo: '',//三方订单号
                    orderType: '',//订单类型
                    userName: '',//用户名称
                    userPhone: '',//用户账户
                    errorType: '',//异常类型
                    beginTime: '',//开始时间
                    endTime: '',//结束时间
                    offset: ''
                };

                var tableConfig = {
                    ajax: function (origin) {
                        http.post(settleConfig.api.settlement.settlement_reconciliation_records_page, {
                            data: pageOptions,
                            contentType: 'form'
                        }, function (rlt) {
                            origin.success(rlt)
                        })
                    },
                    pageSize: pageOptions.rows,
                    pagination: true,
                    sidePagination: 'server',
                    pageList: [10, 20, 30, 50, 100],
                    queryParams: getQueryParams,
                    onClickCell: function (field, value, row, $element) {
                        switch (field) {
                            case 'applyRecord':
                                queryInfo(value, row);
                                break
                        }
                    },
                    columns: [
                        {
                            field: 'orderNo'
                        }, //订单号
                        {field: 'payNo'}, //支付流水号
                        {
                            field: 'orderType',
                            formatter: function (value, row, index) {
                                var _val = value;
                                if (value == "01") {
                                    _val = "充值"
                                } else if (value == "02") {
                                    _val = "提现"
                                } else {
                                    _val = '-'
                                }
                                return _val;
                            }
                        },// 订单类型
                        {
                            field: 'orderStatus',
                            formatter: function (value, row, index) {
                                var _val = '';
                                switch (value) {
                                    case '0':
                                        _val = '未处理';
                                        break;
                                    case '1':
                                        _val = '交易成功';
                                        break;
                                    case '2':
                                        _val = '交易失败';
                                        break;
                                    case '3':
                                        _val = '交易处理中';
                                        break;
                                    case '4':
                                        _val = '超时';
                                        break;
                                    default:
                                        _val = '-';
                                        break;
                                }
                                return _val;
                            }
                        },// 订单状态
                        {field: 'amount', class: 'currency align-right'}, //订单金额（元）
                        {field: 'userName'}, //用户名称
                        {field: 'userPhone'}, ///用户账户
                        {field: 'channelName'}, //支付通道

                        {field: 'outsideOrderNo'}, //三方订单号
                        {
                            field: 'orderType',
                            formatter: function (value, row, index) {
                                var _val = value;
                                if (row.outsideOrderNo) {
                                    if (value == "01") {
                                        _val = "充值"
                                    } else if (value == "02") {
                                        _val = "提现"
                                    }
                                } else {
                                    _val = '-'
                                }
                                return _val;
                            }
                        }, //三方订单类型
                        {
                            field: 'outsideOrderStatus',
                            formatter: function (value, row, index) {
                                var _val = '-';
                                if (value == '1') {
                                    _val = '交易成功'
                                } else if (value == '2') {
                                    _val = '交易失败'
                                }
                                return _val;
                            }
                        }, //三方订单状态
                        {field: 'outsideAmount', class: 'currency align-right'}, //三方订单金额（元）
                        {field: 'memberId'}, //三方商户号

                        {field: 'errorType'}, //异常类型
                        {field: 'errorStatus'}, //异常处理状态
                        {
                            field: 'errorResult',
                            formatter: function (value, row, index) {
                                var _val = value;
                                if (undefined == value) {
                                    _val = '-';
                                } else if (('' + value).length > 10) {
                                    _val = value.substr(0, 10) + "……"
                                }
                                return "<span title='" + value + "'>" + _val + "</span>"
                            }
                        },// 异常处理结果
                        {
                            field: 'applyRecord',
                            formatter: function (value, row, index) {
                                var _val = '-';
                                if (undefined == value) {
                                    _val = '-';
                                } else if (value == 'Y') {
                                    _val = "<span style='color:#169BE2;cursor:pointer'>详情</span>"
                                } else if (value == 'N') {
                                    _val = "-"
                                }
                                return _val;
                            }
                        },// 申购记录
                        {field: 'orderTime'},// 下单时间
                        {field: 'updateTime'},// 更新时间
                        {
                            field: 'remark',
                            formatter: function (value, row, index) {
                                if (undefined == value || null == value) {
                                    value = "-";
                                }
                                var _val = value;
                                if (('' + value).length > 10) {
                                    _val = value.substr(0, 10) + "……"
                                }
                                return "<span title='" + value + "'>" + _val + "</span>"
                            }
                        },// 操作
                        {
                            width: '200',
                            align: 'center',
                            formatter: function (val, row, index) {
                                var subButtons = [];
                                if (row.errorStatus == "等待人工处理") {
                                    if (row.errorType == "三方订单多单" || row.errorType == "金额不匹配") {
                                        subButtons.push({
                                            text: '复核完成',
                                            type: 'button',
                                            class: 'checkComplete',
                                            isRender: true
                                        })
                                    } else {
                                        subButtons.push({
                                            text: '确认成功',
                                            type: 'button',
                                            class: 'confirmSuccess',
                                            isRender: true
                                        });
                                        subButtons.push({
                                            text: '确认失败',
                                            type: 'button',
                                            class: 'configrmFail',
                                            isRender: true
                                        })
                                    }
                                }
                                return util.table.formatter.generateButton(subButtons, 'reconciliationTable')
                            },
                            events: {
                                'click .checkComplete': function (e, val, row) {//复核完成
                                    var checkCompleteApproveForm = $("#checkCompleteApproveForm");
                                    checkCompleteApproveForm.validator('destroy');
                                    util.form.validator.init(checkCompleteApproveForm);
                                    checkCompleteApproveForm.clearForm();
                                    checkCompleteApproveForm.find("[name='oid']").val(row.oid);
                                    $("#checkCompleteModal").modal('show')
                                },
                                'click .confirmSuccess': function (e, val, row) {//确认成功
                                    var confirmSuccessForm = $("#confirmSuccessForm");
                                    confirmSuccessForm.validator('destroy');
                                    util.form.validator.init(confirmSuccessForm);
                                    confirmSuccessForm.clearForm();
                                    confirmSuccessForm.find("[name='oid']").val(row.oid);
                                    $("#confirmSuccessModal").modal('show')
                                },
                                'click .configrmFail': function (e, val, row) {//确认失败
                                    var configrmFailForm = $("#configrmFailForm");
                                    if (row.applyRecord == "Y") {
                                        $("#configrmFailModal").find('.modal-title').html('确认失败并尝试扣款');
                                    }
                                    configrmFailForm.validator('destroy');
                                    util.form.validator.init(configrmFailForm);
                                    configrmFailForm.clearForm();
                                    configrmFailForm.find("[name='oid']").val(row.oid);
                                    $("#configrmFailModal").modal('show')
                                }
                            }
                        }]
                };
                //购买记录
                var pageOptions2 = {
                    page: 1,
                    rows: 30,
                    offset: 0,
                    orderTimeBegin: "",
                    orderTimeEnd: "",
                    order: "desc",
                    investorOid: '',
                    orderType: "invest"
                };
                var tableConfig2 = {
                    ajax: function (origin) {
                        http.post(settleConfig.api.settlement.settlement_reconciliation_records_mimosa, {
                            data: pageOptions2,
                            contentType: 'form'
                        }, function (rlt) {
                            origin.success(rlt)
                        })
                    },
                    pageSize: pageOptions2.rows,
                    pagination: true,
                    sidePagination: 'server',
                    pageList: [100],
                    queryParams: pageOptions2,
                    columns: [
                        {
                            width: 60,
                            align: 'center',
                            formatter: function (val, row, index) {
                                return pageOptions2.offset + index + 1
                            }
                        },//序号
                        {field: 'orderCode'}, //订单号
                        {field: 'realName'}, //投资者名称
                        {field: 'phoneNum'},// 投资者账号
                        {field: 'orderAmount', class: 'currency align-right'},//订单金额(元)
                        {field: 'couponTypeDisp'}, //卡券类型
                        {field: 'couponAmount'}, //奖励
                        {field: 'payAmount', class: 'currency align-right'}, //实付金额(元)
                        {field: 'orderTypeDisp'},//订单类型
                        // {field: 'orderType'},//订单类型
                        {field: 'orderStatusDisp'},//订单状态
                        {field: 'productName'}, //产品名称
                        {field: 'orderTime'}, //创建时间
                        {field: 'createManDisp'},//创建人
                        {
                            field: 'publisherClearStatusDisp',
                            formatter: function (val, row) {
                                return val && row.orderType !== 'fastRedeem' ? val : '--'
                            }
                        }, //发行人清算状态
                        {
                            field: 'publisherConfirmStatusDisp',
                            formatter: function (val, row) {
                                return val && row.orderType !== 'fastRedeem' ? val : '--'
                            }
                        }, //发行人确认状态
                        {
                            field: 'publisherCloseStatusDisp', formatter: function (val, row) {
                            return val && row.orderType !== 'fastRedeem' ? val : '--'
                        }
                        } //发行人结算状态
                    ]
                };
                // 初始化权限表格
                $('#reconciliationTable').bootstrapTable(tableConfig);
                // 初始化权限表格搜索表单
                $$.searchInit($('#searchForm'), $('#reconciliationTable'));

                //确认成功
                $("#confirmSuccessSubmit").on('click', function () {
                    $('#confirmSuccessSubmit').attr('disabled', true);
                    var confirmSuccessForm = document.confirmSuccessForm;
                    if (checkRemark(confirmSuccessForm.remark.value)) {
                        $('#confirmSuccessSubmit').attr('disabled', false);
                        return;
                    }
                    http.post(settleConfig.api.settlement.settlement_reconciliation_success, {
                        data: {
                            oid: confirmSuccessForm.oid.value,
                            remark: confirmSuccessForm.remark.value
                        },
                        contentType: 'form'
                    }, function (rlt) {
                        $("#confirmSuccessModal").modal('hide');
                        $('#reconciliationTable').bootstrapTable('refresh');
                        if (rlt.returnCode == "0000") {
                            toastr.success('操作成功', '提示', {
                                timeOut: 3000
                            })
                        } else {
                            toastr.error(rlt.errorMessage, '错误信息', {
                                timeOut: 3000
                            })
                        }
                        setTimeout(function () {
                            $('#confirmSuccessSubmit').attr('disabled', false);
                        }, 500);
                    })
                });

                //第一次确认失败
                $("#configrmFailSubmit").on('click', function () {
                    $('#configrmFailSubmit').attr('disabled', true);
                    var configrmFailForm = document.configrmFailForm;
                    if (checkRemark(configrmFailForm.remark.value)) {
                        $('#configrmFailSubmit').attr('disabled', false);
                        return;
                    }
                    http.post(settleConfig.api.settlement.settlement_reconciliation_failedWithTryToCharge, {
                        data: {
                            oid: configrmFailForm.oid.value,
                            remark: configrmFailForm.remark.value
                        },
                        contentType: 'form'
                    }, function (rlt) {
                        $('#reconciliationTable').bootstrapTable('refresh');
                        if (rlt.returnCode == "0000") {
                            $("#configrmFailModal").modal('hide');
                            toastr.success('操作成功', '提示', {
                                timeOut: 3000
                            })
                        } else if (rlt.returnCode == "9007") {
                            $("#configrmFailModal").modal('hide');
                            var reConfirmFailModal = $("#reConfirmFailModal");
                            reConfirmFailModal.find('.modal-title').html('提示');
                            reConfirmFailModal.find('.modal-body h5').html('扣款失败请选择是否“确认失败”');
                            reConfirmFailModal.find('[name=oid]').val(configrmFailForm.oid.value);
                            $('#reConfirmFailCancel').hide().siblings().show();
                            reConfirmFailModal.modal('show');
                            $('#reConfirmFail').show();
                        } else {
                            toastr.error(rlt.errorMessage, '错误信息', {
                                timeOut: 3000
                            })
                        }
                        setTimeout(function () {
                            $('#configrmFailSubmit').attr('disabled', false);
                        }, 500);
                    })
                });
                //第二次确认失败
                $("#reConfirmFailSubmit").on('click', function () {
                    $('#reConfirmFailSubmit').attr('disabled', true);
                    var configrmFailForm = document.configrmFailForm;
                    http.post(settleConfig.api.settlement.settlement_reconciliation_failedWithNoCharge, {
                        data: {
                            oid: configrmFailForm.oid.value,
                            remark: configrmFailForm.remark.value
                        },
                        contentType: 'form'
                    }, function (rlt) {
                        $("#reConfirmFailModal").modal('hide');
                       $('#reconciliationTable').bootstrapTable('refresh');
                       if (rlt.returnCode == "0000") {
                           toastr.success('操作成功', '提示', {
                                timeOut: 3000
                            })
                        } else {
                            toastr.error(rlt.errorMessage, '错误信息', {
                                timeOut: 3000
                            })
                        }
                        setTimeout(function () {
                            $('#reConfirmFailSubmit').attr('disabled', false);
                        }, 500);
                    })
                });
                //复核成功
                $("#checkCompleteApproveSubmit").on('click', function () {
                    $('#checkCompleteApproveSubmit').attr('disabled', true);
                    var checkCompleteApproveForm = document.checkCompleteApproveForm;
                    if (checkRemark(checkCompleteApproveForm.remark.value)) {
                        $('#checkCompleteApproveSubmit').attr('disabled', false);
                        return;
                    }
                    http.post(settleConfig.api.settlement.settlement_reconciliation_composite, {
                        data: {
                            oid: checkCompleteApproveForm.oid.value,
                            remark: checkCompleteApproveForm.remark.value
                        },
                        contentType: 'form'
                    }, function (rlt) {
                        $("#checkCompleteModal").modal('hide');
                        $('#reconciliationTable').bootstrapTable('refresh');
                        if (rlt.returnCode == "0000") {
                            toastr.success('操作成功', '提示', {
                                timeOut: 3000
                            })
                        } else {
                            toastr.error(rlt.errorMessage, '错误信息', {
                                timeOut: 3000
                            })
                        }
                        setTimeout(function () {
                            $('#checkCompleteApproveSubmit').attr('disabled', false);
                        }, 500);
                    })

                });

                //条件搜索
                function getQueryParams(val) {
                    var form = document.searchForm;
                    var beginTime = form.beginTime.value;
                    if (beginTime) beginTime += ' 00:00:00';
                    var endTime = form.endTime.value;
                    if (endTime) endTime += ' 23:59:59';
                    // 分页配置
                    pageOptions = {
                        page: parseInt(val.offset / val.limit) + 1,
                        rows: val.limit,
                        channelNo: form.channelNo.value,//渠道编码
                        orderNo: form.orderNo.value,//订单号
                        payNo: form.payNo.value,//支付流水号
                        outsideOrderNo: form.outsideOrderNo.value,//三方订单号
                        orderType: form.orderType.value,//订单类型
                        userName: form.userName.value,//用户名称
                        userPhone: form.userPhone.value,//用户账户
                        errorType: form.errorType.value,//异常类型
                        beginTime: beginTime,//开始时间
                        endTime: endTime//结束时间
                    };
                    return pageOptions
                }

                //查看详情
                function queryInfo(value, row) {
                    if (value == 'Y') {
                        pageOptions2.orderTimeBegin = util.table.formatter.timestampToDate(row.orderTime, "YYYY-MM-DD hh:mm:ss");
                        pageOptions2.investorOid = row.userOid;
                        pageOptions2.orderTimeEnd = row.createTime;
                        $('#applyRecordDetailTable').bootstrapTable(tableConfig2);
                        $('#applyRecordDetailTable').bootstrapTable('refresh');
                        $("#applyRecordDetailModal").modal('show');
                    }
                }

                function checkRemark(str) {
                    if (str.length > 0) {
                        return false;
                    }
                    toastr.error("备注不能为空！", '错误信息', {
                        timeOut: 3000
                    });
                    return true;
                }

            }
        }
    });