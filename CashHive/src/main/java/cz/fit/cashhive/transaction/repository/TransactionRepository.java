package cz.fit.cashhive.transaction.repository;

import cz.fit.cashhive.transaction.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    List<TransactionEntity> findBySender(String sender);
    List<TransactionEntity> findByReceiver(String receiver);
}
