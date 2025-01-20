package beans;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.primefaces.PrimeFaces;

import beans.entities.Friends;
import beans.entities.RequestMoney;
import beans.entities.Transaction;
import beans.entities.User;
import beans.services.EmailService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
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
    
    @Inject
    private BudgetBean budgetBean;
    
    // Form fields for money request
    private String recipientIdentifier; // Can be email or variable symbol
    private double amount;
    private String description;
    
    /** Map to store email-to-name mappings for friends display */
    private final Map<String, String> friendEmailToNameMap = new HashMap<>();
    
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
            // Check if identifier contains only numbers
            boolean isNumeric = identifier.chars().allMatch(Character::isDigit);
            
            if (isNumeric) {
                // Search by variable symbol for numeric-only identifier
                return em.createQuery("SELECT u FROM User u WHERE u.variableSymbol = :identifier", User.class)
                         .setParameter("identifier", identifier)
                         .getResultList()
                         .stream()
                         .findFirst()
                         .orElse(null);
            } else {
                // Search by email for alphanumeric identifier
                return em.createQuery("SELECT u FROM User u WHERE u.email = :identifier", User.class)
                         .setParameter("identifier", identifier)
                         .getResultList()
                         .stream()
                         .findFirst()
                         .orElse(null);
            }
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
                addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid request");
                return;
            }
            
            // Check sufficient balance
            if (receiver.getBalance() < managedRequest.getValue()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Insufficient funds", 
                    "You don't have enough balance to accept this request");
                return;
            }

            // Check if accepting the request would exceed budget limits
            // Check budget status
            double remainingBudget = budgetBean.getRemainingBudget(receiver.getId());
            double requestAmount = managedRequest.getValue();
            
            // If budget has been setted and already exceeded or would be exceeded by this request, show warning
            if (receiver.getBudget()>0 && (remainingBudget < 0 || (remainingBudget - requestAmount) < 0)) {
                // Store the request in view scope to access it later if user confirms
                FacesContext.getCurrentInstance().getViewRoot().getViewMap().put("pendingRequest", managedRequest);
                // Display the warning dialog using PrimeFaces
                PrimeFaces.current().executeScript("PF('budgetWarningDialog').show()");
                return;
            }

            // If budget check passes, process the request normally
            processAcceptedRequest(managedRequest);
            
        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            // Log error details in message
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to process request: " + e.getMessage());
        }
    }
    
    /**
     * Processes the request after user confirms to proceed despite budget warning.
     * This method is called when user clicks "Proceed" in the budget warning dialog.
     * It retrieves the stored request from view scope and processes it even though
     * it exceeds the budget.
     */
    @Transactional
    public void confirmAcceptRequest() {
        try {
            // Retrieve the pending request from view scope that was stored when showing warning
            RequestMoney managedRequest = (RequestMoney) FacesContext.getCurrentInstance()
                .getViewRoot().getViewMap().get("pendingRequest");
                
            if (managedRequest != null) {
                // Refetch the request from database to ensure we have fresh data
                managedRequest = em.find(RequestMoney.class, managedRequest.getId());
                // Verify request still exists
                if (managedRequest == null) {
                    addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Request no longer exists");
                    return;
                }
                processAcceptedRequest(managedRequest);
            }
        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to process request: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to process the accepted request.
     * This method handles the actual processing of money requests, including:
     * - Creating transaction records
     * - Updating user balances
     * - Removing the processed request
     * 
     * @param managedRequest The request to process, must be a managed entity
     * @throws IOException If redirect after processing fails
     */
    @Transactional
    private void processAcceptedRequest(RequestMoney managedRequest) throws IOException {
        User receiver = userBean.getCurrentUser();
        // Ensure entities are managed by the persistence context
        receiver = em.merge(receiver);
        User sender = em.merge(managedRequest.getSender());
        
        // Set up the transaction record
        Transaction transaction = new Transaction();
        transaction.setSender(receiver);
        transaction.setReceiver(sender);
        transaction.setValue(managedRequest.getValue());
        transaction.setType("Transfer");
        transaction.setCategory("Money Request");
        transaction.setTransactionDate(LocalDateTime.now());
        
        // Update the balances of both users
        receiver.setBalance(receiver.getBalance() - managedRequest.getValue());
        sender.setBalance(sender.getBalance() + managedRequest.getValue());
        
        // Persist changes to database
        em.persist(transaction);
        em.remove(managedRequest);
        // Force immediate write to database
        em.flush();
        
        addMessage(FacesMessage.SEVERITY_INFO, "Success", "Request accepted and payment sent");
        FacesContext.getCurrentInstance().getExternalContext().redirect("dashboard.xhtml");
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
    
    /**
     * Gets a list of friends' names and emails for the current user.
     * This combines friends where the current user is either user1 or user2.
     * This method creates SelectItem objects that show the friend's full name
     * in the dropdown while storing their email as the actual value.
     *
     * @return List of SelectItem with friend's name as label and email as value
     */
    public List<SelectItem> getFriendsList() {
        User currentUser = userBean.getCurrentUser();
        List<SelectItem> friendItems = new ArrayList<>();
        // Clear the map before repopulating
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
                // Create display name by combining first and second name
                String displayName = friend.getFirstName() + " " + friend.getSecondName();
                // Store mapping for autocomplete feature
                friendEmailToNameMap.put(friend.getEmail(), displayName);
                // Create SelectItem with display name as label and email as value
                friendItems.add(new SelectItem(friend.getEmail(), displayName));
            }
            
            // Same process for reverse friendships
            for (Friends friendship : friendsAsUser2) {
                User friend = friendship.getUser1();
                String displayName = friend.getFirstName()+" "+friend.getSecondName();
                friendEmailToNameMap.put(friend.getEmail(), displayName);
                friendItems.add(new SelectItem(friend.getEmail(), displayName));
            }
            
            return friendItems;
        } catch (Exception e) {
            return new ArrayList<>(); // Return empty list if error occurs
        }
    }
    
    /**
     * Automatically insert the email when a friend is selected from the dropdown.
     * 
     * @param event The selection event
     */
    public void onFriendSelect() {
        // Update UI components
        if (recipientIdentifier != null && !recipientIdentifier.isEmpty()) {
            PrimeFaces.current().ajax().update("requestForm:recipient");
        }
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
    
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        addGrowlMessage(severity, summary, detail);
    }
} 