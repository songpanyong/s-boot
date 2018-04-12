package com.guohuai.boot.account.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.account.entity.AccFailOrderNotifyEntity;

public interface AccFailOrderNotifyDao extends JpaRepository<AccFailOrderNotifyEntity, String>, JpaSpecificationExecutor<AccFailOrderNotifyEntity> {
	
	@Query(value="SELECT y.orderDesc FROM t_account_fail_order_notify y WHERE y.notified='N' AND y.receiveTime>date_sub(now(), interval ?1 hour)GROUP BY y.orderDesc",nativeQuery = true)
	public List<String> getOrderDescNotifyList(long notifyTime);
	
	@Query(value = "SELECT count(*) FROM t_account_fail_order_notify y WHERE y.receiveTime>date_sub(now(), interval ?1 hour) AND y.notified='Y' AND y.orderDesc= ?2 ", nativeQuery = true)
	public long notifyCount(long notifyTime, String orderDesc);
	
	@Query(value = "select * from t_account_fail_order_notify y WHERE y.orderDesc = ?1 AND y.notified = ?2 ORDER BY receiveTime DESC LIMIT 1;", nativeQuery = true)
	public AccFailOrderNotifyEntity getLastReceiveTime(String orderDesc, String notified);
}
