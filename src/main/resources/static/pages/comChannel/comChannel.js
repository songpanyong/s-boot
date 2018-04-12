/**
 *  结算通道配置
 */
define([
  'http',
  //	'config',
  'settleConfig',
  'util',
  'extension'
], function(http, settleConfig, util, $$) {
  return {
    name: 'comChannel',
    init: function() {
      // 分页配置
      var pageOptions = {
        page: 1,
        row: 10
      }
      //确认提示框
      var confirm = $('#confirmModal');
      // 初始化数据表格
      var tableConfig = {
        ajax: function(origin) {
          http.post(settleConfig.api.settlement.settlement_getChannelPage, {
            data: pageOptions,
            contentType: 'form'
          }, function(rlt) {
            origin.success(rlt)
          })
        },
        pageNumber: pageOptions.page,
        pageSize: pageOptions.row,
        pagination: true,
        sidePagination: 'server',
        pageList: [10, 20, 30, 50, 100],
        queryParams: getQueryParams,
        onLoadSuccess: function() {},
        columns: [{
            width: 30,
            align: 'center',
            formatter: function(val, row, index) {
              return index + 1
            }
          },
          {
            field: 'sourceType'
          },
          {
            field: 'channelName'
          },
          {
            field: 'channelNo',
            align: 'right'
          },
          {
            align: 'left',
            formatter: function(val, row, index) {
              return row.tradeType === '01' ? '充值' : '提现';
            }
          },
          {
            align: 'right',
            formatter: function(val, row, index) {
              return row.minAmount + '元';
            }
          },
          {
            align: 'right',
            formatter: function(val, row, index) {
              return row.maxAmount + '元';
            }
          },
          {
            align: 'right',
            formatter: function(val, row, index) {
              return row.paymentPrescription === 1 ? 'T+0' : 'T+1';
            }
          },
          {
            align: 'left',
            formatter: function(val, row, index) {
              return row.rateCalclationMethod === '1' ? '单笔收费' : '比例收费';
            }
          },
          {
            field: 'rate',
            align: 'right'
          },
          {
            width: 100,
            align: 'left',
            field: 'status',
            formatter: function(val) {
              switch(val) {
                case '1':
                  return '<span class="text-green">启用</span>';
                case '0':
                  return '<span class="text-red">停用</span>';
              }
            }
          },
          {
            field: 'merchantId',
            align: 'right'
          },
          {
            field: 'productId'
          },
          {
            width: 100,
            align: 'left',
            formatter: function(val, row, index) {
              return row.treatmentMethod === '1' ? '实时支付' : '人工';
            }
          }
//        ,
//        {
//          width: 100,
//          align: 'center',
//          formatter: function(val, row, index) {
//            textValue = row.status === '0' ? '启用' : '停用';
//            var buttons = [{
//                text: textValue,
//                type: 'button',
//                class: 'item-disable',
//                isRender: true
//            }]
//            if (row.status === '0') {
//              buttons.push({
//                text: '修改',
//                type: 'button',
//                class: 'item-update',
//                isRender: true
//              })
//            }
//            var format = util.table.formatter.generateButton(buttons, 'formTable');
//            format += '<span style=" margin:auto 0px auto 10px;cursor: pointer;" class="fa fa-pencil-square-o item-update"></span>';
//            return format;
//          },
//          events: {
//            'click .item-update': updateChannel,
//            'click .item-disable': changeStatus
//          }
//        }
        ]
      }

      //获取详情
      function updateChannel(e, value, row) {
        if(row.status === "1") {
          confirm.find('.popover-title').html('提示');
          confirm.find('p').html('启用状态下不可修改！');
          $("#tips_cancle").hide();
          $$.confirm({
            container: confirm,
            trigger: this,
            accept: function() {}
          })
          return;
        }
        var form = document.formModify;
        util.form.reset($(form));
        $$.formAutoFix($(form), row);

        $("#update_channelName_err").text("");
        $("#update_channelNo_err").text("");
        $("#update_minAmount_err").text("");
        $("#update_maxAmount_err").text("");
        $("#update_rate_err").text("");
        $("#update_merchantId_err").text("");
        $("#update_productId_err").text("");
        $('.attModify').modal('show');
      }

      //停用启用
      function changeStatus(e, value, row) {
        var tips = row.status === '0' ? '启用' : '停用';
        var status = row.status === '0' ? '1' : '0';
        confirm.find('.popover-title').html('提示');
        confirm.find('p').html('确定' + tips + '？');
        $("#tips_cancle").show();
        $$.confirm({
          container: confirm,
          trigger: this,
          accept: function() {
            http.get(settleConfig.api.settlement.settlement_changeChannelStatus, {
              data: {
                oid: row.oid,
                status: status = row.status === '0' ? '1' : '0'
              }
            }, function(res) {
              if(res.errorCode == 0) {
                confirm.modal('hide');
                $('#formTable').bootstrapTable('refresh');
              } else {
                errorHandle(res);
              }
            })
          }
        })
      }

      // 初始化数据表格
      $('#formTable').bootstrapTable(tableConfig);

      function getQueryParams(val) {
        var form = document.waitSearchForm
        $.extend(pageOptions, util.form.serializeJson(form)); //合并对象，修改第一个对象
        pageOptions.rows = val.limit
        pageOptions.page = parseInt(val.offset / val.limit) + 1
        pageOptions.size = val.limit
        pageOptions.number = parseInt(val.offset / val.limit) + 1
        return val;
      }

      // 结算通道配置按钮点击事件
      $('#addFormConfig').on('click', function() {
        $("#add_channelName_err").text("");
        $("#add_channelNo_err").text("");
        $("#add_minAmount_err").text("");
        $("#add_maxAmount_err").text("");
        $("#add_rate_err").text("");
        $("#add_merchantId_err").text("");
        $("#add_productId_err").text("");
        $('#addFormModal').modal('show');
      })

      // 新建 - 确定按钮点击事件
      $('#doAddForm').on('click', function() {
        var sourceType = $("#sourceType_add").val().trim();
        var channelName = $("#channelName_add").val().trim();
        var channelNo = $("#channelNo_add").val().trim();
        var minAmount = $("#minAmount_add").val().trim();
        var maxAmount = $("#maxAmount_add").val().trim();
        var rate = $("#rate_add").val().trim();
        var merchantId = $("#merchantId_add").val().trim();
        var productId = $("#productId_add").val().trim();
        $("#add_sourceType_err").text("");
        $("#add_channelName_err").text("");
        $("#add_channelNo_err").text("");
        $("#add_minAmount_err").text("");
        $("#add_maxAmount_err").text("");
        $("#add_rate_err").text("");
        $("#add_merchantId_err").text("");
        $("#add_productId_err").text("");

        if(sourceType == "") {
          $("#add_sourceType_err").text("来源系统不能为空");
          return;
        }
        if(channelName == "") {
          $("#add_channelName_err").text("通道名称不能为空");
          return;
        }
        if(channelNo == "") {
          $("#add_channelNo_err").text("通道编号不能为空");
          return;
        }
        if(minAmount == "") {
          $("#add_minAmount_err").text("单笔金额下限不能为空");
          return;
        }
        if(maxAmount == "") {
          $("#add_maxAmount_err").text("单笔金额上限不能为空");
          return;
        }
        if(rate == "") {
          $("#add_rate_err").text("费率不能为空");
          return;
        }
        if(merchantId == "") {
          $("#add_merchantId_err").text("商户身份ID不能为空");
          return;
        }
        if(productId == "") {
          $("#add_productId_err").text("第三方产品编号不能为空");
          return;
        }
        var minAmount = parseFloat(minAmount);
        var maxAmount = parseFloat(maxAmount);
        var rate = parseFloat(rate);
        if(minAmount < 0) {
          $("#add_minAmount_err").text("单笔金额下限不能小于0");
          return;
        }
        if(maxAmount < 0) {
          $("#add_maxAmount_err").text("单笔金额上限不能小于0");
          return;
        }
        if(maxAmount < minAmount) {
          $("#add_minAmount_err").text("单笔金额下限不能大于单笔金额上限");
          return;
        }
        if(rate < 0) {
          $("#add_rate_err").text("费率不能小于0");
          return;
        }

        $("#status_add").val(0);
        var form = document.addFormForm;
        var data = util.form.serializeJson(form)
        http.post(settleConfig.api.settlement.settlement_addChannel, {
          data: data,
          contentType: 'form'
        }, function(res) {
          util.form.reset($(form));
          $('#formTable').bootstrapTable('refresh');
          $('#addFormModal').modal('hide');
        })

      })

      // 修改
      $('#btUpdate').on('click', function(e) {
        var sourceType = $("#sourceType_update").val().trim();
        var channelName = $("#channelName_update").val().trim();
        var channelNo = $("#channelNo_update").val().trim();
        var minAmount = $("#minAmount_update").val().trim();
        var maxAmount = $("#maxAmount_update").val().trim();
        var rate = $("#rate_update").val().trim();
        var merchantId = $("#merchantId_update").val().trim();
        var productId = $("#productId_update").val().trim();
        $("#update_sourceType_err").text("");
        $("#update_channelName_err").text("");
        $("#update_channelNo_err").text("");
        $("#update_minAmount_err").text("");
        $("#update_maxAmount_err").text("");
        $("#update_rate_err").text("");
        $("#update_merchantId_err").text("");
        $("#update_productId_err").text("");

        if(sourceType == "") {
          $("#update_sourceType_err").text("来源系统不能为空");
          return;
        }
        if(channelName == "") {
          $("#update_channelName_err").text("通道名称不能为空");
          return;
        }
        if(channelNo == "") {
          $("#update_channelNo_err").text("通道编号不能为空");
          return;
        }
        if(minAmount == "") {
          $("#update_minAmount_err").text("单笔金额下限不能为空");
          return;
        }
        if(maxAmount == "") {
          $("#update_maxAmount_err").text("单笔金额上限不能为空");
          return;
        }
        if(rate == "") {
          $("#update_rate_err").text("费率不能为空");
          return;
        }
        if(merchantId == "") {
          $("#update_merchantId_err").text("商户身份ID不能为空");
          return;
        }
        if(productId == "") {
          $("#update_productId_err").text("第三方产品编号不能为空");
          return;
        }
        var minAmount = parseFloat(minAmount);
        var maxAmount = parseFloat(maxAmount);
        var rate = parseFloat(rate);
        if(minAmount < 0) {
          $("#update_minAmount_err").text("单笔金额下限不能小于0");
          return;
        }
        if(maxAmount < 0) {
          $("#update_maxAmount_err").text("单笔金额上限不能小于0");
          return;
        }
        if(maxAmount < minAmount) {
          $("#update_minAmount_err").text("单笔金额下限不能大于单笔金额上限");
          return;
        }
        if(rate < 0) {
          $("#update_rate_err").text("费率不能小于0");
          return;
        }

        var formModify = document.formModify
        var data = util.form.serializeJson(formModify)
        http.post(settleConfig.api.settlement.settlement_updateChannel, {
          data: data,
          contentType: 'form'
        }, function(res) {
          util.form.reset($(formModify));
          $('#formTable').bootstrapTable('refresh');
          $('.attModify').modal('hide');
        })
      })

    }
  }
})