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

@Named
@SessionScoped
public class SendBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @PersistenceContext
    private EntityManager em;

    @Inject
    private UserBean userBean;

    private String recipientIdentifier; // Can be email or variable symbol
    private double amount;

    @Transactional
    public String sendMoney() {
        User sender = userBean.getCurrentUser();
        User recipient;

        try {
            // Validate amount
            if (amount <= 0) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "Please enter a positive amount");
                return null;
            }

            // Check sender's balance
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

            // Persist changes
            em.persist(transaction);
            em.merge(sender);
            em.merge(recipient);

            addMessage(FacesMessage.SEVERITY_INFO, "Success", 
                String.format("Successfully sent %.2f CZK to %s", amount, 
                    recipient.getFirstName() + " " + recipient.getSecondName()));

            // Reset form
            resetForm();

            return "dashboard?faces-redirect=true";

        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred during the transfer: " + e.getMessage());
            return null;
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
    }

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