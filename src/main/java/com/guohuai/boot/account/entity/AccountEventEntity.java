package com.guohuai.boot.account.entity;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.guohuai.basic.component.ext.hibernate.UUID;


@Entity
@Table(name = "t_account_event")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class AccountEventEntity extends UUID implements Serializable{
	
	private static final long serialVersionUID = -9214033139264095634L;
	//状态  0-已生效 1-生效中 2-审核中
	public static final String STATUS_YES = "0";
	public static final String STATUS_NOT = "1";
	public static final String STATUS_AUDIT = "2";
	
	private String eventName; // 事件名称
	private String userOid; // 平台Oid
	private String setUpStatus; // 设置状态  0已生效1生效中2待审核
	private String canBeSetUp; // 是否可被设置,Y可设置N不可设置空不展示
	private Timestamp updateTime; // 更新时间
	private Timestamp createTime; // 创建时间
	@OneToMany(fetch=FetchType.LAZY, mappedBy="eventOid")
	private List<AccountEventChildEntity> childEvents= new ArrayList<AccountEventChildEntity>();
}
