package beans;

import java.io.Serializable;
import java.time.LocalDateTime;

import beans.entities.Transaction;
import beans.entities.User;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Named
@SessionScoped
public class WithdrawalBean implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @PersistenceContext
    private EntityManager em;
    
    @Inject
    private UserBean userBean;
    
    private double amount;
    
    @Transactional
    public String withdraw() {
        User currentUser = userBean.getCurrentUser();
        
        try {
            if (currentUser.getIban() == null || currentUser.getIban().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No IBAN configured", 
                    "Please set up your IBAN in Account Settings before making a withdrawal");
                return null;
            }

            if (amount <= 0) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "Please enter a positive amount");
                return null;
            }
            
            if (currentUser.getBalance() < amount) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Insufficient funds", 
                    "Your balance is insufficient for this withdrawal");
                return null;
            }
            
            // Create withdrawal transaction
            Transaction withdrawal = new Transaction();
            withdrawal.setReceiver(currentUser);
            withdrawal.setValue(amount);
            withdrawal.setType("Withdraw");
            withdrawal.setCategory("Bank Withdrawal - " + currentUser.getIban());
            withdrawal.setTransactionDate(LocalDateTime.now());
            
            // Update user balance
            currentUser.setBalance(currentUser.getBalance() - amount);
            
            // Persist changes
            em.persist(withdrawal);
            em.merge(currentUser);
            
            addMessage(FacesMessage.SEVERITY_INFO, "Success", 
                String.format("Withdrawal of %.2f CZK completed successfully", amount));
            
            // Reset form
            amount = 0;
            
            return "dashboard?faces-redirect=true";
            
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred during withdrawal: " + e.getMessage());
            return null;
        }
    }
    
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
    }
    
    // Getters and setters
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
}
