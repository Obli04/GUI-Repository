package beans.request;

import java.io.Serializable;

import beans.UserBean;
import beans.entities.RequestMoney;
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
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid recipient", 
                    "You cannot request money from yourself");
                return null;
            }
            
            // Validate amount
            if (amount <= 0) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "Please enter a positive amount");
                return null;
            }
            
            // Find recipient user
            User recipient = findUserByIdentifier(recipientIdentifier);
            if (recipient == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "User not found", 
                    "No user found with the provided email or variable symbol");
                return null;
            }
            
            // Double check if recipient is not the current user (in case of case-insensitive email match)
            if (recipient.getId().equals(currentUser.getId())) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid recipient", 
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
            
            addMessage(FacesMessage.SEVERITY_INFO, "Success", 
                "Money request sent successfully");
            
            // Reset form
            resetForm();
            
            return "dashboard?faces-redirect=true";
            
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
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
    
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
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