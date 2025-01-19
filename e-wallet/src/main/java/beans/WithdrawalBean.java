package beans;

import java.io.Serializable;

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

/**
 * Bean for withdrawing money from user account to a bank account using IBAN set in user account page
 * @author Arcangelo Mauro - xmauroa00
 */
@Named
@SessionScoped
public class WithdrawalBean implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @PersistenceContext
    private EntityManager em;
    
    @Inject
    private UserBean userBean;
    
    private double amount;
    private String paymentReference;
    private boolean showPaymentDetails;
    private boolean confirmationStep = false;

    /**
     * Method for withdrawing money, returns null if error occurs
     * @return null if error occurs
     * @return String to redirect to dashboard if successful
    */
    @Transactional
    public String withdraw() {
        if (!confirmationStep) {
            // First click - show confirmation
            if (validateWithdrawal()) {
                this.showPaymentDetails = true;
                this.confirmationStep = true;
                return null;
            }
            return null;
        }
        
        // Second click - process withdrawal
        User currentUser = userBean.getCurrentUser();
        try {
            Transaction withdrawal = new Transaction();
            withdrawal.setReceiver(currentUser);
            withdrawal.setValue(amount);
            withdrawal.setType("Withdraw");
            withdrawal.setCategory("Bank Withdrawal - " + currentUser.getIban());
            
            currentUser.setBalance(currentUser.getBalance() - amount);
            
            em.persist(withdrawal);
            em.merge(currentUser);

            addMessage(FacesMessage.SEVERITY_INFO, "Success", 
                String.format("Withdrawal of %.2f CZK completed successfully", amount));
            
            resetForm();
            return "dashboard?faces-redirect=true";
            
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred during withdrawal: " + e.getMessage());
            return null;
        }
    }
    
    private boolean validateWithdrawal() {
        User currentUser = userBean.getCurrentUser();
        
        if (currentUser.getIban() == null || currentUser.getIban().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "No IBAN configured", 
                "Please set up your IBAN in Account Settings before making a withdrawal");
            return false;
        }

        if (amount <= 0) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                "Please enter a positive amount");
            return false;
        }
        
        if (currentUser.getBalance() < amount) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Insufficient funds", 
                "Your balance is insufficient for this withdrawal");
            return false;
        }
        
        return true;
    }
    
    /**
     * Method for adding messages to the FacesContext
     * @param severity - Severity of the message
     * @param summary - Summary of the message
     * @param detail - Detail of the message
    */
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getPaymentReference() {
        return paymentReference;
    }
    
    public boolean isShowPaymentDetails() {
        return showPaymentDetails;
    }
    
    /**
     * Method for setting showPaymentDetails, which is used to show the payment details after a successful withdrawal
     * @param showPaymentDetails - Boolean to set showPaymentDetails
    */
    public void setShowPaymentDetails(boolean showPaymentDetails) {
        this.showPaymentDetails = showPaymentDetails;
    }
    
    /**
     * Method for resetting the form
    */
    public void resetForm() {
        amount = 0;
        paymentReference = null;
        showPaymentDetails = false;
        confirmationStep = false;
    }
    
    // Add getter for confirmationStep
    public boolean isConfirmationStep() {
        return confirmationStep;
    }
}
