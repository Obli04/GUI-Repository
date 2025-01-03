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
    private String category = "Withdrawal";
    private String recipientBank;
    private String recipientIBAN;
    private String note;
    private String countryCode;
    
    @Transactional
    public String withdraw() {
        User currentUser = userBean.getCurrentUser();
        
        try {
            // Combine country code with IBAN number
            String fullIBAN = countryCode + recipientIBAN;
            
            if (fullIBAN.length() < 15) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid IBAN", 
                    "IBAN must be at least 15 characters long");
                return null;
            }

            // Store the full IBAN
            this.recipientIBAN = fullIBAN;
            
            if (recipientBank == null || recipientIBAN == null || 
                recipientBank.trim().isEmpty() || recipientIBAN.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid details", 
                    "Please provide valid bank and IBAN details");
                return null;
            }

            if (amount <= 0) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", "Please enter a positive amount");
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
            withdrawal.setCategory(String.format("%s - Bank: %s, IBAN: %s", 
                                              category, recipientBank, recipientIBAN));
            withdrawal.setTransactionDate(LocalDateTime.now());
            
            // Update user balance
            currentUser.setBalance(currentUser.getBalance() - amount);
            
            // Persist changes
            em.persist(withdrawal);
            em.merge(currentUser);
            
            addMessage(FacesMessage.SEVERITY_INFO, "Success", 
                String.format("Withdrawal of %.2f CZK to %s (IBAN: %s) completed successfully", 
                            amount, recipientBank, recipientIBAN));
            
            // Reset form
            resetForm();
            
            return "dashboard?faces-redirect=true";
            
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred during withdrawal: " + e.getMessage());
            return null;
        }
    }
    
    private void resetForm() {
        amount = 0;
        recipientBank = null;
        recipientIBAN = null;
        note = null;
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
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getRecipientBank() {
        return recipientBank;
    }
    
    public void setRecipientBank(String recipientBank) {
        this.recipientBank = recipientBank;
    }
    
    public String getRecipientIBAN() {
        return recipientIBAN;
    }
    
    public void setRecipientIBAN(String recipientIBAN) {
        this.recipientIBAN = recipientIBAN;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
