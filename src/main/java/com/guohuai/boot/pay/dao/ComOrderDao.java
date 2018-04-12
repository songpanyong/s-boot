package com.guohuai.boot.pay.dao;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.guohuai.boot.pay.vo.OrderVo;

@RepositoryRestResource(path = "elementValidation")
public interface ComOrderDao extends JpaRepository<OrderVo, String>, JpaSpecificationExecutor<OrderVo> {

    public OrderVo findByorderNo(String orderNo);

    public OrderVo findByPayNo(String payNo);

    @Query(value = "select * from t_bank_order a where a.type=?1 and a.reconStatus=0", nativeQuery = true)
    public List<OrderVo> findByNos(String type);

    @Query(value = "update  t_bank_order set status=?1,updateTime=NOW(),returnCode=?2,failDetail=?3 where orderNo=?4", nativeQuery = true)
    @Modifying
    @Transactional
    public int updateByOrder(String status, String returnCode, String resMessage, String orderNo);

    @Query(value = "update  t_bank_order set failDetail='',updateTime=NOW() where orderNo=?3", nativeQuery = true)
    @Modifying
    @Transactional
    public int updateDetailByOrder(String orderNo);

    @Query(value = "select count(*) from t_bank_order a where a.createTime>=?1 and a.createTime<=?2", nativeQuery = true)
    public long countNum(Timestamp beginTime, Timestamp endTime);

    @Query(value = "select userOid,orderNo,status,type,amount,createTime from t_bank_order  where createTime>=?1 and createTime<=?2  order by  createTime  limit ?3,?4 ", nativeQuery = true)
    public Object[] orderDetailList(Timestamp beginTime, Timestamp endTime, long pageSize, int size);


    @Query(value = "SELECT sum(a.amount) FROM t_bank_order a where IF(?1= '', a.userOid != '', a.userOid= ?1) AND IF(?2= '', 1= 1, a.type= ?2) "
            + "AND IF(?3= '', 1= 1, a.realName LIKE ?3 ) AND IF(?4= '', a.orderNo !='', a.orderNo LIKE ?4) "
            + "AND IF(?5= '', a.payNo !='', a.payNo LIKE ?5) AND IF(?6= '', a.status !='', a.status= ?6) AND IF(?7= '', 1= 1, a.reconStatus= ?7) "
            + "AND IF(?8 IS NULL OR ?8= '', 1= 1, a.amount>= ?8) AND IF(?9 IS NULL OR ?9= '', 1= 1, a.amount<= ?9) AND IF(?10 IS NULL OR ?10= '', 1= 1, a.createTime > ?10) "
            + "AND IF(?11 IS NULL OR ?11= '', 1= 1, a.createTime < ?11)", nativeQuery = true)
    public BigDecimal findAmount(String userOid, String type, String realname, String orderNo, String payNo, String status, String reconStatus, BigDecimal limitAmount, BigDecimal maxAmount, String beginTime, String endTime);


    @Query(value = "update t_bank_order a set a.status = (select b.commandStatus from t_bank_payment b where b.orderNo = ?1) WHERE a.orderNo=?1", nativeQuery = true)
    @Modifying
    @Transactional
    public int updateStatusByOrder(String orderNo);


    @Query(value = "update  t_bank_order set status=?1 , updateTime=NOW() where orderNo=?2", nativeQuery = true)
    @Modifying
    @Transactional
    public int updateStatusByOrderNo(String status, String orderNo);


    @Modifying
    @Transactional
    @Query(value = "update  t_bank_order set status=?1 ,failDetail=?3, updateTime=NOW() where orderNo=?2", nativeQuery = true)
    public int updateStatusAndFailDetailByOrderNo(String status, String orderNo, String failDetail);

    @Modifying
    @Transactional
    @Query(value = "update  t_bank_order set auditStatus=?1 ,status=?5 ,auditRemark=?3 ,payNo=?4 ,updateTime=NOW() where oid=?2 and auditStatus='1'", nativeQuery = true)
    public int updateOrderByOid(String auditStatus, String oid, String auditRemark, String payNo,String status);

}
