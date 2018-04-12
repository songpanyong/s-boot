package com.guohuai.boot.account.dao;

import com.guohuai.boot.account.entity.AccountInfoEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface AccountInfoDao extends JpaRepository<AccountInfoEntity, String>, JpaSpecificationExecutor<AccountInfoEntity> {

	@Query(value = "SELECT * FROM t_account_info WHERE userOid = ?1", nativeQuery = true)
	public List<AccountInfoEntity> findByUserOid(String userOid);

	@Query(value = "SELECT * FROM t_account_info WHERE accountNo = ?1", nativeQuery = true)
	public AccountInfoEntity findByAccountNo(String accountNo);

	@Query(value = "SELECT * FROM t_account_info WHERE oid = ?1 for update", nativeQuery = true)
	public AccountInfoEntity findByOidForUpdate(String oid);

	@Query(value = "SELECT * FROM t_account_info WHERE userOid = ?1 and userType = ?2", nativeQuery = true)
	public List<AccountInfoEntity> findByUserOidAndUserType(String userOid, String userType);

	@Query(value = "SELECT * FROM t_account_info WHERE userType = ?1 and accountType = ?2 and relationProduct = ?3", nativeQuery = true)
	public AccountInfoEntity findByUserAccountProduct(String userType, String accountType, String relationProduct);

	public AccountInfoEntity findByUserTypeAndAccountType(String userType, String accountType);

	@Query(value = "SELECT * FROM t_account_info WHERE userOid = ?1 and accountType = ?2", nativeQuery = true)
	public List<AccountInfoEntity> findByUserOidAndAccountType(String userOid, String accountType);

	@Query(value = "SELECT * FROM t_account_info WHERE userOid = ?1 and userType = ?2 and relationProduct =?3 and accountType = ?4", nativeQuery = true)
	public List<AccountInfoEntity> findByUserOidAndAccountTypeAndProductNo(String userOid, String userType, String relationProduct, String acccountType);

	@Query(value = "SELECT * FROM t_account_info WHERE userOid = ?1 and userType = ?2 and  relationProduct is null and accountType = ?3", nativeQuery = true)
	public List<AccountInfoEntity> findByUserOidAndAccountTypeAndUserTypeNoProduct(String userOid, String userType, String acccountType);
	
	@Query(value = "SELECT * FROM t_account_info WHERE userOid = ?1 and userType = ?2 and accountType = ?3", nativeQuery = true)
	public AccountInfoEntity findByUserOidAndAccountTypeAndUserType(String userOid, String userType, String accountType);

	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_info SET balance = balance + ?1 , updateTime = NOW() WHERE accountNo = ?2", nativeQuery = true)
	public int updateBalance(BigDecimal balance, String accountNo);

	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_info SET balance = balance - ?1 , updateTime = NOW() WHERE accountNo = ?2 and balance >= ?1", nativeQuery = true)
	public int subtractBalance(BigDecimal balance, String accountNo);

	@Transactional
	@Modifying
	@Query(value = " UPDATE t_account_info SET balance =( CASE WHEN balance - ?1 >=0 THEN balance - ?1 ELSE 0 END), updateTime = now() WHERE accountNo = ?2", nativeQuery = true)
	public int subtractBalanceLowerLimitZero(BigDecimal balance, String accountNo);

	@Query(value = "SELECT * FROM t_account_info WHERE userOid = ?1 AND accountType = ?2 ", nativeQuery = true)
	public AccountInfoEntity findByUserOidAndAccountTyp(String userOid, String accountTyp);

	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_info SET balance = balance + ?1 , updateTime = NOW() WHERE accountNo = ?2", nativeQuery = true)
	public int addBalance(BigDecimal balance, String accountNo);

	@Query(value = "SELECT relationProduct,accountName FROM t_account_info WHERE relationProduct IS NOT NULL GROUP BY relationProduct", nativeQuery = true)
	public List<AccountInfoEntity> findRelationProduct();

	@Query(value = "SELECT * FROM t_account_info WHERE userOid = ?1 AND accountType = '10'", nativeQuery = true)
	public AccountInfoEntity findBasicAccountByUserOid(String userOid);

	@Query(value = "SELECT * FROM t_account_info WHERE userOid = ?1 and accountType = '05'", nativeQuery = true)
	public AccountInfoEntity findFrozenAccountByUserOid(String userOid);

	public AccountInfoEntity findAccountByAccountTypeAndUserOid(String accountType, String userOid);

	@Query(value = "SELECT * FROM t_account_info WHERE userType = ?1 and accountType = ?2 and updateTime >= ?3 and updateTime <= ?4 and balance > 0", nativeQuery = true)
	public List<AccountInfoEntity> findFreezeAccount(String userType, String acccountType, String beginTime, String endtime);
	
	@Query(value = "SELECT * FROM t_account_info WHERE accountType = ?1 and updateTime >= ?2 and updateTime <= ?3 and balance > 0", nativeQuery = true)
	public List<AccountInfoEntity> findFreezeAccount(String acccountType, String beginTime, String endtime);

	@Query(value = "SELECT balance FROM t_account_info WHERE userOid = ?1 AND accountType = ?2", nativeQuery = true)
	public BigDecimal finBalanceByUserOidAndType(String userOid, String accountType);

	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_info SET balance = balance + ?1 , lineOfCredit = lineOfCredit + ?1, updateTime = NOW() WHERE accountNo = ?2", nativeQuery = true)
	public int addCreditBalance(BigDecimal balance, String accountNo);
	
	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_info SET balance = balance - ?1 , lineOfCredit = lineOfCredit - ?1, updateTime = NOW() WHERE accountNo = ?2 and balance >= ?1", nativeQuery = true)
	public int subtractCreditBalance(BigDecimal balance, String accountNo);
	
	@Query(value = "SELECT * FROM t_account_info WHERE userType = ?2 AND accountType = ?1", nativeQuery = true)
	public AccountInfoEntity findAccountByAccountTypeAndUserType(String accountType, String userType);

	@Query(value = "SELECT SUM(balance) FROM t_account_info WHERE userOid = ?1 AND accountType = '07' ", nativeQuery = true)
	public BigDecimal findTotalPriductBalanceByUserOid(String userOid);

	@Query(value = "SELECT SUM(lineOfCredit) FROM t_account_info WHERE userOid = ?1 AND accountType = '07' ", nativeQuery = true)
	public BigDecimal findTotalPriductCreditByUserOid(String userOid);

	@Query(value = "SELECT (SELECT balance FROM t_account_info WHERE userOid = ?1 AND accountType = '10') "
			+ " -(SELECT balance FROM t_account_info WHERE userOid = ?1 AND accountType = '05') ", nativeQuery = true)
	public BigDecimal findAvaliableBalanceByUserOid(String userOid);

}
