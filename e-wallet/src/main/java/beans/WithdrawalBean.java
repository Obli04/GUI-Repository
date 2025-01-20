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
    private boolean showPaymentDetails;
    private boolean confirmationStep = false;

    /**
     * Method for withdrawing money from the e-wallet to a bank account
     * @return null if an error occurs during the withdrawal process
     * @return String to redirect to dashboard if the withdrawal is successful
    */
    @Transactional
    public String withdraw() {
        if (!confirmationStep) {
            if (validateWithdrawal()) {
                this.showPaymentDetails = true;
                this.confirmationStep = true;
                return null;
            }
            return null;
        }
        
        User currentUser = userBean.getCurrentUser();
        try {
            Transaction withdrawal = new Transaction();
            withdrawal.setReceiver(currentUser);
            withdrawal.setValue(amount);
            withdrawal.setType("Withdraw");
            withdrawal.setCategory("Bank Withdrawal - " + currentUser.getIban());
            withdrawal.setTransactionDate(LocalDateTime.now());
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
    
    /**
     * Method for validating the withdrawal process
     * @return true if the withdrawal is valid, false otherwise
    */
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
     * @param severity - Severity level of the message, so if it's an error, success, etc.
     * @param summary - Summary/title of the message
     * @param detail - Details of the message
    */
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
    }
    /**
     * Method for getting the amount of money to withdraw
     * @return amount - Amount of money to withdraw
    */
    public double getAmount() {
        return amount;
    }
    /**
     * Method for setting the amount of money to withdraw
     * @param amount
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    /**
     * Method for getting showPaymentDetails, so the details of the withdrawal
     * @return showPaymentDetails
    */
    public boolean isShowPaymentDetails() {
        return showPaymentDetails;
    }
    
    /**
     * Method for setting showPaymentDetails
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
        showPaymentDetails = false;
        confirmationStep = false;
    }

    /**
     * Method for getting confirmationStep
     * @return confirmationStep - Boolean to check if the confirmation step is active
    */
    public boolean isConfirmationStep() {
        return confirmationStep;
    }
}
