package beans;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import beans.entities.RequestMoney;
import beans.entities.Transaction;
import beans.entities.User;
import beans.services.EmailService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Manages money request operations
 * This bean handles the creation, acceptance, decline, and deletion of money requests between users.
 *
 * @author Danilo Spera
 */
@Named
@SessionScoped
public class RequestBean implements Serializable {
    
    @PersistenceContext
    private EntityManager em;
    
    @Inject
    private UserBean userBean;

    @Inject
    private EmailService emailService;
    
    // Form fields for money request
    private String recipientIdentifier; // Can be email or variable symbol
    private double amount;
    private String description;
    
    /**
     * Processes a new money request from the current user to another user.
     * Validates the request details and creates a new RequestMoney entity if valid.
     *
     * @return null if validation fails
     */
    @Transactional
    public String sendRequest() {
        try {
            // Get current user who is sending the request
            User currentUser = userBean.getCurrentUser();
            
            // Prevent self-requests by checking both email and variable symbol
            if (recipientIdentifier.equals(currentUser.getEmail()) || 
                recipientIdentifier.equals(currentUser.getVariableSymbol())) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Invalid recipient", 
                    "You cannot request money from yourself");
                return null;
            }
            
            // Amount validation
            if (amount == 0) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "Amount cannot be zero");
                return null;
            }
            if (amount < 0) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "Please enter a positive amount");
                return null;
            }
            
            // Lookup recipient user in database
            User recipient = findUserByIdentifier(recipientIdentifier);
            if (recipient == null) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "User not found", 
                    "No user found with the provided email or variable symbol");
                return null;
            }
            
            // Double check for self-request (case-insensitive email match)
            if (recipient.getId().equals(currentUser.getId())) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Invalid recipient", 
                    "You cannot request money from yourself");
                return null;
            }
            
            // Create and persist the money request
            RequestMoney request = new RequestMoney();
            request.setSender(currentUser);
            request.setReceiver(recipient);
            request.setValue(amount);
            request.setDescription(description);

            emailService.sendEmailForMoneyRequest(recipient.getEmail(), amount, currentUser);
            em.persist(request);
            
            // Show success message and reset form
            addGrowlMessage(FacesMessage.SEVERITY_INFO, "Success", 
                "Money request sent successfully");
            resetForm();
            
            return null;
            
        } catch (Exception e) {
            addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred while processing your request: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Looks up a user by either email or variable symbol.
     * Tries email first, then falls back to variable symbol if no user is found.
     *
     * @param identifier The email or variable symbol to search for
     * @return User if found, null otherwise
     */
    private User findUserByIdentifier(String identifier) {
        try {
            // First attempt: find by email
            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :identifier", User.class)
                         .setParameter("identifier", identifier)
                         .getResultList()
                         .stream()
                         .findFirst()
                         .orElse(null);
            
            // Second attempt: find by variable symbol if email search failed
            if (user == null) {
                user = em.createQuery("SELECT u FROM User u WHERE u.variableSymbol = :identifier", User.class)
                         .setParameter("identifier", identifier)
                         .getResultList()
                         .stream()
                         .findFirst()
                         .orElse(null);
            }
            
            return user;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Resets the form fields after successful submission
     */
    private void resetForm() {
        recipientIdentifier = null;
        amount = 0;
        description = null;
    }
    
    /**
     * Helper method to add messages to the Growl component
     */
    private void addGrowlMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage("growl", new FacesMessage(severity, summary, detail));
    }
    
    /**
     * Retrieves all money requests received by the current user
     *
     * @return List of received RequestMoney entities
     */
    @Transactional
    public List<RequestMoney> getReceivedRequests() {
        User currentUser = userBean.getCurrentUser();
        return em.createQuery(
            "SELECT r FROM RequestMoney r WHERE r.receiver.id = :userId ORDER BY r.id DESC", 
            RequestMoney.class)
            .setParameter("userId", currentUser.getId())
            .getResultList();
    }
    
    /**
     * Retrieves all money requests sent by the current user
     *
     * @return List of sent RequestMoney entities
     */
    @Transactional
    public List<RequestMoney> getSentRequests() {
        User currentUser = userBean.getCurrentUser();
        return em.createQuery(
            "SELECT r FROM RequestMoney r WHERE r.sender.id = :userId ORDER BY r.id DESC", 
            RequestMoney.class)
            .setParameter("userId", currentUser.getId())
            .getResultList();
    }
    
    /**
     * Deletes a money request. Only the sender can delete their own requests.
     *
     * @param request The RequestMoney entity to delete
     */
    @Transactional
    public void deleteRequest(RequestMoney request) {
        try {
            // Find the managed instance of the request
            RequestMoney managedRequest = em.find(RequestMoney.class, request.getId());
            // Only allow deletion if the current user is the sender
            if (managedRequest != null && managedRequest.getSender().getId().equals(userBean.getCurrentUser().getId())) {
                em.remove(managedRequest);
            }
        } catch (Exception e) {
            addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to delete request");
        }
    }
    
    /**
     * Accepts a money request and processes the payment.
     * Creates a transaction record and updates user balances.
     *
     * @param request The RequestMoney entity to accept
     */
    @Transactional
    public void acceptRequest(RequestMoney request) {
        try {
            User receiver = userBean.getCurrentUser();
            RequestMoney managedRequest = em.find(RequestMoney.class, request.getId());
            
            // Validate request ownership
            if (managedRequest == null || !managedRequest.getReceiver().getId().equals(receiver.getId())) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid request");
                return;
            }
            
            // Check sufficient balance
            if (receiver.getBalance() < managedRequest.getValue()) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Insufficient funds", 
                    "You don't have enough balance to accept this request");
                return;
            }
            
            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setSender(receiver);
            transaction.setReceiver(managedRequest.getSender());
            transaction.setValue(managedRequest.getValue());
            transaction.setType("Transfer");
            transaction.setCategory("Money Request");
            transaction.setTransactionDate(LocalDateTime.now());
            
            // Update balances for both users
            receiver.setBalance(receiver.getBalance() - managedRequest.getValue());
            managedRequest.getSender().setBalance(
                managedRequest.getSender().getBalance() + managedRequest.getValue()
            );
            
            // Persist all changes
            em.persist(transaction);
            em.merge(receiver);
            em.merge(managedRequest.getSender());
            em.remove(managedRequest);
            
            addGrowlMessage(FacesMessage.SEVERITY_INFO, "Success", "Request accepted and payment sent");
            FacesContext.getCurrentInstance().getExternalContext().redirect("dashboard.xhtml");
            
        } catch (IOException e) {
            addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to process request");
        }
    }
    
    /**
     * Declines a money request and creates a declined transaction record.
     *
     * @param request The RequestMoney entity to decline
     */
    @Transactional
    public void declineRequest(RequestMoney request) {
        try {
            User receiver = userBean.getCurrentUser();
            RequestMoney managedRequest = em.find(RequestMoney.class, request.getId());
            
            // Validate request ownership
            if (managedRequest == null || !managedRequest.getReceiver().getId().equals(receiver.getId())) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid request");
                return;
            }
            
            // Create declined transaction record for tracking
            Transaction transaction = new Transaction();
            transaction.setSender(managedRequest.getSender());
            transaction.setReceiver(receiver);
            transaction.setValue(managedRequest.getValue());
            transaction.setType("Declined");
            transaction.setCategory("Money Request");
            transaction.setTransactionDate(LocalDateTime.now());
            
            // Persist changes
            em.persist(transaction);
            em.remove(managedRequest);
            
            addGrowlMessage(FacesMessage.SEVERITY_INFO, "Success", "Request declined");
            FacesContext.getCurrentInstance().getExternalContext().redirect("dashboard.xhtml");
            
        } catch (IOException e) {
            addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to decline request");
        }
    }
    
    /**
     * Retrieves all money requests (both sent and received) for the current user
     *
     * @return List of all RequestMoney entities related to the current user
     */
    @Transactional
    public List<RequestMoney> getAllRequests() {
        User currentUser = userBean.getCurrentUser();
        return em.createQuery(
            "SELECT r FROM RequestMoney r WHERE r.receiver.id = :userId OR r.sender.id = :userId ORDER BY r.id DESC", 
            RequestMoney.class)
            .setParameter("userId", currentUser.getId())
            .getResultList();
    }
    
    // Getters and setters
    public String getRecipientIdentifier() { return recipientIdentifier; }
    public void setRecipientIdentifier(String recipientIdentifier) { 
        this.recipientIdentifier = recipientIdentifier; 
    }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
} 