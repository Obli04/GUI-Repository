package beans;

import java.io.Serializable;
import java.util.List;

import beans.entities.Transaction;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/** 
 * TransactionBean is a managed bean responsible for managing user transactions, 
 * particularly for fetching and returning all transactions done by a user.
 * 
 * @author xromang00
 */
@Named
@SessionScoped
public class TransactionBean implements Serializable {
    
    @PersistenceContext
    private EntityManager em;
    
    private List<Transaction> transactions;
    
    /**
     * Retrieves a list of transactions for a specific user.
     * 
     * @param userId the ID of the user whose transactions are to be retrieved.
     * @return a list of transactions associated with the specified user.
     */
    public List<Transaction> getUserTransactions(Long userId) {
        TypedQuery<Transaction> query = em.createQuery(
            "SELECT t FROM Transaction t WHERE " +
            "(t.sender.id = :userId OR t.receiver.id = :userId OR " +
            "(t.sender IS NULL AND t.receiver.id = :userId)) " +
            "ORDER BY t.transactionDate DESC", Transaction.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }
    
    /**
     * Gets the list of transactions.
     * 
     * @return the list of transactions.
     */
    public List<Transaction> getTransactions() {
        return transactions;
    }
    
    /**
     * Sets the list of transactions.
     * 
     * @param transactions the list of transactions to set.
     */
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
} 