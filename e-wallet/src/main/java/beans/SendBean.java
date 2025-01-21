package beans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.primefaces.PrimeFaces;

import beans.entities.Friends;
import beans.entities.Transaction;
import beans.entities.User;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Bean for sending money to a user account to anoter one using email or variable symbol of another user
 * @author Arcangelo Mauro - xmauroa00
 */
@Named
@SessionScoped
public class SendBean implements Serializable {
    @PersistenceContext
    private EntityManager em;

    @Inject
    private UserBean userBean;

    @Inject
    private BudgetBean budgetBean;

    private String recipientIdentifier;
    private double amount;

    /** Map to store email-to-name mappings for friends display */
    private final Map<String, String> friendEmailToNameMap = new HashMap<>();

    /**
     * Gets a list of friends' names and emails for the current user.
     * @return List of SelectItem with friend's name as label and email as value
     */
    public List<SelectItem> getFriendsList() {
        User currentUser = userBean.getCurrentUser();
        List<SelectItem> friendItems = new ArrayList<>();
        friendEmailToNameMap.clear();
        
        try {
            // Get friends where current user is user1
            List<Friends> friendsAsUser1 = em.createQuery(
                "SELECT f FROM Friends f WHERE f.user1.id = :userId", 
                Friends.class)
                .setParameter("userId", currentUser.getId())
                .getResultList();
                
            // Get friends where current user is user2
            List<Friends> friendsAsUser2 = em.createQuery(
                "SELECT f FROM Friends f WHERE f.user2.id = :userId", 
                Friends.class)
                .setParameter("userId", currentUser.getId())
                .getResultList();
                
            // Process friends from both queries
            for (Friends friendship : friendsAsUser1) {
                User friend = friendship.getUser2();
                String displayName = friend.getFirstName() + " " + friend.getSecondName();
                friendEmailToNameMap.put(friend.getEmail(), displayName);
                friendItems.add(new SelectItem(friend.getEmail(), displayName));
            }
            
            for (Friends friendship : friendsAsUser2) {
                User friend = friendship.getUser1();
                String displayName = friend.getFirstName() + " " + friend.getSecondName();
                friendEmailToNameMap.put(friend.getEmail(), displayName);
                friendItems.add(new SelectItem(friend.getEmail(), displayName));
            }
            
            return friendItems;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Automatically insert the email when a friend is selected from the dropdown.
     */
    public void onFriendSelect() {
        if (recipientIdentifier != null && !recipientIdentifier.isEmpty()) {
            PrimeFaces.current().ajax().update("sendForm:recipient");
        }
    }

    /**
     * Method for sending money between users, returns null if error occurs
     * @return null if error during sending money occurs
     * @return String to redirect to dashboard if successful
    */
    @Transactional
    public String sendMoney() {
        User sender = userBean.getCurrentUser();
        User recipient;

        try {
            if (amount < 0.01) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "The transfer amount must be greater than 0 CZK");
                return null;
            }

            if (sender.getBalance() < amount) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Insufficient funds", 
                    "Your balance is insufficient for this transfer");
                return null;
            }

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

            if (sender.getId().equals(recipient.getId())) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid recipient", 
                    "You cannot send money to yourself");
                return null;
            }

            double remainingBudget = budgetBean.getRemainingBudget(sender.getId());
            
            if (sender.getBudget() != 0 && (remainingBudget < 0 || remainingBudget - amount < 0)) {
                FacesContext.getCurrentInstance().getViewRoot().getViewMap().put("pendingRecipient", recipient);
                FacesContext.getCurrentInstance().getViewRoot().getViewMap().put("pendingAmount", amount);
                PrimeFaces.current().executeScript("PF('budgetWarningDialog').show()");
                return null;
            }

            return processTransfer(sender, recipient, amount);

        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred during the transfer: " + e.getMessage());
            return null;
        }
    }

    /**
     * Processes the transfer after user confirms to proceed despite budget warning.
     * @return String to redirect to dashboard if successful
     * @return null if error during transfer processing occurs, so if it fails or some data are missing
    */
    @Transactional
    public String confirmTransfer() {
        try {
            User sender = userBean.getCurrentUser();
            User recipient = (User) FacesContext.getCurrentInstance()
                .getViewRoot().getViewMap().get("pendingRecipient");
            Double pendingAmount = (Double) FacesContext.getCurrentInstance()
                .getViewRoot().getViewMap().get("pendingAmount");
            
            if (recipient != null && pendingAmount != null) {
                return processTransfer(sender, recipient, pendingAmount);
            }
            return null;
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "Failed to process transfer: " + e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to process the actual transfer
     * @param sender - User sending the money
     * @param recipient - User receiving the money
     * @param amount - Amount to send
     * @return String to redirect to dashboard if successful
    */
    @Transactional
    private String processTransfer(User sender, User recipient, double amount) {
        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(recipient);
        transaction.setValue(amount);
        transaction.setType("Transfer");
        transaction.setCategory("User Transfer");
        transaction.setTransactionDate(LocalDateTime.now());

        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount);

        em.persist(transaction);
        em.merge(sender);
        em.merge(recipient);

        addMessage(FacesMessage.SEVERITY_INFO, "Success", 
            String.format("Successfully sent %.2f CZK to %s", amount, 
                recipient.getFirstName() + " " + recipient.getSecondName()));

        resetForm();

        return "dashboard?faces-redirect=true";
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
     * Method for resetting the form
     */
    private void resetForm() {
        this.recipientIdentifier = null;
        this.amount = 0.0;
    }

    /**
     * Method for getting the recipient identifier
     * @return recipientIdentifier
    */
    public String getRecipientIdentifier() {
        return recipientIdentifier;
    }

    /**
     * Method for setting the recipient identifier
     * @param recipientIdentifier
    */
    public void setRecipientIdentifier(String recipientIdentifier) {
        this.recipientIdentifier = recipientIdentifier;
    }

    /**
     * Method for getting the amount
     * @return amount
    */
    public double getAmount() {
        return amount;
    }

    /**
     * Method for setting the amount
     * @param amount
    */

    public void setAmount(double amount) {
        this.amount = amount;
    }
}