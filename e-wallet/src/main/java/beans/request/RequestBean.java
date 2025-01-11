package beans.request;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import beans.UserBean;
import beans.entities.RequestMoney;
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
public class RequestBean implements Serializable {
    
    @PersistenceContext
    private EntityManager em;
    
    @Inject
    private UserBean userBean;
    
    private String recipientIdentifier; // Can be email or variable symbol
    private double amount;
    private String description;
    
    @Transactional
    public String sendRequest() {
        try {
            // Get current user
            User currentUser = userBean.getCurrentUser();
            
            // Check if user is trying to request from their own email or variable symbol
            if (recipientIdentifier.equals(currentUser.getEmail()) || 
                recipientIdentifier.equals(currentUser.getVariableSymbol())) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Invalid recipient", 
                    "You cannot request money from yourself");
                return null;
            }
            
            // Validate amount
            if (amount <= 0) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "Please enter a positive amount");
                return null;
            }
            
            // Find recipient user
            User recipient = findUserByIdentifier(recipientIdentifier);
            if (recipient == null) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "User not found", 
                    "No user found with the provided email or variable symbol");
                return null;
            }
            
            // Double check if recipient is not the current user (in case of case-insensitive email match)
            if (recipient.getId().equals(currentUser.getId())) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Invalid recipient", 
                    "You cannot request money from yourself");
                return null;
            }
            
            // Create new money request
            RequestMoney request = new RequestMoney();
            request.setSender(currentUser);
            request.setReceiver(recipient);
            request.setValue(amount);
            request.setDescription(description);
            
            // Save to database
            em.persist(request);
            
            addGrowlMessage(FacesMessage.SEVERITY_INFO, "Success", 
                "Money request sent successfully");
            
            // Reset form
            resetForm();
            
            return "dashboard?faces-redirect=true";
            
        } catch (Exception e) {
            addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred while processing your request: " + e.getMessage());
            return null;
        }
    }
    
    private User findUserByIdentifier(String identifier) {
        try {
            // First try to find by email
            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :identifier", User.class)
                         .setParameter("identifier", identifier)
                         .getResultList()
                         .stream()
                         .findFirst()
                         .orElse(null);
            
            if (user == null) {
                // If not found by email, try variable symbol
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
    
    private void resetForm() {
        recipientIdentifier = null;
        amount = 0;
        description = null;
    }
    
    private void addGrowlMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage("growl", new FacesMessage(severity, summary, detail));
    }
    
    @Transactional
    public List<RequestMoney> getReceivedRequests() {
        User currentUser = userBean.getCurrentUser();
        return em.createQuery(
            "SELECT r FROM RequestMoney r WHERE r.receiver.id = :userId ORDER BY r.id DESC", 
            RequestMoney.class)
            .setParameter("userId", currentUser.getId())
            .getResultList();
    }
    
    @Transactional
    public List<RequestMoney> getSentRequests() {
        User currentUser = userBean.getCurrentUser();
        return em.createQuery(
            "SELECT r FROM RequestMoney r WHERE r.sender.id = :userId ORDER BY r.id DESC", 
            RequestMoney.class)
            .setParameter("userId", currentUser.getId())
            .getResultList();
    }
    
    @Transactional
    public void deleteRequest(RequestMoney request) {
        try {
            RequestMoney managedRequest = em.find(RequestMoney.class, request.getId());
            if (managedRequest != null && managedRequest.getSender().getId().equals(userBean.getCurrentUser().getId())) {
                em.remove(managedRequest);
                addGrowlMessage(FacesMessage.SEVERITY_INFO, "Success", "Request deleted successfully");
            }
        } catch (Exception e) {
            addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to delete request");
        }
    }
    
    @Transactional
    public void acceptRequest(RequestMoney request) {
        try {
            User receiver = userBean.getCurrentUser();
            RequestMoney managedRequest = em.find(RequestMoney.class, request.getId());
            
            if (managedRequest == null || !managedRequest.getReceiver().getId().equals(receiver.getId())) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid request");
                return;
            }
            
            // Check if receiver has sufficient balance
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
            
            // Update balances
            receiver.setBalance(receiver.getBalance() - managedRequest.getValue());
            managedRequest.getSender().setBalance(
                managedRequest.getSender().getBalance() + managedRequest.getValue()
            );
            
            // Persist changes
            em.persist(transaction);
            em.merge(receiver);
            em.merge(managedRequest.getSender());
            em.remove(managedRequest);
            
            addGrowlMessage(FacesMessage.SEVERITY_INFO, "Success", "Request accepted and payment sent");
            FacesContext.getCurrentInstance().getExternalContext().redirect("dashboard.xhtml");
            
        } catch (Exception e) {
            addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to process request");
        }
    }
    
    @Transactional
    public void declineRequest(RequestMoney request) {
        try {
            User receiver = userBean.getCurrentUser();
            RequestMoney managedRequest = em.find(RequestMoney.class, request.getId());
            
            if (managedRequest == null || !managedRequest.getReceiver().getId().equals(receiver.getId())) {
                addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid request");
                return;
            }
            
            // Create declined transaction record
            Transaction transaction = new Transaction();
            transaction.setSender(managedRequest.getSender());
            transaction.setReceiver(receiver);
            transaction.setValue(managedRequest.getValue());
            transaction.setType("Declined");
            transaction.setCategory("Money Request");
            transaction.setTransactionDate(LocalDateTime.now());
            
            // Persist transaction and remove request
            em.persist(transaction);
            em.remove(managedRequest);
            
            addGrowlMessage(FacesMessage.SEVERITY_INFO, "Success", "Request declined");
            FacesContext.getCurrentInstance().getExternalContext().redirect("dashboard.xhtml");
            
        } catch (Exception e) {
            addGrowlMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to decline request");
        }
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
} 