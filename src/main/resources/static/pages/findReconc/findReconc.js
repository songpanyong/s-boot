/**
 * 指令管理
 */
define([
    'http',
//	'config',
	'settleConfig',
    'util',
    'extension'
], function (http,settleConfig, util, $$) {
    return {
        name: 'findReconc',
        init: function () {
			$('#date_reconci').on('blur', function (e) {
				var date=$("#date_reconci").val();
				if (date === "") {
                    $("#audit_validTime_show").text("日期不能为空");
                    return;
                }else{
                	$("#audit_validTime_show").text("");
                }
			});
			

            //搜索
            $('#settlement_search_reconciliation_search').on('click', function (e) {
            	var date=$("#date_reconci").val();
            	if (date === "") {
                    $("#audit_validTime_show").text("日期不能为空");
                    return;
                }else{
                	$("#audit_validTime_show").text("");
                }
                e.preventDefault()
				http.post(settleConfig.api.settlement.settlement_banklog_findReconc, {
                         data:{
                        	reconciliationDate:$("#date_reconci").val()
                        },
                        contentType: 'form'
                 	}, function (res) {
                 	}
                 )
            })
        }
    }
})
