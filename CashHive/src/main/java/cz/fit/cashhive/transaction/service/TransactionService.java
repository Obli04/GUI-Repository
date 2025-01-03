package cz.fit.cashhive.transaction.service;

import cz.fit.cashhive.transaction.dto.TransactionDTO;
import cz.fit.cashhive.transaction.entity.TransactionEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final List<TransactionEntity> transactions = new ArrayList<>();

    public void sendMoney(TransactionDTO transactionDTO) {
        // Validate and process the transaction
        TransactionEntity transaction = new TransactionEntity();
        transaction.setSender(transactionDTO.sender());
        transaction.setReceiver(transactionDTO.receiver());
        transaction.setAmount(transactionDTO.amount());
        transaction.setReason(transactionDTO.reason());
        transaction.setTimestamp(LocalDateTime.now());

        // Add the transaction to the list
        transactions.add(transaction);
    }

    public List<TransactionEntity> getTransactionsBySender(String sender) {
        List<TransactionEntity> result = new ArrayList<>();
        for (TransactionEntity transaction : transactions) {
            if (transaction.getSender().equals(sender)) {
                result.add(transaction);
            }
        }
        return result;
    }

    public List<TransactionEntity> getTransactionsByReceiver(String receiver) {
        List<TransactionEntity> result = new ArrayList<>();
        for (TransactionEntity transaction : transactions) {
            if (transaction.getReceiver().equals(receiver)) {
                result.add(transaction);
            }
        }
        return result;
    }
}
