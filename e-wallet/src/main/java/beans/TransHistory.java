package beans;

import beans.entities.Transaction;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;

@Named
@SessionScoped
public class TransHistory implements Serializable {
    
    @PersistenceContext
    private EntityManager em;
    
    private List<Transaction> transactions;
    
    public List<Transaction> getUserTransactions(Long userId) {
        TypedQuery<Transaction> query = em.createQuery(
            "SELECT t FROM Transaction t WHERE t.sender.id = :userId OR t.receiver.id = :userId " +
            "ORDER BY t.transactionDate DESC", Transaction.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }
    
    public List<Transaction> getTransactions() {
        return transactions;
    }
    
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
} 