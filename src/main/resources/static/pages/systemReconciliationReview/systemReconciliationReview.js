define([
    'http',
    'config',
    'settleConfig',
    'util',
    'extension'
], function (http, config, settleConfig, util, $$) {
    return {
        name: 'systemReconciliationReview',
        init: function () {
            // 公用表单按键绑定
            function searchInitWithCheck(form, table, check) {
                var commonInputs = form.find('input:text:not(.datepicker)');
                var datepickers = form.find('input:text.datepicker');
                var selects = form.find('select:not(.search-omit)');
                var radios = form.find('input:radio');

                commonInputs.each(function (index, item) {
                    $(item)
                        .on('focus', function () {
                            $(document).on('keyup.gridSearch', function (e) {
                                if (e.keyCode === 13) {
                                    if (check()) return;
                                    table.bootstrapTable('refresh')
                                }
                            })
                        })
                        .on('blur', function () {
                            $(document).off('keyup.gridSearch')
                        })
                })

                datepickers.each(function (index, item) {
                    $(item)
                        .datetimepicker({
                            showClear: true
                        })
                        .on('dp.hide', function () {
                            if (check()) return;
                            table.bootstrapTable('refresh')
                        })
                });

                selects.each(function (index, item) {
                    $(item).on('change.gridSearch', function () {
                        if (check()) return;
                        table.bootstrapTable('refresh')
                    })
                });

                radios.each(function (index, item) {
                    // icheck checked事件
                    $(item).on('ifChecked', function (e) {
                        if (check()) return;
                        table.bootstrapTable('refresh')
                    })
                })
            }

            // 初始化加载支付渠道
            http.post(settleConfig.api.settlement.settlement_getChannelPage,
                {data: {page: 1, rows: 100}, contentType: 'form'},
                function (data) {
                    data.rows.forEach(function (item) {
                        var channel = $("#channel" + item.channelNo).html();
                        if (typeof(channel) === "undefined") {
                            $("#new_channel").append("<option value=" + item.channelNo + " id=channel" + item.channelNo + ">" + item.channelName + "</option>");
                        }
                    })
                });
            //  ============== 订单审核 start ======================
            // 清空按钮
            $('#search_order_review_reset').on('click', function (e) {
                var form = document.searchOrderReviewForm;
                util.form.reset($(form))
            });
            // 搜索按钮
            $('#search_order_review_search').on('click', function (e) {
                if (checkOrderReviewForm()) return;
                $('#orderReviewTable').bootstrapTable("refresh");
            });
            // 表单参数检查
            var checkOrderReviewForm = function (form) {
                var form = document.searchOrderReviewForm;
                var limitAmount = form.limitAmount.value.trim();
                var maxAmount = form.maxAmount.value.trim();
                if (limitAmount.length > 0 && ( isNaN(new Number(limitAmount)) || parseInt(limitAmount) < 0)) {
                    toastr.error('金额格式错误或小于0!', '错误信息', {timeOut: 3000});
                    return true;
                }
                if (maxAmount.length > 0 && ( isNaN(new Number(maxAmount)) || parseInt(maxAmount) < 0)) {
                    toastr.error('金额格式错误或小于0!', '错误信息', {timeOut: 3000});
                    return true;

                }
                if (parseInt(limitAmount) > parseInt(maxAmount)) {
                    toastr.error('最大金额小于最小金额!', '错误信息', {timeOut: 3000});
                    return true;
                }
                return false;
            };
            // 绑定按键搜索
            searchInitWithCheck($('#searchOrderReviewForm'), $('#orderReviewTable'), checkOrderReviewForm);
            var orderReviewDataOptions = {
                page: 1,
                auditStatusList: "1",
                rows: 10
            };

            //组条件查询
            function orderReviewGetQueryParams(val) {
                var form = document.searchOrderReviewForm;
                orderReviewDataOptions.rows = val.limit;
                orderReviewDataOptions.page = parseInt(val.offset / val.limit) + 1;
                $.extend(orderReviewDataOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
                return val;
            }

            var orderReviewTableConfig = {
                ajax: function (origin) {
                    http.post(settleConfig.api.settlement.settlement_order_page, {
                        data: orderReviewDataOptions,
                        contentType: 'form',
                        traditional: true
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
                                item.channel = $("#channel" + item.channel).html();
                            }
                            if (item.auditStatus == 1) {
                                item.auditStatus = "审核中"
                            } else if (item.auditStatus == 2) {
                                item.auditStatus = "审核通过"
                            } else if (item.auditStatus == 3) {
                                item.auditStatus = "审核驳回"
                            }
//                          item.amount = item.amount + '元';
//                          item.fee = item.fee + '元';
                            if(item.amount.toString().indexOf('元') == -1){
								item.amount = item.amount +'元';
							}
                            if( item.fee.toString().indexOf('元') == -1){
								 item.fee =  item.fee +'元';
							}
                            if(item.auditStatus == "审核中"){
                            	item.updateTime = "";
                            }
                        });
                        origin.success(rlt)
                    })
                },
                pageNumber: orderReviewDataOptions.page,
                pageSize: orderReviewDataOptions.row,
                pagination: true,
                sidePagination: 'server',
                pageList: [10, 20, 30, 50, 100],
                queryParams: orderReviewGetQueryParams,
                onLoadSuccess: function (result) {

                },
                columns: [
                    {
                        width: 100,
                        align: 'left',
                        formatter: function (val, row, index) {
                            return '<div class="func-area"><a href="javascript:void(0)" class="item-detail"  data-toggle="modal">' + row.orderNo + '</a></div>';
                        },
                        events: {
                            //详情
                            'click .item-detail': function (e, val, row) {
                                var form = document.detailForm;
                                util.form.reset($(form));
                                $$.detailAutoFix($('#detailForm'), row); // 自动填充详情
                                $('#orderDetail').modal('show');
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
                            return row.amount;
                        }
                    },
                    {
                        width: 60,
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
                    {width: 100, align: 'left', field: 'createTime'},
                    {
                        width: 100, align: 'left', field: 'auditStatus',
                        formatter: function (val, row, index) {
                            if (row.auditStatus == '审核中') {
                                var subButtons = [];
                                subButtons.push({
                                    text: '通过',
                                    type: 'button',
                                    class: 'confirmSuccess',
                                    isRender: true
                                });
                                subButtons.push({
                                    text: '驳回',
                                    type: 'button',
                                    class: 'confirmFail',
                                    isRender: true
                                });
                                return util.table.formatter.generateButton(subButtons, 'orderReviewTable');
                            }
                        },
                        events: {
                            'click .confirmSuccess': function (e, val, row) {
                                var orderAuditForm = $("#orderAuditSuccessForm");
                                orderAuditForm.validator('destroy');
                                util.form.validator.init(orderAuditForm);
                                orderAuditForm.clearForm();
                                orderAuditForm.find("[name='payNo']").val(row.payNo);
                                orderAuditForm.find("[name='oid']").val(row.oid);
                                $("#orderAuditSuccess").modal('show')
                            },
                            'click .confirmFail': function (e, val, row) {
                                var orderAuditForm = $("#orderAuditFailForm");
                                orderAuditForm.validator('destroy');
                                util.form.validator.init(orderAuditForm);
                                orderAuditForm.clearForm();
                                orderAuditForm.find("[name='payNo']").val(row.payNo);
                                orderAuditForm.find("[name='oid']").val(row.oid);
                                $("#orderAuditFail").modal('show')
                            }
                        }
                    }
                ]
            };
            // 审核通过
            $("#orderAuditSuccessSubmit").on('click', function () {
            	$("#orderAuditSuccess").modal('hide');
            	$("#orderAuditSuccessSubmit").attr('disabled','disabled');
                var form = document.orderAuditSuccessForm;
                util.form.validator.init($(form));
                if (!$(form).validator('doSubmitCheck')){
                	$("#orderAuditSuccessSubmit").attr('disabled',false);
                	return;
                } 
                http.post(settleConfig.api.settlement.settlement_order_checkInOrderAudit, {
                    data: {
                        payNo: form.payNo.value,
                        auditRemark: form.remark.value,
                        auditStatus: 2,
                        oid: form.oid.value
                    },
                    contentType: 'form'
                }, function (result) {
                    $('#orderReviewTable').bootstrapTable('refresh');
                    if (result.returnCode == "0000") {
                        toastr.success('操作成功', '提示', {
                            timeOut: 3000
                        })
                    } else {
                        toastr.error(result.errorMessage, '错误信息', {
                            timeOut: 3000
                        })
                    }
                    $("#orderAuditSuccessSubmit").attr('disabled',false);
                })
            });
            // 审核驳回
            $("#orderAuditFailSubmit").on('click', function () {
            	$("#orderAuditFail").modal('hide');
            	$("#orderAuditFailSubmit").attr('disabled','disabled');
                var form = document.orderAuditFailForm;
                util.form.validator.init($(form));
                if (!$(form).validator('doSubmitCheck')){
                	$("#orderAuditFailSubmit").attr('disabled',false);
                	return;
                } 
                http.post(settleConfig.api.settlement.settlement_order_checkInOrderAudit, {
                    data: {
                        payNo: form.payNo.value,
                        auditRemark: form.remark.value,
                        auditStatus: 3,
                        oid: form.oid.value
                    },
                    contentType: 'form'
                }, function (result) {
                    $('#orderReviewTable').bootstrapTable('refresh');
                    if (result.returnCode == "0000") {
                        toastr.success('操作成功', '提示', {
                            timeOut: 3000
                        })
                    } else {
                        toastr.error(result.errorMessage, '错误信息', {
                            timeOut: 3000
                        })
                    }
                    $("#orderAuditFailSubmit").attr('disabled',false);
                })
            });

            /**
             * 订单审核表格初始化
             */
            $('#orderReviewTable').bootstrapTable(orderReviewTableConfig);

            //  ==============订单审核 end ======================

            //  ==============订单审核记录 start ======================
            // 清空按钮
            $('#search_order_record_reset').on('click', function (e) {
                var form = document.searchOrderRecordForm;
                util.form.reset($(form))
            });
            // 搜索按钮
            $('#search_order_record_search').on('click', function (e) {
                if (checkOrderRecordForm()) return;
                $('#orderRecordTable').bootstrapTable("refresh");
            });
            // 表单参数检查
            var checkOrderRecordForm = function () {
                var form = document.searchOrderRecordForm;
                var limitAmount = form.limitAmount.value.trim();
                var maxAmount = form.maxAmount.value.trim();
                if (limitAmount.length > 0 && ( isNaN(new Number(limitAmount)) || parseInt(limitAmount) < 0)) {
                    toastr.error('金额格式错误或小于0!', '错误信息', {timeOut: 3000});
                    return true;
                }
                if (maxAmount.length > 0 && ( isNaN(new Number(maxAmount)) || parseInt(maxAmount) < 0)) {
                    toastr.error('金额格式错误或小于0!', '错误信息', {timeOut: 3000});
                    return true;

                }
                if (parseInt(limitAmount) > parseInt(maxAmount)) {
                    toastr.error('最大金额小于最小金额!', '错误信息', {timeOut: 3000});
                    return true;
                }
                return false;
            };
            // 绑定按键搜索
            searchInitWithCheck($('#searchOrderRecordForm'), $('#orderRecordTable'), checkOrderRecordForm);
            var orderRecordDataOptions = {
                page: 1,
                auditStatusList: "2,3",
                rows: 10
            };

            //组条件查询
            function orderRecordGetQueryParams(val) {
                var form = document.searchOrderRecordForm;
                orderRecordDataOptions.rows = val.limit;
                orderRecordDataOptions.page = parseInt(val.offset / val.limit) + 1;
                $.extend(orderRecordDataOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
                return val;
            }

            var orderRecordTableConfig = {
                ajax: function (origin) {
                    http.post(settleConfig.api.settlement.settlement_order_page, {
                        data: orderRecordDataOptions,
                        contentType: 'form',
                        traditional: true
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
                                item.channel = $("#channel" + item.channel).html();
                            }
                        });
                        origin.success(rlt)
                    })
                },
                pageNumber: orderRecordDataOptions.page,
                pageSize: orderRecordDataOptions.row,
                pagination: true,
                sidePagination: 'server',
                pageList: [10, 20, 30, 50, 100],
                queryParams: orderRecordGetQueryParams,
                onLoadSuccess: function (result) {
                },
                columns: [
                    {
                        width: 100,
                        align: 'left',
                        formatter: function (val, row, index) {
                            return '<div class="func-area"><a href="javascript:void(0)" class="item-detail"  data-toggle="modal">' + row.orderNo + '</a></div>';
                        },
                        events: {
                            //详情
                            'click .item-detail': function (e, val, row) {
                                var tmp_row = row;
                                if (row.auditStatus == 1) {
                                    tmp_row.auditStatus = "审核中"
                                } else if (row.auditStatus == 2) {
                                    tmp_row.auditStatus = "审核通过"
                                } else if (row.auditStatus == 3) {
                                    tmp_row.auditStatus = "审核驳回"
                                }
                                if(row.amount.toString().indexOf('元') == -1){
									row.amount = row.amount +'元';
								}
		                        if( row.fee.toString().indexOf('元') == -1){
									 row.fee =  row.fee +'元';
								}
		                        tmp_row.amount = row.amount;
                                tmp_row.fee = row.fee;
                                var form = document.detailForm;
                                util.form.reset($(form));
                                $$.detailAutoFix($('#detailForm'), tmp_row); // 自动填充详情
                                $('#orderDetail').modal('show');
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
                            return (row.amount + '元');
                        }
                    },
                    {
                        width: 60,
                        align: 'left',
                        formatter: function (val, row, index) {
                            return row.fee + '元';
                        }
                    },
                    {width: 100, align: 'left', field: 'systemSource'},
                    {width: 100, align: 'left', field: 'realName'},
                    {width: 100, align: 'left', field: 'phone'},
                    {width: 100, align: 'left', field: 'userType'},
                    {width: 100, align: 'left', field: 'channel'},
                    {width: 100, align: 'left', field: 'createTime'},
                    {
                        width: 100, align: 'left', field: 'auditRemark',
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
                    },
                    {
                        width: 100, align: 'left', field: 'auditStatus',
                        formatter: function (val, row, index) {
                            if (row.auditStatus == 2) {
                                return "审核通过";
                            } else if (row.auditStatus == 3) {
                                return "驳回";
                            }
                        }
                    }
                ]
            };

            /**
             * 订单审核表格初始化
             */
            $('#orderRecordTable').bootstrapTable(orderRecordTableConfig);
            //  ==============订单审核记录 end ======================

			// tab切换
            $('#tab2').on('click', function (e) {
                $('#orderRecordTable').bootstrapTable("refresh");
                $('#orderReviewTable').bootstrapTable("refresh");
            });
        }

    }
});