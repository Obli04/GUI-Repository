package beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
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
                return userBudget - calculateTotalSpent();
            }
        }
        return 0.0;
    }

    @Transactional
    private double calculateTotalSpent() {
        try {
            User currentUser = userBean.getCurrentUser();
            if (currentUser == null) return 0.0;

            // Get all transactions for withdrawal sending money
            TypedQuery<Object[]> query = em.createQuery(
                "SELECT t.category, SUM(t.value) FROM Transaction t " +
                "WHERE t.sender.id = :userId " +
                "AND t.type IN ('Withdraw', 'Send') " +
                "GROUP BY t.category", Object[].class);
            query.setParameter("userId", currentUser.getId());
            
            // Calculate total spent
            List<Object[]> results = query.getResultList();
            double totalSpent = 0.0;
            
            // Update budget spent for each category
            for (Object[] result : results) {
                String category = (String) result[0];
                Double value = ((Number) result[1]).doubleValue();
                totalSpent += value;
                
                // Update budget_spent in the Budget entity
                TypedQuery<Budget> budgetQuery = em.createQuery(
                    "SELECT b FROM Budget b WHERE b.user = :user AND b.budgetCategory = :category",
                    Budget.class);
                budgetQuery.setParameter("user", currentUser);
                budgetQuery.setParameter("category", category);
                
                try {
                    Budget budget = budgetQuery.getSingleResult();
                    budget.setBudgetSpent(value);
                    em.merge(budget);
                } catch (Exception e) {
                    // No budget set for this category, can skip
                }
            }
            
            return totalSpent;
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
    
    public Double getCategorySpent(String category) {
        User currentUser = userBean.getCurrentUser();
        if (currentUser == null) return 0.0;
        
        TypedQuery<Budget> query = em.createQuery(
            "SELECT b FROM Budget b WHERE b.user = :user AND b.budgetCategory = :category",
            Budget.class);
        query.setParameter("user", currentUser);
        query.setParameter("category", category);
        
        try {
            Budget budget = query.getSingleResult();
            return budget.getBudgetSpent();
        } catch (Exception e) {
            return 0.0;
        }
    }
}