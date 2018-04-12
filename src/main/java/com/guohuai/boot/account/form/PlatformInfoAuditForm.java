package com.guohuai.boot.account.form;

import lombok.Data;

@Data
public class PlatformInfoAuditForm {
	
	private String oid;
    private String userOid; // 用户、平台id
	private String userName; // 平台名称、发行人名称
	private String userType; // 用户类型
	private String userTypeName; // 用户类型名称
	private String userStatus; // 用户、平台状态
	private String applyType; // 申请原因类型
	private String applyTypeName; // 申请原因类型名称
	private String applyReason; // 申请原因
	private String applicantId; // 申请人id
	private String applicantName; // 申请人姓名
	private String auditStatus; // 审核状态
	private String auditReason; // 审核原因
	private String operatorId; // 审核人id
	private String operatorName; // 审核人姓名
	
    private String changeRecordsList;
    private String beginTime; // 更新时间
	private String endTime; // 创建时间
    private int page = 1;
    private int rows = 10;
    
    private String phone;
    
}
