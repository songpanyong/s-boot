package com.guohuai.boot.account.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.guohuai.boot.account.entity.ReconEntity;

public interface ReconDao extends JpaRepository<ReconEntity, String>, JpaSpecificationExecutor<ReconEntity> {
}
