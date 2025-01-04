package beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import beans.entities.User;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

@Named
@SessionScoped
public class BudgetBean implements Serializable {
    
    @PersistenceContext
    private EntityManager em;
    
    @Inject
    private UserBean userBean;
    
    private double totalBudget;
    private Map<String, Double> categoryBudgets = new HashMap<>();
    private String selectedCategory;
    private double categoryAmount;
    
    private static final String[] CATEGORIES = {
        "Food", "Transportation", "Entertainment", "Shopping", 
        "Bills", "Healthcare", "Education", "Other"
    };
    
    @Transactional
    public String setBudget() {
        User currentUser = userBean.getCurrentUser();
        
        try {
            if (totalBudget <= 0) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "Please enter a positive amount for the budget");
                return null;
            }
            
            currentUser.setBudget(totalBudget);
            em.merge(currentUser);
            
            addMessage(FacesMessage.SEVERITY_INFO, "Success", 
                "Budget set successfully to " + totalBudget + " Kč");
            
            return null;
            
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred while setting the budget: " + e.getMessage());
            return null;
        }
    }
    
    /*@Transactional
    public String setCategoryBudget() {
        try {
            if (categoryAmount <= 0) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "Please enter a positive amount for the category budget");
                return null;
            }
            
            double totalCategoryBudgets = categoryBudgets.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum() + categoryAmount;
                
            if (totalCategoryBudgets > totalBudget) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Budget exceeded", 
                    "Total category budgets cannot exceed total budget");
                return null;
            }
            
            categoryBudgets.put(selectedCategory, categoryAmount);
            
            addMessage(FacesMessage.SEVERITY_INFO, "Success", 
                "Budget for " + selectedCategory + " set to " + categoryAmount + " Kč");
            
            // Reset fields
            selectedCategory = null;
            categoryAmount = 0;
            
            return null;
            
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred while setting the category budget: " + e.getMessage());
            return null;
        }
    }*/
    
    public double getRemainingBudget() {
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            Double userBudget = currentUser.getBudget();
            if (userBudget != 0) {
                return userBudget - calculateTotalSpent();
            }
        }
        return 0.0;
    }
    
    private double calculateTotalSpent() {
        User currentUser = userBean.getCurrentUser();
        if (currentUser == null) return 0.0;

        TypedQuery<Double> query = em.createQuery(
            "SELECT COALESCE(SUM(t.value), 0) FROM Transaction t " +
            "WHERE t.sender.id = :userId " +
            "AND t.type IN ('Withdraw', 'Send')", Double.class);
        query.setParameter("userId", currentUser.getId());
        
        Double totalSpent = query.getSingleResult();
        return totalSpent != null ? totalSpent : 0.0;
    }
    
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
    }
    
    // Getters and setters
    public double getTotalBudget() {
        return totalBudget;
    }
    
    public void setTotalBudget(double totalBudget) {
        this.totalBudget = totalBudget;
    }
    
    public String getSelectedCategory() {
        return selectedCategory;
    }
    
    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
    }
    
    public double getCategoryAmount() {
        return categoryAmount;
    }
    
    public void setCategoryAmount(double categoryAmount) {
        this.categoryAmount = categoryAmount;
    }
    
    public String[] getCategories() {
        return CATEGORIES;
    }
    
    public Map<String, Double> getCategoryBudgets() {
        return categoryBudgets;
    }
}