package beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import beans.entities.Budget;
import beans.entities.User;
import jakarta.annotation.PostConstruct;
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
    
    @Transactional
    public String setCategoryBudget() {
        User currentUser = userBean.getCurrentUser();
        try {
            if (categoryAmount <= 0) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "Please enter a positive amount for the category budget");
                return null;
            }
            
            // Find existing budget for this category
            TypedQuery<Budget> query = em.createQuery(
                "SELECT b FROM Budget b WHERE b.user = :user AND b.budgetCategory = :category",
                Budget.class);
            query.setParameter("user", currentUser);
            query.setParameter("category", selectedCategory);
            
            Budget categoryBudget;
            try {
                categoryBudget = query.getSingleResult();
            } catch (Exception e) {
                // Create new budget if none exists
                categoryBudget = new Budget();
                categoryBudget.setBudgetCategory(selectedCategory);
                categoryBudget.setBudgetSpent(0.0);
                categoryBudget.setUser(currentUser);
            }
            
            // Update or set the budget
            categoryBudget.setBudget(categoryAmount);
            
            // Persist changes
            if (categoryBudget.getId() == null) {
                em.persist(categoryBudget);
            } else {
                em.merge(categoryBudget);
            }
            
            // Update the local map for display
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
    }
    
    public double getRemainingBudget() {
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            Double userBudget = currentUser.getBudget();
            if (userBudget != 0) {
                return userBudget - getTotalSpent();
            }
        }
        return 0.0;
    }

    @Transactional
    public double getTotalSpent() {
        try {
            User currentUser = userBean.getCurrentUser();
            if (currentUser == null) return 0.0;

            // Get total spent amount for withdrawals and sending money
            TypedQuery<Double> query = em.createQuery(
                "SELECT COALESCE(SUM(t.value), 0) FROM Transaction t " +
                "WHERE t.sender.id = :userId " +
                "AND (t.type = 'Withdraw' OR t.category = 'User Transfer')", Double.class);
            query.setParameter("userId", currentUser.getId());
            
            return query.getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
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
    
    @PostConstruct
    public void init() {
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            TypedQuery<Budget> query = em.createQuery(
                "SELECT b FROM Budget b WHERE b.user = :user", Budget.class);
            query.setParameter("user", currentUser);
            
            query.getResultList().forEach(budget -> 
                categoryBudgets.put(budget.getBudgetCategory(), budget.getBudget()));
        }
    }
    
    @Transactional
    public double getCategorySpent(String category) {
        User currentUser = userBean.getCurrentUser();
        if (currentUser == null) return 0.0;
        
        try {
            // Get spent amount for specific category
            TypedQuery<Double> query = em.createQuery(
                "SELECT COALESCE(SUM(t.value), 0) FROM Transaction t " +
                "WHERE t.sender.id = :userId " +
                "AND (t.type = 'WITHDRAW' OR t.type = 'SEND') " +
                "AND t.category = :category", Double.class);
            query.setParameter("userId", currentUser.getId());
            query.setParameter("category", category);
            
            double spentAmount = query.getSingleResult();
            
            // Update the Budget entity with the latest spent amount
            TypedQuery<Budget> budgetQuery = em.createQuery(
                "SELECT b FROM Budget b WHERE b.user = :user AND b.budgetCategory = :category",
                Budget.class);
            budgetQuery.setParameter("user", currentUser);
            budgetQuery.setParameter("category", category);
            
            Budget budget;
            try {
                budget = budgetQuery.getSingleResult();
            } catch (Exception e) {
                budget = new Budget();
                budget.setUser(currentUser);
                budget.setBudgetCategory(category);
                budget.setBudget(0.0);
            }
            
            budget.setBudgetSpent(spentAmount);
            
            if (budget.getId() == null) {
                em.persist(budget);
            } else {
                em.merge(budget);
            }
            
            return spentAmount;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }
}