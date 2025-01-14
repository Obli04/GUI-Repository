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
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Bean for sending money to a user account to anoter one using email or variable symbol
 * @author Arcangelo Mauro - xmauroa00
 */
@Named
@SessionScoped
public class SendBean implements Serializable {
    // EntityManager for database operations
    @PersistenceContext
    private EntityManager em;

    // UserBean for getting current user
    @Inject
    private UserBean userBean;

    private String recipientIdentifier; // Can be email or variable symbol
    private double amount; // Amount to send

    // Transactional method for sending money, returns null if error occurs
    @Transactional
    public String sendMoney() {
        User sender = userBean.getCurrentUser();
        User recipient;

        try {
            // Check if amount is greater than 0 to send money
            if (amount <= 0.01) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "The transfer amount must be greater than 0 CZK");
                return null;
            }

            // Check if sender has enough balance to send money
            if (sender.getBalance() < amount) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Insufficient funds", 
                    "Your balance is insufficient for this transfer");
                return null;
            }

            // Find recipient by email or variable symbol
            try {
                if (recipientIdentifier.contains("@")) {
                    recipient = em.createQuery("SELECT u FROM User u WHERE u.email = :identifier", User.class)
                            .setParameter("identifier", recipientIdentifier)
                            .getSingleResult();
                } else {
                    recipient = em.createQuery("SELECT u FROM User u WHERE u.variableSymbol = :identifier", User.class)
                            .setParameter("identifier", recipientIdentifier)
                            .getSingleResult();
                }
            } catch (NoResultException e) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Recipient not found", 
                    "The specified recipient could not be found");
                return null;
            }

            // Prevent self-transfer
            if (sender.getId().equals(recipient.getId())) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid recipient", 
                    "You cannot send money to yourself");
                return null;
            }

            // Create and save transaction
            Transaction transaction = new Transaction();
            transaction.setSender(sender);
            transaction.setReceiver(recipient);
            transaction.setValue(amount);
            transaction.setType("Transfer");
            transaction.setCategory("User Transfer");
            transaction.setTransactionDate(LocalDateTime.now());

            // Update balances
            sender.setBalance(sender.getBalance() - amount);
            recipient.setBalance(recipient.getBalance() + amount);

            // Persist changes to database
            em.persist(transaction);
            em.merge(sender);
            em.merge(recipient);

            // Show success message
            addMessage(FacesMessage.SEVERITY_INFO, "Success", 
                String.format("Successfully sent %.2f CZK to %s", amount, 
                    recipient.getFirstName() + " " + recipient.getSecondName()));

            // Reset form
            resetForm();

            // Redirect to dashboard
            return "dashboard?faces-redirect=true";

        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred during the transfer: " + e.getMessage());
            return null;
        }
    }

    // Method for adding messages to the FacesContext
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
    }

    // Method for resetting the form
    private void resetForm() {
        this.recipientIdentifier = null;
        this.amount = 0.0;
    }

    // Getters and setters
    public String getRecipientIdentifier() {
        return recipientIdentifier;
    }

    public void setRecipientIdentifier(String recipientIdentifier) {
        this.recipientIdentifier = recipientIdentifier;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

}